# Java backend — current Architecture v2 handover

Java 21, Spring Boot 4.1.2, Maven Wrapper 3.9.11. The modular monolith now includes
private persistence/workflow compatibility, normalized evidence, native/read-only
connectors, graph/time correlation, Responses tool calling, BM25 knowledge,
monitoring, governed ReplayLab and operational telemetry. It is published on the
feature branch; the old Prompt 02 scaffold-only description is historical.

Start the full owned lab from repository root:

```bash
INVESTINATOR_MONITOR_ENABLED=true ./investinator up
./investinator doctor
./investinator smoke
```

For development: `./backend-java/mvnw -B -f backend-java/pom.xml verify` with a JDK 21.
`./investinator test` runs Maven and CLI tests only. Full frontend/Python/browser,
clean-checkout and recovery commands are in the [developer guide](../docs/handover/DEVELOPMENT.md).

Default standalone Java has persistence disabled and serves a limited scaffold;
the owned lab explicitly enables authenticated PostgreSQL/private Flyway V1–V8 and
Java-backed Next. Python remains the global default route. Profiles local/demo/
integration/enterprise exist; enterprise needs external TLS configuration and
application ingress remains denied without certified identity. No real executor.

- [Architecture and sequences](../docs/handover/ARCHITECTURE.md)
- [API/module/config/schema source inventory](../docs/handover/source-inventory.json)
- [Provider billing, profiles, connectors and server-only credentials](../docs/handover/CONFIGURATION.md)
- [Operations, restart, cleanup and failure handling](../docs/handover/OPERATIONS.md)
- [Build/As-Is/To-Be matrix](../docs/handover/STATUS_MATRIX.md)
- [Production security/readiness gates](../docs/implementation/prompt14/RELEASE_RISKS.json)

37 Java migration-declared tables plus Flyway history belong to the dedicated
investinator_java schema by default; public/Alembic/unknown schema adoption is refused.
No JPA rewrite, autonomous agent microservices, enterprise topology discovery or
production Python cutover is implied. Boot-managed JUnit 6/Testcontainers are used;
older JUnit incompatibility and Elasticsearch kernel failures remain in stage reports.

Dependency pins, fixed codec/OTel advisories and real default-tree scan provenance
are in P13/P14. OpenSearch Java 3.11.0 with server 3.4.0 replaces the earlier attempted
Elasticsearch backend. Official OpenAI Java 4.106.0 and instrumentation 2.33.1/stable
OTel 1.67.0 are verified by the current suites. Provider absence is a labeled fallback;
MOCKED SDK tests do not establish live GPT quality. No paid call occurs on startup,
health or ordinary tests. Review [presentation gates](../docs/handover/DEMO.md).
