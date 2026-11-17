# Monitoring operation and recovery

Opt-in Java demo only. Set INVESTINATOR_MONITOR_ENABLED=true for the owned local
lab before `./investinator up`; the CLI generates MONITOR_API_TOKEN privately.
A fresh tenant starts paused. `./investinator monitor status|pause|resume --demo`
uses a fixed loopback API and that owned bearer token. Default environment omits
the flag and makes no polling/health writes or model/notification requests.

POST /api/v1/monitor/events accepts SIMULATED events only, immutable event IDs,
server-bound tenant, stored graph revisions and real declared measurement fields.
Missing capability/value is UNKNOWN; incompatible legacy/KRaft events are ignored.
POST /dry-run returns a verdict without writes; GET /rules provides version and
coverage; GET /status and /incidents/{id} expose bounded replay/lifecycle evidence.
POST /tick drains at most 100 events. All these routes require MONITOR_API_TOKEN
and demo mode; enterprise ingress remains hard-denied without OIDC.

The scheduler polls the durable queue every five seconds and saves actual bounded
health reports every minute while enabled/resumed. Raw health reports remain
separate from SIMULATED replay evidence. No fabricated topology or native metric
substitution creates a live incident from missing optional connector dimensions.
Native live event adapters require a separately verified graph/identity ingestion
boundary; public replay cannot claim LIVE. PostgreSQL is the checkpoint/transport;
a separate Kafka consumer is unnecessary for this first bounded worker.

The bundled rules YAML is the versioned source of threshold, hysteresis, window
and cooldown policy. Two distinct abnormal event-time samples within cooldown
are required; healthy samples reset streaks. Older arrivals enrich already-open
incidents but cannot rewind newer streak state. Correlation uses a sliding event-time
window and explicit graph validity. No cost/customer/SLO value is invented when
measured Impact is absent. Severity only escalates from supplied impact.

A component-scoped tenant/environment/cluster/source/graph fingerprint enforces
one active logical incident. Opening window is recorded in its full fingerprint.
Different symptoms update that incident and retain competing hypotheses. A closed
incident's same-rule recurrence is suppressed for cooldown, but a different cause
can open a new incident. No automatic resolution follows a healthy sample. Lifecycle
resolution requires the independent verification boundary; legacy clients see
investigating until an authorized verified resolution or closed_unresolved.

Budgets: 1000 queued events/tenant, 100 active incidents, 1000 observation IDs per
incident, 200 backfill candidates, 100 events/tick; excess evidence has an explicit
EVIDENCE_BUDGET state. New notification/investigation intents have at most ten/minute
and two of each kind per incident; stable origin identities deduplicate intents.
Duplicate identical events acknowledge without reprocessing; changed payloads 409.
Queue-full 429 means upstream must retain its checkpoint and retry. Normalized
hashes sort map keys so retry identities survive process restart.

INVESTIGATE intents terminate NOT_CONFIGURED if provider is absent, otherwise
AWAITING_OPERATOR_BUDGET. Operator invokes the existing bounded read-only chat
for the incident after budget approval. This version deliberately does not auto-
spend even if a provider key appears; autonomous semantic dispatch remains gated.
Notifications default to a LOCAL_FAKE durable destination, never email/webhook.
Console emission requires investinator.monitor.console-enabled=true and includes
only IDs; its external log delivery is at-least-once on transaction failure.
Email/webhook production adapters are not configured or implemented. No real
recipient/URL is accepted. Fake sink outages retry at most three times, then
MANUAL_INTERVENTION; investigation intents are never retried into paid traffic.

Transactions serialize per tenant. Queue processing, evidence/incident/outbox writes
and checkpoint advance commit together; DB interruption rolls everything back.
Restart resumes QUEUED records; no external executor exists. Inspect manual states,
fix configuration and retain evidence before any explicit operator remediation;
do not reset offsets, drop schemas or delete volumes to retry. History retention
and approved projection/archive maintenance are production readiness follow-ups.
