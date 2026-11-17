# Prompt 05 — Health engine and local CLI

Implemented typed read-only diagnostics, bounded aggregation, private API, JSON
schema, safe Bash/PowerShell entry points and an eight-service local Compose lab.
The Next.js and Python application source and default routing are preserved.
Operational instructions are in [operations](../../operations.md); ADR 012 records
the restart/readiness split and lifecycle safeguards.

The eight configured local dependencies are probed through real service APIs.
Search/trace/metric fixtures are SIMULATED data in real collectors/backends;
Kafka is a real authenticated single-node KRaft cluster. Single-node availability
is not a production resilience claim. GPT and private enterprise integrations are
NOT_CONFIGURED, not successful live tests. The explicit legacy demo is a simulated
coordination fixture; it is not a deployed legacy Kafka/ZooKeeper test.

Outage acceptance: [failure matrix](failure-matrix.json) records all eight stopped
services, nonzero doctor exits, detection, restoration, unchanged container IDs
and no cascading restarts. Liveness stays UP for every external dependency outage.
Java's own intentional stop is correctly reported through CLI-local fallback.
Forwarder failure is detected through the owned container check; retained evidence
does not disappear just because the forwarder stopped.

Corrected findings during testing: an asynchronous circuit update race, Node's
rejection of HTTP upgrade, container proxy interference with loopback wget, and
closed-socket TIME_WAIT false port collisions. Final acceptance artifacts record
only the rerun results; these initial failures were not treated as passes.

The critical Next.js dependency security finding remains a release blocker.
Production identity/authorization, enterprise credential validation, paid provider
access and later agent/execution stages remain unverified. PowerShell/native
Windows and a cold-cache second machine are NOT_TESTED. No production writes,
paid OpenAI requests, push/merge or environment publication were performed.

Final checks: 36 Java tests (zero failure/error/skip), 10 CLI tests, JSON report
schema validation, secret-value absence, baseline artifact validation and diff
checks PASS. Fresh generated volumes and unchanged repeated up/doctor/smoke PASS;
see [lifecycle result](lifecycle-result.json), [sample report](doctor-example.json),
[test summary](test-summary.json) and [smoke](smoke.txt). Initial compilation of
fixtures overlapped an independently invoked test build; CLI test/lifecycle
commands now share the local operation lock, and the final sequential rerun passes.
Prompt 05 acceptance passes on this Linux host. Release remains BLOCKED.
