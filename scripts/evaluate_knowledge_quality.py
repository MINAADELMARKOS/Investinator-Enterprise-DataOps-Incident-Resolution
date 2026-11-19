#!/usr/bin/env python3
"""Score explicit developer fixtures, never certify human or production RCA quality."""
import argparse
import hashlib
import json
from pathlib import Path


def digest(data):
    return hashlib.sha256(data).hexdigest()


def ratio(numerator, denominator):
    return {
        "numerator": numerator,
        "denominator": denominator,
        "value": numerator / denominator if denominator else None,
        "status": "MEASURED_FIXTURE_ONLY" if denominator else "NOT_MEASURED",
    }


def require_fixture(value):
    if (
        value.get("source_mode") != "SIMULATED"
        or value.get("human_approval") is not None
        or value.get("human_reviewer") is not None
        or value.get("usable_for_production_accuracy_claims") is not False
    ):
        raise ValueError("Fixture scorer cannot accept human approval or production claims")


def score(manifest, raw, manifest_bytes):
    if json.loads(manifest_bytes) != manifest:
        raise ValueError("Manifest object differs from hashed bytes")
    require_fixture(manifest)
    require_fixture(raw)
    if raw.get("dataset_sha256") != digest(manifest_bytes):
        raise ValueError("Evaluation input hash mismatch")
    cases = {c["id"]: c for c in manifest["cases"]}
    rows = {r["case_id"]: r for r in raw["cases"]}
    if len(cases) != len(manifest["cases"]) or len(rows) != len(raw["cases"]):
        raise ValueError("Duplicate evaluation case")
    if cases.keys() != rows.keys():
        raise ValueError("Missing or unexpected evaluation cases")
    corpus = {}
    for revision in raw["corpus"]:
        doc = revision["document"]
        hashed = digest(json.dumps(doc, separators=(",", ":"), ensure_ascii=False).encode())
        if hashed != revision["content_hash"]:
            raise ValueError("Corpus content hash mismatch")
        for chunk in revision["chunks"]:
            expected = "KB-" + digest(
                f"{revision['tenant_id']}|{doc['document_id']}|{doc['revision']}|{hashed}|{chunk['chunk_index']}".encode()
            )
            if chunk["chunk_id"] != expected or chunk["content_hash"] != hashed:
                raise ValueError("Corpus chunk identity mismatch")
            if chunk["chunk_id"] in corpus:
                raise ValueError("Duplicate corpus chunk")
            corpus[chunk["chunk_id"]] = (revision, chunk)
    totals = dict(tp=0, fp=0, fn=0, tn=0, top1=0, top3=0, positives=0,
                  citations=0, valid_citations=0, hits=0, unauthorized=0, outdated=0, relevant_hits=0)
    retrieval = {k: {"relevant": 0, "slots": 0, "expected": 0} for k in (1, 3, 5)}
    details = []
    for case_id, expected in cases.items():
        row = rows[case_id]
        prediction = row["investigation"]
        if prediction.get("confirmed_cause") is not None:
            raise ValueError("Fixture output must never assert a confirmed cause")
        if prediction["source_mode"] != "SIMULATED" or prediction["incident_id"] != expected["incident_id"]:
            raise ValueError("Incident/source scope mismatch")
        if prediction["graph_id"] != expected["graph"]["graph_id"]:
            raise ValueError("Graph scope mismatch")
        candidates = prediction["candidate_causes"]
        observed = {o["id"]: o for o in expected["observations"]}
        if len(observed) != len(expected["observations"]):
            raise ValueError("Duplicate source evidence identity")
        truth, detected = expected["expected_detection"], bool(candidates)
        totals["tp" if truth and detected else "fn" if truth else "fp" if detected else "tn"] += 1
        ranks = [f"{c['entity_id']}:{c['signal']}" for c in candidates]
        if truth:
            totals["positives"] += 1
            totals["top1"] += bool(ranks and ranks[0] == expected["expected_origin"])
            totals["top3"] += expected["expected_origin"] in ranks[:3]
        for candidate in candidates:
            for ref in candidate["supporting"] + candidate["contradicting"]:
                source = observed.get(ref["evidence_id"], {})
                valid = all(source.get(k) == ref.get(k) for k in ("entity_id", "observed_at"))
                valid = valid and source.get("tenant_id") == expected["tenant_id"]
                valid = valid and source.get("incident_id") == expected["incident_id"]
                totals["citations"] += 1
                totals["valid_citations"] += valid
        hits = row["retrieval"]["items"]
        if len({h["chunk_id"] for h in hits}) != len(hits):
            raise ValueError("Duplicate retrieval hit")
        relevant = []
        for hit in hits:
            entry = corpus.get(hit["chunk_id"])
            authorized, current, valid = False, False, False
            if entry:
                revision, chunk = entry
                doc = revision["document"]
                authorized = revision["tenant_id"] == expected["tenant_id"] and expected["principal"] in doc["acl_principals"]
                authorized = authorized and hit.get("tenant_id") == expected["tenant_id"] and expected["principal"] in hit.get("acl_principals", [])
                compatible = doc["technology"] == expected["technology"] and doc["metadata_mode"] in ("ANY", expected["mode"])
                version = doc["platform_version"]
                compatible = compatible and (version == "ANY" or version == expected["version"] or expected["version"].startswith(version + "."))
                current = revision["current"] and compatible and hit.get("revision") == doc["revision"]
                current = current and hit.get("platform_version") == doc["platform_version"] and hit.get("metadata_mode") == doc["metadata_mode"]
                pointer = f"knowledge:{doc['document_id']}@{doc['revision']}#{chunk['chunk_index']}"
                valid = all(hit.get(k) == chunk.get(k) for k in ("tenant_id", "document_id", "revision", "content_hash", "text", "source_time", "acl_principals"))
                valid = valid and hit.get("reference") == pointer and hit.get("evidence_kind") == "HISTORICAL_ADVICE"
            totals["hits"] += 1
            totals["unauthorized"] += not authorized
            totals["outdated"] += not current
            totals["citations"] += 1
            totals["valid_citations"] += valid and authorized and current
            relevant.append(hit["document_id"] if valid and authorized and current and hit["document_id"] in expected["relevant_documents"] else None)
        totals["relevant_hits"] += len(set(relevant) - {None})
        per_k = {}
        for k in retrieval:
            found = len(set(relevant[:k]) - {None})
            denominator = len(set(expected["relevant_documents"]))
            retrieval[k]["relevant"] += found
            retrieval[k]["slots"] += k
            retrieval[k]["expected"] += denominator
            per_k[str(k)] = {"precision": ratio(found, k), "recall": ratio(found, denominator)}
        details.append({"case_id": case_id, "expected_origin": expected["expected_origin"],
                        "candidate_ranking": ranks, "detected": detected, "retrieved_documents": [h["document_id"] for h in hits],
                        "retrieval": per_k, "retrieval_status": row["retrieval"]["status"],
                        "partial": row["retrieval"]["partial"], "rejected_index_records": row["retrieval"]["rejected_records"], "human_approval": None})
    missing = raw["missing_data_checks"]
    injections = raw["injection_checks"]
    diagnostics = {
        "returned_retrieval_precision": ratio(totals["relevant_hits"], totals["hits"]),
        "detection_precision": ratio(totals["tp"], totals["tp"] + totals["fp"]),
        "detection_recall": ratio(totals["tp"], totals["tp"] + totals["fn"]),
        "false_positive_rate": ratio(totals["fp"], totals["fp"] + totals["tn"]),
        "upstream_cause_top1": ratio(totals["top1"], totals["positives"]),
        "upstream_cause_top3": ratio(totals["top3"], totals["positives"]),
        "evidence_citation_validity": ratio(totals["valid_citations"], totals["citations"]),
        "unauthorized_knowledge_rate": ratio(totals["unauthorized"], totals["hits"]),
        "outdated_or_incompatible_knowledge_rate": ratio(totals["outdated"], totals["hits"]),
        "missing_data_abstention": ratio(sum(c["abstained"] for c in missing), len(missing)),
        "prompt_injection_rejection": ratio(sum(c["quarantined"] and c["approval_denied"] for c in injections), len(injections)),
    }
    operational_pass = (
        totals["citations"] > 0 and totals["valid_citations"] == totals["citations"]
        and totals["hits"] > 0 and totals["unauthorized"] == 0 and totals["outdated"] == 0
        and totals["fp"] == 0 and missing and all(c["abstained"] for c in missing)
        and injections and all(c["quarantined"] and c["approval_denied"] for c in injections)
    )
    return {
        "operational_gate": "PASS_FUNCTIONAL_ONLY" if operational_pass else "FAIL_OR_MISSING_DIAGNOSTICS",
        "report_version": "09B-1", "dataset_sha256": digest(manifest_bytes),
        "source_dataset_sha256": manifest["source_dataset_sha256"],
        "human_reviewer": None, "human_approval": None,
        "review_state": "PENDING_HUMAN_REVIEW", "usable_for_production_accuracy_claims": False,
        "fixture_tests": {"source_mode": "SIMULATED", "label_author": manifest["label_author"],
                          "held_out_set": manifest["held_out_set"], "threshold_tuning_performed": manifest["threshold_tuning_performed"],
                          "detection_definition": "Presence of a deterministic upstream correlation candidate; not Prompt 10 monitoring",
                          "precision_at_k_definition": "Relevant distinct documents / k, including unfilled slots; recall uses all proposed relevant documents",
                          "causal_denominator": "Fault cases only; healthy cases excluded from causal top-k",
                          "confusion_matrix": {k: totals[k] for k in ("tp", "fp", "fn", "tn")},
                          "diagnostics": diagnostics,
                          "retrieval": {str(k): {"precision": ratio(v["relevant"], v["slots"]), "recall": ratio(v["relevant"], v["expected"])} for k, v in retrieval.items()},
                          "cases": details},
        "expert_reviewed_tests": {"status": "PENDING_INDEPENDENT_HUMAN_REVIEW", "cases": 0, "metrics": None, "human_approval": None},
        "live_observations": {"transport": raw["transport"], "source_incident_data": "SIMULATED",
                              "production_incidents": 0, "production_accuracy": None, "provider_calls": raw["provider_calls"]},
    }


def write_review_package(manifest, raw, report, destination):
    lines = ["# Independent knowledge review package — Prompt 09B", "",
             "Human reviewer: **PENDING**. Human approval: **unfilled**. No independent sign-off exists.", "",
             "These are developer-authored SIMULATED fixtures on actual local PostgreSQL/OpenSearch transport.",
             "They are not a held-out set or production incidents. No thresholds, BM25 weights or correlation rules were tuned.",
             "Read-only functional GO is separate from expert quality approval and production deployment.", "",
             f"Review dataset SHA256: `{report['dataset_sha256']}`.",
             f"Original Prompt 09 dataset SHA256: `{manifest['source_dataset_sha256']}`.",
             f"Raw native observation SHA256: `{report['raw_observations_sha256']}`.", "",
             "Sources: [exact graphs and evidence](../../../eval/knowledge_review_cases.json),",
             "[native outputs and revision/chunk corpus](../prompt09b/raw-observations.json),",
             "[scored report](EVAL_REPORT.json), [pending review worksheet](GOLD_REVIEW.md).", "",
             "The corpus is proposed fixture guidance, not an expert-validated operational runbook library.",
             "It contains immutable revisions 1 and 2 plus foreign-tenant, private-ACL and version-99.9 distractors.",
             "Distinct SIMULATED reviewer credentials exercise software approval boundaries, not human governance.",
             "Inspect each proposed source for correctness/licensing before any operational adoption.", "",
             "Scoring: detection means nonempty correlation candidates, not autonomous Prompt 10 monitoring.",
             "Top-1/top-3 RCA denominators exclude the healthy case. Precision@k divides by k even with fewer hits;",
             "recall divides by proposed relevant documents. Every metric includes its numerator/denominator.",
             "Historical knowledge cannot itself confirm a cause. BM25 only; no vectors, embeddings or training.", ""]
    outputs = {r["case_id"]: r for r in raw["cases"]}
    for case in manifest["cases"]:
        output = outputs[case["id"]]
        lines += [f"## {case['id']}", "",
                  f"Platform: **{case['technology']} {case['version']} / {case['mode']}**.",
                  f"Tenant/principal: `{case['tenant_id']}` / `{case['principal']}`; incident `{case['incident_id']}`.",
                  f"Graph `{case['graph']['graph_id']}` revision {case['graph']['revision']}; all edges SIMULATED, known from their recorded validity/discovery times.",
                  "Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.", "",
                  "Dependency edges (child DEPENDS_ON parent): " + "; ".join(f"`{e['from']} -> {e['to']}`" for e in case["graph"]["edges"]) + ".", "",
                  "| Exact evidence ID | Entity / signal | State | Observed at |",
                  "|---|---|---|---|"]
        for event in sorted(case["observations"], key=lambda e: (e["observed_at"], e["id"])):
            lines.append(f"| `{event['id']}` | {event['entity_id']} / {event['signal']} | {event['state']} | {event['observed_at']} |")
        expected = case["expected_origin"] or "ABSTAIN: no originating candidate"
        actual = [f"{c['entity_id']}:{c['signal']}" for c in output["investigation"]["candidate_causes"]]
        lines += ["", f"Proposed originating hypothesis: `{expected}`.",
                  "Proposed competing hypotheses: " + (", ".join(case["expected_alternatives"]) or "none from supplied observations") + ".",
                  "Actual deterministic ranking: " + (", ".join(actual) or "ABSTAIN") + ".",
                  "Counterevidence/contradictions: " + case["counterevidence"],
                  "Blind spots: " + case["blind_spots"],
                  "Expected uncertainty: " + case["expected_uncertainty"] + ".", "",
                  f"BM25 query: `{case['query']}`. Proposed relevant source: `{case['relevant_documents'][0]}` revision 2."]
        source = next(c for c in raw["corpus"] if c["document"]["document_id"] == case["relevant_documents"][0] and c["document"]["revision"] == 2)
        doc = source["document"]
        lines += [f"Source `{doc['source']}`, owner `{doc['owner']}`, time `{doc['source_time']}`; hash `{source['content_hash']}`.",
                  "Approved SIMULATED chunk IDs: " + ", ".join(f"`{c['chunk_id']}`" for c in source["chunks"]) + ".",
                  "Readable exact text: " + doc["content"],
                  f"Native retrieval status `{output['retrieval']['status']}`, partial `{output['retrieval']['partial']}`; rejected index records {output['retrieval']['rejected_records']}.",
                  "Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.", "",
                  "Independent reviewer notes/corrections: **unfilled**.",
                  "Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.", ""]
    lines += ["## Review procedure and release of labels", "",
              "Review clocks/topology/version/mode, alternative causal chains and contradictory evidence independently.",
              "Correct proposed labels in a separately versioned review record before approving; sign the exact dataset hash.",
              "Retain this original fixture result. Create a genuinely held-out set before tuning or claiming improved accuracy.",
              "Source/tenant/ACL/version and injected-instruction checks must remain negative tests.",
              "Required review fields: case ID, source hashes, reviewer role/identity, timestamp, corrected expected labels,",
              "accepted blind spots and uncertainty, decision and rationale. No field is auto-filled as approval.", ""]
    destination.write_text("\n".join(lines))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=Path("eval/knowledge_review_cases.json"))
    parser.add_argument("--raw", type=Path, default=Path("backend-java/target/quality-review-observations.json"))
    parser.add_argument("--output", type=Path, default=Path("docs/implementation/prompt09/EVAL_REPORT.json"))
    parser.add_argument("--review-package", type=Path)
    args = parser.parse_args()
    data = args.manifest.read_bytes()
    manifest = json.loads(data)
    if manifest["source_dataset_sha256"] != digest(Path("eval/knowledge_cases.json").read_bytes()):
        raise ValueError("Original Prompt 09 dataset changed; review provenance required")
    raw_bytes = args.raw.read_bytes()
    result = score(manifest, json.loads(raw_bytes), data)
    result["raw_observations_sha256"] = digest(raw_bytes)
    result["scorer_sha256"] = digest(Path(__file__).read_bytes())
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    if args.review_package:
        write_review_package(manifest, json.loads(raw_bytes), result, args.review_package)
    print("Scored developer fixtures; human review PENDING; no production accuracy claim")
    if result["operational_gate"] != "PASS_FUNCTIONAL_ONLY":
        raise SystemExit("Operational isolation/citation/abstention diagnostics failed or are missing")


if __name__ == "__main__":
    main()
