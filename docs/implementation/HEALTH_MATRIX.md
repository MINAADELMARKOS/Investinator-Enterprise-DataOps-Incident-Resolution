# Integration health matrix

Audit observations are not production health. No service startup was performed this stage.

| Component | Present implementation | Evidence/provenance | Missing acceptance |
|---|---|---|---|
| API | /health always ok in demo | code; NOT_TESTED live this stage | functional DB readiness and negative dependency tests |
| Next.js | HTTP app and API proxy | build PASS | runtime/proxy smoke this stage NOT_TESTED |
| DB | SQLite/PG SQLAlchemy | tests LIVE temporary SQLite with SIMULATED fixtures | PG SELECT 1, schema/version, persistence/restore |
| Kafka | list_topics/describe_cluster | NOT_CONFIGURED in audit environment | version, SASL/TLS, ISR, lag, quorum and failures |
| Platform status | seeded threshold scores | SIMULATED | missing metrics currently default healthy; require UNKNOWN/freshness |
| Search | absent main; PR bounded query | NOT_CONFIGURED | authenticated search, mapping, freshness and real ingestion |
| Prometheus | Compose /metrics scrape config | NOT_TESTED | scrape target + PromQL functional check |
| OTel | optional HTTP exporter, debug collector | NOT_CONFIGURED | end-to-end retained trace arrival |
| GPT | main optional structured parse, PR tool loop | NOT_CONFIGURED; fake PR tests only | opt-in live provider and strict loop negative tests |
| Java | not scaffolded | NOT_IMPLEMENTED | Java 21 wrapper verify, Actuator, container smoke |
| Spark/YARN/HDFS/Hive/Kudu/NiFi/Airflow | runbook text / simulation only | NOT_CONFIGURED | real authenticated version-aware adapters |
| ZooKeeper | absent; KRaft Compose | NOT_CONFIGURED | legacy-only fixture and mode detection |

Proposed health results: HEALTHY/DEGRADED/UNHEALTHY/UNKNOWN/NOT_CONFIGURED/NOT_TESTED,
source_mode, checked_at, duration, evidence references, diagnosis, next safe action,
required_for_readiness. Bounded parallel checks with deadlines, partial results and
circuit breakers; no writes or paid GPT requests in doctor. Implement in Prompt 05,
not this audit. CLI up/down/status/doctor/health/smoke/test/logs/restart/demo/ai-check
must target allowlisted local services, preserve volumes and return useful exit codes.
