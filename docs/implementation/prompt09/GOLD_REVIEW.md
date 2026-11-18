# Independent human evaluation review — pending

The eight cases in [knowledge_cases.json](../../../eval/knowledge_cases.json) are
implementation-authored SIMULATED fixtures. They are **not a human-reviewed gold
set**. The test output measures fixture correctness and is provisional; it must
not be cited as production RCA accuracy or evidence that vectors improve retrieval.

A qualified reviewer should inspect each case's platform/version/mode, causal
ordering, graph provenance, expected runbook, contradictory evidence, healthy
counterexamples and whether expected uncertainty is appropriate. Review corpus
sources and licenses, tenant/ACL scoping, any injection text, and the outcome
promotion boundary. Record corrections before changing evaluation thresholds.

| Case | Proposed target / expectation | Human status |
|---|---|---|
| Kafka KRaft | Network/quorum chain; no ZooKeeper dependency | PENDING |
| Legacy ZooKeeper | Session instability before legacy broker/downstream symptoms | PENDING |
| HDFS HA | No active NameNode; no diagnostic failover action | PENDING |
| Kudu tablet | Tablet availability/quorum unknowns distinct from master connectivity | PENDING |
| NiFi | Backpressure propagates through a bounded flow dependency | PENDING |
| Spark | Executor OOM precedes downstream delay | PENDING |
| Hive | Metadata failure distinct from HS2 query responsiveness | PENDING |
| Healthy false positive | No originating candidate despite related historical advice | PENDING |

Review record to complete independently: reviewer identity/role, timestamp,
dataset revision/hash, source references, corrected labels, accepted limitations,
and approval/rejection with rationale. No reviewer identity or approval has been
invented. Until that record exists, empirical model-quality sign-off remains
BLOCKED. Functional isolation/review/retrieval tests are separate acceptance.

Prompt 14B provides an additional [eight-case reviewer packet](../prompt14b/INDEPENDENT_REVIEW_PACKET.json).
All independent label and signature fields remain null; no approval is inferred.
