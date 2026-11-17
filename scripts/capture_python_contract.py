"""Capture deterministic Python API behavior in a disposable SQLite database; no paid calls."""
import json
import os
import tempfile
from pathlib import Path

from fastapi.testclient import TestClient


def capture() -> list[dict]:
    steps = []
    with tempfile.TemporaryDirectory(prefix="investinator-contract-") as directory:
        os.environ["DATABASE_URL"] = f"sqlite:///{directory}/reference.db"
        os.environ["INVESTINATOR_DEMO_MODE"] = "true"
        for key in ("OPENAI_API_KEY", "INVESTINATOR_OPENAI_MODEL", "KAFKA_BOOTSTRAP_SERVERS", "OTEL_EXPORTER_OTLP_ENDPOINT"):
            os.environ.pop(key, None)
        from incident_copilot.config import get_settings
        from incident_copilot.db import engine

        get_settings.cache_clear()
        engine.cache_clear()
        from incident_copilot.api import app

        with TestClient(app) as client:
            incident = ""
            conversation = ""

            def request(method, path, body=None):
                nonlocal incident, conversation
                actual = path.replace("{incident}", incident).replace("{conversation}", conversation)
                response = client.request(method, actual, **({"json": body} if body is not None else {}))
                data = response.text if response.headers.get("content-type", "").startswith("text/plain") else response.json()
                steps.append({"method": method, "path": path, "body": body, "status": response.status_code, "response": data})
                if response.status_code == 200 and isinstance(data, dict):
                    if data.get("incident_id"):
                        incident = data["incident_id"]
                    if data.get("conversation_id"):
                        conversation = data["conversation_id"]
                return data

            read_paths = ["/health", "/api/v1/health", "/api/v1/about", "/api/v1/platform/health", "/command-center",
                          "/api/v1/telemetry/metrics", "/pulse", "/api/v1/telemetry/logs", "/api/v1/telemetry/events", "/api/v1/telemetry/traces",
                          "/api/v1/topology", "/nerve-map", "/api/v1/topology/broker-02/blast-radius", "/api/v1/alerts",
                          "/api/v1/incidents", "/incidents", "/api/v1/knowledge", "/api/v1/runbooks", "/api/v1/replay/scenarios",
                          "/api/v1/replay/runs", "/api/v1/approvals", "/api/v1/audit", "/audit", "/api/v1/evaluations",
                          "/api/v1/ai/usage", "/ai/health", "/api/v1/integrations", "/api/v1/integrations/kafka/health", "/metrics"]
            for path in read_paths:
                request("GET", path)
            request("POST", "/api/v1/incidents/analyze", {"description": "Kafka lag high", "logs": "DiskErrorException broker-02", "tenant_id": "forged"})
            for path in ["/api/v1/incidents/{incident}", "/incidents/{incident}", "/api/v1/incidents/{incident}/timeline", "/api/v1/incidents/{incident}/evidence"]:
                request("GET", path)
            request("POST", "/incidents/{incident}/verify")
            request("POST", "/api/v1/incidents/{incident}/actions/ACT-004/execute")
            request("POST", "/api/v1/incidents/{incident}/investigate")
            request("POST", "/api/v1/investigator/messages", {"incident_id": incident, "message": "What changed?", "mode": "ask"})
            request("GET", "/api/v1/investigator/conversations/{conversation}")
            # Keep a clean reference state so the manual incident cannot affect replay grouping.
            for scenario in ("kafka_disk_pressure", "spark_executor_oom", "data_freshness_failure"):
                request("POST", "/api/v1/replay/runs", {"scenario": scenario})
                request("GET", "/api/v1/incidents/{incident}")
                request("POST", "/api/v1/incidents/{incident}/verify", {"checks": {"passed": True}})
                request("POST", "/api/v1/incidents/{incident}/actions/ACT-DEMO/execute")
                request("POST", "/incidents/{incident}/actions/ACT-DEMO/approval", {"decision": "approve", "decided_by": "client-actor", "reason": "reviewed"})
                request("POST", "/api/v1/incidents/{incident}/actions/ACT-DEMO/execute")
                request("GET", "/api/v1/incidents/{incident}/postmortem")
                request("GET", "/api/v1/platform/health")
            for path in ("/api/v1/alerts", "/api/v1/replay/runs", "/api/v1/approvals", "/api/v1/audit", "/api/v1/evaluations", "/api/v1/telemetry/events", "/api/v1/telemetry/logs"):
                request("GET", path)
            request("POST", "/api/v1/telemetry/metrics", {"entity_id": "broker-02", "name": "custom_metric", "value": 12})
            request("POST", "/incidents/analyze", {"description": "Something odd happened"})
            request("POST", "/api/v1/incidents/analyze", {"description": "HDFS unavailable", "logs": "safe mode", "metrics": {"blocks": 12}})
            request("POST", "/api/v1/incidents/analyze", {"description": "Spark executor lost", "logs": "OutOfMemoryError", "metrics": {"memory": 92.0}})
        engine().dispose()
    return steps


if __name__ == "__main__":
    output = Path("backend-java/src/test/resources/contracts/python-main.json")
    output.write_text(json.dumps(capture(), indent=2) + "\n")
    print(f"Captured disposable Python baseline: {output}")
