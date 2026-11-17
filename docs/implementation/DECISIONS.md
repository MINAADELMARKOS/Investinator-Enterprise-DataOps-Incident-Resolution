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
