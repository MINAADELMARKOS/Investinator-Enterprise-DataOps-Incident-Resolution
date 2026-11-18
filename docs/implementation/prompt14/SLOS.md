# Proposed SLOs and observable boundaries

Targets below are proposals requiring SRE/product approval. No 30-day production
window, financial return or customer-impact baseline was measured. Operational
snapshot is bounded, serial local HTTP traffic against SIMULATED data, not a load test.

| Indicator / window | Proposed objective | Instrument / current status |
|---|---|---|
| Authorized API availability / 30d | 99.9% (43.2 min error budget in 30d) | http_server_requests_seconds_count; define eligible traffic, exclude intentional enterprise block probes; no production window |
| API latency / 30d | p95 <= 500ms for simple reads | HTTP SLO buckets 50ms..10s; local samples in operational-baseline.json only |
| Health freshness | 99% core samples <=60s | investinator_monitor_health_age_seconds; missing = unknown, never healthy; paused/disabled monitor cannot meet continuous sampling SLO |
| Detection latency | p95 <=30s ingest -> durable incident | Queue age is observable; end-to-end per-event completion timestamp/latency histogram not implemented; NOT_MEASURED |
| Queue / outbox age | <=30s queued / <=120s pending | New fixed-label gauges, DB failure yields NaN; dashboards must treat NaN as missing |
| MTTA / MTTI / MTTR | Proposed P2 5m acknowledge / 15m investigate; recovery target agreed per service | Production lifecycle dataset absent; simulator approval/sample timings are not production MTTR |
| Evidence validity / citations | 100% authorized, current membership | Functional negative tests; sampled production invalidity/abstention/false-positive rate NOT_MEASURED |
| Connector response | <= bounded timeout, no unbounded retry | Health probe hist/times in local snapshot; native private-service SLO NOT_MEASURED |
| Notifications | <=10 outbox candidates/min, <=2 per kind/incident | Fixed queue budgets; LOCAL_FAKE only, no real delivery SLO |
| Incident compression | Report unique abnormal inputs / unique incidents | P13 fixture 10 events ->1 incident per scenario, 200 identical redeliveries bounded; not measured alert reduction or ROI |
| GPT tokens/cost per investigation | Caps: <=3 requests default, <=1024 output/request, <=6 tools, <=20s turn, 2 active turns | Fixed-label response/outcome timers, provider attempts/token counters and unknown-usage counter; no model/tenant/incident/prompt labels; no durable org monetary cap |
| JVM / DB | Heap, GC pause, CPU, pool pressure within approved capacity | Existing Boot/Micrometer JVM/GC/process/Hikari meters; no long-duration capacity envelope established |

Dashboard definitions: API rate/error/p95 from HTTP buckets; chat timer by fixed
response/outcome; provider attempts/tokens/unknown usage; active incidents, queue
age, outbox counts by fixed kind/state and pending age; health age; JVM heap/GC and
Hikari pool. Do not substitute seeded business charts for these operational meters.
No user-controlled or private strings are metric tags. The lab now configures a
private generated metrics capability; baseline capture authenticates directly. The
existing Prometheus job scrapes the collector, not this private Java endpoint;
provisioning reviewed secret-backed Java scrape configuration remains a staging gate. Token counters omit unknown
usage; a separate unknown counter prevents interpreting missing data as free usage.

Prometheus rules in infra/operations/slo-rules.yaml are syntax-validated proposals with NaN/missing-series unit checks.
They are not mounted/enabled automatically, and there is no remote alert delivery.
Fast-burn threshold 1.44% corresponds to 14.4x a 0.1% error budget; review long/short
windows, low-volume handling, NaN/missing series and traffic scope before adoption.
Telemetry default profiles may disable tracing; actual trace fixture transport was
verified in P13, but no always-on enterprise trace/SLO coverage is claimed.
