# InvestiNator — version 2 prompt chain after Prompt 09

> Historical target/execution baseline. Java is now published; current checked-in architecture, completed-stage evidence and production blockers are in [the handover](../handover/README.md) and [STATE](../implementation/STATE.md). Original authoring-time status is retained below. No automatic cutover is authorized.

**Version:** v2.0, 2026-11-17. **Purpose:** Coding-agent instructions for the Java architecture transition **after user-reported local completion of Prompts 03–09**.

**Source of truth:** See [Java Architecture v2](JAVA_AI_ARCHITECTURE_V2.md) and [worktree synchronization](SYNC_JAVA_WORKTREE.md). The `feature/java-ai-audit` changes were reported committed **locally**, not verified on remote GitHub. These prompts must be executed against the **real local worktree**, not against a reconstructed imitation. Do not claim that `backend-java/` exists remotely until an actual verified push.

**Order:**
- **09A — Worktree reconciliation + GitHub publication** (mandatory first).
- **09B — Gold-set evaluation, security/health inventory and live GPT validation** (independent review required; some checks may stay BLOCKED).
- **10 — Event-driven monitoring and incident lifecycle**.
- **11 — Policy-controlled remediation, verification and rollback**.
- **12 — Java-backed Next.js operator experience**.
- **13 — End-to-end reproducible lab and hosted CI**.
- **14 — Security, SLOs, cost, performance, release hardening**.
- **15 — Handover, operator documentation, business-outcome measurement**.
- **16 — Optional enterprise connector certification/cutover**, **not** an automatic deployment.

Every prompt below is an independent, copyable coding-agent instruction. Run in sequence; do not let the agent silently skip gates. **GO for development is not equivalent to GO for deployment.**

## Shared execution contract — prepend for every stage

> Act as a Java 21 / Spring Boot Principal Platform Architect, Big Data SRE, and AI Safety Engineer. Inspect the actual branch, `docs/implementation/STATE.md`, ADRs, tests and prior stage reports before changing anything. Preserve the Python default route and its data, current Next.js UX, private Flyway-owned Java schema, replay-only simulator and read-only tools. Maintain the five layers: Infrastructure → Model → Platform → Agent → Application. No production commands, file deletion, unknown network destinations, unapproved GitHub merge, or forced push. Keep `LIVE`, `SIMULATED`, `MOCKED`, `NOT_CONFIGURED`, `NOT_TESTED`, and `BLOCKED` distinct. Never treat mocked GPT tests or fixture RCA scores as live outcomes.
>
> Follow red/green/refactor: record existing failures; make a bounded change; add positive, negative, tenant isolation, idempotency, outage and restart tests; run relevant suites; report output and actual provenance. Don't disable security audits or weaken assertions. Do not assert production readiness without enterprise identity, connector certification and independent evaluation. All writes outside the isolated ReplayLab remain prohibited.
>
> Each stage must update `docs/implementation/STATE.md`, `DECISIONS.md`, `GAPS.md`, `RISK_REGISTER.md`, `HEALTH_MATRIX.md` and the relevant prompt-specific report; show files changed, commit SHAs, commands run, test counts and statuses, unreproduced claims, next gate and rollback. STOP after the stage; do not run the next prompt autonomously. Do not conceal a failure.

---

## PROMPT 09A — Synchronize the local Java platform with GitHub (run FIRST)

> **Goal:** Make the already-implemented Java system visible on a remote feature branch without destroying either line of development.
>
> 1. Inspect `pwd`, `git status --porcelain=v1`, `git branch -vv`, `git log --all --decorate -15`, `git remote -v`, and the actual Java module and implementation evidence. Reject a wrong repository/worktree.
> 2. Record a recovery point (tag or backup branch) and capture uncommitted changes without exposing secrets or modifying user data. Do not reset, rebase destructively, force-push, rewrite another user's history, remove generated reports, or delete volumes.
> 3. Fetch remote `main`, verify the 2026-11-17 Next.js security upgrade to **16.4.5** and merged Python GPT/Elasticsearch code; reconcile with the local `feature/java-ai-audit` via a normal merge, with explicit decisions for any conflicts in `README.md`, `docker-compose.yml`, `apps/web`, workflow files, `src/incident_copilot`, and migrations. Both Python/Alembic and Java/Flyway remain separate owners.
> 4. Verify no release-gate regressions. Re-run Java Maven Verify with Testcontainers, Java connector/integration negative tests, CLI unit and lint, Python pytest/ruff/mypy, frontend `npm ci`, `npm run build`, `npm audit --audit-level=high`, `docker compose config` (all profiles), and documented Java health/doctor/smoke. Record untestable cases as NOT_TESTED.
> 5. Ensure `backend-java/`, `docs/implementation/`, `eval/knowledge_cases.json`, Prompt 03–09 reports, and reproducible nonsecret test commands are committed. Audit for accidentally tracked .env, API keys, tokens, connection strings and certificates before any push.
> 6. Push the completed branch **only after validation** and open a PR against `main`. Ensure hosted CI actually runs Java + CLI + Python + frontend jobs; verify run IDs and logs. If the remote push is unavailable, stop and report the precise reason: don't claim success.
> 7. Produce `docs/implementation/SYNC_REPORT.md`: local vs remote SHAs, merge resolution table, API and schema owners, failing/passing gates, pushed commit, PR URL, CI run IDs, remaining blockers.
>
> **Gate:** Remote Java branch exists, content matches locally committed code, no known conflicts, no secret leakage, all required applicable checks pass or are explicitly flagged. PR remains unmerged, default Python route unchanged.
>
> **Note:** A cloud coding agent with a different checkout cannot recover the local branch from GitHub. In that case STOP and request the operator to attach/push the existing worktree. Do not regenerate purported Prompt 03–09 deliverables.

---

## PROMPT 09B — Independent knowledge review + live GPT preflight (before quality claims)

> **Goal:** Close quality and integration knowledge gaps without inventing evidence.
>
> 1. Inspect `prompt09/GOLD_REVIEW.md`, `eval/knowledge_cases.json`, dataset hashes, reviewed runbook revisions, Prompt 07 temporal hypotheses, Prompt 08 Responses SDK tool loop and Prompt 05 health/doctor reports. Preserve independent reviewer sign-off as PENDING until a qualified human actually signs.
> 2. Build a **review package**, not a synthetic sign-off. Each of the eight cases (KRaft, legacy ZK, HDFS HA, Kudu, NiFi, Spark, Hive, healthy) needs exact input evidence IDs, platform version/mode, event sequence, expected competing hypotheses, contradictions, blind spots, expected uncertainty and reviewer notes. Never tune thresholds against test answers without a held-out set.
> 3. Add scorer diagnostics: detection precision/recall, upstream-cause top-1/top-3, false-positive rate, evidence-citation validity, retrieval recall@k/precision@k, outdated/unauthorized knowledge rate, missing-data abstention and prompt-injection rejection. Split fixture tests, expert-reviewed tests and live observations in separate reports.
> 4. Validate BM25 first; only prototype embeddings/hybrid search behind a flag if a reviewed dataset shows material gain after measured latency/cost tradeoff. Do not call BM25-only RAG a vector RAG system.
> 5. For GPT, use actual provider traffic only with configured credentials and opt-in budget: backend-only secret, model ID from environment, request timeout, retry budget, token cap, and zero production writes. Test two user turns, one authenticated tenant-scoped read tool, multi-call continuation, an invalid evidence ID, provider outage, timeout and non-configured fallback. If no credentials, mark `NOT_CONFIGURED`, not PASS.
> 6. Reconcile Prompt 03/04/06 legacy references to the old Next.js audit: check current lockfile against the patched `main` rather than copying a stale blocker. Review Prompt 05 actual artifacts; if missing record `UNKNOWN`.
> 7. Produce `docs/implementation/prompt09/QUALITY_REVIEW_PACKAGE.md`, `EVAL_REPORT.json`, `GPT_LIVE_REPORT.md`, `PROMPT05_AUDIT.md`; leave the human approval field unfilled until truly reviewed.
>
> **Gate:** Operational evaluation harness works and cannot launder synthetic tests into human-verified accuracy. External provider and expert approval may remain explicitly blocked; development can continue if read-only, flagged and tested.

---

## PROMPT 10 — Continuous proactive monitoring and causal incident lifecycle

> **Role:** Distributed-system SRE and Java streaming engineer. Build the missing monitoring loop on the **existing Java 21/Spring Boot architecture**, not a new Python service or independent set of agent microservices.
>
> **Business use case:** Kafka consumer lag spikes, but the earliest originating fault may be KRaft controller/network; NiFi queues back up from a downstream HDFS outage; a stale dataset is the final business symptom, not necessarily the cause.
>
> **Implementation requirements:**
> 1. Audit existing Prompt 05 health scheduler, Prompt 06 connector capabilities, Prompt 07 immutable topology/time correlation, Prompt 09 knowledge and incident API parity. Define the event contract `SignalReceived` → `NormalizedObservation` → `DetectionCandidate` → `CorrelationWindow` → `IncidentAggregate` → `InvestigationRequested` → `NotificationCandidate`.
> 2. Implement a **deterministic**, bounded, replayable detection/correlation worker using the smallest justified approach (Spring scheduling, Kafka consumer where available, or a durable PostgreSQL outbox); commit offsets or checkpoints only after durable state changes. Account for event-time windows, late/out-of-order arrivals, clock skew, stale/missing data, duplicated deliveries and backpressure.
> 3. Create a tenant/environment/cluster/resource-aware incident fingerprint with idempotent database uniqueness. Deduplicate and update incidents instead of opening a new P1 for every log line. Prevent flapping via cooldown, hysteresis, stable severity progression and suppression budgets; never hide a genuinely new underlying cause.
> 4. Correlate upstream first and preserve competing hypotheses; use declared connector version/mode so **KRaft never queries ZooKeeper**. Surface healthy counterexamples and reasons the data do not establish causation.
> 5. Implement configurable lifecycle `NEW → TRIAGED → INVESTIGATING → MITIGATING/AWAITING_APPROVAL → VERIFYING → RESOLVED`, with `CLOSED_UNRESOLVED` and explicit manual interventions. Only authorized transitions; persist actor, timestamps, source mode and immutable audit. Map to backward-compatible existing API statuses without silently changing clients.
> 6. Trigger the GPT investigator **only** after deterministic candidate gating or an operator request; enforce per-tenant/minute and per-incident budgets, circuit breakers, concurrency bounds, bounded context, and no auto-execution. Do not bill GPT per log record. Mark `NOT_CONFIGURED` provider cases clearly.
> 7. Add optional notifications through an outbox with opt-in console/webhook/email adapters and delivery idempotency. Default to local fake destination; no unknown outbound URL or real recipient by default.
> 8. Add versioned detection rules and a ruleset dry-run, coverage and replay UI/API. Health check cadence must not equate missing signals with zero or healthy.
>
> **Tests:** Kafka KRaft/network causal chain; legacy ZooKeeper case only with declared legacy broker; NiFi/HDFS chain; delayed data freshness; identical duplicate flood (one incident); short/long flapping; restart and offset resume; parallel workers exactly-one logical incident; missing telemetry; wrong tenant; connector 503; out-of-order data; prompt-injection text in log; rate-limited/absent GPT; notification outage; actor/audit semantics.
>
> **Deliverables:** `prompt10/REPORT.md`, `incident-detection-rules.yaml`, explicit event and lifecycle schemas, outbox/offset migrations, metrics/dashboard checks, fixtures, CLI `monitor status|pause|resume --demo`, monitoring failure matrix, benchmark of generated incidents and suppression.
>
> **Gate:** Repeated simulated streams deterministically open/update a single bounded incident with evidence, suppress storms, preserve state across restart, and make no dangerous external action. No auto-notification without explicit config. User-visible status distinguishes live capture from synthetic replay.

---

## PROMPT 11 — Policy-controlled remediation, independent verification and rollback

> **Role:** Enterprise change-control and SRE lead. Preserve the model as a **read-only recommender**; architecture separates `InvestigationAgent`, `RemediationPlan`, `PolicyDecision`, `Approval`, `Executor`, `Verification`, and `Postmortem`.
>
> 1. Audit current persisted Prompt 03 replay and approval state, tenant isolation, idempotency, and Prompt 10 incident lifecycle. Write a threat model for forged requests, model instruction injection, replayed approval, role spoofing, double execution, stale evidence, changed parameters, partial commits, DB outages, timeouts and UI retries.
> 2. Define **fixed action definitions**: action code, target, schema-validated arguments, expected platform/version, environment, risk class, ownership, preconditions, required authenticated role, explicit scoped approval, expiry, idempotency key, execution slot/lease, rollback strategy, postcondition checks and audit correlation ID.
> 3. Only the **named ReplayLab simulation** can execute by default; all real Kafka/HDFS/Kudu/NiFi/Spark/OS writes remain disabled. No arbitrary command interpreter, dynamic SQL executor, arbitrary endpoint or model-produced shell argument.
> 4. Implement authenticated approval boundary or, if OIDC remains absent, make enterprise actions **hard-denied** while keeping server-bound local demo simulation. `decided_by` supplied in body must not confer identity. Require two-person approval where documented risk policy requires it; do not simulate that as real governance.
> 5. Use durable outbox/execution state, bounded retries and explicit recovery after crash; distinguish `PROPOSED`, `AUTHORIZED`, `EXECUTING`, `EXECUTED`, `VERIFIED`, `FAILED`, `REQUIRES_MANUAL_INTERVENTION` and `ROLLED_BACK`. An executor acknowledgement cannot mark incident resolved; independent current post-state must satisfy predeclared criteria.
> 6. Rollback is an explicit named operation with its own authorization and dry-run; never execute inferred reverse commands.
> 7. Preserve compatibility aliases, log redaction, immutable audit and the existing Python default route.
>
> **Tests:** wrong-tenant approval/execute; expired or revoked approval; modified target/params; missing authentication; parallel duplicate execute; interrupted execution and process restart; false-positive verification; stale verification; negative read-only mode; illegal production write; rollback denied; synthetic successful action + independent metric verification.
>
> **Deliverables:** `prompt11/REPORT.md`, state-machine diagram, action registry and schemas, policy decision matrix, isolated demo executor integration, verification evidence, policy/audit test artifacts, recovery manual.
>
> **Gate:** No unauthorized action can cross the safety boundary and replay-only execution is correct under retries and failures. Enterprise actions remain blocked until identity and approved change-control policy exist.

---

## PROMPT 12 — Next.js operator console on the Java APIs (no implicit cutover)

> **Role:** Staff full-stack engineer and incident-response UX architect. Preserve the existing Next.js design. The remote `apps/showcase` GitHub Pages bundle is intentionally static; do not leak OpenAI credentials or present it as a live GPT app.
>
> 1. Inspect front-end API proxy, current Python contracts, Java Prompt 03 parity matrix, Prompt 05 health and CLI, Prompt 07–11 APIs. Build a typed API adapter allowing controlled `python` vs `java` routing **without silent database ownership changes**. Document rollback to Python. Ensure backend-only credentials and same-origin server proxy.
> 2. Add an operator-facing **System Health** view: configured/readiness/semantic status; timestamps, freshness, evidence and source mode for PostgreSQL, Java API, OpenSearch, collector/OTel, Prometheus, local Kafka/KRaft, and optional Big Data connectors. Distinguish `NOT_CONFIGURED` from `UNHEALTHY`; do not display simulated infrastructure as enterprise-live.
> 3. Update Investigator Chat: real vs fallback model status, per-turn tool-call provenance, evidence IDs with click-to-inspect excerpts, incident association, persist/reload conversation, out-of-scope and no-incident questions, hypothesis/contradiction display and latency/token/cost data only when measured.
> 4. Extend Incident Room/NerveMap to show the upstream graph, time-aligned observations, affected downstream datasets, severity/business impact, accepted or contested root cause and explicit uncertainty.
> 5. Add monitoring controls (rules enabled/cadence/alert suppression) behind authenticated policy boundaries; all demo rerun/restart actions must pass server-side constraints and never restart enterprise services. Approval UI must show immutable action definition, risk, evidence freshness, scope and verification.
> 6. Keep accessible keyboard controls, empty/loading/error states, refresh consistency, no credential exposure in client logs. Include a visible `SIMULATED DEMO` banner in any synthetic mode.
> 7. Check `apps/web/package.json` / lockfile against the actual patched `main` (Next.js 16.4.5 as of 2026-11-17) and run `npm ci`, `npm run build`, `npm run lint`, `npm audit --audit-level=high`. Do not override critical security gating.
>
> **Tests:** API compatibility across Python/Java; unauthorized page/API access; false health states; backend unavailable; invalid evidence reference; stale conversation; approved/rejected state change; malicious message; slow model and tool timeout; static Pages continues being clearly a non-live showcase.
>
> **Deliverables:** `prompt12/REPORT.md`, contract snapshots, UX evidence, screenshots if possible, exact environment switch and rollback, browser smoke script, build/audit logs.
>
> **Gate:** Java-backed UI works in an explicitly opt-in local demo, default Python still works, Pages remains static, all frontend tests/audit pass, no security boundary regression.

---

## PROMPT 13 — Reproducible end-to-end lab + GitHub Actions quality gates

> **Role:** SDET and distributed-systems platform engineer. Build an **isolated and replayable** lab, plus hosted CI that validates the actual Java implementation.
>
> 1. Audit Docker profiles, Maven/Testcontainers, Kafka admin read-only fixture, authenticated OpenSearch, PostgreSQL private Flyway schema, OTel/Fluent Bit, Next.js and `./investinator` CLI. Pin versions and health dependencies. No shared cloud/private services or real write permissions in CI.
> 2. Provide repeatable `./investinator up|down|doctor|health --all|smoke|test|logs|demo kafka-kraft|demo kafka-legacy|investigate|ai-check --live|restart api`. `down` must preserve volumes; `reset-demo` alone can destroy isolated demo data with explicit operator confirmation, never production.
> 3. Compose a **real local transport + synthetic fault** demo: network/KRaft controller instability → Kafka metadata disruption → consumer lag → streaming/freshness delay. Build a *separate*, explicitly legacy Kafka/ZooKeeper topology or a clearly labeled representative protocol fixture; never attribute ZooKeeper to a Kafka 4.x KRaft instance.
> 4. Add negative scenarios: missing Kafka broker, OpenSearch network outage/403, slow or absent GPT, DB interruption, duplicate and delayed events, stale timestamps, malformed tenant metadata, corrupt log, simulated bad runbook prompt injection, restart mid-recovery, unauthorized approval and healthy baseline.
> 5. Capture sanitized logs/traces, source/tenant/time, incident count, hypothesis alternatives, rate limits, health matrix, CLI exit codes, deterministic golden fixture results and side-effect checks. Ensure test order independence and automatic cleanup only of test-owned resources.
> 6. Add GitHub Actions jobs for JDK 21 Maven Verify with Testcontainers, CLI test/lint, Python pytest/ruff/mypy, Next.js type/build/audit, Compose config, local E2E smoke and security dependency scans. GitHub Action CI must actually run on the remote Java branch and report run IDs.
> 7. Make sure the `java` feature branch is pushed before asking hosted CI to validate it. If a hosted runner cannot run service dependencies, mark the job `BLOCKED` with an exact cause instead of silently skipping.
>
> **Tests/gates:** a fresh checkout reproduces the complete documented demo; backend and frontend answer HTTP health requests; no uncontrolled side effects, no critical npm vulnerabilities, no false external live proof; duplicate flooding remains bounded; role/tenant boundaries enforced. `npm audit` remains enabled.
>
> **Deliverables:** `prompt13/REPORT.md`, redacted run artifacts, scripts, workflows, fixture hashes, test matrix, local and hosted CI provenance, resource and timeout budgets.
>
> **Gate:** All required checks green on the pushed Java branch, or explicit blockers recorded with owners. No assertion that a build passing proves live GPT or full enterprise connector readiness.

---

## PROMPT 14 — Production-security hardening, observability, financial and reliability SLOs

> **Role:** Enterprise security architect, SRE, FinOps lead. This is a **production-readiness assessment**, not permission to deploy.
>
> 1. Threat-model the five layers and attack surfaces: collection spoofing, malicious runbook text, prompt injection, tenant leak, SSRF/tool scope abuse, unauthorized connector query, session impersonation, API key leakage, replayed action approval, cloud egress, runaway model spend, denial of service, OpenSearch index/ACL bypass, untrusted telemetry.
> 2. Implement OIDC/RBAC or leave enterprise ingress hard-blocked. Verify least-privilege read identities for Kafka, ZooKeeper legacy, Spark/YARN, HDFS/Hive/HS2/HMS, Kudu, NiFi, Airflow, Cloudera Manager, Atlas and optional Ranger. Certificate, Kerberos, token refresh, outage/timeout and multi-tenant negative tests are mandatory before certifying connectors.
> 3. Review Actuator and API public/private exposure, TLS/mTLS, secure cookies, CSRF where applicable, secrets rotation, allowlisted network egress, redaction, audit retention, PII/privacy processing, knowledge licensing, index retention, backups/restore, schema ownership and DR. Never enable an unverified production executor.
> 4. Define operational SLOs: health freshness, detection latency, API error budget, MTTA/MTTI/MTTR, evidence validity, false-positive rate, abstention rate, connector response latency, notification budget, incident compression, and GPT tokens/cost per investigation. Establish initial baselines from measured data; don't fabricate ROI. Instrument Java metrics, tracing, error taxonomy, outbox backlog and JVM heap/GC.
> 5. Test model/tool budget exhaustion, provider 429/outage, host/network partition, queue backlog, OpenSearch slow query, replica/read interruption, race conditions, restart recovery and long-duration data loss. Quantify only what measured.
> 6. Produce a release risk register with owner, remediation plan, verification artifact, target milestone and disposition `GO / CONDITIONAL / BLOCKED`. Update the business case with measurable support efficiency, cost and operational risk; explicitly mark unmeasured business impact.
>
> **Gate:** Security reviews and applicable tests run, high/critical findings remediated or release BLOCKED. A product demo may remain safe and useful while production readiness remains BLOCKED.
>
> **Deliverables:** `prompt14/REPORT.md`, threat model, auth matrix, security scan outputs, tenant-isolation proof, dashboard/SLO definitions, FinOps model and decision record.

---

## PROMPT 15 — Final architecture documentation, developer/operator handover, demo script

> **Role:** Lead architect and technical product owner. Document exactly what the checked-in system can reproduce.
>
> 1. Refresh context/container/component/deployment/sequence/data-flow C4 views and ADRs to match code. Generate module and API maps for the Java system, private Flyway schema, coexistence with Python, configured source connectors and the five-layer architecture.
> 2. Update README with a `one-command quickstart`, Java 21 prerequisites, `./investinator doctor`, `smoke`, safe rerun, log inspection, demo reset semantics, known failures, provider configuration, optional connector credentials, development/test commands and security warnings.
> 3. Document the live demo narrative: surface Kafka symptoms, show upstream KRaft/network causal candidates with timestamped **SIMULATED** data, retrieve OpenSearch records/runbooks, ask real GPT only with opt-in provider credentials, show alternatives and missing evidence, propose **no production changes**, approve isolated ReplayLab recovery and verify post-state.
> 4. Publish a factual implementation matrix per layer and service: `VERIFIED_REMOTE`, `REPORTED_LOCAL`, `LIVE_LOCAL`, `MOCKED`, `SIMULATED`, `NOT_CONFIGURED`, `NOT_TESTED`, `BLOCKED` and planned, supported by commit/run identifiers.
> 5. Provide architecture/business narrative for executive stakeholders: root-cause identification problem, cost of investigation time, potential productivity and risk improvement, architecture cost drivers, security value, build-vs-buy trade-offs and a phased ROI measurement plan; never invent measured savings.
> 6. Reconcile all documentation's historical security advisories and fake links; ensure Pages source links, README preview, routes and default backend ownership are accurate.
>
> **Gate:** Another engineer can reproduce the isolated demo from clean checkout, and executives can distinguish proven functionality from future promise.
>
> **Deliverables:** `prompt15/REPORT.md`, handover package, versioned architecture, runbooks, verified build matrix, executive one-page product brief and reproducible demo narrative.

---

## PROMPT 16 — Optional enterprise connector certification and Python-to-Java cutover

> **Do not execute as a routine continuation.** This phase requires explicit business/security owner authorization, verified representative enterprise access and a documented change window.
>
> 1. Audit production connector versions/auth modes; confirm KRaft vs legacy ZK rather than guessing; build read-only compatibility certification per cluster and service. Include network/HA behavior, timeout/rate limit, Kerberos/mTLS renewal, permission denial, tenant scoping and evidence freshness.
> 2. Run Java backend shadow traffic and differential Python/Java contract + persistence comparison; preserve different schema ownership. Snapshot/backup, restore drill, data migration and bidirectional rollback must be designed, tested and approved before flipping any route.
> 3. Add feature-flagged, monitored canary route switch with latency/error/accuracy/tenant policy gates and instant rollback. No automatic `main` merge or production cutover.
> 4. Produce change request, deployment architecture, operational runbook, measured SLO results, incident escalation owners, rollback and sign-off.
>
> **Gate:** Independent owner/InfoSec/SRE approval and clean shadow/canary evidence. Otherwise keep current Python default route unchanged.

---

## Compact continuation message for each coding-agent session

> Read `docs/roadmap/JAVA_AI_ARCHITECTURE_V2.md`, `docs/roadmap/POST_P09_PROMPTS_V2.md`, `docs/implementation/STATE.md` and the previous report. Execute **PROMPT NN only** from the v2 file; maintain Python/Java data ownership, no unsafe infrastructure writes, explicit evidence provenance, real validation and phase-gated commits. End with test evidence, exact changed files and SHA, issue list and next prompt readiness. Do not proceed automatically.

## Suggested milestone delivery

| Milestone | Prompts | Definition of done |
|---|---|---|
| A — Repo synchronization | 09A | Java code actually pushed, PR/CI verified |
| B — Evaluation review | 09B | Review package and provider probe, no fake human sign-off |
| C — Autonomous observation | 10 | Deduplicated, bounded monitored incident lifecycle |
| D — Authorized recovery | 11 | Simulation-only, policy enforced, independent verification |
| E — Operator experience | 12 | Java opt-in UI, source/evidence clarity, Python rollback |
| F — Repeatable product demo | 13 | Fresh clone replay, CI and outage matrix |
| G — Enterprise risk review | 14 | Security/SLO/FinOps evidence, gate outcome |
| H — Complete handover | 15 | Reproducible deployment docs and executive narrative |
| I — Optional enterprise canary | 16 | Explicit owner authorization and rollback drill |
