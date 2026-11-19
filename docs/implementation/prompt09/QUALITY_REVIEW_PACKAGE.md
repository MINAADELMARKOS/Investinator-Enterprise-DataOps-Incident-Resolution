# Independent knowledge review package — Prompt 09B

Human reviewer: **PENDING**. Human approval: **unfilled**. No independent sign-off exists.

These are developer-authored SIMULATED fixtures on actual local PostgreSQL/OpenSearch transport.
They are not a held-out set or production incidents. No thresholds, BM25 weights or correlation rules were tuned.
Read-only functional GO is separate from expert quality approval and production deployment.

Review dataset SHA256: `a0e7a4b5fa5bdfd6ae4c5983ab0b405dcf7cda77447a02118ff4ddf9fc29394a`.
Original Prompt 09 dataset SHA256: `fc301b09ae1fea6d1053fc8e7084bd1d15be02d517937f55c513a417426b37d0`.
Raw native observation SHA256: `4fb9762a23eff215c039ce49de7fbbd681ab21fc6bb80110276141200c0233dd`.

Sources: [exact graphs and evidence](../../../eval/knowledge_review_cases.json),
[native outputs and revision/chunk corpus](../prompt09b/raw-observations.json),
[scored report](EVAL_REPORT.json), [pending review worksheet](GOLD_REVIEW.md).

The corpus is proposed fixture guidance, not an expert-validated operational runbook library.
It contains immutable revisions 1 and 2 plus foreign-tenant, private-ACL and version-99.9 distractors.
Distinct SIMULATED reviewer credentials exercise software approval boundaries, not human governance.
Inspect each proposed source for correctness/licensing before any operational adoption.

Scoring: detection means nonempty correlation candidates, not autonomous Prompt 10 monitoring.
Top-1/top-3 RCA denominators exclude the healthy case. Precision@k divides by k even with fewer hits;
recall divides by proposed relevant documents. Every metric includes its numerator/denominator.
Historical knowledge cannot itself confirm a cause. BM25 only; no vectors, embeddings or training.

## kafka-kraft

Platform: **Kafka 4.3 / KRAFT**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-kafka-kraft`.
Graph `quality-kafka-kraft` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `quorum -> network`; `broker -> quorum`; `consumer -> broker`; `spark -> consumer`; `dataset -> spark`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-kafka-kraft-00` | network / NETWORK_UNSTABLE | ABNORMAL | 2026-11-17T00:00:00Z |
| `Q-kafka-kraft-01` | quorum / QUORUM_UNAVAILABLE | ABNORMAL | 2026-11-17T00:00:40Z |
| `Q-kafka-kraft-02` | broker / BROKER_ERROR | ABNORMAL | 2026-11-17T00:01:20Z |
| `Q-kafka-kraft-06` | broker / DISK_EXHAUSTED | HEALTHY | 2026-11-17T00:01:25Z |
| `Q-kafka-kraft-03` | consumer / CONSUMER_LAG | ABNORMAL | 2026-11-17T00:02:00Z |
| `Q-kafka-kraft-04` | spark / SPARK_DELAY | ABNORMAL | 2026-11-17T00:02:40Z |
| `Q-kafka-kraft-05` | dataset / DATASET_STALE | ABNORMAL | 2026-11-17T00:03:20Z |

Proposed originating hypothesis: `network:NETWORK_UNSTABLE`.
Proposed competing hypotheses: quorum:QUORUM_UNAVAILABLE, broker:BROKER_ERROR.
Actual deterministic ranking: network:NETWORK_UNSTABLE, quorum:QUORUM_UNAVAILABLE, broker:BROKER_ERROR.
Counterevidence/contradictions: Disk-health evidence is a capability-specific counterexample, not proof all broker resources are healthy.
Blind spots: No actual network experiment, quorum membership or customer SLO evidence.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `network packet loss metadata quorum instability`. Proposed relevant source: `review-kafka-kraft` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `d3d5890a2e8c819cfd7e2359961219dc6406db1c7e0c1402434832518342b03f`.
Approved SIMULATED chunk IDs: `KB-346cdbe2b1391a646c6c0ab443fbf37cde040ba79c3825fe262e4912e2375801`.
Readable exact text: Network packet loss destabilizes KRaft metadata quorum. Read controller quorum, broker ISR and consumer lag before proposing changes.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 2.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## legacy-zookeeper

Platform: **Kafka 3.9 / LEGACY_ZOOKEEPER**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-legacy-zookeeper`.
Graph `quality-legacy-zookeeper` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `zk -> network`; `broker -> zk`; `consumer -> broker`; `spark -> consumer`; `dataset -> spark`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-legacy-zookeeper-00` | network / NETWORK_UNSTABLE | HEALTHY | 2026-11-17T00:00:00Z |
| `Q-legacy-zookeeper-01` | zk / ZK_SESSION_EXPIRED | ABNORMAL | 2026-11-17T00:00:00Z |
| `Q-legacy-zookeeper-02` | broker / BROKER_ERROR | ABNORMAL | 2026-11-17T00:01:00Z |
| `Q-legacy-zookeeper-03` | consumer / CONSUMER_LAG | ABNORMAL | 2026-11-17T00:02:00Z |
| `Q-legacy-zookeeper-04` | spark / SPARK_DELAY | ABNORMAL | 2026-11-17T00:02:40Z |
| `Q-legacy-zookeeper-05` | dataset / DATASET_STALE | ABNORMAL | 2026-11-17T00:03:20Z |

Proposed originating hypothesis: `zk:ZK_SESSION_EXPIRED`.
Proposed competing hypotheses: broker:BROKER_ERROR.
Actual deterministic ranking: zk:ZK_SESSION_EXPIRED, broker:BROKER_ERROR.
Counterevidence/contradictions: Healthy network weakens a generic network claim; no abnormal network candidate is expected.
Blind spots: Legacy Kafka mode is fixture-declared, not discovered from a real legacy broker.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `ZooKeeper session expired legacy broker coordination`. Proposed relevant source: `review-legacy-zookeeper` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `754f8b0cd5aa38630ec89bee266f61745aa0d9a907c798e9c8ca8ccdde480923`.
Approved SIMULATED chunk IDs: `KB-edd0f74abfba47bf28a7be28653fbd5e336131d9ed69743a81a91352bc5055c2`.
Readable exact text: Legacy Kafka ZooKeeper session expiration affects broker coordination. Inspect authenticated legacy session evidence; do not attach this runbook to KRaft.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 1.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## hdfs-ha

Platform: **HDFS 3.4 / UNKNOWN**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-hdfs-ha`.
Graph `quality-hdfs-ha` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `datanode -> namenode`; `pipeline -> datanode`; `dataset -> pipeline`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-hdfs-ha-00` | namenode / HDFS_HA_UNAVAILABLE | ABNORMAL | 2026-11-17T00:00:00Z |
| `Q-hdfs-ha-01` | datanode / HDFS_MISSING_BLOCKS | ABNORMAL | 2026-11-17T00:01:00Z |
| `Q-hdfs-ha-02` | pipeline / SPARK_DELAY | ABNORMAL | 2026-11-17T00:02:00Z |
| `Q-hdfs-ha-03` | dataset / DATASET_STALE | ABNORMAL | 2026-11-17T00:03:00Z |

Proposed originating hypothesis: `namenode:HDFS_HA_UNAVAILABLE`.
Proposed competing hypotheses: datanode:HDFS_MISSING_BLOCKS.
Actual deterministic ranking: namenode:HDFS_HA_UNAVAILABLE, datanode:HDFS_MISSING_BLOCKS.
Counterevidence/contradictions: No contemporaneous healthy counterevidence was supplied; absence is not health.
Blind spots: No paired NameNode/journal quorum probe or authorized failover experiment.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `HDFS no active NameNode HA failover`. Proposed relevant source: `review-hdfs-ha` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `06fad57afbbb7740c240879b25997f5ab416ff320e37606bfbba4db5e2fb0c9e`.
Approved SIMULATED chunk IDs: `KB-a20730b4bd2cffb0b7adf30932f0fe3e5476e4a7e60dc4248504d7def396b5d1`.
Readable exact text: When HDFS has no active NameNode during an HA failover, inspect active standby state, journal quorum and DataNode liveness. Never trigger failover from diagnosis.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 1.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## kudu-tablet

Platform: **Kudu 1.18 / UNKNOWN**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-kudu-tablet`.
Graph `quality-kudu-tablet` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `tserver -> network`; `pipeline -> tserver`; `dataset -> pipeline`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-kudu-tablet-00` | tserver / KUDU_TABLET_UNAVAILABLE | ABNORMAL | 2026-11-17T00:00:00Z |
| `Q-kudu-tablet-01` | pipeline / SPARK_DELAY | ABNORMAL | 2026-11-17T00:01:00Z |
| `Q-kudu-tablet-02` | dataset / DATASET_STALE | ABNORMAL | 2026-11-17T00:02:00Z |
| `Q-kudu-tablet-03` | network / NETWORK_UNSTABLE | ABNORMAL | 2026-11-17T00:08:20Z |

Proposed originating hypothesis: `tserver:KUDU_TABLET_UNAVAILABLE`.
Proposed competing hypotheses: network:NETWORK_UNSTABLE.
Actual deterministic ranking: tserver:KUDU_TABLET_UNAVAILABLE, network:NETWORK_UNSTABLE.
Counterevidence/contradictions: The late network event is contradicted by earlier downstream failures beyond the 30-second skew tolerance.
Blind spots: Missing tablet replica/leader quorum and host evidence; master connectivity alone is insufficient.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `Kudu tablet unavailable leader quorum`. Proposed relevant source: `review-kudu-tablet` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `0f939e49167919fe5962a0d2a2faaf926f75a719c2f54f702ce0b7ebbdb0bf22`.
Approved SIMULATED chunk IDs: `KB-7b52a27f3493a83c32b5ed437b93a770374abbe1a06b28e754b97edfff351861`.
Readable exact text: Kudu tablet unavailable requires read-only tablet leader, replica and tserver evidence. Master connectivity alone does not prove tablet quorum.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 1.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## nifi-backpressure

Platform: **NiFi 2.0 / UNKNOWN**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-nifi-backpressure`.
Graph `quality-nifi-backpressure` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `nifi -> storage`; `pipeline -> nifi`; `dataset -> pipeline`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-nifi-backpressure-00` | storage / HDFS_MISSING_BLOCKS | ABNORMAL | 2026-11-17T00:00:00Z |
| `Q-nifi-backpressure-01` | nifi / NIFI_BACKPRESSURE | ABNORMAL | 2026-11-17T00:01:00Z |
| `Q-nifi-backpressure-02` | pipeline / SPARK_DELAY | ABNORMAL | 2026-11-17T00:02:00Z |
| `Q-nifi-backpressure-03` | dataset / DATASET_STALE | ABNORMAL | 2026-11-17T00:03:00Z |

Proposed originating hypothesis: `storage:HDFS_MISSING_BLOCKS`.
Proposed competing hypotheses: nifi:NIFI_BACKPRESSURE.
Actual deterministic ranking: storage:HDFS_MISSING_BLOCKS, nifi:NIFI_BACKPRESSURE.
Counterevidence/contradictions: No healthy storage evidence supplied; this fixture proposes an upstream storage cause, not an automatic NiFi fault.
Blind spots: No per-connection queue age, backpressure settings, throughput or live HDFS evidence.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `NiFi backpressure queue age backlog`. Proposed relevant source: `review-nifi-backpressure` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `93557dc96133b3e285fcbfd92f824eb44429b2a58a584209c0865989122bd6b9`.
Approved SIMULATED chunk IDs: `KB-39c3c2b70d675a1a62618dbd64240abfa439454a1bdbf02f55428b58bc6533cf`.
Readable exact text: NiFi backpressure and queue age indicate flow backlog. Inspect connection capacity and bulletins without dropping queues or restarting processors.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 1.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## spark-oom

Platform: **Spark 4.0 / UNKNOWN**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-spark-oom`.
Graph `quality-spark-oom` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `executor -> network`; `pipeline -> executor`; `dataset -> pipeline`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-spark-oom-00` | executor / SPARK_EXECUTOR_OOM | ABNORMAL | 2026-11-17T00:00:00Z |
| `Q-spark-oom-01` | pipeline / SPARK_DELAY | ABNORMAL | 2026-11-17T00:01:00Z |
| `Q-spark-oom-02` | dataset / DATASET_STALE | ABNORMAL | 2026-11-17T00:02:00Z |
| `Q-spark-oom-03` | network / NETWORK_UNSTABLE | ABNORMAL | 2026-11-17T00:08:20Z |

Proposed originating hypothesis: `executor:SPARK_EXECUTOR_OOM`.
Proposed competing hypotheses: network:NETWORK_UNSTABLE.
Actual deterministic ranking: executor:SPARK_EXECUTOR_OOM, network:NETWORK_UNSTABLE.
Counterevidence/contradictions: Late network instability is contradicted by earlier OOM/downstream timing.
Blind spots: Heap dump, driver/executor distinction, GC, skew and resource allocation are unmeasured.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `Spark executor OutOfMemory lost stage failure`. Proposed relevant source: `review-spark-oom` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `d198902b0732607222a69c023c134603d2f3026c4781c4061cdad1cf9296aa2e`.
Approved SIMULATED chunk IDs: `KB-ac457d6b6803c531f6a866168730679469807f86cf7b126d99dd599ed9b77307`.
Readable exact text: Spark executor OutOfMemory and lost executors can delay stages and datasets. Inspect executor memory, GC and failed stages before recommending resource changes.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 1.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## hive-metadata

Platform: **Hive 4.0 / UNKNOWN**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-hive-metadata`.
Graph `quality-hive-metadata` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `hms -> database`; `hs2 -> hms`; `pipeline -> hs2`; `dataset -> pipeline`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-hive-metadata-00` | hms / HIVE_METADATA_FAILURE | ABNORMAL | 2026-11-17T00:00:00Z |
| `Q-hive-metadata-01` | hs2 / UNKNOWN | ABNORMAL | 2026-11-17T00:01:00Z |
| `Q-hive-metadata-02` | pipeline / SPARK_DELAY | ABNORMAL | 2026-11-17T00:02:00Z |
| `Q-hive-metadata-03` | dataset / DATASET_STALE | ABNORMAL | 2026-11-17T00:03:00Z |
| `Q-hive-metadata-04` | database / DISK_EXHAUSTED | ABNORMAL | 2026-11-17T00:08:20Z |

Proposed originating hypothesis: `hms:HIVE_METADATA_FAILURE`.
Proposed competing hypotheses: database:DISK_EXHAUSTED.
Actual deterministic ranking: hms:HIVE_METADATA_FAILURE, database:DISK_EXHAUSTED.
Counterevidence/contradictions: Late backing-disk evidence cannot explain the earlier metadata failure by timing alone. HS2 UNKNOWN is not a successful query.
Blind spots: No metastore SASL read, backing-DB state, HS2 query or partition listing proof.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `Hive metastore unavailable metadata failure`. Proposed relevant source: `review-hive-metadata` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `007572137c1afceedcbe6fceee11a54f770a94a0fca28fee7bc6fd4d1b86c3a1`.
Approved SIMULATED chunk IDs: `KB-be3f903361ae6e40f69f122cc09af35ed7f55c1fabe1497c21921a16320f0d5d`.
Readable exact text: Hive metastore unavailable is distinct from HiveServer2 SELECT responsiveness. Inspect SASL metastore namespace reads and backing database separately.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 1.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## healthy-false-positive

Platform: **Kafka 4.3 / KRAFT**.
Tenant/principal: `demo` / `demo.senior_sre`; incident `QUALITY-healthy-false-positive`.
Graph `quality-healthy-false-positive` revision 1; all edges SIMULATED, known from their recorded validity/discovery times.
Environment/cluster: disposable local test / synthetic graph identity; enterprise identity UNKNOWN.

Dependency edges (child DEPENDS_ON parent): `quorum -> network`; `broker -> quorum`; `dataset -> broker`.

| Exact evidence ID | Entity / signal | State | Observed at |
|---|---|---|---|
| `Q-healthy-false-positive-00` | network / NETWORK_UNSTABLE | HEALTHY | 2026-11-17T00:00:00Z |
| `Q-healthy-false-positive-01` | quorum / QUORUM_UNAVAILABLE | HEALTHY | 2026-11-17T00:00:40Z |
| `Q-healthy-false-positive-02` | broker / DISK_EXHAUSTED | HEALTHY | 2026-11-17T00:01:20Z |
| `Q-healthy-false-positive-03` | dataset / DATASET_STALE | HEALTHY | 2026-11-17T00:02:00Z |

Proposed originating hypothesis: `ABSTAIN: no originating candidate`.
Proposed competing hypotheses: none from supplied observations.
Actual deterministic ranking: ABSTAIN.
Counterevidence/contradictions: Only capability-specific healthy observations are supplied; historical advice must not create a failure.
Blind spots: A small healthy sample does not certify the whole cluster or measure production false positives.
Expected uncertainty: HYPOTHESIS only; never confirmed cause or permission to act.

BM25 query: `healthy baseline no failure false positive`. Proposed relevant source: `review-healthy-false-positive` revision 2.
Source `repo:eval/knowledge_review_cases.json`, owner `implementation-review-fixture`, time `2026-11-17T00:00:00Z`; hash `7a037946eccdbad6737e8f2d6fe1913b6ccf649750289adc0dde088588edd8de`.
Approved SIMULATED chunk IDs: `KB-9e81dfcd9e40fa8876e03fc513a3d990b182006d7c8e7c0876fa5f20c9eaba14`.
Readable exact text: A healthy Kafka baseline with no failure must not be attributed to ZooKeeper, disk or network merely from related historical advice.
Native retrieval status `DEGRADED`, partial `True`; rejected index records 1.
Stale index revisions may be rejected by authoritative SQL; partial/degraded status is not hidden.

Independent reviewer notes/corrections: **unfilled**.
Reviewer identity, role, timestamp, approved dataset hash and decision: **unfilled**.

## Review procedure and release of labels

Review clocks/topology/version/mode, alternative causal chains and contradictory evidence independently.
Correct proposed labels in a separately versioned review record before approving; sign the exact dataset hash.
Retain this original fixture result. Create a genuinely held-out set before tuning or claiming improved accuracy.
Source/tenant/ACL/version and injected-instruction checks must remain negative tests.
Required review fields: case ID, source hashes, reviewer role/identity, timestamp, corrected expected labels,
accepted blind spots and uncertainty, decision and rationale. No field is auto-filled as approval.
