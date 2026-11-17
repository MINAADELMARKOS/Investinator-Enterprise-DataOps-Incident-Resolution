# Prompt 07 — Versioned dependency graph and explainable correlation

Adds immutable tenant-scoped graph revisions, typed nodes/directional edges,
provenance/confidence/validity/discovery times and incident-to-graph bindings.
Flyway V3 adds only Java-owned tables; Python and existing operational contracts
remain intact. Graph imports and observation registration are explicit SIMULATED
fixtures, guarded by a distinct GRAPH_ADMIN_TOKEN in demo mode. GET dependencies
requires INVESTIGATION_API_TOKEN. Public import cannot label observations LIVE.
Trusted future connector ingestion must provide actual provenance, not inferred edges.

Queries: GET /api/v1/dependencies/graphs/{id}?revision=...&at=... and
GET /api/v1/dependencies/incidents/{id}/investigation?graph=.... Bindings prevent
substituting another cluster's graph or an unbound future revision. Existing
NerveMap shape remains unchanged; setting INVESTINATOR_TOPOLOGY_GRAPH explicitly
projects a registered typed graph through the existing topology endpoint.

The deterministic engine checks clock skew (default 30s), four-hour windows,
known-at-time graph edges, directional reachability, bounded failure intensity,
recovery evidence and healthy/reverse-order contradictions. Distinct affected
components contribute bounded ranking; repeated source lines do not inflate
confidence. Simulated/live observations never corroborate each other. Results
include evidence IDs/timestamps, candidate explanations, contradictions, unknowns,
impact, read-only checks, prevention and risk. No result automatically confirms a
cause or executes remediation. Confidence bands describe evidence corroboration,
not probabilities or production accuracy.

Independent simulated KRaft and legacy chains rank the upstream network candidate.
Healthy ZooKeeper with exhausted disk selects disk. Normal, contradictory,
reverse-time, invalid mode, duplicate/conflicting ID, cross-tenant, stale edge and
mixed-source cases exercise conservative behavior. PostgreSQL/MockMvc integration
checks real persistence, immutable versions, scoped authenticated Java queries and
incident graph bindings. Samples kraft.json/legacy.json are SIMULATED outputs.

Limitations: no automatic production graph discovery, no causal confirmation from
correlation, and no production identity/cutover. Existing critical Next.js advisory,
private connector coverage and production security gates remain open. Initial
compile lambda capture and pinned-image compatibility declaration errors were
corrected before the final acceptance run; no failed criterion was bypassed.

Final ./investinator test: 57 Java tests + 13 CLI tests PASS, zero failure/error/skip.
Baseline artifact validator and git diff checks PASS. No production change, paid
provider call, push/merge or publication.
