# Implementation state

Updated 2026-11-17. Scope authorized: Prompts 00–04; user requested 03 and 04 together.
Branch: `feature/java-ai-audit` (chosen because no feature branch name was supplied).
Base and observed remote main: `920fefe1b86e55f038fb0a7e56dd0b42e9d34118`.

## Prompt 00 — PASS

Repository and PR #3 inspected before changes. Engineering contract is in
[ENGINEERING_RULES.md](ENGINEERING_RULES.md); decisions in [DECISIONS.md](DECISIONS.md).
No runtime behavior, frontend, dependency, schema or production changes authorized by this phase.
Validation: clean initial working tree; feature branch created; state and rules exist.

## Prompt 01 — PASS (audit acceptance); release BLOCKED

- Baseline source, PR review, 44 routes, 13-table SQL inventory and OpenAPI are versioned.
- Main checks: 8 tests, lint, mypy, 7 evaluation cases, Compose syntax and web build PASS.
- PR #3 isolated archive tests: 12 PASS; no provider/search live test.
- Web audit FAIL (exit 1, critical Next.js finding); showcase audit PASS.
- Java, real search ingestion, complete Compose health and paid OpenAI NOT_TESTED.
- Audited configuration presence: provider/model/Kafka/OTel not configured in this process.
- Gate assessment: Prompt 01 requires failures to be exposed, not erased. That audit
  gate passes; dependency security/release gate FAILS. No release or migration progression.

## Artifacts and remaining work

Read AUDIT.md, PR3_REVIEW.md, API_PARITY_MATRIX.md, HEALTH_MATRIX.md, GAPS.md,
RISK_REGISTER.md, ARCHITECTURE.md, MIGRATION_PLAN.md and DECISIONS.md.
Specification retained verbatim in SPECIFICATION.md; prompts after 02 are not executed.
Baseline commands, logs, contract and schema are under baseline/. Reproduction generators
run from repository root with .venv/bin/python. Schema output is read-only metadata,
not a claim of deployed PostgreSQL parity. UI and Python source unchanged.

Prompt 02 is now authorized; do not begin Prompt 03 automatically.
No main push/merge, production writes or paid provider calls performed.

## Prompt 02 — PASS (scaffold acceptance); release remains BLOCKED

User requested Prompt 02 explicitly. Delivered independent backend-java service,
checksum-pinned wrapper, Boot 4.1.2/JDK21, module boundaries, health/security/logging,
profiles, validation, conditional SDK, Docker/Compose, smoke and additive CI.
Eight Java tests pass with no errors/failures/skips. JUnit5 was incompatible with
Spring 7 and replaced by supported Boot-managed JUnit6; failed experiment retained.
Container HTTP, local/demo/integration profiles, fail-closed enterprise configuration,
invalid model/budget rejection and live Python coexistence pass. Python regressions
8/8, lint/type checks and 7/7 evaluation cases pass. See prompt02/REPORT.md.

No Python/Next.js business logic, base Compose, schema or traffic ownership changed.
No Java business API parity, live connector/provider or production readiness claim.
The critical Next.js release blocker remains unresolved. CI changes are locally
specified, not a claim that hosted CI ran. No stage after 02 is authorized yet.

## Prompt 03 — PASS (local migration gate)

Fresh PostgreSQL schema only; preserve Alembic and current routing. Prompt 04 is authorized but must wait for the Prompt 03 gate. No environment draft or publication changes requested.

Prompt 03 checkpoint: 17 Java tests including live PostgreSQL and 74 differential
Python API steps pass with no skips. Restart/proxy/outage acceptance passes; schema
ownership refusal, tenant isolation, idempotency, approval and audit tested. Python
8 tests, lint/type checks pass. No existing data or default traffic switched.
See prompt03/REPORT.md and MIGRATION_AND_ROLLBACK.md. Critical Next.js release
blocker and enterprise/provider limitations remain. Prompt 04 is now eligible.
