# Prompt 13 — Repeatable lab and regression validation

2026-11-18. Local functional acceptance PASS. Hosted CI is pending publication/observation;
local Java advisory lookup is explicitly BLOCKED by egress, not clean.
Stop after this stage. No Prompt 14 work, main merge, Pages/environment publication,
production route change or paid provider request is authorized or claimed.

## Source reconciliation and scope

Prompt 12 committed `bfe757b`. Remote main advanced to `afb8183` (PR #6 merged the
previous 09A work). Backup `backup/pre-p13-bfe757b` preserves the recovery point;
normal merge `9924bc5` brings main into feature/java-ai-audit without conflicts.
All existing commits and Next 16.4.5 survive. User/Java/Python schemas and volumes
are preserved. The feature remote initially remained `6e981b9`.

Code checkpoints `216467a` and `4b484b4` add fixed CLI commands, causal fixtures,
fresh-checkout harness and required CI jobs. Extra commits are needed to reproduce
committed source in a clean clone and run hosted CI; no history is rewritten.

## Implemented

- `investigate [kafka-kraft|kafka-legacy|incident-id]` performs bounded scoped reads;
  `restart api` aliases only the owned java service. Unknown targets fail early.
- Named KRaft and representative legacy demos emit ten SIMULATED events each over
  actual Java/JDBC transport. Network -> controller/session -> broker lag -> Spark
  -> dataset paths rank network first while retaining alternatives and uncertainty.
  Repeated named commands reuse durable incident/identical event identities.
- KRaft never queries ZooKeeper. The legacy graph is a **representative protocol
  fixture**, not a real legacy Kafka installation. Separate actual Kafka Admin and
  digest-ACL ZooKeeper protocol tests remain in Maven Verify.
- The fresh lab harness owns separate loopback ports, checks unchanged-up container
  identity, doctor/health/smoke, duplicate storms, graph/lifecycle persistence after
  API restart, authorization/tenant denial, provider-less fallback and actual
  Java/default-Python Chromium workflows. Optional failure injection stops/starts
  each owned dependency and proves failure detection without cascading restarts.
- CI now has Python/CLI, frontend build/types/contracts/audit, Java/Testcontainers,
  eight-service browser/outage E2E and Python/Maven dependency-security jobs.
  Missing OSV service/partial responses fail explicitly with a BLOCKED artifact;
  security errors are not continue-on-error. No credentials/runtime config uploaded.

## Verified local evidence

107 Java tests (zero failures/errors/skips), 29 CLI tests, 12 Python tests, Ruff,
mypy 17 source files and baseline validation pass. Frontend npm ci/build/types,
eight contract tests and npm audit pass; zero npm findings. Native evidence pipeline
passes scoped search/auth 403, invalid query 422, source/trace/redaction/Prometheus,
corrupt-log rejection and search outage with independent Java liveness.

Actual disposable PG + Java process acceptance passes monitor offset/duplicate/
metric persistence across termination/restart and governed effect receipt/retry/
independent sampling/separately approved rollback. No fake provider call is treated
as actual traffic. Existing Java SDK failure/timeout tests are MOCKED_PROVIDER;
actual live GPT remains NOT_CONFIGURED. Human gold-set approval remains PENDING.

Python audit initially found six advisories in the local pip 25.0.1 installer;
application packages had none. Upgraded test installer to pip 26.3 and reran:
61 installed distributions, zero known findings. CI installs that patched installer.
Java's 233-package resolved dependency tree was generated, but the local proxy
rejects api.osv.dev CONNECT (403). Java advisory status is **BLOCKED**, not clean;
build/security maintainer must validate the hosted scan or supply permitted egress.

See junit-summary.json, negative-matrix.json, evidence-pipeline.json,
monitor-process.json, remediation-process.json, python-audit.json, java-audit.json
and fixture-hashes.json. Simulated fixture ranks are functional diagnostics, never
production RCA precision or business outcomes. No independent human sign-off exists.

## Reproduction incidents and recovery

Initial fresh run correctly refused the occupied default port; the harness now
allocates distinct ports before first setup. The failed config was retained, and
a separate clean clone tested corrected source. Fresh clone initial source is
`4b484b4`; dependency/image caches are reused on this host, not a cold-host claim.
Python dependencies were installed in a test virtualenv, with PYTHONPATH selecting
the clean clone's source. No tracked file modification was present at clone start.

Host disk exhaustion then prevented startup and failed local search shard allocation.
Removed only regenerable task npm caches, obsolete untagged task image builds and
unused Elasticsearch diagnostic images (no container referenced them). No source,
evidence, credentials, database volume, active image or user data was deleted.
This freed 16 GB. Retried failed shard allocation only in the named fresh test
OpenSearch instance; native readiness returned healthy. Failed attempts remain
reported; final retry must pass rather than bypass assertions or reset volumes.

## Operations and remaining boundaries

[Reproduction](REPRODUCTION.md) describes commands, cleanup, resource/time budgets,
source modes and rollback. `down` preserves data. Only explicitly confirmed owned
reset can destroy demo volumes. Scanner artifacts and result/screenshots have fixed
upload allowlists; `.runtime/lab.json`, raw environment/cookies/traces are excluded.

Enterprise OIDC/two-person approval, certified private connectors, provider budget,
human quality review, TLS/security/HA/DR and measured business SLOs remain blockers
for deployment. CI passing alone would not close them. Earlier PR #6 is already
merged by the remote owner; this task does not merge anything into main. Current
GitHub API CONNECT remains denied, so creating a new PR may remain blocked even
when HTTPS git and public Actions pages work.

## Final local acceptance

Clean-clone source 4b484b4 reproduces the full demo using cached dependencies/images.
Actual eight-service up twice retains container identity; all doctor/health/smoke
commands pass. Two 10-event streams each produce one bounded incident, 200 duplicate
deliveries do not multiply incidents, and API restart retains graph/ranking.
Unknown roles and wrong tenant receive 403. ai-check --live exits 2 with no key/model
or paid traffic. Thirteen browser checks pass in that clone; an additional 14-check
run on the primary checkout proves actual bound graph/observations/uncertainty.
All eight owned dependency stop/start cases pass with no cascading restart or
container replacement. See fresh-checkout.json, outages.json and browser-with-graph.json.

The fresh test's disposable dependencies reuse a separately installed virtualenv;
PYTHONPATH selects the clean source. Private runtime ownership/ports are separate.
Native pipeline and process fixtures automatically remove only their own disposable
containers/networks; the two main labs/data are retained until explicit down.
No security finding is waived: Java advisory validation still needs hosted/allowed
egress. Feature publication and actual hosted run status will be recorded below.

## Hosted gate exposed advisory fixes

Actual run [44389688456](https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/actions/runs/44389688456)
on pushed 4597517 reports Python/CLI, frontend, Java/Testcontainers and local E2E
completed successfully. Dependency-security failed. Run 44390174065 on f717b12
adds public advisory annotations because authenticated logs are inaccessible; it
identifies seven advisories in Snappy 1.1.10.7, LZ4 1.11.2 and OTel API 1.62.0.
No scan or assertion was disabled to make that run green.

Verified public advisory pages prescribe Snappy 1.1.10.9, LZ4 1.11.4 and OTel 1.67.0.
The two codec pins use dependencyManagement; the stable OTel BOM is aligned with
existing instrumentation 2.33.1 (its incubator already resolves 1.67.0-alpha).
See java-advisories.json for all seven IDs/source links. Full local regression and
a new hosted security run are required after these targeted fixes. Optional inactive
Hive JDBC profile and enterprise connector certification remain outside this default
dependency-tree scan; no complete production security certification is claimed.
