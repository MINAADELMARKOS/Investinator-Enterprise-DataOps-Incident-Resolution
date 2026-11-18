# Prompt 06 — Read-only distributed-system connectors

> 09B security reconciliation (2026-11-17): the Next.js finding described below
> is historical. Current main/local Next.js 16.4.5 package and lockfile match;
> fresh npm audit reports zero vulnerabilities. See the
> [current security assessment](../prompt09/PROMPT05_AUDIT.md#security-advisory-reconciliation).

Implemented a trusted `DiagnosticConnector` SPI, capability declarations,
version/authentication contracts, health integration, private read API and a
backend service allowlist. Existing Next.js/Python source, default routing and
production execution boundaries are preserved. See the detailed
[connector matrix](CONNECTORS.md) and ADR 013.

Adapters cover Kafka KRaft/legacy mode, isolated legacy ZooKeeper, Spark, YARN,
HDFS, HiveServer2, Hive Metastore, Kudu, NiFi, Airflow, and optional Impala,
Cloudera Manager and Atlas. Optional Ranger is explicitly NOT_TESTED rather than
using an unverified distribution endpoint. All configured native/REST operations
are fixed read contracts; no arbitrary command/URL/SQL API exists. Unsupported
capabilities and missing private credentials are explicit, not fake successes.

Validation distinguishes three sources:

- Real local infrastructure: authenticated Kafka KRaft Admin reads, denied topic
  creation/record production using the read principal, persisted five records and
  offset five across down/up, and the protected running Java connector API.
- Real protocol fixtures: actual ZooKeeper 3.9.7 server/digest ACL reads with
  unchanged znode state; actual encrypted SASL Thrift handshake plus simulated
  HMS version/namespace replies. ZooKeeper's legacy Kafka guard evidence is
  simulated, not a real legacy broker.
- Representative mocks: authenticated WireMock version/semantic/read-only and
  failure contracts for the REST services; JDBC fixed query/resource behavior;
  Kudu read RPCs and explicit missing quorum/version verification; Kafka config
  modes and the rule that KRaft/unknown never queries ZooKeeper.

Full suite: 49 Java tests, no failures/errors/skips, and 13 CLI tests pass.
The real [lab result](lab-result.json) records authorization and persistence
checks. Health/doctor/smoke acceptance is rerun after connector integration.
No private live enterprise cluster or Kerberos/mTLS identity is configured here.
No paid OpenAI call, production infrastructure change, push/merge or environment
publication was performed. Prompt 07 is not started.

Readiness is deliberately limited. Spark structured streaming/driver dependency
errors, YARN per-queue pressure, HDFS data-path/paired-NameNode latency, NiFi
connection queue age/backpressure/per-node state, Airflow worker/retry/SLA details,
Kudu full quorum and exact server release, Impala JDBC responsiveness, CM service
health and Atlas lineage freshness are explicit gaps. The adapters verify their
implemented read surfaces, not complete subsystem health. Private live credentials
are not required to develop these adapters, but their deployment validation remains
NOT_CONFIGURED/NOT_TESTED. Production identity/security review and the known
critical Next.js dependency finding remain release blockers.

Final acceptance: [test summary](test-summary.json), [doctor](doctor-example.json),
[smoke](smoke.txt), [outage matrix](failure-matrix.json) and [lab result](lab-result.json)
PASS. JSON schema, absence of generated secret values in reports, baseline contract/
source validation, CLI lint and diff checks PASS. The optional official Hive driver
profile resolves and loads; its actual TLS connection to an absent loopback service
fails within deadline. This is a negative driver compatibility test, not a private
Hive query test. The initial real ZK fixture ACL list issue was corrected.
A final smoke rerun refused an earlier CLI-owned awaiting-approval demo; smoke now
resumes only its recorded own run, persists ownership before action, serializes
operations, checks unapproved rejection and honors an already stored approval.
Three new CLI tests cover own-run resume, post-approval resume and refusal of an
unrelated active run. No existing user workflow is silently overwritten. Final
tests have no failures or skips. Prompt 06's declared adapter/read-only gate passes;
complete semantic/production readiness remains partial as listed above.

CI now includes CLI unit tests and lint alongside the unchanged Python and Java
jobs. Local commands pass; the hosted workflow was not triggered or represented
as passing. The existing frontend security-audit failure remains explicit.
