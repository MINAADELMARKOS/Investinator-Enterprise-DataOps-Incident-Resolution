"""Fixed-index bootstrap for the owned loopback demo only; never called by a model tool."""

import json
import urllib.error
import urllib.parse
from pathlib import Path


def bootstrap(module, base, admin_password, writer_password):
    if urllib.parse.urlsplit(base).hostname != "127.0.0.1":
        raise ValueError("Knowledge bootstrap is limited to owned local demo loopback")

    def admin(path, body=None, method=None):
        return module.request(base, path, body, "admin", admin_password, method=method)

    if admin("/")["version"]["number"] != "3.4.0":
        raise RuntimeError("Knowledge bootstrap requires verified OpenSearch 3.4.0")
    try:
        mapping = admin("/investinator-knowledge/_mapping")
        if (
            mapping["investinator-knowledge"]["mappings"]
            .get("_meta", {})
            .get("schema_version")
            != 1
        ):
            raise RuntimeError(
                "Existing knowledge schema requires a reviewed migration"
            )
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        source = Path(__file__).resolve().parents[1] / "infra/knowledge/index.json"
        admin("/investinator-knowledge", json.loads(source.read_text()), "PUT")
    admin(
        "/_plugins/_security/api/roles/knowledge_writer",
        {
            "cluster_permissions": [],
            "index_permissions": [
                {
                    "index_patterns": ["investinator-knowledge"],
                    "allowed_actions": ["indices:data/write/index*", "indices:data/write/bulk*"],
                }
            ],
        },
        "PUT",
    )
    admin(
        "/_plugins/_security/api/internalusers/knowledge_writer",
        {"password": writer_password},
        "PUT",
    )
    admin(
        "/_plugins/_security/api/rolesmapping/knowledge_writer",
        {"users": ["knowledge_writer"]},
        "PUT",
    )
    # Extend only this owned lab's existing reader, preserving its previous fixed index grants.
    admin(
        "/_plugins/_security/api/roles/evidence_reader",
        {
            "cluster_permissions": ["cluster_monitor"],
            "index_permissions": [
                {
                    "index_patterns": [
                        "investinator-evidence",
                        "investinator-traces",
                        "investinator-knowledge",
                    ],
                    "allowed_actions": [
                        "read",
                        "indices:admin/mappings/get",
                        "indices:admin/get",
                    ],
                }
            ],
        },
        "PUT",
    )
