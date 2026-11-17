# Prompt 03 implementation report

> 09B security reconciliation (2026-11-17): the Next.js finding described below
> is historical. Current main/local Next.js 16.4.5 package and lockfile match;
> fresh npm audit reports zero vulnerabilities. See the
> [current security assessment](../prompt09/PROMPT05_AUDIT.md#security-advisory-reconciliation).

Date 2026-11-17, feature/java-ai-audit. Python/default routing and its existing database
are preserved. No production infrastructure action, provider call, push or publication.

## Implemented

Fresh Flyway-owned PostgreSQL schema for the 13 inspected operational tables plus
durable request/execution keys; Hikari/JDBC with bounded queries; typed contracts
and lifecycle/action enums; deterministic incident, metric correlation, topology,
knowledge, replay, chat/history, approval, verification, audit and evaluation APIs
including all legacy aliases. All API commands persist their audit and idempotency
state atomically. Schema adoption/clean are disabled and existing Alembic/public
schemas are refused. Source/time/provenance/simulation values remain explicit.

Only the named ReplayLab simulation executor exists. Client decided_by/tenant values
do not become authority; non-demo access fails closed until identity exists. Java
OpenAI endpoints honestly return deterministic fallback and must not replace the
provider-enabled Python path before its own stage. Basic Kafka metadata connectivity
is implemented but live broker/semantic parity was not tested. No default UI cutover.

## Evidence

- Maven verify: 17 tests, zero failures/errors/skips, including real PostgreSQL via
  Testcontainers and MockMvc. Captured Python-to-Java comparison exercises 74 HTTP
  steps across all declared operations, three scenarios, error codes and aliases.
- Durable incident/approval state survives terminating and starting a new Java
  process, then approved recovery and audit succeed. Duplicate/idempotent concurrent
  requests do not duplicate writes; tampered keys conflict. Forced command failure
  rolls back, and foreign tenant reads cannot see incidents/approvals/audit.
- Existing Alembic data is left intact when attempted schema adoption is refused.
- Unchanged Next.js page and GET/POST proxy work against the opt-in Java demo.
- Required DB outage: readiness 503, liveness UP. Non-demo identity: access 503.
- Python 8 tests pass; lint and mypy pass. Base/Java/persistence Compose syntax passes.
- No live OpenAI, complete Kafka probe, hosted CI or production data import was run.

See verify.txt, restart-proxy-outage.txt and test-summary.json. Reproduce with
`./backend-java/mvnw -B -f backend-java/pom.xml verify` using JDK21/Docker; then
`python3 backend-java/scripts/verify_persistence.py --with-web` (installed Next.js).
Managed-cloud Maven uses the prepared proxy/system CA options documented in
backend-java/README.md. No verification was disabled.

## Corrections and limits

Compile attempts caught regex escaping and Java generic inference errors; corrected.
Testcontainers required explicitly declaring the digest-pinned official image as a
PostgreSQL-compatible image. Differential checks caught Python title-casing around
digits and SQLite timestamp timezone loss; title behavior was ported and timestamps
validated/normalized explicitly. Runtime testing caught 403 versus 503 on missing
identity; both authentication and authorization error paths now preserve 503.
An initial lint check on new scripts failed and was corrected. Final checks pass.

Validation uses sanitized 422 messages rather than echoing Pydantic's detailed input.
Generated IDs/time precision and unspecified collection order are normalized, not
claimed identical; semantic values and ordered history/evidence are compared.
Tenant isolation is demo server binding, not enterprise OIDC/RBAC. Existing data
import/cutover still needs a concrete reviewed backup/restore and ownership handover.
Critical Next.js findings from the baseline remain a release blocker. No production
readiness claim or waived release gate follows from passing this local stage.

## Stage gate

PASS for Prompt 03: migrated API matrix is recorded, restart durability demonstrated,
tenant/approval/audit regression checks pass, and production execution stays absent.
Prompt 04 may start only after this checkpoint is committed.
