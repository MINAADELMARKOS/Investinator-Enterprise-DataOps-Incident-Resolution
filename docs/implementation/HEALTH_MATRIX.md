# Integration health matrix — after Prompts 03/04

These are local development acceptance observations, not production health. Test
resources were disposable; persisted user data and default UI routing remain Python.

| Component | Implemented / verified | Provenance | Remaining acceptance |
|---|---|---|---|
| Java API | live embedded HTTP, liveness/readiness, workflow contracts | LIVE API / SIMULATED demo data | enterprise identity, load/SLOs |
| Next.js | unchanged page and GET/POST proxy against Java demo | LIVE local requests | Java provider-backed UI path |
| PostgreSQL | Flyway schema, SELECT 1/readiness, restart persistence, transactions, ownership refusal | LIVE disposable PostgreSQL | existing-data import, backup/restore/cutover review |
| Kafka | basic metadata adapter | NOT_CONFIGURED in local parity gate | semantic KRaft/ISR/lag/auth and failure tests |
| Platform status | Python-equivalent seeded threshold scores | SIMULATED | universal health/freshness/UNKNOWN engine in 05 |
| Search | authenticated OpenSearch, strict mapping, actual file forwarding, Java tenant-filtered read-only search, unavailable 503 | LIVE retrieval / SIMULATED source | enterprise ingress/TLS, HA, production retention |
| Prometheus | actual OTel metric exporter scrape and bounded PromQL query | LIVE transport / SIMULATED fixture metric | production target coverage/SLOs |
| OTel/traces | actual OTLP trace batch -> private file -> Fluent Bit -> separate search index | LIVE transport / SIMULATED spans | tenant-aware native trace query API, real service instrumentation |
| GPT | Python optional path retained; Java deterministic fallback | live provider NOT_CONFIGURED/NOT_TESTED | official tool loop and opt-in provider gate in 08 |
| Java runtime | JDK21/Boot4.1.2 and 25 passing tests, real collector/index lab | LIVE local acceptance | hosted CI and Java CVE audit not run |
| Spark/YARN/HDFS/Hive/Kudu/NiFi/Airflow | runbook text / simulated signals | NOT_CONFIGURED | real version-aware least-privilege adapters |
| ZooKeeper | absent, main Compose is KRaft | NOT_CONFIGURED | legacy-only fixture and mode detection |

Prompt 04 search health reports observed cluster/mapping status; green/yellow on
single-node search is accepted. This does not claim source observations are live.
Per-record source mode, timestamps and pointers are returned independently. Missing
backend configuration and outage return explicit non-success; liveness excludes
optional systems. Detailed health and search require the backend bearer credential.

The planned universal runner/CLI belongs to Prompt 05: bounded parallel probes,
partial results, circuit breakers, freshness thresholds, optional-versus-required
dependencies and no paid GPT/writes by default. That prompt has not started.
