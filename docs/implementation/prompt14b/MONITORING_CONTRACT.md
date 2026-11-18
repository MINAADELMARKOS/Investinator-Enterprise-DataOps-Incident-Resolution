# Monitoring ingestion boundary and bounds

Native ingress remains DISABLED by default. Requesting native enablement reports
NOT_CERTIFIED and adds no endpoint/adapter scheduler; public monitoring still
accepts only authenticated, server-tenant-bound SIMULATED replay. The new fixed
NativeSignalContract tests tenant/cluster/service/version/mode, server ingestion
time, evidence ID, finite bounded values/capabilities, skew and freshness. It
rejects asserted LIVE. Kafka 4.x cannot use legacy coordination and KRaft cannot
request ZooKeeper capability. Fixture normalization is not enterprise certification.

The durable queue remains ingestion-sequence ordered, capped at 1000 pending
records with 429/backpressure (retain source checkpoint and retry). One tick works
at most 100 events. No event-time holding buffer is claimed. Hysteresis now requires
a strictly newer microsecond timestamp; equal/delayed samples cannot vote or create
a new incident. Delayed evidence may enrich an existing bounded incident. Event
window 4h, future skew 30s, correlation window 600s, cooldown 120s and incident
observation limit 1000 remain. Durable offset is the maximum completed ingestion
sequence, not an event-time watermark; active-incident correlation derives its own
maximum observation watermark. Physical restart tests remain the evidence for
checkpoint recovery, not a claim of native exactly-once collection.

Pending capacity is bounded; lifetime history/processed-event disk retention,
durable native drop counters, event-time holdback and long-duration retention
certification are NOT_IMPLEMENTED. There is no approved deletion policy and no
automatic purge. Queue responses/metrics expose backlog; rejected records are not
silently dropped. No remote notification or paid model invocation is added.

Focused suite: 15 tests pass, including 1000 duplicates, parallel workers, paused
sampling, notification outage and durable offsets. Initial added equal-timestamp
case failed due to database precision and passed after correction. This is a short
functional fixture run (30.783s), not a production soak or private-service read.
