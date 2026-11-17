"""Read-only Elasticsearch/OpenSearch log search for cross-service investigations.

Only a server-configured endpoint, index pattern and tenant filter are queried.
This adapter never accepts raw Elasticsearch DSL or a user-provided URL.
"""
import hashlib
import os
import re
from urllib.parse import urlsplit

import httpx

from .config import get_settings
from .safety import redact_sensitive


def search_logs(tenant: str, query: str, window_minutes: int = 60) -> dict:
    url = os.getenv("INVESTINATOR_ELASTICSEARCH_URL", "").strip().rstrip("/")
    index = os.getenv("INVESTINATOR_ELASTICSEARCH_INDEX", "logs-*").strip()
    tenant_field = os.getenv("INVESTINATOR_ELASTICSEARCH_TENANT_FIELD", "tenant_id").strip()
    if not url:
        return {"status": "unavailable", "reason": "Log search backend not configured", "items": []}
    parsed = urlsplit(url)
    if (
        parsed.scheme not in {"http", "https"}
        or parsed.username or parsed.password or parsed.query or parsed.fragment
        or not re.fullmatch(r"[a-zA-Z0-9_,.*+-]+", index)
        or not re.fullmatch(r"[a-zA-Z_][a-zA-Z0-9_.-]*", tenant_field)
    ):
        return {"status": "unavailable", "reason": "Invalid log backend configuration", "items": []}
    local_hosts = {"localhost", "127.0.0.1", "elasticsearch", "opensearch"}
    if parsed.scheme != "https" and not (get_settings().demo_mode and parsed.hostname in local_hosts):
        return {"status": "unavailable", "reason": "HTTPS required outside the local demo", "items": []}
    safe_window = max(1, min(int(window_minutes), 240))
    payload = {
        "size": 10,
        "sort": [{"@timestamp": {"order": "desc", "unmapped_type": "date"}}],
        "_source": ["@timestamp", "message", "log.original", "log.level", "service.name",
                    "host.name", "error.message", "event.dataset", "source_mode", "environment"],
        "query": {"bool": {"filter": [
            {"term": {tenant_field: tenant}},
            {"range": {"@timestamp": {"gte": f"now-{safe_window}m", "lte": "now"}}},
        ], "must": [{"multi_match": {
            "query": query[:200], "fields": ["message", "log.original", "error.message", "service.name"]
        }}]}},
    }
    headers = {"Accept": "application/json"}
    api_key = os.getenv("INVESTINATOR_ELASTICSEARCH_API_KEY", "").strip()
    if api_key:
        headers["Authorization"] = f"ApiKey {api_key}"
    try:
        result = httpx.post(
            f"{url}/{index}/_search", json=payload, headers=headers, timeout=6.0
        )
        result.raise_for_status()
        hits = result.json().get("hits", {}).get("hits", [])
        items = []
        for hit in hits[:10]:
            source = hit.get("_source") or {}
            raw_message = source.get("message") or (source.get("error") or {}).get("message")
            if not raw_message:
                raw_message = (source.get("log") or {}).get("original") or ""
            stable_key = f"{hit.get('_index', '')}:{hit.get('_id', '')}"
            items.append({
                "id": "ES-" + hashlib.sha256(stable_key.encode()).hexdigest()[:12],
                "source": "elasticsearch",
                "index": hit.get("_index"),
                "observed_at": source.get("@timestamp"),
                "service": (source.get("service") or {}).get("name"),
                "message": redact_sensitive(str(raw_message)[:900]),
                "mode": str(source.get("source_mode") or "external"),
                "retrieval_mode": "live",
                "environment": source.get("environment"),
            })
        return {"status": "ok", "source": "elasticsearch", "items": items, "retrieval_mode": "live"}
    except (httpx.HTTPError, ValueError, TypeError):
        return {"status": "unavailable", "reason": "Log backend request failed", "items": []}
