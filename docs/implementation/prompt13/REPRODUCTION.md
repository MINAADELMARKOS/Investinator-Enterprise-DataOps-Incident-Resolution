# Fresh checkout and isolated laboratory

Prerequisites: Linux Docker Engine/Compose with a **local** daemon, Java 21, Python
3.12 + project dev dependencies, Node 22.6+ (Node 24 also tested), npm, writable cache
and at least 10 GB free disk / 6 GB available RAM. The local verification host has
34 GB RAM; Kafka/OpenSearch/PG/Testcontainers and two lab stacks need more than one
small service. Shared image/build/dependency caches accelerate local reproduction;
that is not a cold-host or air-gapped claim. CI budget: 25 minutes E2E, 12 minutes
security; Maven/CLI commands are bounded individually. No production destinations.

```
git clone https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution.git
cd Investinator-Enterprise-DataOps-Incident-Resolution
git checkout feature/java-ai-audit
python -m venv .venv
.venv/bin/python -m pip install --upgrade pip==26.3
.venv/bin/python -m pip install -e '.[dev]'
npm ci --prefix apps/web
(cd apps/web && npx playwright install --with-deps chromium)
INVESTINATOR_MONITOR_ENABLED=true .venv/bin/python scripts/verify_e2e_lab.py --outages
./investinator test
```

The runner builds/starts the eight services, verifies unchanged up does not replace
containers, probes doctor/health/smoke, emits two bounded causal streams, repeats
200 identical deliveries, independently reads correlation, restarts only the owned
Java API, checks authorization/tenant denial and runs actual Java + default Python
browser workflows. Optional --outages stops/starts each owned dependency, checks
failure detection and recovery and asserts no cascading restarts/replacements.
It restores each stopped service even when its assertion fails. No volume deletion.
No paid provider request: key/model are explicitly empty in the harness.

```
./investinator demo kafka-kraft
./investinator demo kafka-legacy
./investinator investigate kafka-kraft
./investinator investigate kafka-legacy
./investinator restart api
./investinator status
./investinator logs java
./investinator down
```

`restart api` is an alias for the fixed local java service; unknown targets fail
before reading private runtime state. `investigate` reads only an existing bound
graph and correlation. Demos use generated events over real Java/PG transport:
network -> controller/session -> broker metadata/lag -> Spark -> dataset freshness.
They do not physically damage enterprise services. KRaft uses no ZooKeeper node or
query. kafka-legacy (legacy-zk alias retained) is clearly a representative protocol
fixture; real local legacy ZooKeeper protocol tests are separately covered by
Maven/Testcontainers. No claim of real legacy Kafka certification.

Repeated named demo commands read the same durable incident rather than fabricate
new timestamps or create new incidents. If a disposable lab is intentionally reset,
only `reset-demo --confirm-destroy-demo-data` may remove owned volumes/markers.
`down` preserves data. Do not erase a shared runtime to force a test to pass.

Reports under .runtime contain sanitized result fields and screenshots only; private
lab.json, cookies, role credentials, provider secrets and raw environment files are
never uploaded by CI. Each script checks or derives owned Compose scope. Browser
503 is MOCKED; native protocol, HTTP, JDBC and failure-injection checks are actual
local transport with SIMULATED workload content. Production actions stay zero.

Dependency checks: npm audit --audit-level=high; pip-audit (pinned scanner 2.9.0);
resolved Maven dependency JSON submitted to OSV by audit_java_dependencies.py.
OSV failure exits 2 and writes BLOCKED with an owner; it cannot become a clean scan.
Security findings fail hosted CI. A clean database of known advisories is not a
complete application security or enterprise connector review.
