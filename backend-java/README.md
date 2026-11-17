# Java backend — Prompts 02–04

Java 21 JDK, Spring Boot 4.1.2, Maven Wrapper 3.9.11 (SHA-256 pinned).
This is a separate modular monolith on port 8081. Persistence remains opt-in; the default is the Prompt 02 health scaffold. Python still owns all existing
application behavior and Alembic schema migrations. Next.js routing is unchanged.

From the repository root, with a **JDK** 21 on JAVA_HOME:

```bash
./backend-java/mvnw -B -f backend-java/pom.xml verify
java -jar backend-java/target/backend-java-0.1.0-SNAPSHOT.jar
python3 backend-java/scripts/smoke.py
```

Or build and start only the Java container after the verified jar exists:

```bash
docker compose -f docker-compose.yml -f compose.java.yaml --profile java up --build --no-deps --wait --wait-timeout 90 java-api
python3 backend-java/scripts/smoke.py
# Stop only the new Java service; preserve Python, other services and volumes.
docker compose -f docker-compose.yml -f compose.java.yaml --profile java stop java-api
```

The runtime image is pinned by digest, runs as UID 10001 and probes actual HTTP
readiness. The Dockerfile consumes the verified jar; it does not compile source.
CI builds the jar before the image. JAVA_HOST_PORT changes the host mapping; pass
the matching local URL to `smoke.py --base-url`. No real cluster changes or model
requests occur in startup/tests/smoke.

## Endpoints and configuration

| Endpoint | Semantics |
|---|---|
| GET /actuator/health/liveness | process probe, no external dependency |
| GET /actuator/health/readiness | framework readiness; no DB-backed routes exist yet |
| GET /api/v1/platform/health | legacy top-level keys where practical, explicit java-scaffold / NOT_IMPLEMENTED / NOT_TESTED, null unknown measurements |
| GET /actuator/prometheus | requires backend METRICS_TOKEN as Bearer token; denied when unset |

Other routes, including detailed Actuator health/env and unmigrated application
routes, are denied. Platform health is a scaffold response, **not API parity or
connector health**. With persistence disabled (the default), no data-source bean, Flyway migration
or database connection is created. Prompt 03 enables these only for its dedicated schema. Add essential DB readiness only when persistence routes are
introduced in Prompt 03. Readiness is not a claim that Kafka/search/GPT are available.

Default `local`, explicit `demo`, `integration` and `enterprise` profiles exist.
The first three use loopback HTTP by default. Docker binds inside its network and
publishes only host loopback. Enterprise requires a configured Spring SSL bundle
named by JAVA_TLS_BUNDLE and fails closed without it. Supply certificates via secret
mounts/external Spring configuration; never commit keystores or passwords.
Enterprise TLS with actual certificates has NOT been tested. Forwarded headers are
not trusted; configure a trusted reverse proxy explicitly before enterprise use.
Use the JVM's approved trust store and proxy configuration; do not disable TLS checks.

| Setting | Default / effect |
|---|---|
| JAVA_BIND_ADDRESS / JAVA_PORT | 127.0.0.1 / 8081 on host |
| SPRING_PROFILES_ACTIVE | local |
| METRICS_TOKEN | unset; metrics denied |
| INVESTINATOR_OPENAI_ENABLED | false; no SDK client bean |
| INVESTINATOR_OPENAI_MODEL | unset; required if connector enabled |
| OPENAI_API_KEY | backend-only; SDK requires it if enabled |
| INVESTINATOR_MAX_AI_CALLS | 3; configuration validation bounds 1–10, execution budget implementation is future work |
| otel.sdk.disabled | true; no telemetry exports by default |

OpenAI client construction is conditional and uses the official SDK's environment
configuration. No function-call loop is implemented in this stage. Other connectors
are dependencies/module boundaries only, not live integrations. `API_INTERNAL_URL`
continues to target Python; do not switch the whole UI to this incomplete scaffold.
Endpoint routing and parity belong to Prompt 03.

Logs use ECS JSON. Bounded X-Request-ID is returned and attached through MDC; request
completion logs include status, not URL/query/body/headers/secrets. Exception responses
use generic ProblemDetail messages. Config validation rejects enabled OpenAI without
a model and invalid request budgets. No interactive generated security credentials,
production executors, model calls or arbitrary command endpoints are installed.

## Dependencies and compatibility evidence

Boot BOM pins framework, JDBC/PostgreSQL, Flyway, Kafka, Prometheus and test libraries.
Explicit pins: OpenAI Java 4.106.0, OpenSearch Java 3.11.0 (verified against server 3.4.0),
Reactive Streams 1.0.4, OTel starter 2.33.1, WireMock standalone 3.13.2. Reactor Core is
Boot-managed and needed for SDK bean introspection. Elasticsearch from Prompt 02 was
replaced by OpenSearch after a demonstrated kernel incompatibility (ADR-011).
JDBC is chosen over JPA for the scaffold: no entities or schema ownership are ported.
No agent framework is added. Current resolved tree: ../docs/implementation/prompt04/dependency-tree.txt; Prompt 02 artifacts retain the historical baseline.

**JUnit compatibility adjustment:** Boot 4.1.2 / Spring 7 test extension fails with
JUnit 5.13.4 (missing Store.computeIfAbsent). Use Boot-managed JUnit 6.0.3 with its
Jupiter API. Failure evidence is retained; Mockito and Testcontainers are BOM-managed.
Prompt 02 had eight tests. The current 25-test suite also covers PostgreSQL workflows,
74 Python differential requests, normalization and the enabled search SDK context. WireMock is explicitly a simulated
HTTP fixture, not a live cluster. Prompt 03 now exercises real PostgreSQL with Testcontainers; the Prompt 04 lab exercises
real collectors and search. Neither is proof of existing-data production cutover.

## Managed cloud reproduction

This machine initially had a JRE without javac. A JDK 21 was extracted from official
Temurin's JDK image to /workspace/.cache/jdk-21. Maven's writable cache and cloud proxy
settings are outside the repository. In this instance the verified command is:

```bash
JAVA_HOME=/workspace/.cache/jdk-21 \
MAVEN_USER_HOME=/workspace/.cache/maven-user \
MAVEN_OPTS='-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts' \
./backend-java/mvnw -B -s /workspace/.cache/maven/settings.xml \
  -Dmaven.repo.local=/workspace/.cache/m2 -f backend-java/pom.xml verify
```

Those paths are instance-specific; standard development/CI uses the first command.
For a fresh managed machine, use supplied proxy/CA guidance to prepare local Maven
settings and a JDK without committing credentials or machine endpoints. Wrapper and
Maven dependencies retain HTTPS/checksum verification. Do not infer that JRE availability
means javac is installed. No environment snapshot/publication was performed this stage.

## Prompt 03 — PostgreSQL demo and workflow parity

The opt-in persistence mode ports the 13 operational tables, typed incident and
request contracts, seeded reads, deterministic investigation, chat/history, metric
ingestion, replay, approval, audit and verified simulated recovery. A dedicated
Flyway schema and additive idempotency/execution records preserve transaction
boundaries. No existing Python data or traffic has moved.

See [migration ownership, local commands and rollback](../docs/implementation/prompt03/MIGRATION_AND_ROLLBACK.md).
For an isolated restart/proxy/outage acceptance run after building:

```bash
python3 backend-java/scripts/verify_persistence.py --with-web
```

The optional web check uses the already installed/built Next.js app without editing
its source. Java live OpenAI behavior remains unported until Prompt 08; provider-enabled
workflows stay on Python. Local deterministic demo parity is tested against captured
Python API traces; it is not a production-readiness claim.

## Prompt 04 — normalized evidence and authenticated search

See [the local evidence lab](../infra/evidence/README.md) for collector/index ownership,
credentials, safe startup/stop, retention, query bounds and the real ingestion gate.
The default remains the original Python application; no existing data or routing is
changed. With Docker/JDK21 and the verified jar/classpath built by Maven:

```bash
python3 backend-java/scripts/evidence_lab.py verify
```

This starts disposable local services and checks actual collection, tenant filtering,
read-only roles, provenance, metrics/traces and unavailable-backend behavior. It makes
no paid model call. `/api/v1/evidence/search` and `/health` under that evidence prefix
require EVIDENCE_API_TOKEN; credentials/index/tenant remain server configuration.
