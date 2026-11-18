# Prompt 13 — Repeatable lab and regression acceptance

2026-11-18. **PASS for the isolated laboratory/development gate.** All five hosted
jobs passed on implementation commit `f1488a70e906efa6932fa72066d9b0e56cf5b15c`.
Production release remains **BLOCKED**. Stop after Prompt 13.

[Hosted run 44391359760](https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/actions/runs/44391359760)
validates Python/CLI, frontend, Java/Testcontainers, full local E2E and dependency
security. Evidence is the public GitHub job conclusions and scanner annotation;
authenticated logs/artifacts could not be downloaded because API access is denied.
Final follow-up commits contain reports only, not a new implementation-validation claim.

## Source and publication

| Checkpoint | Factual state |
|---|---|
| Prompt 12 | `bfe757b`, completed and committed before Prompt 13 |
| Remote main observed at synchronization | `afb8183`; owner merged earlier PR #6 |
| Recovery point | Local `backup/pre-p13-bfe757b` |
| Reconciliation | Normal main-into-feature merge `9924bc5`; no conflicts |
| Initial feature remote | `6e981b9` |
| Lab/CI implementation | `216467a`, port correction `4b484b4`, acceptance `4597517` |
| Advisory diagnostics/fixes | `f717b12`, `f1488a7` |
| Verified pushed implementation | `f1488a70e906efa6932fa72066d9b0e56cf5b15c` |
| Branch | [feature/java-ai-audit](https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/tree/feature/java-ai-audit) |

Existing commits, private schema ownership, Python default, Next.js 16.4.5,
evidence and volumes are preserved. Multiple checkpoints enabled clean-clone and
hosted validation; no history was rewritten. This task did not merge into main,
publish Pages/the cloud environment, change production routing or make paid GPT calls.

## Implemented and reproduced

The owned eight-service Java/Next/PostgreSQL/OpenSearch/OTel/Fluent Bit/Prometheus/
Kafka lab has pinned images and bounded operations. `investigate` performs fixed
scoped graph/correlation reads. `restart api` aliases only the owned Java service;
unknown targets fail before private state is read. `down` retains volumes; only
explicitly confirmed owned `reset-demo` can remove them.

KRaft and representative legacy demos each emit ten SIMULATED events over actual
Java/JDBC transport: network -> controller/session -> broker lag -> Spark -> dataset.
Each opens one bounded incident and ranks network first with alternatives. Named
retries retain incident/event identities. KRaft never queries ZooKeeper. Legacy is
explicitly a representative protocol fixture, not a real legacy Kafka cluster.
Actual SASL Kafka Admin and digest-ACL ZooKeeper protocol tests remain separate.

A clean local clone first reproduced `4b484b4` with shared dependency/image caches;
see fresh-checkout-initial.json. After the fixes, the same retained-data clone
reproduced `f1488a7`: unchanged up preserves container identity, 200 identical
redeliveries remain bounded, API restart retains correlation, role/tenant requests
fail correctly and actual Java/default-Python browser flows pass. Fourteen browser
checks include bound graph, observations, uncertainty and separate governed
recovery/rollback sampling. All eight dependency stop/start cases detect failure,
recover and show no cascading restart/replacement. Hosted E2E separately validates
an actual runner checkout of `f1488a7`. No cold-host/air-gap guarantee is inferred.

## Executed checks

| Check | Result / provenance |
|---|---|
| Java Maven Verify after dependency fixes | 107 tests; zero failures/errors/skips; real PG/OpenSearch/Kafka/ZK fixtures and labeled SDK mocks |
| CLI | 29 tests pass, including ownership, target/scope denial, idempotence and non-billing paths |
| Python | 12 pytest; Ruff; mypy 17 files; baseline 44 routes/42 paths/13 tables + links pass |
| Frontend | npm ci/build/types; 8 contracts; npm audit zero vulnerabilities; Next 16.4.5 retained |
| Browser | 14 checks; real Java + disposable default Python; only declared diagnostic outage is MOCKED |
| Native evidence | Scoped search, 403/422, redaction, trace/metrics, corrupt-log rejection and bounded outage/liveness pass |
| Actual process restart | Monitoring offset/dedupe/metrics and remediation receipt/retry/sample/separate rollback pass after patch |
| Owned lab | Up/unchanged-up/doctor/health/smoke/restart and all eight failure injections pass |
| Python advisory audit | 61 installed distributions; zero known findings after patched pip 26.3 |
| Java hosted OSV | PASS; explicit public annotation: `resolved_packages=233; findings=0` |
| Hosted CI | All five jobs completed successfully on run 44391359760 |
| Credentials | Actual credentials of both owned labs scanned against public files; zero matches, no tracked .env; fixed CI upload allowlist excludes private runtime config |

See test-summary.json, junit-summary.json, negative-matrix.json, fresh-checkout.json,
outages.json, browser-with-graph.json, evidence-pipeline.json, monitor-process.json,
remediation-process.json, fixture-hashes.json and hosted-ci.json.

Provider timeout/failure tests use the official SDK with MOCKED_PROVIDER transport;
actual GPT remains NOT_CONFIGURED. `ai-check --live` exits 2 without credentials,
with zero paid calls. Fixture ranks are functional diagnostics; expert-reviewed
production RCA accuracy and business outcomes are not measured or manufactured.

## Failures corrected without weakening gates

The initial fresh attempt refused occupied default ports; the harness now reserves
separate loopback ports before first setup. The failed configuration was retained
and corrected source tested in a separate clean clone. Host disk exhaustion then
prevented startup/shard allocation. Removed regenerable task npm caches, obsolete
untagged task builds and unused Elasticsearch diagnostic images referenced by no
container, freeing 16 GB. Retried allocation only in the named test OpenSearch.
No repository, credential, evidence or database volume was deleted. Both complete
local runs subsequently passed; no reset or assertion bypass forced success.

Initial hosted run 44389688456 passed all four functional jobs but correctly failed
Java security. Run 44390174065 exposed seven advisories in Snappy, LZ4 and OTel API
using public annotations because authenticated logs were unavailable. Verified
public advisories prescribe Snappy 1.1.10.9, LZ4 1.11.4 and OTel 1.67.0. Two codec
pins and an aligned stable OTel BOM fix them; no suppression or severity filter was
introduced. Full Java/native/E2E checks were rerun and the final hosted scan passed.
All seven IDs and public sources are in java-advisories.json. Local OSV egress
failure is retained separately in java-audit-local-blocked.json; hosted evidence
resolves the default-tree advisory gate. The inactive optional Hive JDBC profile
is not covered by this default-tree scan.

## Operational handover and remaining blockers

[Reproduction](REPRODUCTION.md) gives exact commands, source labels, cleanup and
resource/timeout budgets (25-minute E2E / 12-minute security jobs). Shared local
caches and test virtualenv reuse are explicit; PYTHONPATH selected clean source.
The disposable second lab was brought down with volumes retained. Primary owned
lab remains healthy on the patched build. No uncontrolled enterprise side effect.

A new PR could not be created: GitHub GraphQL returns Forbidden through the proxy.
[Prepared comparison](https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/compare/main...feature/java-ai-audit?expand=1)
and PR_BODY.md are ready; earlier PR #6 was merged by the remote owner. Owner:
repository maintainer/network administrator for API access and PR creation.

GO for local/demo development. Production remains blocked by enterprise identity/
independent change approval, native private-connector certification, actual provider
credentials/budget, independent human gold review, TLS/security/HA/DR and measured
operational/business SLOs. Python minimum-only dependency declarations and inactive
optional profile auditing also need release hardening. No approval is fabricated.
Prompt 14 requires a new user request. No merge, deployment or cutover follows.
