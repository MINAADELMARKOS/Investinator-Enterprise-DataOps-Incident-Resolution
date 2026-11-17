# InvestiNator — Java AI Architecture, Health Automation & Root Cause Investigation

**Prompt chain for a coding agent (Codex / Cursor / Claude Code) with access to the repository.**

Updated: 15 November 2026. This is an implementation specification and reusable sequence of prompts; it does **not** claim the Java migration has been completed.

## Mission and verified starting point

Repository: https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution

Prior Python GPT/log integration proposal: https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/pull/3 (open when checked). Review its status again before starting.

Existing foundation includes a Python FastAPI backend, Next.js UI, PostgreSQL, local Kafka KRaft demo, Prometheus/OpenTelemetry, ReplayLab simulations, controlled demo approvals, and a partial/branch-specific GPT integration. Do not confuse this foundation with a production deployment. The GitHub Pages UI is a static showcase. Build on the actual repository; never invent integrations or test results.

**Product goal:** Detect distributed-system incidents across platforms, distinguish symptoms from originating failures, correlate log/metric/trace/config/dependency evidence, produce explainable root-cause hypotheses, recommend safe next checks, facilitate approved remediation, verify recovery, and improve operational reliability and business outcomes.

**Five layers:** (1) Infrastructure (Docker Compose; PostgreSQL; Kafka; Elasticsearch/OpenSearch; OTel; Prometheus), (2) Model (OpenAI GPT and optional embeddings), (3) Platform (Java 21 + Spring Boot; official OpenAI Java SDK; tool registry; RAG; security; health), (4) Agent (detect/correlate/investigate/plan/verify), and (5) Application (Next.js chat, incident room, dependency map, health console).

**Reference version policy:** Java 21; Spring Boot 4.1.2 was a stable release as of the authoring date (verify starter/driver compatibility); official `com.openai:openai-java` (verify current supported version and use directly as a bean, not the obsolete legacy Spring Boot 2 starter). Use Maven Wrapper for reproducibility. Pin runtime/container images and dependencies after compatibility tests. No fake working code or fabricated SDK methods.

---

## How to use the chain

Start with **Prompt 00**, then issue **Prompt 01**. Continue one prompt at a time, in order. Each prompt's acceptance gate is mandatory. If work spans coding-agent sessions, the next session must read `docs/implementation/STATE.md` and `docs/implementation/DECISIONS.md` before changing code. Do not advance on a failed gate. Use a feature branch, commits per phase, and a PR for review; never push or merge into main without approval.

The coding agent must implement and test the stage, not merely write suggestions. Missing credentials or cluster access must be reported as `NOT_CONFIGURED` or `NOT_TESTED`, never replaced by fake pass results.

---

## PROMPT 00 — Permanent execution contract / system prompt

> You are the Principal Java Platform Architect, Senior DataOps Engineer, Distributed Systems SRE, AI Agent Engineer and Security Lead for InvestiNator.
>
> Inspect the real repository before any changes. The latest main branch and existing feature PRs are the sources of truth. Keep the working Next.js design, operational APIs, data, audit, approval semantics, and demo scenarios. Migrate the backend from FastAPI to Java 21/Spring Boot incrementally (strangler pattern), without deleting the Python backend until contractual parity is verified and a switchover is approved. Avoid speculative microservices; implement a modular Spring Boot monolith first.
>
> Apply five layers: Infrastructure, Model, Platform, Agent, Application. Treat `Observation`, `Evidence`, `Hypothesis`, `RootCause`, `RemediationPlan`, `Approval`, and `Verification` as distinct typed domain objects. An error appearing in Kafka may originate in an upstream network, KRaft controller, ZooKeeper (legacy Kafka only), disk, storage, security, or client dependency. Never equate correlation with causality.
>
> All execution must be repeatable: use pinned dependencies, Maven Wrapper, Docker Compose health checks, safe scripts, deterministic fixture seeding, and idempotent operations. Never silently delete volumes or data. Never generate or execute arbitrary commands from GPT. Read-only diagnostics by default. Production changes remain disabled; any future write must pass authenticated authorization, approval, allowlisting, and post-action verification.
>
> For every feature: add unit, integration, negative, and regression tests; run them; report real outputs. Do not claim a real OpenAI call, Elasticsearch ingestion, Spark query, or cluster health check when not verified. Show `LIVE`, `SIMULATED`, `CACHED`, `NOT_CONFIGURED`, or `NOT_TESTED` provenance. Keep all credentials in backend-only environment/secrets; avoid logging prompts containing secrets. Configure OpenAI model IDs from environment; never hardcode a guessed API model.
>
> Every stage must: (1) inspect relevant files, (2) explain an architecture decision and migration risks, (3) implement the change, (4) run available tests, (5) update `docs/implementation/STATE.md` and `DECISIONS.md`, (6) report changed files, verified tests, unresolved failures, and commands to reproduce, (7) stop until the next prompt. Do not ask me to approve routine safe coding work; preserve approvals for production writes, destructive changes, billing, and security-sensitive operations.
>
> Quality gate: no mocked success described as production health, no invented tool responses, no credentials in code, no unsafe shell execution, no breaking old API behavior without migration and contract tests, no skipped high-severity dependency vulnerabilities.

**Definition of done:** Agent understands the contract, identifies repo/branch, and creates or updates the implementation-state files before starting migration.

---

## PROMPT 01 — Repository audit, baseline, architecture description, business case

> Inspect the entire InvestiNator repository, main branch and PR #3. Produce a factual **as-is inventory** of modules, APIs, database schemas, Docker services, CI checks, secrets usage, tests, alert workflows, GPT integration, demo simulations, and missing live adapters. Determine the status of prior GPT work and whether it should be ported or refactored; do not blindly cherry-pick obsolete Python implementation into Java.
>
> Record all current public API routes, request/response schemas, behavior and frontend consumers. Generate an OpenAPI contract snapshot and a read-only SQL schema inventory; record baseline `docker compose config`, test, lint, build, and dependency-audit results. Identify critical Next.js security findings and treat them as release blockers. Separate *already implemented*, *partially implemented*, *simulated*, and *not implemented* capabilities.
>
> Write a business problem statement, user personas (Data Engineer, SRE, Admin, Incident Manager), functional and nonfunctional requirements, and target KPIs: MTTD, mean time to identify, MTTR, false positive rate, evidence citation validity, AI cost per investigation, triage hours saved, incident recurrence, change failure rate, availability, and auditability. Do not invent baseline measurements.
>
> Produce C4 context/container/component diagrams plus data flow, deployment, and operations viewpoints; create ADRs for Java 21/Spring Boot, Elasticsearch versus OpenSearch (compatibility-based choice), Python-to-Java strangler migration, Kafka KRaft/legacy ZooKeeper support, direct OpenAI Java SDK, and an initial modular monolith.
>
> Create `docs/implementation/STATE.md`, `DECISIONS.md`, `API_PARITY_MATRIX.md`, `HEALTH_MATRIX.md`, `GAPS.md`, and `RISK_REGISTER.md`. Add a stage-by-stage dependency plan with rollback points. Do not migrate business logic yet.

**Gate:** Versioned baseline exists, API inventory is grounded in code, architectural decisions are reviewable, security blockers are named, and baseline failures are not concealed.

---

## PROMPT 02 — Scaffold Java 21 + Spring Boot backend, without breaking Python

> Create a new `backend-java/` Java 21 Spring Boot module using Maven Wrapper, pinned verified Spring Boot stable version, layered/modular package boundaries, proper Dockerfile, configuration profiles (`local`, `demo`, `integration`, `enterprise`), and externalized secrets. Preserve the Python backend as an independent service for the migration window.
>
> Suggested packages: `com.investinator.api`, `.domain`, `.incidents`, `.evidence`, `.connectors`, `.health`, `.observability`, `.ai`, `.agent`, `.knowledge`, `.security`, `.policy`, `.persistence`, `.workflows`, `.config`.
>
> Dependencies: Spring Web MVC, Validation, Actuator, Security, JDBC/JPA as justified, PostgreSQL, Flyway, Micrometer Prometheus, OpenTelemetry instrumentation, Apache Kafka clients, official Elasticsearch Java client matched to the server, `com.openai:openai-java`, JUnit 5, Mockito, Testcontainers, WireMock. Prefer direct vendor SDKs and explicit adapters; do not add a framework merely to have an 'agent platform'. Verify all dependencies with Spring Boot 4 and Java 21; if incompatible, document a secure supported alternative.
>
> Implement minimal `GET /actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus`, and `/api/v1/platform/health` (same old external contract where practical). Add structured JSON logging, correlation IDs, exception mapping, validation, no credentials in logs, TLS/proxy configuration appropriate to environment, and conditional connector wiring.
>
> The service must compile, run locally in Docker and pass a simple JUnit + HTTP smoke check before proceeding. It should coexist with the Python API behind explicit environment-configured routing; no live traffic or data schema ownership change yet.

**Gate:** `./backend-java/mvnw -f backend-java/pom.xml verify` (adjust wrapper path as needed) passes, Spring Actuator probes work, Java container starts, and Python API still works.

---

## PROMPT 03 — Migrate persistence, API contracts, and workflow behavior

> Port the existing PostgreSQL domain and API behavior into Java while preserving customer-visible endpoints and data. Start with Incident, Alert, Telemetry, Topology Entity/Relationship, Evidence, Runbook/Knowledge, Conversation/Message, AI Usage, Replay, Approval, Audit, and Evaluation. Introduce typed Java records/classes and explicit enums. Keep evidence provenance, tenant ID, source timestamp, incident lifecycle, and simulation flag.
>
> Create Flyway migrations based on the inspected Alembic schemas. Use a fresh test database first. Implement migrations with forward/backward compatibility and a documented rollback; do not let Python Alembic and Java Flyway concurrently own the same schema. Implement database-backed idempotency and transactional behavior. No destructive migration against existing data without reviewed backup and migration plan.
>
> Port read-only endpoints first; then incident creation, replay and approval flows, preserving `/api/v1` contracts. Use Spring MockMvc and contract tests comparing Python versus Java responses for seeded fixture data, including negative and authorization cases. Ensure Next.js can switch backend via `API_INTERNAL_URL` without redesign. Keep the Python service running as fallback until parity and regression tests pass.

**Gate:** API parity matrix is complete for migrated endpoints; data survives API restarts; tenant, approval, and audit regression tests pass; production execution remains disabled.

---

## PROMPT 04 — Log/metric/trace collection and normalized evidence

> Implement a portable observability ingestion architecture using Fluent Bit or OpenTelemetry Collector (evaluate integration and ownership) forwarding normalized, timestamped logs into Elasticsearch/OpenSearch and metrics to Prometheus. Integrate Java with a typed Elasticsearch/OpenSearch client matched to the chosen backend version. Separate **data-plane ingestion** from **control-plane investigation APIs**.
>
> Model normalized evidence fields: `evidence_id`, `tenant_id`, `environment`, `source_mode`, `service.name`, `service.version`, `host.name`, `resource_id`, `cluster_id`, `log.level`, `event.original`, `@timestamp`, `ingested_at`, `trace_id`, `span_id`, `incident_id`, `technology`, `operation`, `error.signature`, `error.category`, `sensitivity`, `ingestion_lag_ms`, and source URL/record pointer. Preserve raw source where allowed; redact secrets/PII before model transfer. Deduplicate by stable source ID with event time and index uniqueness. Handle out-of-order events, clock skew, rate-limiting, backpressure, timeouts, schema changes, and retention.
>
> Provide a small real local Elasticsearch/OpenSearch profile in Compose, plus **simulated** multicomponent log fixtures. Expose bounded, tenant-filtered, read-only search tools with time-window and service filters, pagination, verified evidence IDs and timestamps. Reject user-provided raw URLs or unrestricted DSL. Build an ingestion functional test that sends a synthetic log event through the actual collector into the actual search index, then retrieves it through Java. Make connector credentials/configuration explicit; never equate a real Elasticsearch query with real production incident data.

**Gate:** Synthetic logs visibly reach and can be queried from a running index, strict tenant filtering works, unavailable ES returns a truthful status, and there is no open unauthenticated production endpoint.

---

## PROMPT 05 — Health engine + one-command, safe, idempotent scripts (highest operational priority)

> Build a **universal integration health engine** in Java, plus a user-friendly local CLI wrapper. Health is multidimensional: process/liveness, app readiness, dependency connectivity, functional probe, and distributed-system semantic health. A successful TCP connection is not sufficient. Represent results as `HEALTHY`, `DEGRADED`, `UNHEALTHY`, `UNKNOWN`, `NOT_CONFIGURED`, or `NOT_TESTED` with evidence source, source mode, checked-at, duration, human-readable diagnosis, next safe action, and redacted error type.
>
> Design `HealthProbe` (Java interface), `HealthCheckResult` (typed response), per-connector probes, a `HealthAggregator`, and a bounded parallel runner with timeouts, partial results, retry/jitter, circuit breakers, and interval scheduling. Health must never execute writes or charge OpenAI tokens by default. Distinguish critical mandatory versus optional dependencies. Spring Boot *liveness must not depend on Kafka, Elastic, GPT or other external systems*, to prevent restart storms; readiness only tests dependencies needed to serve core API traffic; diagnostic health checks examine external integrations separately.
>
> Implement health checks with service-specific evidence and thresholds in configuration, not hardcoded values. Minimum checks:
>
> - Java API: liveness/readiness, essential DB access, critical route smoke.
> - Next.js: HTTP readiness + backend proxy route.
> - PostgreSQL: authenticated `SELECT 1`, schema version, pool utilization, persistence roundtrip only in dedicated demo test.
> - Elasticsearch: authenticated cluster health, bounded test search, index mapping and document freshness; treat single-node yellow appropriately.
> - Kafka: AdminClient broker reachability, topic/partition state, ISR/offline partitions, consumer-group lag; inspect KRaft metadata quorum for Kafka 4.x; inspect ZooKeeper only for correctly detected legacy mode.
> - OTel: collector process and actual export/trace arrival to configured backend (when supported).
> - Prometheus: healthy endpoint, scrape target status, a bounded PromQL query.
> - GPT: CONFIGURED/NOT_CONFIGURED based on secrets/model config by default; explicit opt-in paid live API probe (`ai-check --live`) with cost and rate limits.
> - Spark/YARN: REST health, app/driver status, stalled streams, pending containers/queue pressure, if connector configured.
> - HDFS/Hive/Kudu/NiFi/Airflow: distinct service probes if configured; report unsupported capabilities clearly.
>
> Add a root CLI `./investinator` for Bash, and `./investinator.ps1` for Windows, plus small reusable scripts in `scripts/`. Required commands: `up`, `down`, `status`, `doctor`, `doctor --json`, `health --all`, `health --service kafka`, `smoke`, `test`, `logs <service>`, `restart <local-service>`, `demo kafka-kraft`, `demo legacy-zk`, `ai-check --live`, `reset-demo --confirm-destroy-demo-data`. Prefer Compose `up --wait` and `depends_on: condition: service_healthy` where appropriate, bounded retries, actionable error messages, nonzero exit codes on failure, no duplicate resources on rerun. `down` must NOT delete volumes; destructive clean must require explicit confirmation and print the target volume names before deletion. Limit local restart actions to an explicit list of demo services; never connect this convenience CLI to a production cluster.
>
> Define JSON health report schema and document examples. Emit a useful console table, a compact JSON file for CI, and the API `GET /api/v1/health/integrations`. The `doctor` command checks installed tools, port collisions, Compose syntax, free resources, secrets presence (never values), healthy containers, failed migrations, API contract, logs/metrics indexing, and UI proxy routing. Health scripts must be repeatable and have positive and negative tests. Provide scripts that run from the repository root regardless of the current working directory.

**Gate:** From a clean local clone, `./investinator up && ./investinator doctor && ./investinator smoke` produces real reports and stable exit codes; deliberately stop each local dependency and prove the corresponding check identifies it without cascading process restarts; rerunning the commands is safe.

---

## PROMPT 06 — Java connectors for real distributed systems (read-only first)

> Define a version-aware, least-privilege `DiagnosticConnector` SPI with capabilities (`health`, `metrics`, `logs`, `topology`, `config`, `metadata`, `readOnlyChecks`) and adapters for: Kafka, Kafka KRaft, legacy ZooKeeper, Spark (Standalone/YARN REST), YARN, HDFS, Hive Metastore/HiveServer2, Impala (if supported), Kudu, NiFi, Airflow, and optional Cloudera Manager/Atlas/Ranger.
>
> For each adapter: document protocol, endpoint, authentication (Kerberos/SPNEGO, SASL/TLS, OAuth/API key, mTLS as relevant), minimum scopes, connection timeout, retries, API version support, read-only operations, semantic health metrics, limitations, RBAC enforcement, and representative mock + live test. Never assume `localhost` for real clusters or expose administrative credentials to GPT.
>
> Kafka adapter: distinguish KRaft metadata quorum from legacy ZooKeeper using reliable version/config evidence; read broker/controller state, leader/ISR, offsets, consumer lag, under-replicated partitions, and cluster identity with the Java AdminClient. Do not query ZooKeeper merely because the user mentioned it. KRaft for Kafka 4.x, ZooKeeper only in an isolated legacy fixture. Create separate cross-service incident signatures for each mode.
>
> HDFS: check HA NameNode active/standby, missing/corrupt/under-replicated blocks, DataNode liveness, safe mode and read latency; no failover actions from a health probe. YARN: active RM, queue backlog, applications stuck/failed and exhausted resources. Hive: HMS reachability, HiveServer2 query responsiveness using a safe bounded read, metastore/backend distinction. Spark: executor memory, failing stages, streaming backlog, dependency errors. Kudu: master/tserver availability, tablet health and quorum, read-only; no rebalancing. NiFi: process-group status, queue age/backpressure, bulletins, node state. Airflow: scheduler/worker/DAG/task state, failed retries and SLA/freshness effects.
>
> Do not block development on private cluster credentials: use Testcontainers/WireMock/fixtures, return `NOT_CONFIGURED` for unavailable live connectors and describe which endpoint/permission is needed. Do not create a generic arbitrary command-execution API.

**Gate:** Every configured adapter has version-aware, authenticated read-only tests; incompatible/unconfigured clusters are reported truthfully; no connector performs a write during diagnosis.

---

## PROMPT 07 — Dependency graph, temporal correlation, explainable root cause

> Upgrade the NerveMap from a static seeded graph to a typed, directional, versioned service-dependency model. Graph nodes: service, cluster, host, network, broker, controller/quorum, topic, consumer group, Spark job, pipeline, table, dataset, dashboard and business process. Edges: `DEPENDS_ON`, `HOSTED_ON`, `FEEDS`, `WRITES_TO`, `USES_QUORUM`, `PRODUCES_TO`, `CONNECTS_TO`, each with source, confidence, discovered_at, valid_from/to and provenance. Avoid inventing edges.
>
> Build a deterministic correlation engine that aligns timestamps (including time skew tolerance), normalizes error signatures, groups symptoms across correlated components, detects candidate originating causes, and ranks hypotheses using event precedence, graph position, failure intensity, recovery behavior, and contradictory evidence. Keep root cause hypotheses distinct from confirmed causes. Include explicit data gaps and confidence calibration; do not equate symptom count with high confidence.
>
> Run two *independent*, clearly simulated tests: (A) network instability -> Kafka KRaft metadata quorum instability -> broker errors -> consumer lag -> Spark delay -> stale dataset; (B) legacy ZooKeeper session instability -> Kafka legacy broker issues -> downstream lag. The source/version must determine which dependency chain is valid. Also test false attribution: when ZooKeeper is healthy and disk is exhausted, the system must prefer the disk hypothesis.
>
> Each investigation result must have: `incident_id`, observed symptoms, candidate causes ranked, supporting/contradictory evidence IDs and timestamps, unknowns, impact radius, next read-only checks, prevention opportunities, risk, and detected source mode. Produce explainable confidence, not a model-invented numerical certainty.

**Gate:** Cross-service failures identify an upstream candidate with traceable evidence; normal and adversarial counterexamples do not misattribute causes; graph relationships can be queried from Java.

---

## PROMPT 08 — Real GPT API in Java with function calling and conversation history

> Implement the actual **OpenAI Responses API** through the official `com.openai:openai-java` SDK as a Spring-managed `OpenAIClient` bean. Do NOT call GPT from the browser, use the retired Spring Boot 2 SDK starter, or simulate a response while claiming GPT was used.
>
> Port the existing `/api/v1/investigator/messages` contract: `conversation_id`, `incident_id`, `mode`, `message`; preserve conversation history, tenant boundaries, incident context, safe follow-ups, output compatibility and existing frontend behavior. Model ID and API key come from backend environment. Provide model routing by task/severity only when supported by configured model IDs.
>
> Build a complete bounded Responses function-call loop: model receives user question + context; it can request only named read-only Java tools; validate arguments and permissions; execute using actual connector; return each tool result to GPT via its matching call ID; continue until final response, per-turn budget, timeout, or failure. Ensure that all response output items needed for correct stateless continuation are passed forward securely according to the official SDK/API examples. Avoid keeping model reasoning in public logs. Set provider storage policy explicitly; if `store=false`, ensure stateless continuation remains correct.
>
> Provide typed root-cause output: summary, observed evidence, probable originating cause(s), alternatives, missing checks, business impact, recommended read-only checks, preventive recommendations, safe remediation proposal, and `source_mode`. Verify that each cited evidence ID exists and belongs to the tenant/incident. Separate provider unavailable/bad key/rate limit/model not supported from normal success; return honest deterministic fallback with `response_mode`, not fabricated GPT text. Enforce token/time/request budgets, redaction, prompt injection defenses, and no direct remediation tool.
>
> Tests: WireMock/fake OpenAI client (multi-turn + tool-call continuation), prompt injection attempt in retrieved log, malicious fake approval, nonexistent evidence ID, service unavailable, unknown tool, cross-tenant access, no incident selected, actual question used, and follow-up memory. A real paid provider call is allowed **only** as an explicit, opt-in `./investinator ai-check --live` with a configured key/model and small token cap. Never expose credentials or raw provider exception contents.

**Gate:** Live opt-in call returns actual GPT text when credentials are provided, read-only tools execute with real API data, and user follow-up uses persisted history. If no key, report `NOT_CONFIGURED` and show expected steps; do not claim end-to-end live proof.

---

## PROMPT 09 — Incident knowledge, RAG, feedback and operational learning

> Build an ingestion pipeline for versioned runbooks, resolved incident postmortems, known errors, topology snapshots, configuration changes, and validated corrective actions. Store source/owner/time/revision/tenant/ACL, hash and normalized structure. Implement BM25 text search first using the existing search index and add vector retrieval only when evaluation shows need. If embeddings are used, isolate ingestion and online retrieval with model/version awareness. Apply chunking, hybrid retrieval, reranking, ACL filtering and deduplication. No retrieved instruction may override system policy.
>
> For each proposed root cause, return verifiable incident/log/knowledge evidence with readable references and timestamps, distinguish historical advice from fresh observations, and prioritize runbooks specific to the detected platform version and deployment mode. Add user outcome feedback (accepted/rejected root cause, final confirmed cause, whether repair worked); ensure rejected model claims never become unquestioned knowledge.
>
> Build an evaluation set for Kafka KRaft, legacy ZooKeeper, HDFS HA failover, Kudu tablet unavailable, NiFi backpressure, Spark executor OOM, Hive metadata failure, and false positives. Measure retrieval precision and correct causal ranking with a human-reviewed gold set.

**Gate:** Knowledge retrieval is tenant-aware, version-aware, and evidence-backed; negative prompt-injection and unsupported-citation tests pass; automatic knowledge updates require suitable review.

---

## PROMPT 10 — Proactive monitoring, conflict prediction and incident lifecycle

> Make monitoring continuous rather than waiting for a user chat request. Use Java scheduling and Kafka consumers/webhook ingestion as justified, not an LLM call for every log line. Begin with deterministic rules, anomaly signals, time-windowed correlation, incident grouping, deduplication, suppression, escalation, cooldown and alert budgets. An LLM call is invoked only when an incident needs semantic investigation or an operator asks.
>
> Detect conflicts and emerging risk patterns: rising Kafka lag plus shrinking ISR, KRaft controller instability, ZooKeeper quorum events in *legacy* clusters, Spark retry storms, HDFS missing blocks, YARN queue saturation, NiFi backpressure cascading upstream, Kudu tserver failures, missing Hive table partitions, freshness SLA violations, infrastructure disk/network exhaustion, and recent rollout correlation. Use thresholds and source capability awareness; never assume unavailable metrics are zero.
>
> Add a scheduler to rerun `health` checks and record time series in PostgreSQL/Prometheus. Implement active incident lifecycle `new -> triaged -> investigating -> awaiting_approval -> mitigated -> verified -> resolved / closed_unresolved`; avoid repeated incidents for the same active issue. Notify by configured console/email/webhook only after opt-in. Add a safe notification simulation in the local demo. Escalation and severity must account for affected datasets, customers, SLOs, and business criticality; include cost/time impact estimates only when inputs are available.

**Gate:** Given synthetic event streams, incidents are opened, updated and deduplicated automatically, upstream hypotheses change when evidence changes, alert storms are suppressed, and a missing external dependency does not trigger an uncontrolled number of GPT calls.

---

## PROMPT 11 — Controlled remediation, approval, verification and rollback

> Keep model investigation strictly read-only. Implement a separate deterministic `RemediationPolicy` and state machine with action ID, target resource, approved parameter schema, environment, risk classification, required role, explicit human approval, time bound, idempotency key, timeout, preconditions, rollback plan, and post-action checks.
>
> Implement **simulation-only** action executors for the local MVP (e.g., resolving synthetic broker disk pressure). Do not add shell execution, real service restarts, Kafka topic deletion, failover or destructive SQL to a model tool registry. For production, build only interfaces and approval requirements; write integrations stay disabled by default until reviewed.
>
> Verify recovery using independent actual post-state metrics where available. A successful executor return value alone must not resolve an incident. Add failure, approval expiry, parameter tampering, repeated request, partial-success and rollback-needed tests; full audit trail including operator, action, evidence, outcome and correlation IDs.

**Gate:** Unapproved, expired, repeated, cross-tenant and unknown operations are denied; a simulated approved action only resolves when verification passes; production remains read-only.

---

## PROMPT 12 — Frontend: operationally useful Java-backed Next.js experience

> Retain InvestiNator's established Next.js UI and style. Update its API proxy to target the Java backend through environment config without hardcoding URLs/secrets. Build a **System Health** screen and expand Investigator Chat and Incident Room with evidence provenance, real GPT/deterministic fallback labels, model/tool execution status, source timestamps, graph visualization, hypotheses versus confirmed causes, dependency impact, approval workflow, and recovery verification.
>
> Show health statuses individually for API, DB, Kafka, KRaft/ZooKeeper based on actual mode, Elasticsearch, Prometheus, OpenTelemetry, model configuration, and optional big-data adapters. Include `Last checked`, `Live vs simulated`, `Check again`, and actionable reason for degraded state. Expose rerun controls only for the local demo and route them through authenticated policy-controlled server APIs; no browser-triggered production restart.
>
> Provide keyboard-accessible error states and progress feedback. Preserve conversation history and user prompts. Ensure the user can ask a question with no selected incident (general technical guidance) but cannot accidentally present generic knowledge as proof of a current cluster state. Fix reported high/critical frontend dependency vulnerabilities before a production release.

**Gate:** UI shows real Java API results, error/fallback state is visible, Next.js build and dependency security checks pass, and end-user demo flows work.

---

## PROMPT 13 — Repeatable end-to-end laboratory and regression tests

> Build a reliable local E2E test suite based on Docker Compose profiles and Testcontainers (PostgreSQL, Kafka, Elasticsearch/OpenSearch), WireMock for optional remote services, and optional Toxiproxy/netem in isolated tests. Produce complete **simulated incidents over real running infrastructure**, not only static strings. Include a network/KRaft scenario, separate legacy ZooKeeper scenario, disk pressure, missing metrics, consumer lag, Spark pipeline delay, stale dataset and a healthy baseline.
>
> Demonstrate: `./investinator up`, `doctor`, `health --all`, `smoke`, `demo kafka-kraft`, `investigate --incident ...`, `ai-check --live` (opt-in only), `verify --demo`, `test`, `restart api`, `doctor`, `down`. Simulated fault injection must never target production. Ensure all scripts are repeatable, finish within documented timeouts, have useful error codes, and leave data intact unless `reset-demo --confirm-destroy-demo-data` is explicitly used.
>
> Test dependency failure matrix: disconnect ES, stop local broker, make DB unavailable, prevent GPT connectivity, corrupt a log input, attempt a prompt injection, include intentionally stale telemetry, and configure an optional connector with no access. Ensure each failure is visible and classified, the application does not invent results, and no uncontrolled restart cascade occurs. Include golden tests for RCA correctness, evidence citations, idempotency and approval isolation.
>
> Collect test artifacts: service health JSON, Compose service status, test summaries, synthetic incident timeline, expected-vs-observed results, and selected redacted traces. Run backend and frontend CI in GitHub Actions with reproducible dependency locks, Java unit/integration tests, frontend build/audit, Docker configuration validation, and smoke tests.

**Gate:** A newly cloned repository can reproduce all local scenarios from documentation; negative tests pass; quality gates run in CI; failures are surfaced rather than hidden.

---

## PROMPT 14 — Security, operability, SLOs, costs and production readiness

> Perform an enterprise review across each of the five AI layers. Threat model: prompt injection via logs/runbooks, external API data leakage, SSRF through tool URLs, command injection, malicious runbook actions, multi-tenant data leakage, hardcoded secrets, excessive model costs, log ingestion spoofing, weak Actuator exposure, absent TLS, Kafka ACL overreach, Kerberos ticket lifecycle, and denial of service via unbounded concurrent diagnostics.
>
> Implement or explicitly gate: OIDC/RBAC for operators, least-privilege service identities, secrets rotation, TLS/mTLS where applicable, approved outbound egress, encryption, retention, backups/restore, database migration ownership, audit retention, rate limits, circuit breakers, per-tenant budgets and provider data-handling policy. Secure Actuator endpoints; public liveness and authorized detailed diagnostics must not reveal secrets or topology.
>
> Define separate SLOs for API availability, health-check freshness, incident detection delay, root-cause triage latency, valid evidence percentage, live connector availability, and AI cost per investigation. Monitor JVM heap/GC, thread pools, HTTP latency, tool failure rate, token usage, bad answers, drift, fallback rate and alert compaction. Build Grafana dashboards and alert rules with owner/runbook links. Conduct load and failure testing with documented results, not invented benchmarks.
>
> Production release remains **blocked** until identity, credential security, data handling, HA/DR, threat assessment and real connector access are signed off. Document what is safe only for an isolated demo.

**Gate:** CI and security review complete; no critical unresolved dependency advisories; objective production release checklist shows every incomplete item and its owner.

---

## PROMPT 15 — Final handover, developer experience and business impact

> Assemble a complete handover. Update README and architecture views; document local setup, profiles, config variables, Java development, connector setup, OpenAI API billing/preflight, demo seed/replay, health commands, rerun/restart, safe cleanup, CI, known limitations, failure troubleshooting, and production hardening. Create a one-page executive narrative explaining the incident problem, platform value, cost drivers and phased roadmap.
>
> Produce a precise **As-Is versus To-Be** matrix and a build-status matrix for every component: `implemented / tested / partially tested / not configured / simulated / planned`. Show real CI artifacts, Java migration parity, service health results, API compatibility, user-visible GPT status, tested failure scenarios, latency and cost only where measured.
>
> Create a polished E2E demo script for a live presentation: start stack, show `doctor`, inject the clearly simulated KRaft failure, ask GPT why Kafka is failing, show cross-system logs + topology + ranked evidence, propose safe check, demonstrate separate approval for simulated recovery, verify system metrics, and show business impact. Highlight the distinction between GPT being real and fault telemetry being simulated.
>
> Do not merge into main or claim production-ready unless I approve after verifying the release gates.

**Gate:** A reviewer can reproduce the entire demo using documented commands, inspect evidence, understand limits, and know what remains before real enterprise deployment.

---

## Reusable health JSON contract (illustrative schema/output)

```json
{
  "check_id": "hc-20261115-001",
  "environment": "local-demo",
  "checked_at": "2026-11-15T05:00:00Z",
  "service": "kafka-metadata",
  "capability": "metadata_quorum",
  "status": "HEALTHY",
  "source_mode": "SIMULATED",
  "probe_type": "semantic",
  "probe_duration_ms": 47,
  "evidence_ids": ["EVIDENCE-DEMO-001"],
  "finding": "Demo controller heartbeat within configured range",
  "recommendation": "No action required",
  "required_for_readiness": false
}
```

The schema is illustrative; real monitoring checks must report their actual source provenance, timestamp and output. `SIMULATED` must not be used for real metadata quorum observations.

## Health coverage matrix (minimum planned support)

| Component | Technical check | Semantic/functional check | What a failure might mean |
|---|---|---|---|
| Java API | Actuator liveness/readiness | Contract request/DB read | API not serving correctly |
| Next.js | HTTP | Java API proxy | Broken UI/backend link |
| PostgreSQL | Authenticated SELECT 1 | Schema/version/transaction in demo | Data or migration failure |
| Elasticsearch | Cluster status | Index exists, query & freshness | Logs unavailable/stale |
| Kafka | AdminClient cluster info | ISR, offline partitions, lag | Topic/broker/controller impact |
| KRaft | Metadata-quorum description | leader/voters/replication | Controller plane issue |
| ZooKeeper (legacy only) | Mode-aware connection | ensemble health/session events | Legacy metadata coordination risk |
| Prometheus | HTTP readiness | Target/PromQL query | Missing metrics |
| OpenTelemetry | Collector ready | test span export/arrival | Tracing broken |
| GPT | Env configuration | Explicit paid low-token smoke | AI unavailable or not authorized |
| HDFS | HA status/NN HTTP/JMX | block health, active/standby | Storage durability issue |
| YARN | RM REST HA/queue | stuck apps/backlog | Compute starvation |
| Spark | REST app/driver | stages/stream lag | Pipeline execution issue |
| Hive | HMS/HS2 endpoints | bounded query/read | Metadata/query failure |
| Kudu | master/tserver | tablet consistency/lag | Realtime store issue |
| NiFi | HTTPS REST auth | processor/queue health | Ingestion backpressure |
| Airflow | API/scheduler status | DAG/task failed state | Orchestration delay |

## Suggested CLI behavior contract

```text
./investinator up                       # Start/reuse services, --wait for readiness
./investinator status                   # Human readable container/service status
./investinator doctor                   # Diagnostic report; no writes
./investinator doctor --json            # CI/machine-readable output
./investinator health --all             # All configured integration probes
./investinator health --service kafka   # Targeted semantic probe
./investinator smoke                    # Local API/DB/search/metrics functional tests
./investinator test                     # Maven/JUnit + frontend tests + integration suite
./investinator ai-check --live          # Explicit opt-in paid low-token API probe
./investinator demo kafka-kraft         # Safe simulated failure replay
./investinator demo legacy-zk           # Separate legacy Kafka fixture
./investinator logs api                 # Read local API logs
./investinator restart api             # Allowlisted local-only restart
./investinator down                     # Stop only; NEVER delete volumes
./investinator reset-demo --confirm-destroy-demo-data
                                           # Explicit destructive demo-only reset
```

Do **not** use `docker compose down -v` in the normal convenience commands. Treat scripts as a safe operations interface, not unrestricted production automation.

## Official implementation references

- Spring Boot stable releases: https://docs.spring.io/spring-boot/
- Spring Boot Actuator health groups and Kubernetes readiness: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Boot Prometheus metrics: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
- Spring Boot Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Official OpenAI Java SDK and Responses function calling: https://developers.openai.com/api/reference/java
- Elasticsearch Java Client: https://github.com/elastic/elasticsearch-java
- Kafka AdminClient metadata quorum API: https://kafka.apache.org/40/javadoc/org/apache/kafka/clients/admin/Admin.html
- Docker Compose service healthy dependencies: https://docs.docker.com/compose/how-tos/startup-order
