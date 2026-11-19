# Authentication / connector certification matrix

| Surface | Demo authority | Non-demo / production disposition |
|---|---|---|
| Native business/platform APIs | Fixed server-bound demo tenant; core compatibility reads are demo-public | HARD-BLOCKED with demo mode false, including platform-health route |
| Liveness/readiness | Minimal status, no details | Public minimal probes only; not a claim of dependency/enterprise readiness |
| Prometheus | Dedicated METRICS_TOKEN | Scoped bearer required; no public scrape/Actuator env endpoint |
| Native diagnostics / graph | Health / connector / investigation / graph-admin distinct capabilities | Demo capabilities do not unlock non-demo ingress |
| KB ingestion/review/read/feedback | Distinct writer/reviewer/reader/feedback capabilities; independent submitted_by checks | No enterprise identity or human quality approval fabricated |
| Remediation | Planner/approver/executor/verifier capabilities; simulator only | Enterprise actions denied; no registered real executor |
| Next console | Signed expiring role cookie + backend-bound capabilities, exact Origin | Demo identity only; enterprise OIDC/RBAC absent, deployment BLOCKED |

| Connector | Available implementation / positive evidence | Certification limits |
|---|---|---|
| Kafka KRaft | Actual local SASL Admin metadata/lag; denied write-principal tests | No enterprise TLS/SASL/OAuth/Kerberos/rotation certificate |
| Legacy ZooKeeper | Actual digest-ACL isolated server; no znode mutations; KRaft never contacts ZK | No enterprise ensemble/realm/session renewal certificate; legacy Kafka lab is representative only |
| Spark / YARN | Fixed authenticated REST GET; MOCKED semantic/auth/outage/version tests | Private principal, TLS/mTLS, token refresh, multi-tenant cluster ACLs NOT_TESTED |
| HDFS | Fixed HA/read diagnostics; MOCKED REST failures/semantics | Kerberos realm, proxy-user ACL, failover and cert rotation NOT_TESTED |
| Hive HS2 | Fixed bounded JDBC read, resource closure; optional driver profile | Native production driver/TLS/Kerberos/tenant auth NOT_TESTED; bundled standalone driver audit incomplete |
| Hive Metastore | Actual digest-SASL Thrift fixture with fixed metadata reads | Production realm, TLS and identity refresh NOT_TESTED |
| Kudu | Official read RPC interface / bounded fixture tests | Native private cluster, principals, certs and ACLs NOT_TESTED |
| NiFi / Airflow | Fixed GET/status adapters and MOCKED semantic/auth negatives | Enterprise token rotation/tenant policy/certification NOT_TESTED |
| Cloudera Manager / Atlas | Fixed read interfaces; MOCKED request/version/error tests | Enterprise API permissions, realm and metadata visibility NOT_TESTED |
| Ranger (optional) | Unsupported/unconfigured is explicit | NOT_CERTIFIED; no guessed policy-write capability |

All enterprise connector certificates are BLOCKED, owned by integration/security
leads before Prompt 16/cutover. Fixture least-privilege proof applies only to its
actual local protocol/server, never to a named private service. No live credentials
were requested, printed, used or inferred. Separate tenant negatives in JUnit prove
application boundaries, not external cluster authorization certification.

Rotation runbook (not executed): independently authorize; generate distinct scoped
replacement credentials; update only the owning secret store/service; expire old
sessions/provider ciphertext keys according to reviewed recovery policy; verify old
credential denial and new read scope in staging; retain redacted audit. Current demo
rotation requires a controlled server restart and invalidates signed sessions; there
is no OIDC refresh/revocation or dual-key automatic rotation implementation.
