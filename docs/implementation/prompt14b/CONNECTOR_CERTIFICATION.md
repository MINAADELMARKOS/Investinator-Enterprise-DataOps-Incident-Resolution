# Read-only certification inventory

Authenticated GET /api/v1/connectors/certification combines the existing bounded
health probes with the supported fixed read contracts. It accepts NO URL, SQL,
command, tenant or capability override. Tenant comes from server configuration;
the catalog's former hardcoded demo label is corrected for non-default demo tenants.

All 14 registered native/REST/optional adapters retain their existing protocol,
version/auth/capability and negative tests: Kafka KRaft vs legacy/ZooKeeper guard,
Spark/YARN/HDFS/Hive HS2 + metastore/Kudu/NiFi/Airflow, optional Impala/CM/Atlas/Ranger.
Unsupported capabilities (including representative optional vendor gaps) remain
NOT_TESTED. Health success/fixture transport is NOT a private certificate.

Inventory always returns production_certificate=BLOCKED and
native_ingestion_authorized=false. Missing target credentials produce NOT_CONFIGURED;
otherwise enterprise status is NOT_CERTIFIED, including healthy local/mocked checks.
Each row names required real version/mode, TLS/realm/rotation, denied-write and tenant
ACL evidence, and integration/security reviewer role. No caller-supplied certificate
or health response text can authorize monitoring ingestion or production execution.
No certificate, private cluster query, OIDC identity or native event adapter is added.

Regression: 26 protocol/monitor cases pass; final 15-case monitoring/certification API
suite passes. The 3000-duplicate test is a short functional burst; monitor-burst.json
records its actual case and suite durations. It is not extended production soak.
Shared history retention, native holdback/drop accounting, full vendor capability
coverage and independently signed private certification remain BLOCKED/NOT_TESTED.
