This branch reduces post-P14 engineering gaps while preserving the completed Java
work and Prompt 15 handover, Python default routing and separate Alembic/Flyway owners.
It also normally merges newer main's Pages/public gateway without deploying either.

Changes: bounded SQL-current/ACL/version-filtered BM25 retrieval; domain-aware UNKNOWN;
strict equal-time hysteresis; disabled native provenance/certification contracts;
tenant-redacted audit export/manual ambiguous-effect recovery; durable Java USD quota
reservations and UNKNOWN reconciliation; zero-call standalone preflight; explicit mock
provider metadata/typed UI; default-deny data use; offline unwired JWT experiment;
disposable WAL/PITR, bounded read workload, default/optional Hive metadata SBOM/OSV CI.
Inherited public Worker gets actual stream/UTF-8 bounds and default-off cost/data-use
opt-in. An older lab marker is preserved using a separate acceptance fixture namespace.

Validation: local Java141 (no failures/errors/skips), CLI34, Python12/Ruff/mypy,
frontend13/type/build/audit, public15/check/dry-run, showcase build/audit, browser14,
eight owned outages, physical Java restarts, evidence pipeline, 3000 duplicate burst,
500 paced reads, 41-table/1120-row restore and disposable WAL restore. Exact source,
exit codes and provenance: docs/implementation/prompt14b/VALIDATION.json. Hosted current
seven jobs are recorded separately after push, never inferred from prior green CI.

V9 adds three unseeded tables and an audit export index only in the private Java schema.
Rollback disables provider/ingestion opt-ins and retains data, V9 and uncertain budget
holds. No destructive down migration, schema merge, default Java switch, new live
executor, Pages publication or Worker deployment. Existing P10–15 commits carried from
the Java source branch are preserved; they are not rebuilt or silently called merged.

Production NO-GO: P14-01..09 retain real identity, private connector, HA/DR, actual
provider/price/approval, unsigned independent gold, perimeter/legal, image/shaded security
and staging SLO gates. Local OSV egress and image scanner DB are blocked; missing scans
are not passed. Metadata SBOM cannot certify shaded contents. Java budgets do not cover
the preserved Python/public Worker paths. Public gateway actual deployment is unverified.
Developer fixture precision stays 8/9; no production RCA accuracy/ROI/signoff invented.
No paid provider calls, production actions, merge into main or release authorization.

Review docs/implementation/prompt14b/REPORT.md, GAP_CLOSURE_MATRIX.json,
RELEASE_DECISION.md and CONFLICT_LEDGER.md. No automatic merge requested.

The first hosted checkpoint32069f2 passed all seven jobs on44425318558; final-head
checks must independently pass. Additional actual public-browser testing found and
fixed a React effect cleanup return-value failure; four rendered/protocol cases
now run in local-e2e, including repository-prefix mode. No old green SHA substitutes
for this new source. Existing fixed legacy DEMO aliases remain for non-governed
incidents; governed-plan bypass is denied, but full alias retirement is not claimed.
