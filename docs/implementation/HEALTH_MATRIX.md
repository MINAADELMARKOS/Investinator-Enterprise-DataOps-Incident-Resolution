# Integration health matrix

## Prompt 09B current observations

| Surface | Verified now | Remaining boundary |
|---|---|---|
| Java / CLI | full 81 Java, final focused 5 RAG, final 24 CLI tests PASS | new hosted 09B NOT_RUN |
| Reviewed knowledge | native SQL/BM25, exact hash/ACL/version/citation diagnostics PASS | sources SIMULATED, human approval null, no held-out set |
| Knowledge freshness | 0/9 stale/unauthorized returned; all eight queries DEGRADED/partial after stale rejection | projection cleanup and larger-scale candidate-bound validation |
| GPT protocol | two turns, real scoped read, multi-call, citation/outage/timeout/fallback PASS with mocked provider | actual provider NOT_CONFIGURED; no approved budget or paid call |
| Prompt 05 artifacts / current probes | all original files match original commit; current doctor/health/schema PASS | original outages historical; Windows/second-host acceptance not rerun |
| Python / frontend security | 12 Python tests, Ruff/mypy; fresh npm audit zero, 16.4.5 lock matches main | enterprise security/private scopes remain gated |
| AI quality | reviewable proposed cases and denominator-based reports | independent expert review PENDING; production accuracy unknown |

## Prompt 09A observations

| Component | Current result | Boundary |
|---|---|---|
| Java/API + CLI | 78 Java / 14 CLI tests, actual doctor/health/smoke PASS | local; no enterprise identity claim |
| Python | 12 tests, lint/mypy, seven fixtures and 74-step API exercise PASS | remote-main code preserved |
| Next.js 16.4.5 | install/build/types/audit PASS; zero vulnerabilities | patched main lock retained, default Python route |
| PostgreSQL | actual restart/private-schema/transactions/outage PASS | no migration of existing Python data |
| OpenSearch/OTel/Fluent Bit/Prometheus | actual pipeline/scoped evidence/knowledge PASS | SIMULATED source data on LIVE local transport |
| Provider | SDK protocol mocks PASS; actual live NOT_CONFIGURED | no paid calls |
| GitHub Git/API/CI | feature pushed and remote SHA verified; Java/backend/CLI/frontend hosted PASS (run 44102365986); API CONNECT denied | PR creation blocked; latest hosted status in SYNC_REPORT.md |
| Independent AI quality | PENDING human review | fixture metrics are provisional |

Detailed current artifacts: [sync report](SYNC_REPORT.md).

## Historical matrix — after Prompts 05/06

These are local development acceptance observations, not production health. The optional local lab is isolated; persisted user data and default UI routing remain Python.

| Component | Implemented / verified | Provenance | Remaining acceptance |
|---|---|---|---|
| Java API | live embedded HTTP, liveness/readiness, workflow contracts | LIVE API / SIMULATED demo data | enterprise identity, load/SLOs |
| Next.js | unchanged page and GET/POST proxy against Java demo | LIVE local requests | Java provider-backed UI path |
| PostgreSQL | Flyway schema, SELECT 1/readiness, restart persistence, transactions, ownership refusal | LIVE disposable PostgreSQL | existing-data import, backup/restore/cutover review |
| Kafka | real SASL KRaft Admin/ISR/quorum/lag, denied writes, persisted offsets, outage | LIVE local Kafka / SIMULATED orders | private TLS/Kerberos scopes, full multi-node resilience |
| Platform status | existing seeded UI scores preserved; separate typed diagnostic engine/CLI | LIVE / SIMULATED / CACHED / missing statuses explicit | production identity/SLOs |
| Search | authenticated OpenSearch, strict mapping, actual file forwarding, Java tenant-filtered read-only search, unavailable 503 | LIVE retrieval / SIMULATED source | enterprise ingress/TLS, HA, production retention |
| Prometheus | actual OTel metric exporter scrape and bounded PromQL query | LIVE transport / SIMULATED fixture metric | production target coverage/SLOs |
| OTel/traces | actual OTLP trace batch -> private file -> Fluent Bit -> separate search index | LIVE transport / SIMULATED spans | tenant-aware native trace query API, real service instrumentation |
| GPT | Python optional path retained; Java deterministic fallback | live provider NOT_CONFIGURED/NOT_TESTED | official tool loop and opt-in provider gate in 08 |
| Java runtime | JDK21/Boot4.1.2 and 49 passing tests, real collector/index lab | LIVE local acceptance | hosted CI and Java CVE audit not run |
| Spark/YARN/HDFS/Hive/Kudu/NiFi/Airflow | fixed read-only native/REST adapters, auth/version tests and documented capability gaps | private live NOT_CONFIGURED; protocol fixtures/mocks tested | private scopes, advanced semantic gaps in prompt06/CONNECTORS.md |
| ZooKeeper | real digest-ACL server test; legacy guard; no writes; KRaft never contacts ZK | LIVE protocol / SIMULATED legacy Kafka evidence | real legacy broker and production Kerberos NOT_TESTED |

Prompt 04 search health reports observed cluster/mapping status; green/yellow on
single-node search is accepted. This does not claim source observations are live.
Per-record source mode, timestamps and pointers are returned independently. Missing
backend configuration and outage return explicit non-success; liveness excludes
optional systems. Detailed health and search require the backend bearer credential.

The planned universal runner/CLI belongs to Prompt 05: bounded parallel probes,
partial results, circuit breakers, freshness thresholds, optional-versus-required
dependencies and no paid GPT/writes by default. That prompt has not started.

Current scope and evidence: [Prompt 05](prompt05/REPORT.md), [Prompt 06](prompt06/REPORT.md). A successful read surface does not verify the explicitly missing semantic dimensions.
