# Local operations

The CLI manages only this checkout's generated, isolated Docker Compose lab. It
preserves the Python backend and Next.js interface. Requires Python 3.10+, a Java
21 JDK, Node 22+, npm, Docker Engine/Desktop with Compose v2, and approximately
6 GiB free RAM and 8 GiB disk. Initial builds need network access. On Windows use
`./investinator.ps1` with Python on PATH. Native Windows execution has not been
validated in this Linux environment.

From any directory, invoke the wrapper by its path. From the repository root:

```sh
./investinator up
./investinator doctor
./investinator doctor --json
./investinator health --all
./investinator health --service kafka
./investinator smoke
./investinator test
./investinator status
./investinator logs java
./investinator restart kafka
./investinator demo kafka-kraft
./investinator demo legacy-zk
./investinator down
```

`up` builds Java and the existing Next.js app, starts the eight local services,
initializes scoped search identities, sends explicitly SIMULATED log/metric/trace
fixtures, and creates a local Kafka topic/consumer group with five sample records.
The seeder requires the randomly generated local cluster identity and local-only
bootstrap address. It is separate from the read-only health engine. Subsequent
`up` calls reuse the same named containers, volumes, credentials and fixture IDs.
Search duplicate create conflicts are observable; this is not exactly-once delivery.

Default ports: UI 13000, Java 18081, search 19200, Prometheus 19090, Kafka 19092,
OTLP HTTP 14318, collector health 13134. All published listeners bind loopback.
Before first `up`, set the corresponding `LAB_WEB_PORT`, `LAB_API_PORT`,
`LAB_SEARCH_PORT`, `LAB_PROM_PORT`, `LAB_KAFKA_PORT`, `LAB_OTLP_PORT` or
`LAB_OTEL_HEALTH_PORT` to avoid existing listeners. Port choices are then persisted.
A remote Docker host/context is refused. No existing default Compose stack or
production cluster is targeted. Generated credentials are stored in ignored,
private `.runtime/lab.json`; preserve that file with its volumes. Never paste it
into a ticket or commit it. File permissions also depend on Windows ACLs there.

`down` removes containers/network and preserves named volumes. To erase only this
lab's data, explicitly invoke:

```sh
./investinator reset-demo --confirm-destroy-demo-data
```

This prints the exact five volume names, checks their Compose ownership labels,
and removes only matching demo volumes and generated fixture markers. It keeps
credentials. Never use a general `docker system prune` as an InvestiNator command.
Restarts accept only the eight allowlisted local service names. No arbitrary URL,
SQL, shell command or production restart API is exposed.

`demo kafka-kraft` creates an explicitly simulated disk-pressure replay in the
real local application; Kafka's observed metadata mode remains KRaft. The legacy
command writes a separate, explicitly SIMULATED coordination/session fixture,
without claiming a live ZooKeeper cluster. It never queries ZooKeeper on KRaft.
`smoke` tests persistence, tenant search, the UI proxy and the stored approval
boundary, then performs only allowlisted simulated recovery. A pending run recorded by this CLI can be resumed; an unrelated active run is
refused. Each successful smoke run leaves its incident resolved and preserves the audit history.

Health report contract: [JSON schema](implementation/prompt05/health-report.schema.json).
`GET /api/v1/health/integrations` requires the backend-only `HEALTH_API_TOKEN` bearer
credential in demo mode; production identity is still a separate release gate.
The CLI handles that token privately. `?refresh=false` returns a labeled cached
report; expired reports become UNKNOWN. A service filter has an explicit empty/
unknown error. Reports contain source, timestamp, duration, diagnosis, next safe
action, redacted error class and readiness requirement. A live request to a local
service is LIVE evidence; its sample data remains separately labeled SIMULATED.
Missing optional integrations are NOT_CONFIGURED, never green connectivity.

The engine uses bounded workers/queue, per-probe and aggregate deadlines, one
transient retry with jitter by default, per-service circuit cooldown, and optional
interval scheduling. All limits use `investinator.health.*` properties. Probe
HTTP has verified JVM TLS, no redirects, a 1 MiB response cap, 2s connect/3s read
timeouts. Kafka uses bounded Admin calls; no consumer/producer is in its probe.
Readiness uses only the dedicated database and owned Flyway schema. Liveness is
independent of external dependencies. Search accepts healthy single-node yellow;
empty/stale tenant results are degraded. OTel checks actual retained trace arrival,
Prometheus checks actual scrape status and PromQL samples. Unsupported enterprise
capabilities remain NOT_TESTED until their adapter is installed and verified.

`doctor` adds prerequisite, Compose syntax, local listeners, free disk/memory,
container, migration, API, indexing and proxy checks. It writes the compact CI
artifact `.runtime/last-health.json`; use `--json` for machine-readable stdout.
Exit 0 means requested runtime checks passed, 1 means a diagnostic failed, and
argparse uses 2 for invalid commands. `ai-check` returns 2 when unconfigured.
Optional missing integrations do not fail all-service runtime readiness; asking
for a specific missing integration does fail. Doctor always reports the known
critical Next.js dependency finding as a **release blocker**, even when local
runtime checks pass.

No OpenAI request is made by health, doctor, up, smoke or test. `ai-check` inspects
only presence by default. An operator may supply the provider secret and model
through backend environment/secret injection (not chat) and explicitly run
`ai-check --live`. That command uses the official Java OpenAI Responses SDK, sends only a one-word probe with `store=false`, has
8s SDK timeout, zero SDK retries, 64 maximum output tokens, and a per-checkout limit
of one request/minute. It may incur a small charge. The Java chat API also implements the bounded read-only function loop when explicitly
enabled and configured. No paid probe was run during implementation.

For the explicit owned-lab outage test, run `python scripts/verify_health_lab.py`.
It stops and restores each local dependency and fails on unexpected restarts or
missing diagnostics. This is an opt-in test script; ordinary health never injects
faults. Native Windows, cold image pulls on another host and private enterprise
clusters need independent validation; this Linux run is evidence for this host.


Prompt 06 adds the distinct backend-only `CONNECTOR_API_TOKEN` and a separate
Kafka diagnostic principal with metadata-only ACLs. `up` upgrades only this owned
lab's private configuration, ensures local ACLs and checks the existing five-record
fixture without reseeding duplicates. Kafka log storage is explicitly in the
named volume; connector acceptance verifies record and group-offset retention
across down/up. See the [connector deployment matrix](implementation/prompt06/CONNECTORS.md)
for private endpoint/authentication variables, supported versions and unverified
semantic dimensions. Run `python scripts/verify_connectors_lab.py` for the explicit
owned-lab authorization/durability acceptance (it tears down/restarts this lab).

Provider setup and limits: [Prompt 08](implementation/prompt08/REPORT.md). Provider
keys belong in backend secret storage; never paste them in chat. Chat requires
the private state-encryption key as well as provider/model configuration.
