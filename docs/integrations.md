# Integration status — current entry

Use the [component As-Is/To-Be matrix](handover/STATUS_MATRIX.md) for actual build
status, test provenance and missing certification. The Java lab uses authenticated
OpenSearch, native local SASL Kafka/KRaft reads, isolated legacy ZooKeeper/SASL Thrift
fixtures and explicit unconfigured/private-service statuses. REST adapters exist
for Spark/YARN/HDFS/NiFi/Airflow/Cloudera Manager/Atlas, but their fixture coverage is
not private-cluster certification. Kudu/HS2 driver/realm/cert/refresh coverage is
incomplete. Ranger is explicitly unsupported/unconfigured. KRaft never queries ZK.

[Connector settings](handover/CONFIGURATION.md),
[per-adapter detail](implementation/prompt06/CONNECTORS.md) and
[certification/auth matrix](implementation/prompt14/AUTH_MATRIX.md) identify scopes.
Native caller/model arguments cannot select arbitrary endpoints, SQL, tenant or writes.
No enterprise service is claimed LIVE because a URL/credential or dependency exists.

The preserved Python route has its own Kafka broker health and optional Elasticsearch
log adapter; it is not the Java OpenSearch/connector platform. Prometheus collector
scraping, Java private metrics and legacy Python metrics are distinct configurations.
Static Pages hosts none of them. Check actual native health timestamps and mode;
missing integrations remain NOT_CONFIGURED/NOT_TESTED, never seeded-score health.
