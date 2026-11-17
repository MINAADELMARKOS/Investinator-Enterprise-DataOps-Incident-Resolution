# Architecture decisions

All decisions dated 2026-11-17. Accepted direction, implementation pending unless stated.

## ADR-001 — Java 21 / Spring Boot

Use Java 21 LTS and a supported pinned Spring Boot release; modular Spring MVC service
with validation, Actuator, security, JDBC/JPA as justified and Micrometer. The spec's
4.1.2 assertion is not independent compatibility evidence. Do not pin that version
until official release, drivers, plugins and Java 21 build/container tests pass in
Prompt 02. Maven Wrapper and resolved dependency versions must be committed there.
Risk: Jakarta/driver/plugin incompatibility; rollback: leave Python serving all traffic.

## ADR-002 — Search backend choice

Propose Elasticsearch with its matching official Java client because PR #3 already
uses Elasticsearch conventions. Decision is provisional pending license, supported
server/client and Spring Boot compatibility tests. OpenSearch is an alternative with
its own matching client, not assumed wire-compatible under an Elasticsearch client.
No search service exists on main. Do not add vectors before BM25 evaluation justifies them.

## ADR-003 — Strangler and schema ownership

Keep FastAPI, API aliases and Next.js unchanged. Route explicitly selected parity-tested
endpoints to Java; API_INTERNAL_URL currently switches the whole backend, so a dispatcher
or deliberate proxy mapping is needed for endpoint-by-endpoint routing. No shadow writes.
Alembic owns current schema until a reviewed migration handover; Flyway first targets a
fresh isolated database. Never run competing migration owners. Roll back routes before
schema changes; prove backup/restore and compatibility before moving writes.

## ADR-004 — Kafka modes

Kafka 4.x is KRaft; use AdminClient metadata quorum and broker/topic/partition checks.
Support ZooKeeper only in a separate verified legacy fixture/connector profile. A
mentioned ZooKeeper error does not establish that a KRaft cluster depends on it.

## ADR-005 — Direct official OpenAI SDK

Use com.openai:openai-java directly as a Spring-managed client, environment model IDs,
Responses function-call continuation and explicit storage policy. No obsolete starter,
no browser calls, no arbitrary commands. Port PR #3 intent with strict schemas, whole-turn
budgets, validated citations and typed fallback. Pin SDK only after compile/contract tests;
real provider smoke is opt-in, paid, and NOT_CONFIGURED until usable credentials exist.

## ADR-006 — Modular monolith and five layers

Start one deployable Java monolith, not speculative services. Platform modules: api,
domain, incidents, evidence, connectors, health, observability, ai, agent, knowledge,
security, policy, persistence, workflows, config. Keep Observation, Evidence, Hypothesis,
RootCause, RemediationPlan, Approval and Verification distinct. Infrastructure owns data
services; Model owns provider adapters; Platform owns authorization/tools/persistence;
Agent owns detect/correlate/investigate/plan/verify; Application is existing Next.js.

## ADR-007 — Health and execution separation

Prioritize honest integration health and simple safe local scripts. Liveness depends
only on process; readiness on essential core API dependencies; diagnostics on optional
connectors. Model configuration presence is not live health. No billed probe by default.
Production remediation remains disabled pending independent identity, authorization,
approval, allowlisting, expiry/idempotency and independent post-state verification.

## ADR-008 — Audit scope and release gate

Prompts 00–01 add rules, source-backed artifacts and plans only. Record baseline failures
without modifying the app to make the audit pass. Critical Next.js findings block release;
no migration phase is started in this task. Python and Next.js remain the rollback path.

## ADR-009 — Prompt 02 verified scaffold (2026-11-17)

User explicitly authorized Prompt 02. Maven Central contains stable Boot 4.1.2;
resolve and test it on JDK 21 rather than using the 4.2 release candidate. Use Maven
Wrapper 3.9.11/3.3.4 with distribution checksum; BOM-managed versions and explicit vendor
pins are captured in prompt02/dependency-tree.txt. Use JDBC dependencies without a
DataSource or Flyway runner until Prompt 03; no schema ownership changes now.

Compatibility experiment: JUnit 5.13.4 fails Spring 7's ExtensionContext.Store usage
with NoSuchMethodError. Choose Boot-managed JUnit 6.0.3/Jupiter as the supported
alternative; preserve failure evidence. OpenAI client has close() but is not
AutoCloseable, verified by javap and client-construction test. The SDK bean declares
close as its destroy method. OTel is installed but export disabled by default.

A health-only scaffold keeps top-level platform contract keys, explicitly marks
unmigrated behavior and leaves measurements null. It must not receive existing UI
traffic. Readiness excludes nonexistent persistence and optional connectors. Restrict
Actuator exposure, deny unknown routes, require an external bearer secret for metrics,
keep forwarded-header trust off, and require external TLS bundle in enterprise mode.
No enterprise identity or production write authorization is implied.

Use a separate opt-in Compose overlay and host port; preserve base Compose/Python/UI.
The non-root digest-pinned runtime image packages the locally verified jar. Rollback:
stop java-api only; all current traffic and migrations remain owned by Python.

## ADR-010 — Prompt 03 persistence and parity

Use explicit Hikari/JDBC and typed incident/request/evidence/action contracts with
the Python-compatible JSON payloads. Flyway V1 mirrors bf50bd87cb88; V2 is additive
idempotency/execution uniqueness. Java has a separate fresh schema/database; never
adopt public or Alembic-owned data. Tests prove ownership refusal and transaction
rollback. Tenant-scoped advisory locks serialize mutations and keep audit, approval,
verification and request-key writes atomic. Optional Idempotency-Key responses are
durable; changed payload reuse returns 409. All infrastructure actions remain absent;
only simulated telemetry can be repaired after stored approval.

Retain Python as the default and data owner for existing records. Compare actual
Python fixture traces against Java/PostgreSQL through MockMvc, retaining semantic
values and order where specified. Do not fabricate provider equivalence: Java's
model-enabled investigation remains fallback until its own stage; users needing
provider behavior keep that workflow on Python. Unknown-error details remain
sanitized; input validation uses 422 but does not echo Pydantic's sensitive payload.

## ADR-011 — Prompt 04 search backend compatibility

Choose OpenSearch 3.4.0 and official opensearch-java 3.11.0 (official compatibility
matrix supports client 3.x with server 1–3 and JDK21). Elasticsearch 9.5.5 was
attempted, but this runtime lacks /proc/self/coredump_filter, which its bootstrap
unconditionally requires. 9.4/9.2 source has the same requirement. Do not fake that
OS interface or disable integrity verification. OpenSearch started with authentication
enabled in an isolated local test. Remove the now-unused Elasticsearch SDK dependency
rather than assuming cross-vendor Java-client compatibility.

Use Fluent Bit's OpenSearch output for normalized log files and OTel Collector for
OTLP metrics/traces. OTel rotates native trace batches to a private volume, then
Fluent Bit forwards them to a separate trace index; Prometheus scrapes the OTel
metric exporter. Native traces are not exposed as a cross-tenant query API. Logs
use the normalized evidence contract and a fixed configured index; Java holds only
reader credentials and enforces a server-bound tenant and bounded time/service/page
filters. Admin bootstrap and writer data-plane identities are separate.

Version-1 strict mappings and an ingest pipeline reject malformed identity/schema,
redact source text, stamp actual ingestion time and preserve event time/skew flags.
Create-only stable document IDs deduplicate retries. No default search-data deletion:
retention attachment requires explicit operator confirmation. Queue/rotation limits
are visible local-demo settings, not an exactly-once production guarantee.

Profiles are isolated demos with loopback publication and backend-only credentials.
Enterprise evidence service startup/access remains disabled until identity/TLS review.
Existing Python/UI defaults and production execution stay unchanged.

## ADR 012 — Separate read-only diagnostic health from restart decisions

Accepted for Prompt 05. Introduce typed service probes with bounded concurrency,
queue, deadlines, jittered transient retry, per-service circuit cooldown and
optional interval refresh. Never make external Kafka/search/GPT health part of
Spring liveness. Core readiness keeps the dedicated database/schema checks.
Optional missing integrations remain explicit, and a requested missing service
fails its specific CLI check. Cached delivery is labeled; stale cache is UNKNOWN.
Diagnostics never produce Kafka records, mutate infrastructure, invoke recovery
or spend provider tokens. An explicit separate local fixture seeder verifies a
random owned cluster identity. Production identity remains fail-closed.

The root Bash/PowerShell wrappers share a small Python standard-library operations
core rather than duplicating lifecycle policy. Local-only Docker, fixed service
allowlist, checkout/project ownership, generated private credentials and volume
labels bound its scope. `down` preserves data; destructive reset requires the
spelled-out confirmation flag and prints exact volume names. A dedicated Compose
file avoids altering the existing Python/Next default deployment. Cached build
fingerprints avoid unnecessary rebuilds/replacements on unchanged `up` runs.

A LIVE service response does not make its incident data production data: sample
logs, metrics, traces and recovery stay SIMULATED. A single-node yellow search
cluster is acceptable; absent/stale evidence and missing traces are degraded.
Provider health checks presence only unless a user explicitly runs a cost-bounded,
rate-limited live probe. The known Next.js audit failure stays a release blocker
independent of a healthy local runtime. Native Windows validation is NOT_TESTED.

## ADR 013 — Trusted read-only, version-aware connector SPI

Accepted for Prompt 06. Keep `DiagnosticConnector` in the Java Platform layer,
implemented through official native clients or fixed documented GET contracts.
Infrastructure supplies endpoints/credentials, Model has no credentials, and Agent
capability exposure is a later stage. A declared capability never implies that a
live cluster was tested. Default credentials are absent; unsupported/unconfigured
capabilities remain NOT_TESTED/NOT_CONFIGURED. API callers cannot supply endpoints,
SQL, tenant, raw payloads or commands. A distinct read token and server-side service
allowlist gate the demo API; production identity remains fail-closed.

Kafka config and metadata API evidence determine KRaft/legacy mode. The ZooKeeper
adapter refuses KRaft and unknown modes before any ZK request, and is intentionally
restricted to an isolated loopback legacy fixture. Separate simulated signatures
record the two dependency chains without implementing Prompt 07 correlation.
A real ZooKeeper server plus simulated legacy Kafka metadata tests this guard;
it is not represented as a real legacy Kafka broker deployment.

The local Kafka health principal is separate from the setup principal and has only
Describe/DescribeConfigs metadata permissions, not Produce/Create/Alter/Delete.
Owned-lab fixture setup remains a separate explicit CLI action, verifies cluster
identity, ensures ACLs idempotently and avoids duplicate seed records/offsets.
Negative authorization tests prove topic creation and record production are denied.
Testing found Apache Kafka's default `/tmp/kraft-combined-logs` was outside the
mounted named volume; set `KAFKA_LOG_DIRS=/var/lib/kafka/data` explicitly and verify
five records/committed offset five survive down/up. Earlier fixture records were
simulated; no existing Python or user data was migrated or deleted.

HMS and HiveServer2 are distinct probes: SASL-protected Thrift namespace metadata
versus optional JDBC `SELECT 1`. Kudu uses official authenticated encrypted RPC;
it does not pretend that master discovery proves every quorum/replica or exact
server release. REST adapters return selected aggregates plus capability gaps.
Optional Ranger remains unsupported until a distribution/version contract is
verified, rather than inventing an endpoint. Scoped reads can pass while unobserved
streaming/queue-age/worker/quorum dimensions remain explicitly unverified. Private
Kerberos/mTLS/scopes and production readiness require independent validation.
