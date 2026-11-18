# Prompt 14 — Security, operability and production readiness

2026-11-18. Assessment completed with explicit blockers. **Production release:
BLOCKED.** This stage does not authorize deployment, production infrastructure,
main merge, routing cutover or paid provider calls. Stop after Prompt 14.

Prompt 13 checkpoint `5e4be3a` and five-layer architecture/ADRs/reports inspected on
feature/java-ai-audit before changes. Existing Python default, Next design, separate
private Flyway/Alembic ownership and all completed commits are preserved.

## Implemented corrections

- Non-demo platform-health no longer bypasses the enterprise application deny rule.
  Valid demo capabilities cannot unlock non-demo operational routes. Minimal probes
  remain public; Actuator env/private metrics are denied without the metrics token.
  No OIDC or independent enterprise approval is pretended: ingress/executors blocked.
- Base demo host ports bind loopback, preserving local ports and Python routing.
- Next consumes actual request bytes with a strict bound and upstream cancellation;
  missing/false Content-Length cannot bypass it. Invalid UTF-8 is a 422 client error.
  Login has the same bounded reader. Secure cookies derive from the deployment-owned
  HTTPS public origin, including TLS termination, without trusting forwarded headers.
- Fixed-cardinality chat latency/outcome/provider-attempt/token/unknown-usage meters,
  pending queue/outbox counts/age, HTTP SLO buckets and existing JVM/GC/Hikari metrics
  support operational inspection. A scoped Java investigation span ends on success
  and failure without recording questions, identifiers, model names or exception
  content; actual in-memory SDK export is tested. Tracing remains disabled by default.
  No prompt, incident, tenant, model or secret tag.
  Missing usage is unknown, not zero cost. Private generated lab metrics token added.
- Six proposed Prometheus rules and NaN/missing-series unit checks; not auto-enabled.
  Existing Prometheus still scrapes collector; reviewed private Java scrape setup
  remains a staging gate. CLI capture reads the authenticated Java endpoint directly.
- Actual read-only owned-schema backup and disposable restore, plus bounded local
  baseline capture. CI gains restore/baseline artifacts and Prometheus rule tests;
  dumps, secrets and runtime configuration are never uploaded.

## Evidence and tests

Local full Maven Verify: **110 tests, no failures/errors/skips**, plus the subsequent
11-test SDK budget suite containing one added tool/rate exhaustion case and a
13-test focused tracing/budget suite. Combined JUnit reports contain 112 distinct
passing cases, not a claim of one 112-case local full run. CLI 29, Python 12/Ruff/mypy 17 files, frontend 11/type/build/audit and actual
14-check Java/default-Python browser flows pass. Provider tests are explicitly
MOCKED_PROVIDER. No actual GPT call, human quality approval or accuracy claim.

Backup/restore passes for **38 tables / 781 rows**, preserving a separate public
owner sentinel, with zero source writes. The fixture took **18.634 seconds**; this
is not production RTO/RPO, PITR or replica failover evidence. The private temporary
dump was removed and only the newly created disposable target was cleaned up.

Actual local baseline: 30 serial requests each, zero errors. Health median/p95
4.845/10.997 ms; incident read 12.973/62.376 ms. These are client elapsed local
measurements on SIMULATED data, not a load/capacity/production SLO result. The
snapshot contains 33 whitelisted metric lines, including JVM, pool and outbox state.
Production detection latency, 30-day availability, MTTA/MTTI/MTTR, causal accuracy,
customer impact, costs and ROI are NOT_MEASURED.

npm audit and Python pip-audit report zero known findings. Java POM is byte-identical
to P13's default-tree scan (233 packages / zero findings); fresh P14 hosted scan is
pending publication. Local OSV API egress was previously blocked. Container OS CVEs,
optional shaded Hive driver and independent penetration review are not certified;
release remains blocked. No security gate or severity threshold was weakened.

See test-results.json, tenant-isolation-proof.json, SECURITY_SCANS.json,
backup-restore.json, operational-baseline.json, browser-results.json and scan outputs.

## Corrected verification failures

The first backup harness readiness check saw PostgreSQL's temporary Unix startup
server; waiting for the final TCP listener fixed it. The initial operational capture
correctly failed because the lab had no metrics capability configured; a generated
private token now enables authenticated capture. Prometheus rule tests initially
needed writable test storage despite a read-only root; bounded tmpfs fixed the
harness. NaN rules now explicitly treat NaN as missing rather than healthy. Final
local checks pass; no source data or production infrastructure was changed to force
an assertion to pass.

## Assessment and handover

[Threat model](THREAT_MODEL.md), [auth/certification matrix](AUTH_MATRIX.md),
[SLO/dashboard definitions](SLOS.md), [FinOps model](FINOPS.json),
[data/recovery review](RECOVERY_AND_DATA.md), [failure matrix](FAILURE_MATRIX.md) and
[release risks](RELEASE_RISKS.json) define owners, mitigations, evidence and milestones.

High/critical production blockers: OIDC/RBAC and independent approval, private
connector TLS/Kerberos/refresh/tenant certification, real network/replica/long-duration
recovery tests, encrypted offsite/PITR/HA, immutable audit/retention/licensing/PII review,
durable financial caps and approved live budget, independent human knowledge review,
reviewed network/perimeter policy, optional-profile/container security and staging
SLO/load evidence. No synthetic sign-off or financial return is manufactured.

Local demo is CONDITIONAL on loopback/private capabilities and retained safety
constraints. Production NO-GO is deliberate and explicit, not an unfinished deployment.
Prompt 15 requires a new request. Do not enable enterprise ingress/executors or
execute infrastructure changes from this assessment.
