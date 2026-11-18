#!/usr/bin/env python3
"""Explicit acceptance for this checkout's owned SIMULATED lab. Never a human gold review."""

import datetime
import importlib.util
import json
import urllib.error
import urllib.parse
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "cli", Path(__file__).resolve().parent / "investinator_cli.py"
)
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)


def main():
    state = cli.load_state()
    with cli.lock():
        base = cli.api(state)
        tokens = state["secrets"]
        marker = cli.RUNTIME / "knowledge-demo.json"
        if marker.exists():
            owned = json.loads(marker.read_text())
        else:
            incident = cli.http(
                base,
                "/api/v1/incidents/analyze",
                {
                    "description": "SIMULATED Kafka network/quorum acceptance",
                    "logs": "SIMULATED network packet loss and broker timeout",
                    "metrics": {},
                    "environment": "demo",
                },
            )
            now = datetime.datetime.now(datetime.timezone.utc)
            times = [
                (now - datetime.timedelta(seconds=s)).isoformat()
                for s in (180, 120, 90, 60)
            ]
            graph = {
                "graph_id": "lab-knowledge-kraft",
                "revision": 1,
                "source_mode": "SIMULATED",
                "metadata_mode": "KRAFT",
                "platform_version": "Kafka 4.3",
                "nodes": [
                    {"id": "network", "type": "NETWORK", "name": "Simulated network"},
                    {"id": "broker", "type": "BROKER", "name": "Simulated broker"},
                    {"id": "dataset", "type": "DATASET", "name": "Simulated dataset"},
                ],
                "edges": [
                    {
                        "id": "network-broker",
                        "from": "broker",
                        "to": "network",
                        "type": "DEPENDS_ON",
                        "source": "owned-lab-fixture",
                        "confidence": 1,
                        "discovered_at": times[0],
                        "valid_from": times[0],
                        "valid_to": None,
                        "provenance": "SIMULATED acceptance fixture",
                    },
                    {
                        "id": "broker-dataset",
                        "from": "dataset",
                        "to": "broker",
                        "type": "DEPENDS_ON",
                        "source": "owned-lab-fixture",
                        "confidence": 1,
                        "discovered_at": times[0],
                        "valid_from": times[0],
                        "valid_to": None,
                        "provenance": "SIMULATED acceptance fixture",
                    },
                ],
            }
            observations = [
                {
                    "id": "lab-knowledge-" + entity,
                    "tenant_id": "demo",
                    "incident_id": incident["incident_id"],
                    "entity_id": entity,
                    "signal": signal,
                    "state": "ABNORMAL",
                    "observed_at": time,
                    "source_mode": "SIMULATED",
                    "intensity": 0.8,
                    "signature": "SIMULATED",
                }
                for entity, signal, time in zip(
                    ("network", "broker", "dataset"),
                    ("NETWORK_UNSTABLE", "BROKER_ERROR", "DATASET_STALE"),
                    times[1:],
                    strict=True,
                )
            ]
            owned = {
                "incident_id": incident["incident_id"],
                "graph": graph,
                "observations": observations,
                "source_time": now.isoformat(),
            }
            marker.write_text(json.dumps(owned))
        cli.http(
            base,
            "/api/v1/dependencies/fixtures",
            {"graph": owned["graph"], "observations": owned["observations"]},
            token=tokens["GRAPH_ADMIN_TOKEN"],
        )
        path = (
            "/api/v1/dependencies/incidents/"
            + owned["incident_id"]
            + "/investigation?graph=lab-knowledge-kraft"
        )
        investigation = cli.http(base, path, token=tokens["INVESTIGATION_API_TOKEN"])
        assert (
            investigation["candidate_causes"][0]["entity_id"] == "network"
            and investigation["confirmed_cause"] is None
        )
        document = {
            "document_id": "lab-kraft-network",
            "revision": 1,
            "source_type": "RUNBOOK",
            "title": "Simulated Kafka KRaft network runbook",
            "technology": "Kafka",
            "platform_version": "4.3",
            "metadata_mode": "KRAFT",
            "source": "repo:runbooks/local-kraft-network.md",
            "owner": "simulated-operational-test",
            "source_time": owned["source_time"],
            "acl_principals": ["demo.senior_sre"],
            "content": "SIMULATED historical guidance: network packet loss can destabilize a Kafka KRaft metadata quorum. Read quorum, broker ISR and downstream freshness before proposing any change. Historical advice does not confirm this incident's cause.",
            "source_mode": "SIMULATED",
        }
        metadata = cli.http(
            base,
            "/api/v1/knowledge/revisions",
            document,
            tokens["KNOWLEDGE_WRITE_TOKEN"],
        )
        review_path = "/api/v1/knowledge/revisions/lab-kraft-network/1/review"
        try:
            cli.http(
                base,
                review_path,
                {
                    "decision": "APPROVE",
                    "reason": "A writer cannot approve its own corpus change",
                },
                tokens["KNOWLEDGE_WRITE_TOKEN"],
            )
        except urllib.error.HTTPError as error:
            assert error.code == 403
        else:
            raise AssertionError("Writer impersonated independent reviewer")
        publish_path = "/api/v1/knowledge/revisions/lab-kraft-network/1/publish"
        if metadata["review_state"] == "PENDING_REVIEW":
            try:
                cli.http(base, publish_path, {}, tokens["KNOWLEDGE_WRITE_TOKEN"])
            except urllib.error.HTTPError as error:
                assert error.code == 409
            else:
                raise AssertionError("Unreviewed knowledge was indexed")
            cli.http(
                base,
                review_path,
                {
                    "decision": "APPROVE",
                    "reason": "Explicit SIMULATED role-boundary acceptance; not independent human gold validation",
                },
                tokens["KNOWLEDGE_REVIEW_TOKEN"],
            )
        elif metadata["review_state"] != "APPROVED":
            raise RuntimeError(
                "Existing demo knowledge was revoked/rejected; preserve that review decision"
            )
        published = cli.http(base, publish_path, {}, tokens["KNOWLEDGE_WRITE_TOKEN"])
        assert published["indexed"]
        assert cli.http(base, publish_path, {}, tokens["KNOWLEDGE_WRITE_TOKEN"])[
            "indexed"
        ]
        query = urllib.parse.urlencode(
            {
                "q": "network packet loss metadata quorum",
                "incident_id": owned["incident_id"],
            }
        )
        results = cli.http(
            base,
            "/api/v1/knowledge/search?" + query,
            token=tokens["KNOWLEDGE_READ_TOKEN"],
        )
        assert (
            results["retrieval_mode"] == "LIVE"
            and results["items"][0]["document_id"] == document["document_id"]
        )
        assert results["items"][0]["evidence_kind"] == "HISTORICAL_ADVICE"
        readable = cli.http(
            base,
            "/api/v1/knowledge/documents/lab-kraft-network/1",
            token=tokens["KNOWLEDGE_READ_TOKEN"],
        )
        assert readable["metadata"]["content_hash"] == published["content_hash"]
        try:
            cli.http(
                base,
                "/api/v1/knowledge/search?" + query + "&tenant_id=other",
                token=tokens["KNOWLEDGE_READ_TOKEN"],
            )
        except urllib.error.HTTPError as error:
            assert error.code == 422
        else:
            raise AssertionError("Caller controlled tenant scope")
        if "feedback_id" not in owned:
            feedback = cli.http(
                base,
                "/api/v1/knowledge/feedback",
                {
                    "incident_id": owned["incident_id"],
                    "decision": "REJECTED",
                    "claimed_cause": "SIMULATED rejected claim: ZooKeeper caused a KRaft incident",
                    "repair_worked": False,
                    "evidence_ids": [owned["observations"][0]["id"]],
                },
                tokens["KNOWLEDGE_FEEDBACK_TOKEN"],
            )
            assert not feedback["automatic_knowledge_update"]
            owned["feedback_id"] = feedback["id"]
            marker.write_text(json.dumps(owned))
        after = cli.http(
            base,
            "/api/v1/knowledge/search?" + query,
            token=tokens["KNOWLEDGE_READ_TOKEN"],
        )
        assert [item["document_id"] for item in results["items"]] == [
            item["document_id"] for item in after["items"]
        ]
        chat_check = "NOT_TESTED; no paid provider call authorized"
        integrations = cli.http(base, "/api/v1/integrations")
        if (
            next(item for item in integrations if item["name"] == "OpenAI")["status"]
            == "not-configured"
        ):
            first = cli.http(
                base,
                "/api/v1/investigator/messages",
                {
                    "incident_id": owned["incident_id"],
                    "message": "What evidence supports this hypothesis?",
                },
            )
            second = cli.http(
                base,
                "/api/v1/investigator/messages",
                {
                    "conversation_id": first["conversation_id"],
                    "message": "What remains unknown?",
                },
            )
            assert (
                first["provider_status"] == "NOT_CONFIGURED"
                and second["response_mode"] == "deterministic-fallback"
            )
            history = cli.http(
                base, "/api/v1/investigator/conversations/" + first["conversation_id"]
            )
            assert len(history["messages"]) == 4
            chat_check = (
                "PASS actual API persisted fallback history; provider NOT_CONFIGURED"
            )
        report = {
            "source_mode": "SIMULATED",
            "real_infrastructure": "Java/PostgreSQL/OpenSearch BM25",
            "graph_query": "PASS upstream hypothesis, unconfirmed",
            "independent_writer_review_boundary": "PASS using distinct simulated test credentials",
            "native_publish_and_repeat": "PASS",
            "historical_reference": results["items"][0]["reference"],
            "content_hash": published["content_hash"],
            "retrieval_mode": results["retrieval_mode"],
            "tenant_parameter_denied": True,
            "feedback_auto_promoted": False,
            "chat_check": chat_check,
            "human_gold_review": "PENDING; no human review claimed",
            "paid_provider_calls": False,
        }
        folder = cli.ROOT / "docs/implementation/prompt09"
        folder.mkdir(exist_ok=True)
        (folder / "lab-result.json").write_text(json.dumps(report, indent=2) + "\n")
        print(
            "PASS: real graph/RAG/history APIs; scoped native BM25; review/publish/feedback boundaries; all data SIMULATED"
        )


if __name__ == "__main__":
    main()
