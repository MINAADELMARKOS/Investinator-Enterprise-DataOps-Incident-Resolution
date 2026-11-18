# Architecture decisions

Initial decisions dated 2026-11-17; subsequent stage dates are recorded with their reports. Accepted direction, implementation pending unless stated.

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

## ADR 014 — Immutable graph observations and conservative causal hypotheses

Prompt 07 uses Java-owned PostgreSQL graph revisions/evidence with explicit
incident bindings, independent from the legacy topology projection. Relationship
direction defines impact traversal; validity and discovery times gate edges.
Public fixture imports require a separate admin token and SIMULATED provenance.
Production discovery is not invented from seeded entities or model text.
Ranking uses distinct reachable components, temporal precedence/skew, bounded
intensity, recovery and contradictions. Repeated lines cannot vote for high
confidence. Source modes are not combined for corroboration. Confidence is a
qualitative evidence band; RootCause remains distinct and unconfirmed. No model,
remediation or external infrastructure mutation is involved. Default frontend/
Python behavior is preserved; typed graph projection is explicitly configured.

## ADR 015 — Stateless Responses protocol, private continuation, read-only capabilities

Prompt 08 uses the official Java client bean with explicit store=false, encrypted
reasoning inclusion, zero transport retries and bounded requests/tokens/time/rate.
All supported output protocol items are retained across function loops and turns;
unknown output kinds are rejected rather than silently omitted. Private state uses
AES-GCM with tenant/conversation authenticated data; public history stores only
redacted user text and final assistant text. Model reasoning is not public audit.
Missing optional key/model does not prevent core startup, superseding the scaffold's
earlier model-required startup validation. A state key is required for enabled chat.

Caller tenant/incident/service scope is bound on the server. No model-controlled
endpoint, SQL, shell, approval or remediation exists. Exact citation source/time/
identity is revalidated, including persisted read-tool evidence. Simulated incident
observations plus actual diagnostic reads are labeled MIXED; no live-source claim
is inferred from merely calling a real model. Provider errors and deterministic
fallback stay explicit, with legacy response fields/audit behavior preserved.
Admission precedes taking database connections so provider latency cannot fill the
pool with waiting chat requests. CI forces provider calls off except explicit
loopback WireMock test configurations. Paid probing is solely explicit CLI --live,
uses the Java SDK and no incident data, and is locally rate limited. The no-key
acceptance case is NOT_CONFIGURED, never a fabricated successful provider proof.

## ADR 016 — Reviewed BM25 corpus with authoritative citation revalidation

Prompt 09 keeps corpus revisions and outcomes in the Java-owned PostgreSQL schema;
OpenSearch is a replaceable BM25 projection. SQL approval and index publication
are separate states. Tenant-specific content hashes, deterministic chunks and
create-only writes make retries safe. SQL revalidation of every native hit/citation
prevents stale ACLs, revoked versions or tampered projection content being trusted.
A revocation is a tombstone across earlier revisions; model continuation is reset
when policy epochs change. Specific version/mode advice outranks generic advice.

Separate ingestion, feedback and reviewer credentials enforce review mechanics;
quarantine cannot self-approve. Reviewed feedback remains historical user evidence.
Validated-action promotion additionally requires actual stored resolution/execution
records, not a model claim. No automatic learning or vector infrastructure is
introduced without evaluated need. Raw config text is redacted before hashing.

Eight implementation-authored cases provide reproducible functional measurements,
not an independent gold set. Human review is explicitly pending and blocks empirical
quality sign-off. An automated role-boundary test is never described as human review.
Production identity/security and provider validation remain separate gates.


## ADR 017 — Preserve both histories and apply Architecture v2 from 09A

User authorized 09A repository reconciliation and feature publication only.
Preserve original Java commits behind a local backup, then normally merge fetched
main rather than regenerate/rebase implementation. Main's Python GPT/search and
Next.js 16.4.5 security patch are authoritative for those paths; Java private-schema
ownership and opted-in lab remain unchanged. Keep historical reports immutable
and distinguish their old security findings from the current clean npm audit.
The original OpenAPI differs only by an added route description, recorded in a
separate synchronized snapshot with a strict structural comparison.

V2 makes human gold review a measurement gate, not a development synchronization
blocker. Do not invent sign-off, provider calls or hosted CI success. Git repository
push and GitHub API access are separate capabilities; a proxy CONNECT denial is
not proof that the backing credential is invalid. No production/main merge or
configuration publication is authorized by 09A.


## ADR 018 — Evaluation provenance and independent review remain separate

09B preserves the original eight-case dataset and adds a separately hashed review
fixture manifest with exact graphs, scoped evidence, proposed competing labels and
counterevidence. Tests capture actual local SQL/native BM25 outputs and canonical
corpus/chunks; a standard-library scorer independently checks hashes, references,
tenancy, versions and explicit denominators. Missing denominators return null,
never perfect scores; healthy cases are excluded from fault-cause accuracy. Do not
change algorithms, weights or thresholds to fit these labels. No vector work is
justified without independent/held-out evidence of a material benefit.

All labels remain developer proposals. Expert metrics and approvals stay unfilled;
reject attempts to launder a fixture input as human-approved, live or production
quality. Functional isolation/citation/abstention failures make the scorer fail;
fixture ranking metrics do not confer independent review. Stale SQL-rejected index
records remain visible as partial/degraded, and the extra irrelevant BM25 hit is
reported. Preserve old audit artifacts while linking current security status.

No key/model and no approved budget means no provider traffic. Extend mocked SDK
multi-call/timeout tests without claiming a live result; preserve encrypted history,
read-only capabilities and independent authorization. User requested local 09B
commit only; no new hosted validation or publishing claim. V2 allows subsequent
flagged/read-only development on explicit request while human/provider quality
claims and enterprise deployment stay gated.


## ADR 019 — Durable replay monitoring, independent native health, no implicit spend

Use the existing private PostgreSQL store as queue/checkpoint/outbox. Per-tenant
transactions and an active-scope unique index make parallel processing one logical
incident without a new Kafka consumer/microservice. Capability/mode checks precede
thresholds; missing values remain UNKNOWN. Component scope groups related symptoms;
sliding event-time windows/hysteresis/cooldown avoid flapping, preserve late evidence
and allow new causes after closure. Immutable graph/time correlation remains a
hypothesis and never resolves an incident. Monitoring IDs cannot reuse the legacy
analyzer's deterministic API IDs across graphs.

Health reports are actual bounded native samples stored separately from simulated
replay incidents. Live semantic ingestion needs verified graph/identity adapters;
public events cannot claim LIVE. Operator-only bounded chat follows durable gated
investigation intents; no automatic spend, even when keys appear. LOCAL_FAKE is the
only default delivery, console explicit opt-in, remote recipients unsupported.
New lifecycle maps to old statuses and public resolution is denied; independent
verification must own success. Retain queue/evidence/offsets on rollback.


## ADR 020 — Separate governed ReplayLab execution and independent verification (2026-11-18)

Only two named low-risk simulation actions exist. Separate fixed capability roles,
immutable plan/definition/target/parameter hashes, approvals <=120s, proposal expiry,
unique keys, <=10s execution lease, version fencing and a transactional effect
receipt constrain execution. A durable claim commits before the effect; lost ack
and partial completion become explicit manual states, never automatic retries.
No enterprise executor is registered and non-demo writes fail closed.

Verification is a separate role and fresh sampler reading actual current simulator
state/version after acknowledgement, not an executor return/body-provided success.
Only this post-state moves incident lifecycle to resolved. Named rollback is a new
plan with its own approval/key/lease/sample; verified restoration closes unresolved
because it restores the fault. Postmortems are separate, review-pending records.

Explicitly triaged SIMULATED monitored disk incidents may create an owned ReplayLab
sandbox, never act on a live broker. Existing legacy routes remain compatible for
ungoverned incidents and deny bypass once a governed plan exists. Preserve old
replay payload fields and record server-bound actors/correlation IDs. Demo token
roles do not pretend to be independent people; real identity, risk-dependent
two-person change policy and external audit/HA/DR remain production prerequisites.

## ADR 021 — Explicit backend and capability-bound demo console (2026-11-18)

Python remains default. Java selection is explicit per Next server, independent of
schema ownership. Fixed server proxy permissions bind expiring signed demo roles
to backend-only capabilities; Origin must equal an explicitly configured public
origin when container addressing differs. No forwarded identity or tenant override.
No graph/knowledge write capabilities reach the web server. Public login/liveness
allow native readiness without exposing operational pages. Retain current design.

Chat persists only public turn metadata and rechecks current evidence/knowledge
access on inspection; private continuation remains private. Display measured usage
only. Missing/stale health becomes UNKNOWN, not seeded-score health. Bounded
monitor controls persist separately from versioned thresholds. Governed UI mirrors
server-owned immutable plans and distinct capabilities; independent current sampling
is mandatory. Rollback frontend routing without deleting schemas or evidence.

## Prompt 13 checkpoint

ADR 022 (2026-11-18): keep fixed scoped lab commands, representative legacy separate from actual KRaft, repeatable durable event identities and fresh-checkout ownership/ports. CI publishes only sanitized fixed artifacts and fails dependency scans on errors instead of laundering unavailable OSV as clean. Next.js 16.4.5 lock and Python default persist. Cached local reproduction is distinct from cold hosted runner proof.

Prompt 13 local final: fresh-checkout full acceptance and all eight owned outage/recovery cases PASS; additional actual bound-graph browser run PASS (14 checks). See prompt13/fresh-checkout.json and outages.json. Local Java OSV audit remains BLOCKED; hosted CI pending. No production readiness or live GPT claim.

## ADR 023 — Audit-driven codec pins and aligned OTel BOM (2026-11-18)

Hosted OSV exposed seven advisories rather than accepting an unavailable local
scan. Preserve failures, add safe public package/advisory annotations and fix
Snappy 1.1.10.9, LZ4 1.11.4 plus stable OTel BOM 1.67.0 (instrumentation already
2.33.1). Full 107-test/native/browser/E2E regression and hosted 233-package scan
pass with zero findings. No severity suppression or gate bypass. Reassess pins
when the parent BOM changes. Default-tree audit does not certify optional inactive
profiles, private connectors, identity or production security.

## ADR 024 — Readiness is evidence-gated, not inferred from a demo (2026-11-18)

Keep non-demo application ingress hard-blocked while OIDC/RBAC and independently
identified approval remain absent. Close the platform-health exception. Loopback demo
bindings preserve Python/local URLs without enterprise exposure. Enforce request byte
limits during streaming and Secure cookies from trusted public HTTPS origin.
Fixed metric labels omit private identifiers; missing token data is unknown. New
outbox/queue age gauges and explicit NaN alert tests cannot promote absent data to
health. A private generated lab metrics capability supports direct authenticated
capture; collector-only Prometheus scraping is explicitly not Java scrape coverage.

Snapshot backup/restore is tested only into a disposable target; no production RTO,
PITR, HA or financial return is inferred. Proposed SLOs/rates/ROI stay unapproved or
null until measured/reviewed. Enterprise principal/certificate/realm/rotation tests,
long-duration/replica loss, offsite encryption, data governance and durable financial
caps remain release blockers. No synthetic quality sign-off or automatic deployment.

Prompt 14 final evidence: all five hosted jobs PASS on implementation 8e41c2c,
run 44397526897, including full lab/restore/operational capture and fresh OSV
233 packages / zero findings. Java spans verified in-memory without private text.
Production remains BLOCKED per prompt14/RELEASE_RISKS.json; no subsequent stage.
