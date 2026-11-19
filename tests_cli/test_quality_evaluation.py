"""Regression checks against inflated, fabricated or provenance-mismatched scores."""
import copy
import importlib.util
import json
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "quality", Path(__file__).resolve().parents[1] / "scripts/evaluate_knowledge_quality.py"
)
quality = importlib.util.module_from_spec(spec)
spec.loader.exec_module(quality)


def fixture():
    safety = {"source_mode": "SIMULATED", "human_approval": None,
              "human_reviewer": None, "usable_for_production_accuracy_claims": False}
    doc = {"document_id": "runbook", "revision": 2, "technology": "Kafka",
           "platform_version": "4.3", "metadata_mode": "KRAFT",
           "acl_principals": ["demo.reader"], "source_time": "2026-11-17T00:00:00Z"}
    hashed = quality.digest(json.dumps(doc, separators=(",", ":")).encode())
    chunk = {**doc, "tenant_id": "demo", "content_hash": hashed, "text": "Guidance", "chunk_index": 0,
             "chunk_id": "KB-" + quality.digest(f"demo|runbook|2|{hashed}|0".encode())}
    hit = {**chunk, "reference": "knowledge:runbook@2#0", "evidence_kind": "HISTORICAL_ADVICE"}
    observation = {"id": "obs-1", "entity_id": "network", "observed_at": doc["source_time"],
                   "tenant_id": "demo", "incident_id": "INC-1"}
    case = {"id": "one", "tenant_id": "demo", "principal": "demo.reader", "incident_id": "INC-1",
            "expected_detection": True, "expected_origin": "network:NETWORK_UNSTABLE",
            "graph": {"graph_id": "graph"}, "observations": [observation],
            "technology": "Kafka", "version": "4.3", "mode": "KRAFT", "relevant_documents": ["runbook"]}
    manifest = {**safety, "cases": [case], "source_dataset_sha256": "original", "label_author": "developer", "held_out_set": False, "threshold_tuning_performed": False}
    data = json.dumps(manifest).encode()
    prediction = {"incident_id": "INC-1", "source_mode": "SIMULATED", "graph_id": "graph", "confirmed_cause": None, "candidate_causes": [
        {"entity_id": "network", "signal": "NETWORK_UNSTABLE", "supporting": [
            {"evidence_id": "obs-1", "entity_id": "network", "observed_at": doc["source_time"]}], "contradicting": []}]}
    raw = {**safety, "dataset_sha256": quality.digest(data), "cases": [
        {"case_id": "one", "investigation": prediction, "retrieval": {"items": [hit], "status": "HEALTHY", "partial": False, "rejected_records": 0}}],
           "corpus": [{"tenant_id": "demo", "document": doc, "content_hash": hashed, "chunks": [chunk], "current": True}],
           "missing_data_checks": [], "injection_checks": [], "transport": "LIVE_LOCAL_TEST", "provider_calls": 0}
    return manifest, raw, data


class QualityScorerTest(unittest.TestCase):
    def test_top_k_uses_real_slots_and_empty_diagnostics_are_not_perfect(self):
        result = quality.score(*fixture())
        measured = result["fixture_tests"]
        self.assertEqual(measured["retrieval"]["3"]["precision"]["value"], 1 / 3)
        self.assertEqual(measured["diagnostics"]["upstream_cause_top1"]["denominator"], 1)
        self.assertIsNone(measured["diagnostics"]["false_positive_rate"]["value"])
        self.assertIsNone(measured["diagnostics"]["prompt_injection_rejection"]["value"])
        self.assertIsNone(result["expert_reviewed_tests"]["metrics"])
        self.assertFalse(result["usable_for_production_accuracy_claims"])

    def test_forged_source_id_or_timestamp_degrades_citation_validity(self):
        for field, bad in [("evidence_id", "invented"), ("observed_at", "2099-01-01T00:00:00Z")]:
            manifest, raw, data = fixture()
            raw["cases"][0]["investigation"]["candidate_causes"][0]["supporting"][0][field] = bad
            result = quality.score(manifest, raw, data)
            self.assertEqual(result["fixture_tests"]["diagnostics"]["evidence_citation_validity"]["value"], 0.5)

    def test_tenant_acl_tampering_is_counted_even_for_a_known_chunk(self):
        for field, bad in [("tenant_id", "other"), ("acl_principals", ["other.reader"])]:
            manifest, raw, data = fixture()
            raw["cases"][0]["retrieval"]["items"][0][field] = bad
            result = quality.score(manifest, raw, data)["fixture_tests"]
            self.assertEqual(result["diagnostics"]["unauthorized_knowledge_rate"]["value"], 1)
            self.assertEqual(result["retrieval"]["1"]["recall"]["value"], 0)

    def test_stale_revision_is_not_relevant_or_current(self):
        manifest, raw, data = fixture()
        raw["corpus"][0]["current"] = False
        measured = quality.score(manifest, raw, data)["fixture_tests"]
        self.assertEqual(measured["diagnostics"]["outdated_or_incompatible_knowledge_rate"]["value"], 1)
        self.assertEqual(measured["retrieval"]["1"]["precision"]["value"], 0)

    def test_duplicate_or_missing_cases_and_duplicate_hits_are_rejected(self):
        for mutation in ("duplicate_case", "missing_case", "duplicate_hit"):
            manifest, raw, data = fixture()
            if mutation == "duplicate_case":
                raw["cases"].append(copy.deepcopy(raw["cases"][0]))
            elif mutation == "missing_case":
                raw["cases"] = []
            else:
                raw["cases"][0]["retrieval"]["items"].append(copy.deepcopy(raw["cases"][0]["retrieval"]["items"][0]))
            with self.assertRaises(ValueError):
                quality.score(manifest, raw, data)

    def test_human_approval_production_claim_and_wrong_input_hash_are_rejected(self):
        for field, bad in [("human_approval", "invented approval"), ("usable_for_production_accuracy_claims", True), ("dataset_sha256", "wrong")]:
            manifest, raw, data = fixture()
            raw[field] = bad
            with self.assertRaises(ValueError):
                quality.score(manifest, raw, data)

    def test_corpus_hash_tampering_and_fake_confirmed_cause_are_rejected(self):
        manifest, raw, data = fixture()
        raw["corpus"][0]["document"]["source_time"] = "2099-01-01T00:00:00Z"
        with self.assertRaises(ValueError):
            quality.score(manifest, raw, data)
        manifest, raw, data = fixture()
        raw["cases"][0]["investigation"]["confirmed_cause"] = "model said so"
        with self.assertRaises(ValueError):
            quality.score(manifest, raw, data)

    def test_empty_retrieval_is_zero_recall_and_no_claim_about_authorized_hits(self):
        manifest, raw, data = fixture()
        raw["cases"][0]["retrieval"]["items"] = []
        measured = quality.score(manifest, raw, data)["fixture_tests"]
        self.assertEqual(measured["retrieval"]["1"]["recall"]["value"], 0)
        self.assertIsNone(measured["diagnostics"]["unauthorized_knowledge_rate"]["value"])

    def test_manifest_labels_cannot_be_changed_without_updating_provenance(self):
        manifest, raw, data = fixture()
        manifest["cases"][0]["expected_origin"] = "different:DISK_EXHAUSTED"
        with self.assertRaises(ValueError):
            quality.score(manifest, raw, data)

    def test_live_or_wrong_incident_outputs_cannot_enter_fixture_measurements(self):
        for field, bad in [("source_mode", "LIVE"), ("incident_id", "FOREIGN")]:
            manifest, raw, data = fixture()
            raw["cases"][0]["investigation"][field] = bad
            with self.assertRaises(ValueError):
                quality.score(manifest, raw, data)
