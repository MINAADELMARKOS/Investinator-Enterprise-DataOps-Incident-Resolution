# Prompt 04 implementation report

> 09B security reconciliation (2026-11-17): the Next.js finding described below
> is historical. Current main/local Next.js 16.4.5 package and lockfile match;
> fresh npm audit reports zero vulnerabilities. See the
> [current security assessment](../prompt09/PROMPT05_AUDIT.md#security-advisory-reconciliation).

2026-11-17, feature/java-ai-audit. Prompt 03 was committed first. Existing Python
and Next.js routing/data remain unchanged. No production infrastructure write,
paid provider call, environment draft update or publication was performed.

## Delivered

Typed normalized evidence/schema: tenant, stable source/time identity, provenance,
service/host/resource/cluster, event/ingestion time, log/error, trace/span IDs,
sensitivity, lag/skew and source pointer. Source text is bounded/redacted by the
normalizer and again by the ingest pipeline. Event time is preserved; ingestion
time comes from actual ingest metadata. Strict mappings and schema-version checks
reject drift. Create-only IDs deduplicate retries without overwriting source records.

The data plane is separate: Fluent Bit tails normalized files into authenticated
OpenSearch; OTel receives fixture metrics/traces, exposes metrics for Prometheus and
rotates native trace batches for Fluent Bit to forward into a separate trace index.
The control plane holds only reader credentials and exposes bearer-protected,
server-tenant-bound GET /api/v1/evidence/search and /api/v1/evidence/health. User
index/tenant/URL/DSL inputs are rejected. Search bounds: <=50 records/page, offset
<=1000, <=4-hour window, fixed fields/index, 2s query and 3s transport timeouts,
automatic HTTP retries disabled. Returned IDs/tenant/timestamps are validated.

OpenSearch 3.4.0 / official Java client 3.11.0 is the tested alternative to
Elasticsearch, which could not bootstrap on this managed kernel. No cross-vendor
client assumption is made. See ADR-011 and elasticsearch-platform-blocker.txt.
SDK runtime needs Reactive Streams and Boot-managed Reactor Core; enabled Spring
context and transport construction regressions cover those dependencies.

Operational details and safe retained/disposable startup, credentials, retention,
queue bounds and stop instructions: infra/evidence/README.md. Images and dependencies
are pinned. Normal stop preserves demo volumes; search retention attachment needs
explicit operator confirmation. No enterprise evidence mode is enabled.

## Checks

| Check | Actual result / evidence |
|---|---|
| Maven verify | PASS: 25 tests, 0 failures/errors/skips; verify.txt, test-summary.json |
| PostgreSQL workflow/API regressions | PASS, including 74 captured Python HTTP steps |
| Normalization negatives | PASS: identity, redaction, late arrival/skew, bounds, stable IDs |
| Authentication/unconfigured scope | PASS: wrong/missing credential denied, NOT_CONFIGURED truthful |
| Official SDK enabled Spring context | PASS without provider calls |
| Actual file -> Fluent Bit -> index -> typed Java | PASS; lab-result.json |
| Deduplication / tenant filter | PASS: 4 documents after duplicates; only 3 demo records returned |
| Read-only roles | PASS: reader write and writer search return 403 |
| Pagination/service/text/unsafe-input tests | PASS against actual Java/OpenSearch |
| OTLP trace arrival and Prometheus query | PASS, sources explicitly SIMULATED |
| Backend outage | PASS: 503 UNAVAILABLE in bounded time; Java liveness remains UP |
| Collector validate | PASS: official binary exit 0; collector-validation.txt |
| Compose / JSON syntax and Python lint/type checks | PASS |
| Hosted GitHub Actions | NOT_RUN; local evidence-lab step committed |

Reproduce from repository root with JDK21 and local Docker:

```bash
./backend-java/mvnw -B -f backend-java/pom.xml verify
python3 backend-java/scripts/evidence_lab.py verify
.venv/bin/ruff check src tests scripts/capture_python_contract.py backend-java/scripts
.venv/bin/mypy src
```

Maven writes target/runtime-classpath.txt for fixture generation. Managed-cloud
proxy/system CA setup is documented in backend-java/README.md; verification stayed
enabled. Only private service aliases were appended to container no_proxy/NO_PROXY.

## Failed attempts diagnosed and corrected

- Elasticsearch 9.5.6 was unavailable on the usable registry; 9.5.5 then failed on
  the missing kernel interface. Authenticated OpenSearch was proven as an alternative.
- Vendor API differences were checked against resolved SDK signatures and corrected.
- Non-root collector could not read private-mode non-secret config binds; corrected
  readability and reran the actual collector validator successfully.
- Painless disallows Instant.now(); ingest metadata now supplies the timestamp.
- Inherited cloud proxy misrouted private Docker traffic; scoped no_proxy fixed it.
- Native traces needed predefined mappings rather than granting the writer dynamic
  mapping administration; reader/writer remain unable to write/search respectively.
- Enabled SDK startup exposed missing Publisher/MonoSink types. Explicit runtime
  dependencies plus a full enabled Spring context regression resolved this.

## Gate and limitations

Prompt 04 local acceptance gate PASS: actual collection/query, strict tenant filtering,
truthful unavailable response and denied unauthenticated access are demonstrated.
Collection/retrieval are LIVE; incident/log/span/metric fixture sources are SIMULATED.
This is not production incident evidence, causal accuracy or production readiness.

Native trace queries are not exposed as a tenant-aware API. Enterprise ingress,
identity/TLS, HA, comprehensive quarantine/replay, retention rollout and Java CVE
review remain later work. Create-only duplicate conflicts can retry whole Fluent Bit
batches: document uniqueness is verified, not exactly-once successful delivery.
Long outages/full bounded buffers can drop data and must be monitored. The isolated
lab uses bundled demo search security/certificates and private plaintext HTTP only;
enterprise evidence startup/access stays disabled.

Critical Next.js advisories remain a release blocker. No model tool loop, production
executor, push, merge, environment publication or Prompt 05 work was performed.
