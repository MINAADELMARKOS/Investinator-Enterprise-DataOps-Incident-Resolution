# Integration health matrix

## Current Prompts 12/13 observations

Actual native diagnostics, corrupt-log/search/auth failures, monitoring/remediation
process restart and eight owned dependency outages pass. Java/Python browser and
all five hosted gates pass on f1488a7 (prompt13/hosted-ci.json). Default-tree Java
OSV 233 packages / zero findings; local egress failure is historical evidence.
Actual GPT remains NOT_CONFIGURED; independent human quality review PENDING.
Fixture workload/effects remain SIMULATED, provider fixtures MOCKED; LIVE local
transport does not certify enterprise connectors or production recovery.

## Previous Prompts 10/11 observations

| Surface | Verified | Remaining boundary |
|---|---|---|
| Monitoring | full regression + nine focused cases; actual process/offset/metric proof | opt-in, SIMULATED incident events; native health sampled separately |
| Governed recovery | 14 policy cases; actual HTTP restart, fresh independent sample and named rollback | private ReplayLab only; no native production executor |
| Isolation and regression | 104 Java + 26 CLI + 12 Python, lint/types, Compose, quality replay PASS | new hosted CI NOT_RUN |
| Identity | four distinct demo capability secrets, scoped role/tenant/hash/expiry denial | independent human/OIDC/two-person production policy absent |
| Provider / AI quality | MOCKED protocol tests; fixture scorer operational | actual provider NOT_CONFIGURED; expert review PENDING |
| Security | fresh npm audit zero, Next.js lock unchanged | enterprise security/Java CVE/private connector/DR gates remain |

## Prompt 09B observations

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


Prompt 10: local opt-in monitoring gate passes; see prompt10/REPORT.md and failure
matrix. Durable dedupe/checkpoint, capability-aware rules, actual health samples,
local fake notifications, rate/incident/evidence budgets and guarded lifecycle are
verified. Native LIVE semantic ingestion and automatic paid investigation are not
enabled; operator budget and verified native topology remain prerequisites. Remote
email/webhook, production identity, retention/load/DR and independent quality gates
remain deployment gaps. Python/default routing and patched Next.js stay intact.

## Prompt 12 operator evidence

System Health reads native integration diagnostics with original check times,
readiness/capability/source and safe next step. Missing/future/stale data is UNKNOWN;
optional absent connectors remain NOT_CONFIGURED. A browser 503 fixture preserves
old timestamps and errors visibly. Public login + actual health proxy are the
native Next probe, while operational pages require signed demo sessions.
Actual Java/Python browser flows pass; provider-less turns remain fallback, no live
GPT success or measured cost is claimed. See prompt12/results.json.

## Prompt 13 checkpoint

Prompt 13: separate actual corrupt-log validation/auth403/search outage and monitoring/remediation process restart checks pass. Fresh owned service outage/hosted acceptance pending. Actual GPT remains NOT_CONFIGURED and SDK provider fixtures remain MOCKED. Representative legacy graph is not a live legacy Kafka cluster.

Prompt 13 local final: fresh-checkout full acceptance and all eight owned outage/recovery cases PASS; additional actual bound-graph browser run PASS (14 checks). See prompt13/fresh-checkout.json and outages.json. Local Java OSV audit remains BLOCKED; hosted CI pending. No production readiness or live GPT claim.

## Prompt 14 operational/security evidence

Native local health and authenticated metrics snapshot pass; 30 serial reads per
route are a small development baseline, not production availability. DB probe/metric
failure returns UNKNOWN/NaN, proposed rules test NaN/missing series. Minimal probes
remain public; non-demo application/platform health is denied, metrics requires its
own capability. Local snapshot restore proves 38 tables/781 rows and a separate owner
sentinel only; no production RPO/RTO/replica/HA claim. See prompt14 evidence and gaps.
