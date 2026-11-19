# Operator runbook and troubleshooting

Use only the owning checkout and its local Docker daemon. Native host/container
commands are fixed allowlists, not a production automation interface. Before first
setup select free LAB_* ports; afterwards preserve `.runtime/lab.json` with its data.
Do not print that file while screen sharing. It contains actual capability secrets.

| Command | Behavior / what it does not prove |
|---|---|
| INVESTINATOR_MONITOR_ENABLED=true ./investinator up | Build/reuse eight owned services, seed scoped SIMULATED fixtures and local Kafka records; no production routing |
| ./investinator status | Owned container state; not semantic health |
| ./investinator doctor [--json] | Prerequisites/Compose/resources/listeners + actual diagnostics; no infrastructure writes |
| ./investinator health --all | Core/optional service checks with timestamps/provenance; NOT_CONFIGURED is not healthy |
| ./investinator health --service kafka | Targeted actual local Admin metadata/ISR/lag/quorum; KRaft no ZK |
| ./investinator smoke | Demo API/DB/search/metrics/proxy and guarded simulator writes; do not run against an unrelated active replay |
| ./investinator demo kafka-kraft | Fixed SIMULATED causal stream; named retry reuses incident/times |
| ./investinator demo kafka-legacy (or legacy-zk) | Explicit representative legacy fixture; no real legacy Kafka certification |
| ./investinator investigate [kafka-kraft\|kafka-legacy\|incident-id] | Read-only bound graph/correlation; no model billing or mutation |
| ./investinator monitor status\|pause\|resume --demo | Scoped operator control, requires enabled local monitor; no production monitor changes |
| ./investinator logs api | Alias for owned Java logs, bounded tail; inspect locally, redact before sharing |
| ./investinator restart api | Fixed alias for owned Java container; caller must wait for readiness afterwards |
| ./investinator down | Removes owned containers/network; retains volumes and private credentials |
| ./investinator reset-demo --confirm-destroy-demo-data | Explicitly destructive owned-volume reset only; never a routine retry |

Exit 0 means that command's checks passed; 1 is diagnostic failure, 2 is argparse
failure or unconfigured AI probe as documented. Optional absent services do not fail
all-service core readiness; an explicitly requested missing service fails. Doctor
keeps release_ready=false for incomplete production gates, not the historical Next
advisory: current Next 16.4.5 audit is clean in scoped scans. No claim that all images,
optional shaded drivers or the application are independently security-certified.

## Troubleshooting

| Symptom | Inspect / safe next action |
|---|---|
| Wrong checkout/ownership marker | pwd, git branch/status, project marker; return to owning checkout. Never delete marker to adopt someone else's volumes. |
| Port occupied | Choose free LAB_* ports before first setup. Preserve failed runtime state; inspect existing listener/owner. Do not kill unrelated services or reset shared data. |
| API process alive / readiness down | Check DB authenticated query, private Flyway history, pool and owned logs. Liveness is intentionally independent. |
| Java UI 401/redirect | Sign in using matching private role. Only explicit Java lab requires demo session; don't paste token into client headers/storage. |
| 403 / origin mismatch | UI_PUBLIC_ORIGIN must exactly match externally accessed origin; no trusted spoofed forwarded headers. Check capability/role separately. |
| Expired/revoked action / 409 | Refresh durable plan, compare hash/version/expiry. Never blindly retry a mutation with a new key. Inspect receipt/manual state. |
| Verify fails after execution | Obtain fresh independent simulator sample with verifier role. Ack is not recovery; rollback requires a new named plan/approval. |
| Missing graph/KB citation | Don't replace it with seeded lineage. Check bound revision/current ACL/revocation. No approved KB hit is an honest result. |
| GPT fallback / 429 / timeout | Read provider_status; check backend presence and budget approval without printing secrets. No automatic retries/paid diagnostics. |
| Search stale/unavailable/403 | Check scoped reader/writer identity, validated index/mapping, pipeline rejection, freshness and disk. Never disable auth to force green. |
| OTel/Prometheus healthy but Java metrics absent | Collector scrape and Java private metrics are separate; set private metrics capability, use authenticated baseline capture. No Java scrape job is auto-provisioned. |
| Build missing files | Stop concurrent npm ci/build on the same directory, rerun sequentially. Don't downgrade Next or bypass audit. |
| Low disk / search red | Free only owned regenerable caches/unused test images, retain data. Review failed allocation in owned lab; never issue production repair or global volume prune. |
| OSV/GitHub API egress blocked | Record BLOCKED and owner; use actual hosted evidence when available. Public job pages are not authenticated logs/artifact downloads. |

## Rollback / cleanup / recovery

Route Next back to Python using INVESTINATOR_BACKEND=python and API_INTERNAL_URL
for the preserved Python API; restart only the owning Next service. Do not migrate,
reverse or delete Java data. Disable monitoring/model opts if not needed. To stop the
Java lab use down, then keep credentials with retained volumes. Confirm destructive
reset only when no data is required and the ownership marker matches.

Read-only backup/isolated restore and baseline commands are in the developer guide.
They do not implement encrypted offsite/PITR/HA or production RTO/RPO. Rotation,
retention/deletion, certificate refresh and real infrastructure changes require
independent authorization and validated staging procedures. See [recovery review](../implementation/prompt14/RECOVERY_AND_DATA.md).
