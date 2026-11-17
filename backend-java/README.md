# Java scaffold — Prompt 02

Java 21 JDK, Spring Boot 4.1.2, Maven Wrapper 3.9.11 (SHA-256 pinned).
This is a separate modular monolith on port 8081. Python still owns all existing
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
connector health**. No data-source bean, automatic Flyway migration or database
connection is created. Add essential DB readiness only when persistence routes are
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
Explicit pins: OpenAI Java 4.106.0, Elasticsearch Java 9.5.6 (target server 9.5.6,
real server compatibility NOT_TESTED), OTel starter 2.33.1, WireMock standalone 3.13.2.
JDBC is chosen over JPA for the scaffold: no entities or schema ownership are ported.
No agent framework is added. Full resolved tree: ../docs/implementation/prompt02/dependency-tree.txt.

**JUnit compatibility adjustment:** Boot 4.1.2 / Spring 7 test extension fails with
JUnit 5.13.4 (missing Store.computeIfAbsent). Use Boot-managed JUnit 6.0.3 with its
Jupiter API. Failure evidence is retained; Mockito and Testcontainers are BOM-managed.
Eight tests cover real embedded HTTP, denied access, config validation, error redaction,
correlation and vendor class/client construction. WireMock is explicitly a simulated
HTTP fixture, not a live cluster. No Testcontainers database tests until persistence
is introduced; merely including the dependency does not establish PostgreSQL behavior.

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
