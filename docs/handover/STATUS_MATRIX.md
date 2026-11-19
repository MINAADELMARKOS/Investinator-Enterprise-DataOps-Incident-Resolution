# As-Is / To-Be and component build matrix

2026-11-19. Build status describes code, evidence labels describe what was executed.
Current release is BLOCKED. Source code publication and green CI do not certify
enterprise services, model quality or production readiness.

Evidence anchors: [P03 parity](../implementation/prompt03/REPORT.md),
[P06 connectors](../implementation/prompt06/REPORT.md), [P09B review](../implementation/prompt09b/REPORT.md),
[P13 lab](../implementation/prompt13/REPORT.md), [P14 security/SLO](../implementation/prompt14/REPORT.md).
Hosted implementation `8e41c2c`:
[44397526897](https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/actions/runs/44397526897),
all five jobs pass. Actual local P15 health/preflight and rehearsal are in
[Prompt 15 evidence](../implementation/prompt15/REPORT.md). Historical reports retain
failures rather than silently revising history. No human review approval is implied.

| Component / layer | Build status | As-Is / evidence | To-Be / unmet gate |
|---|---|---|---|
| Java API / Platform | tested | VERIFIED_REMOTE, LIVE_LOCAL; modular monolith, private persistence, negative tests | Certified identity, ingress, operations |
| Python API / Application | tested | VERIFIED_REMOTE; preserved default Next route, actual compatibility browser | Optional reviewed cutover; do not delete backend |
| Next operator console / Application | tested | VERIFIED_REMOTE, LIVE_LOCAL; explicit Java opt-in, signed demo roles, native health/history/evidence/graph/plan UI | Enterprise OIDC/accessibility/security certification |
| Pages showcase / Application | simulated | VERIFIED_REMOTE static synthetic source; no API or provider | Remain explicit preview; not a deployment |
| PostgreSQL / Infrastructure | tested | LIVE_LOCAL authenticated Java queries; 38-table isolated restore | Least-privilege runtime/migrator separation, encrypted offsite/PITR/HA |
| Flyway / Platform | tested | V1–V9 private schema, no public/Alembic adoption | Reviewed production schema/restore operations |
| Alembic / Python | tested | Preserved owner, compatibility paths | Separate production ownership/backup proof |
| OpenSearch / Infrastructure | tested | LIVE_LOCAL normalized ingest, scoped search, corruption/auth/outage tests | HA/retention/ACL identity and load certification |
| Optional Python Elasticsearch | partially tested | Separate older compatibility profile; not Java lab backend | Current kernel/version/security certification before use |
| Fluent Bit / Infrastructure | tested | LIVE_LOCAL transport, SIMULATED files; corrupt log rejected | Authenticated trusted sources, durability/replay/retention |
| OTel collector / Infrastructure | tested | LIVE_LOCAL synthetic span arrival; not production span coverage | Reviewed authenticated receiver/exporter/PII/sampling |
| Java investigation spans / Platform | tested | Actual SDK in-memory export, no secret exception text; default disabled | End-to-end private trace propagation/coverage certificate |
| Prometheus / Infrastructure | tested | LIVE_LOCAL collector scrape/PromQL; private Java endpoint observed separately | Secret-backed Java scrape and approved remote alerts/SLOs |
| JVM/HTTP/queue/usage metrics / Platform | tested | LIVE_LOCAL fixed dimensions, missing usage unknown, proposed rule NaN checks | Production capacity/error-budget windows |
| Kafka local broker / Infrastructure | tested | LIVE_LOCAL SASL Admin/metadata/lag; fixture producer separate from diagnostic principal | Enterprise principal/TLS/refresh certificate |
| KRaft metadata quorum / Connector | tested | LIVE_LOCAL read leader/voters; causal fault observations SIMULATED | Real multi-controller failure/realm certification |
| Legacy ZooKeeper / Connector | partially tested | LIVE_LOCAL isolated digest ACL protocol; SIMULATED representative chain | Private ensemble and real legacy Kafka certification |
| Spark / YARN / Connectors | partially tested | Implemented fixed REST reads, MOCKED auth/version/semantic negatives; NOT_CONFIGURED privately | Real realm/ACL/TLS/rotation/load proof |
| HDFS / Connector | partially tested | Implemented fixed HA/block diagnostics; MOCKED; NOT_CONFIGURED | Real HA, Kerberos/proxy-user and tenant proof |
| Hive HS2 JDBC / Connector | partially tested | Fixed bounded reads/resource closure; optional driver; NOT_CONFIGURED | Shaded-driver audit + native TLS/realm/tenant certificate |
| Hive Metastore / Connector | partially tested | Actual local SASL Thrift fixture, not enterprise | Real server/version/realm/certification |
| Kudu / Connector | partially tested | Official read RPC interface and fixture tests; NOT_CONFIGURED | Private cluster/principal/tablet/TLS proof |
| NiFi / Airflow / Connectors | partially tested | Fixed REST adapters, MOCKED semantic/auth errors; NOT_CONFIGURED | Native token refresh/ACL/certification |
| Cloudera Manager / Atlas / Connectors | partially tested | Fixed read interface/fixtures; NOT_CONFIGURED | Private version/principal/metadata visibility certificate |
| Ranger optional / Connector | planned | Explicit unsupported/unconfigured; no guessed writes | Read-only adapter and certification |
| Versioned graph/time correlation / Agent | tested | LIVE_LOCAL storage; SIMULATED causal streams, alternatives/contradictions/unknowns | Verified native topology/events, independent causal review |
| Continuous monitor/lifecycle / Agent | tested | Durable bounded queue/checkpoint/outbox, replay-only intake, actual restart negatives | Certified LIVE adapters/HA/retention/soak |
| Notifications / Agent | simulated | LOCAL_FAKE delivery or explicit console; no external recipient | Approved real delivery/rate/identity certificate |
| OpenAI Responses gateway / Model | tested | Official SDK tool loop + MOCKED_PROVIDER requests; user-visible fallback | Actual configured credential/model/approved live budget |
| Live GPT service / Model | not configured | NOT_CONFIGURED; no paid handover calls | Explicit authorized two-turn/read-tool live preflight |
| Conversations/citations / Platform | tested | Redacted public metadata, encrypted continuation/current scoped inspection | Production retention, keys, privacy and durability policies |
| BM25 knowledge/RAG / Platform | tested | LIVE_LOCAL retrieval fixtures/current ACL/revocation; historical advice | Human reviewed corpus/licensing/quality; no vector claim |
| Embeddings/hybrid / Model | planned | Not implemented/evaluated | Reviewed gain vs latency/cost evidence |
| Feedback/learning / Agent | partially tested | Stored review-pending outcomes; no auto-promotion | Independent qualified review and held-out evaluation |
| Governed remediation / Agent | simulated | Tested separate capabilities/hash/expiry/receipts, independent simulator sample | Real identity/change approval; no enterprise executor |
| Named rollback / Agent | simulated | Separately approved sandbox restoration/verification | Certified per-action real rollback, no inferred reverse commands |
| Audit / Platform | partially tested | Durable DB actor/hash/outcome records; DB admin trusted | Immutable external audit/legal retention |
| OIDC/RBAC / Platform | planned | BLOCKED; non-demo application ingress denied | Certified issuer/audience/roles/tenant/rotation |
| FinOps / Platform | partially tested | Request/context/output/time/concurrency/rate bounds; rates/cost unknown | Durable distributed monetary caps and reconciliation |
| CI / Engineering | tested | VERIFIED_REMOTE five jobs, default Maven/Python/npm scans, E2E/restore | Optional/image/independent security certification |
| Production SLOs/ROI / Business | planned | Local serial samples/fixture metrics only, NOT_MEASURED production | Approved windows/cohorts/held-out review and measured business study |

Migration parity is a tested seeded contract/74-step trace, not complete enterprise
feature parity. Known critical Next advisories in early reports are historical;
Next 16.4.5 remains patched and scoped current audits pass. Seven Java codec/OTel
advisories were fixed in P13; default OSV 233-package scan passes. Container OS,
optional shaded Hive driver and independent penetration review remain unverified.
See the precise [release register](../implementation/prompt14/RELEASE_RISKS.json).
