# Diagnostic connector contract and deployment matrix

`DiagnosticConnector` is trusted backend code. Its declared capabilities are
`health`, `metrics`, `logs`, `topology`, `config`, `metadata`, `readOnlyChecks`.
A declaration describes a supported read surface, not successful connectivity.
Unsupported operations return NOT_TESTED/UNSUPPORTED_CAPABILITY. Raw logs and
automatic topology inference are not implemented by these adapters; normalized
log search remains the separate tenant-filtered evidence API. Prompt 07 owns
inferred graph/correlation behavior. No command execution interface was added.

`GET /api/v1/connectors` and `/{service}/{capability}` require the distinct
`CONNECTOR_API_TOKEN` bearer credential, the `diagnostics.read` role embodied by
that backend token, and the `CONNECTOR_ALLOWED_SERVICES` allowlist. The current
identity is explicitly local demo; all non-demo requests remain fail-closed until
OIDC/tenant identity is independently implemented. This is not production RBAC
certification. No browser/model receives connector credentials. Every request
parameter is rejected, including URL, tenant, SQL, command and unrestricted DSL.
Only GET routes exist; mutating methods are denied even with a valid token.
`config` returns a safe descriptor, never the endpoint URL or a secret value.
Other supported reads run through the bounded health aggregator.

All endpoint/identity values are injected into the Java backend by the deployment
operator. No real cluster defaults to localhost. REST prefixes:
`CONNECTOR_<SERVICE>_` (uppercase; hyphens become underscores). Set `URL`, `AUTH`
(`BEARER`, `BASIC`, `SPNEGO`, `MTLS`) and the matching `TOKEN` or `USER`/`PASSWORD`.
SPNEGO uses JVM JAAS/GSS credentials, mutual authentication and no credential
delegation. mTLS uses the JVM keystore/truststore. TLS certificates are verified;
no redirects are followed. An explicit `LOCAL_FIXTURE=true` allows HTTP only to
literal loopback, for authenticated tests. REST reads have 2s connection/3s request
timeouts and 1 MiB body cap. The outer runner has a default 6s per-probe budget,
12s aggregate deadline, four workers, one transient retry with jitter and circuit
cooldown. No retry on authorization/contract/version failure. Large fanout may
produce partial results; it is not silently promoted to healthy.

REST `MAX_ITEMS` defaults to 100, maximum 1000. Other numeric thresholds are
backend configuration under the same service prefix. Response facts contain only
selected counts, state and version, not raw JMX/config/environment/credential
payloads. A successful scoped read does not verify every service subsystem;
`capability_gaps` lists dimensions that were not observed. Version families are
checked before semantic reads. Missing credentials yield NOT_CONFIGURED; wrong
credentials, malformed contracts and incompatible versions cannot be HEALTHY.

| Adapter / version | Fixed protocol and reads | Minimum permission / semantics / limitations |
|---|---|---|
| Kafka KRaft or legacy | Official Kafka AdminClient: describeCluster, describeConfigs broker, list/describeTopics, listConsumerGroups, group offsets and latest offsets; describeMetadataQuorum only if `process.roles` proves KRaft | Cluster Describe + DescribeConfigs; Topic Describe; Group Describe. SASL/TLS settings are `KAFKA_*`. No Produce/Consume/Create/Alter/Delete permissions. KRaft evidence includes leader, voters, high watermark; partition leader/ISR/offline counts and sampled max consumer lag. Missing groups/offsets mean lag NOT_CONFIGURED. Mode is UNKNOWN unless actual config proves it. Exact broker release is not reliably exposed by Admin; do not invent one from client version. Up to configured max topics/groups/partitions; a single topic's native metadata response can still be large. |
| Legacy ZooKeeper 3.5–3.9 | Native digest-authenticated getData on fixed `/investinator-health`, plus read-only `srvr` server version | Explicit loopback legacy fixture only: `CONNECTOR_ZOOKEEPER_URL=zk://...`, LOCAL_FIXTURE, USER, PASSWORD; READ on that fixed znode; server must explicitly allow `srvr`. Actual Kafka config must prove LEGACY_ZOOKEEPER before any ZK contact. KRaft/unknown is NOT_TESTED without a ZK request. No production ZK endpoint is accepted. No create/setData/delete/ACL/reconfig diagnostic. |
| Spark 3.x / 4.x | Monitoring REST v1 `/version`, bounded `/applications`, sampled application executors/stages | Monitoring read role or authenticated proxy/SPNEGO. Executor memory threshold `MAX_MEMORY_PERCENT` (90), failed stages, inactive executors, max three apps (`MAX_APPS`, up to 10). Structured streaming backlog, driver dependency errors and logs require a separately scoped metrics/log source and remain explicit gaps. |
| YARN Hadoop 3.x | ResourceManager `/ws/v1/cluster/info`, metrics, bounded apps, scheduler | RM read/view permissions; no submission/kill. HA ACTIVE, pending containers (`MAX_PENDING_CONTAINERS`,100), available memory, ACCEPTED duration (`MAX_PENDING_SECONDS`,600), failed apps. Scheduler endpoint is verified; arbitrary nested queue pressure is not inferred from overall resource capacity. |
| HDFS Hadoop 3.x | Fixed NameNode JMX bean queries for NameNodeInfo, NameNodeStatus, FSNamesystem | JMX read/view through SPNEGO/TLS proxy; no WebHDFS writes/failover. Observe active/standby, missing/corrupt/under-replicated blocks (`MAX_UNDER_REPLICATED_BLOCKS`,0), live/dead DNs and safe mode. Runner measures metadata read latency only. It does not prove paired NN availability or data-path latency. |
| HiveServer2 server 3.x / 4.x | Optional official Hive JDBC 4.2.1; database product metadata and exactly `SELECT 1` with query timeout 3s/maxRows1 | `CONNECTOR_HIVE_JDBC_URL`, USER/PASSWORD with verified TLS, or Kerberos principal + JVM JAAS. Only safe connection options accepted; duplicate TLS and init scripts rejected. Backend appends driver socket timeout (binary milliseconds vs HTTP seconds per pinned driver implementation). Requires `-Phive-jdbc` build. No user SQL, table writes or backend/metastore health inference. Driver absent = NOT_TESTED. Read-only flag requested; scoped server authorization remains essential if driver lacks flag enforcement. |
| Hive Metastore server 3.x / 4.x | SASL protected binary Thrift `getVersion`, `get_database("default")`; 1 MiB message/frame cap, recursion32, 2s connect/read | `CONNECTOR_HIVE_METASTORE_URL=thrift://...`; GSSAPI auth-conf with backend JAAS/service principal. DIGEST-MD5 supported only for explicit loopback fixture credentials. Minimum read default-namespace metadata. No get_all_databases, arbitrary methods, DDL or HS2 query. Version must actually be exposed; incompatible/hidden version is NOT_TESTED or read failure. Backing metastore RDBMS remains separately NOT_TESTED. |
| Kudu official client 1.18.2 | Authenticated encrypted native RPC listTabletServers, findLeaderMasterServer, optional allowlisted table locations | `CONNECTOR_KUDU_MASTERS`, JWT or backend Kerberos Subject; optional CA_FILE; no anonymous/unencrypted RPC. Master metadata read + metadata for `TABLES` allowlist, max3 tables/100 tablets by default; MIN_REPLICAS3. No sessions, scans, table writes, rebalance. SDK capability negotiation does not expose exact server release or prove every master/replica's quorum health: these remain NOT_TESTED, and adapter does not return globally HEALTHY from mere connectivity. A table's native location response is bounded by SDK deadline plus post-read count check, not server-side tablet pagination. |
| NiFi 1.x / 2.x | `/nifi-api/flow/about`, system diagnostics, root process-group status, bounded bulletin board | Read flow, system diagnostics, bulletins scopes; OAuth bearer or TLS/SPNEGO gateway. Queue count `MAX_QUEUE_COUNT`10000, queued bytes, active threads and warning/error bulletins. Oldest queue age, connection backpressure and per-node cluster status require additional scoped sources; not inferred from root aggregate. No controller/process-group edits/start/stop/drop/listing-request writes. |
| Airflow 2.x REST v1 | Version, health, bounded active DAG listing; optional configured DAG's recent runs | Authenticated read-only API role for versions/health/DAG/DAG-run read; no triggers/task state edits. Scheduler/metadatabase health, failed sampled DAG runs (`DAG_ID`). Worker, retries, SLA/freshness unverified without scoped telemetry. Airflow 3.x is deliberately incompatible, never treated as v1. |
| Impala 4.x, optional | Authenticated debug web root `/?json` and `/backends?json` | Read debug web via authenticated TLS gateway; version + active/known backend counts. Distribution-specific JSON, no query execution or catalog alteration. JDBC responsiveness not implemented by this optional adapter. |
| Cloudera Manager 7.x, optional | `/api/version` v40+, `/cm/version`, `/clusters?view=summary` | Read-only CM role; Basic/TLS or gateway bearer. Connectivity/version and visible cluster counts only; per-cluster service health is an explicit gap. No commands/config changes. |
| Atlas 2.x, optional | `/api/atlas/admin/version`, `/admin/status` | Read-only status/version via authenticated gateway; ACTIVE server state only. No entity/lineage changes; no data lineage freshness claim. |
| Ranger, optional | No unverified distribution endpoint is guessed | Endpoint/auth present still returns NOT_TESTED/UNSUPPORTED_CAPABILITY. Version adapter and scoped policy metadata contract must be supplied before enabling it. No policy writes or fabricated healthy status. |

Representative validation: authenticated WireMock GET/semantic/version/malformed/
403 tests for all supported REST adapters; actual SASL-authenticated Thrift wire
fixture for HMS; mocked JDBC query/resource-boundary tests; mocked Kudu read RPCs
and explicit unverified claims; a real embedded ZooKeeper 3.9.7 server with digest
ACLs and unchanged znode verification; Kafka mocked config-mode separation and a
real SASL KRaft lab with separate read principal and negative write authorization.
Tests never present a mock service as a private live cluster. Live private Spark,
YARN/HDFS/Hive/Kudu/NiFi/Airflow/Impala/CM/Atlas/Ranger are NOT_CONFIGURED on this
host. Kerberos KDC, private mTLS/OAuth scopes and a real legacy Kafka broker are
NOT_TESTED here. The actual ZK server test uses explicitly simulated legacy Kafka
mode evidence, and labels that source SIMULATED.

The separate [mode signatures](../../../infra/connectors/incident-signatures.json)
are simulated candidate patterns, not confirmed root causes or a correlation
engine. No Kafka mode is selected by text in a user prompt.

Protocol references: [Kafka Admin](https://kafka.apache.org/41/javadoc/org/apache/kafka/clients/admin/Admin.html),
[Spark monitoring](https://spark.apache.org/docs/4.0.0/monitoring.html),
[YARN REST](https://hadoop.apache.org/docs/r3.4.2/hadoop-yarn/hadoop-yarn-site/ResourceManagerRest.html),
[HDFS metrics](https://hadoop.apache.org/docs/r3.4.2/hadoop-project-dist/hadoop-common/Metrics.html),
[NiFi REST](https://nifi.apache.org/docs/nifi-docs/rest-api/index.html),
[Airflow 2 API](https://airflow.apache.org/docs/apache-airflow/2.10.4/stable/stable-rest-api-ref.html),
[Kudu Java](https://kudu.apache.org/docs/developing.html),
[ZooKeeper admin](https://zookeeper.apache.org/doc/r3.9.7/zookeeperAdmin.html).
Hive JDBC socket timeout units were checked in the exact 4.2.1 source artifact,
not guessed from a generic JDBC property; binary and HTTP paths differ.
