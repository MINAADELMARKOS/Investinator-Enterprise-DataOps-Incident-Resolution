# Prompt 10 — Opt-in continuous monitoring and durable incident lifecycle

User authorized v2 Prompts 10 and 11, in sequence. Prompt 09B commit 135bb52 is
preserved; human review/provider readiness remains pending. No default routing,
Python/Alembic ownership, Next.js lock, production executor or paid request changes.

The Java modular monolith gains a durable PostgreSQL queue/offset/outbox in Flyway
V6, versioned capability-aware [rules](../../../backend-java/src/main/resources/incident-detection-rules.yaml),
[event schema](event.schema.json), [lifecycle schema](lifecycle.schema.json), scoped
APIs and CLI. [Operations](OPERATIONS.md) records precise bounds, opt-in flags,
rollback and unsupported production surfaces. Scheduling is disabled by default;
fresh enabled tenants are paused. Per-tenant serialization plus a unique active
scope index deduplicates parallel workers and survives process loss.

SignalReceived -> NormalizedObservation -> DetectionCandidate -> CorrelationWindow
-> IncidentAggregate -> InvestigationRequested / NotificationCandidate separates
raw measurements, uncertain causal explanations and governed side effects. Public
input is SIMULATED only; actual health samples stay separate. Native topology/metric
certification remains a prerequisite to any live autonomous incident pipeline.

Rules cover network/KRaft/legacy ZooKeeper, lag+ISR, Spark retries/OOM, HDFS HA/missing
blocks, YARN pressure, NiFi backpressure, Kudu tservers, Hive partitions, freshness,
disk and rollout/error risk. Missing capability/metric is UNKNOWN, never zero;
KRaft rejects legacy coordination rules. Rollout association stays unconfirmed.
Event-time hysteresis/window/cooldown, graph validity and healthy counterevidence
preserve uncertainty; origin changes update the same active incident. Dataset,
customer, criticality and SLO impact exist only when supplied; no invented ROI/cost.

Prometheus gauges expose event states, active incidents and health-sample age;
query dashboards with `investinator_monitor_events{state="QUEUED"}`,
`investinator_monitor_active` and `investinator_monitor_health_age_seconds`.
Missing/unavailable observations return NaN, not a healthy zero. PostgreSQL retains
actual typed health reports while resumed; existing bounded probes are reused.

Lifecycle NEW -> TRIAGED -> INVESTIGATING -> AWAITING_APPROVAL/MITIGATING -> VERIFYING
-> RESOLVED or CLOSED_UNRESOLVED is durable/audited. Existing API statuses remain
investigating until terminal, and public monitor routes cannot resolve directly.
Only the independent verification boundary may confirm resolution. Monitoring
uses new incident identities rather than overwriting legacy deterministic IDs.

Investigation intents are bounded by distinct origin and rate/incident budgets.
Missing GPT becomes NOT_CONFIGURED; configured GPT stays AWAITING_OPERATOR_BUDGET
until the operator uses existing bounded read-only chat. No automatic provider
spend occurs just because a key appears. Notification defaults to durable LOCAL_FAKE;
console is explicit opt-in. No recipient or remote URL is accepted; email/webhook
adapters and autonomous paid semantic dispatch remain deliberately unavailable.
Fake delivery outages stop after three tries with manual-intervention state.

## Acceptance evidence

Current automated results and [failure matrix](failure-matrix.json) are generated
from actual tests in test-summary.json. Tests cover KRaft/upstream hypothesis change,
legacy and HDFS/NiFi chains, freshness delay, 1000 identical duplicate deliveries,
parallel workers, hysteresis/long flapping/cooldown/new cause, missing/stale/mode/
tenant input, HTTP 503 fixture sampling, notification outage, transaction failure
and durable checkpoint resume. The actual process acceptance is in
[process-restart.json](process-restart.json); no production cluster is contacted.
The storm [benchmark](storm.json) records generated deliveries, actual incident
compression and zero GPT calls; it is not production latency/throughput evidence.

The first focused run caught incident identity reuse across graph scopes. Fixed by
creating a new monitoring aggregate ID and preserving the old workflow. The first
full run caught the expected Flyway migration-count update (5 -> 6); schema-owner
and production-action assertions remain intact. Final checks are recorded after
correction; no failed criterion was bypassed to begin Prompt 11.

Native event ingestion remains a trusted-adapter follow-up, and only SIMULATED
streams drive this MVP incident loop. Native live health sampling is independently
verified. Large-scale retention/archive, webhook/email transports, OIDC/role policy,
held-out human quality review and live GPT budget validation remain deployment gaps.
No fixture accuracy or full private connector health claim follows from this gate.

Rollback: disable monitoring/pause; retain queue/offsets/evidence and Java private
schema. Do not drop V6 or reset shared volumes. Python/default UI stay intact.
Prompt 11 must wait for the committed final acceptance checkpoint. No push/merge,
environment publication or production infrastructure change is part of this stage.
