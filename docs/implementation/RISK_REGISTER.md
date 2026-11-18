# Risk register

Current 12/13 disposition: isolated demo gates and all five hosted jobs PASS on
f1488a7. Seven Java dependency advisories were remediated (Snappy/LZ4/OTel); OSV
reports 233 packages / zero findings. npm/Python audits clean. Local API egress and
new PR creation remain blocked; production identity/certification/human quality/
operational security and optional-profile auditing remain unresolved. Historical
records below retain earlier pending/failing evidence.

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R1 | Resolved locally in 09A / frontend maintainer | Main patch Next 16.4.5 retained; fresh npm ci/build/types/audit pass, zero vulnerabilities | hosted CI verification pending; historical audit below remains preserved |
| R2 | Critical / security lead | fixed demo actor, PG trust | OIDC/RBAC, TLS/credentials, network isolation before enterprise exposure |
| R3 | High / AI lead | PR regex citations, no total deadline | typed membership, prompt injection and budget tests before port acceptance |
| R4 | High / SRE | platform missing metrics default healthy | freshness/UNKNOWN semantics and dependency failure tests |
| R5 | High / data owner | Alembic/Flyway and JSON/schema parity | one migration owner, backup/restore and compatibility tests before writes |
| R6 | High / platform lead | Prompt 02 pins, JDK21 wrapper verify and container smoke pass; live connector compatibility remains unverified | retain per-connector integration gates; JUnit6 compatibility decision in ADR-009 |
| R7 | High / integration lead | no live big-data/search proof | version-aware credential-scoped functional tests; label unconfigured |
| R8 | High / security + change owner | demo approvals are not independent auth | production executor stays absent; separate authorization required |
| R9 | Medium / UI/API lead | generic dict contracts, proxy drops identity/request headers | explicit contracts and routing/auth propagation tests |
| R10 | Medium / AI/data lead | deterministic confidence/runbooks, synthetic eval | human-reviewed causal gold set; no accuracy/business claims |
| R11 | Medium / build owner | Python minimum-only dependencies | reproducible lock strategy and Python dependency audit before release |

Owner roles are proposed responsibilities, not assignments to named people.

Prompt 04 updates: search/trace/metric transport is verified locally with simulated
inputs, so R7 is partially addressed for the lab only. OpenSearch 3.4.0 / official
Java 3.11.0 replaces unavailable Elasticsearch on this kernel (ADR-011). Reactive
transport dependencies and enabled Spring context are regression tested.

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R12 | High / SRE | bounded file buffers/retries can drop data on prolonged outage; create-only duplicate conflicts retry whole batches | monitor errors/drop counters and add production quarantine/replay coverage |
| R13 | High / security lead | local demo uses bundled OpenSearch security/certificates and private plaintext HTTP; no enterprise authenticated ingestion | identity/TLS/certificate/role/secret review before exposure; enterprise evidence mode disabled |
| R14 | Medium / data owner | native trace batches are stored separately but have no tenant-aware query API | keep index credentials private; add scoped trace adapters before exposing queries |

Critical Next.js R1 remains a release blocker. No Java dependency CVE audit or
production security signoff was performed; passing compatibility tests is not that audit.


Prompt 05/06 updates: local health/CLI, eight dependency outages, real KRaft
metadata/lag/auth and read-only authorization/persistence are verified. R7 is
further reduced for the isolated lab, not for private or production clusters.

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R15 | High / connector owner | private live credentials/Kerberos/mTLS, full Kudu quorum/version and advanced service semantics unverified; optional Ranger unsupported | scope adapters to declared capabilities; independent live version/role/TLS validation before deployment |
| R16 | Medium / build owner | native Windows and cold-cache second-host CLI not run; optional Hive driver changes dependency surface | host-specific acceptance and dependency/security audit before release |
| R17 | High / SRE/security lead | demo Kafka controller listener remains private plaintext with lab-only authorization; search bundled demo security persists | no production exposure; provision separate authenticated/TLS controller and broker identities/ACLs through authorized deployment |

Prompt 06 fixes the Kafka image log-directory/named-volume mismatch and verifies
retained records/offsets across teardown. This is local fixture durability, not a
backup/restore or production disaster-recovery certification.

Prompt 07: deterministic graph/time correlation passes simulated and adversarial
cases; it does not prove causation. Graph discovery and production topology
provenance remain unverified. Qualitative hypothesis bands must not be presented
as statistical causal probabilities. R1 remains a release blocker.

Prompt 08: SDK protocol/history and native read-tool integration are verified with
a mocked provider. Real OpenAI key/model are absent; live paid proof remains
NOT_CONFIGURED. Review encryption-key rotation/retention and provider budgets before
production enablement. No provider capability authorizes infrastructure mutation.

Prompt 09: native BM25, SQL tenant/ACL/version checks, review separation, revocation
and non-promotion of rejected feedback pass functional tests. R10 remains open:
eight provisional simulated cases have no independent human gold reviewer; quality
sign-off is BLOCKED pending the review record in prompt09/GOLD_REVIEW.md. BM25
precision on these fixtures is not production accuracy. No embedding benefit claim.
Demo capability tokens do not establish enterprise identities or group authorization;
index certificates, review-role ownership, retention, key rotation and security audit
remain deployment gates. R1 critical Next.js remains unresolved. No Prompt 10 work.


Prompt 09A current assessment supersedes historical Next.js status above: R1 is
cleared locally by main's exact 16.4.5 package/lock and a clean fresh npm audit.
Publication/PR/hosted CI are distinct gates: repository Git is available, but
api.github.com CONNECT is denied by the environment destination allowlist. Do not
misreport credential invalidity or hosted success. R10 human review remains open;
V2 permits scoped development while prohibiting measured accuracy claims. Production
identity, private cluster certification, provider validation and cutover remain gates.

R9 follow-up in 09A: fetched main's new UI labels response_mode=openai and uses
tool_calls; existing Java returns gpt/tool_trace. Default Python is unchanged and
Java fallback proxy smoke passes. Java provider-backed presentation remains
NOT_TESTED; align explicitly in Prompt 12 before opting into provider-backed UI.

09A hosted result: run 44102365986 for implementation head 08cc387 passed Java,
backend/CLI and frontend jobs, observed through public GitHub job statuses.
Authenticated logs and PR creation remain blocked by api.github.com destination
policy; report-only follow-up commits do not alter the validated implementation.


Prompt 09B: R10 remains open: human reviewer/approval null, no held-out set and only
seven fault/one healthy developer fixtures. Independent scorer integrity and
functional negative checks pass, not production quality. Live key/model/approved
budget absent, so provider integration remains NOT_CONFIGURED despite mocked
protocol tests. R1 remains cleared for the exact patched 16.4.5 lock/fresh clean
audit. Original historical artifacts remain inspectable.

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R18 | Medium / knowledge + SRE | Eight queries DEGRADED after stale projection rejection; fixed 30-hit native bound may starve newer approved sources at scale | reviewed retention/reindex and load/adversarial retrieval evaluation before deployment |
| R19 | Medium / AI-quality owner | returned precision 8/9; one irrelevant healthy runbook; optimistic query/corpus overlap, no independent labels | held-out expert review before relevance tuning or embedding claims |
| R20 | Low / correlation owner | UNKNOWN-mode uncertainty text mentions Kafka on non-Kafka cases | domain-aware uncertainty wording; do not infer complete capability coverage |

09B does not change application routing, schemas or production capabilities. R9
Java provider UI metadata alignment, enterprise identity/private connector security
and 09A PR access remain unresolved. Flagged read-only development may proceed only
when requested; notification/write/production deployment approvals remain separate.


Prompt 10: local opt-in monitoring gate passes; see prompt10/REPORT.md and failure
matrix. Durable dedupe/checkpoint, capability-aware rules, actual health samples,
local fake notifications, rate/incident/evidence budgets and guarded lifecycle are
verified. Native LIVE semantic ingestion and automatic paid investigation are not
enabled; operator budget and verified native topology remain prerequisites. Remote
email/webhook, production identity, retention/load/DR and independent quality gates
remain deployment gaps. Python/default routing and patched Next.js stay intact.


Prompt 11 simulation safety gate passes; see prompt11/REPORT.md. No production
executor exists. Role capabilities, hashes, expiry/fencing, independent samples,
manual uncertainty states and separately approved rollback are verified. This
reduces simulation bypass risks, not enterprise deployment/identity risks.

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R21 | High / security + change owner | demo capability roles are not independent humans; no real identity/two-person production governance | enterprise writes stay hard-denied until reviewed identity/change control |
| R22 | Medium / recovery + SRE | uncertain no-receipt executions intentionally stop; no automatic retry/abandon/reset workflow or external tamper-proof audit | retain evidence, manual escalation; reviewed native recovery/retention/DR before deployment |

Human RCA review, live provider budget/credentials, native live monitoring and
connector certification remain pending. No measured production quality, recovery
success or business-value claim. No real actions, paid calls, main merge or publish.

## Prompt 12 disposition

R9 partially addressed: fixed typed routing, role/query allowlists, private capability
binding, origin checking, signed cookie tamper/expiry and current citation access
checks pass. Owner: UI/security lead. Demo sessions are not enterprise identity and
remain loopback-only; OIDC/TLS/audit/DR are production gates. Browser outages are
labeled MOCKED; actual provider availability and human quality remain unverified.
Current Next 16.4.5 audit has zero findings; older critical-Next statements above are
historical, not a current diagnosis. Doctor no longer repeats that stale advisory.

## Prompt 13 checkpoint

Prompt 13: known local pip installer advisories were remediated with 26.3; rerun has zero findings. Java advisory lookup for 233 resolved packages is BLOCKED by OSV egress; build/security maintainer must verify hosted scan. Host disk exhaustion is a lab capacity risk; reserve documented disk budget, retain volumes, fail readiness rather than infer health.

Prompt 13 local final: fresh-checkout full acceptance and all eight owned outage/recovery cases PASS; additional actual bound-graph browser run PASS (14 checks). See prompt13/fresh-checkout.json and outages.json. Local Java OSV audit remains BLOCKED; hosted CI pending. No production readiness or live GPT claim.

Prompt 13 hosted update: four functional jobs pass on 4597517; Java security scan finds seven advisories in three dependencies. Targeted Snappy 1.1.10.9, LZ4 1.11.4 and stable OTel BOM 1.67.0 fixes are under re-verification. Gate remains blocked until a clean hosted scan; no suppression/waiver. See prompt13/java-advisories.json.

## Prompt 14 release decision — BLOCKED

prompt14/RELEASE_RISKS.json is the current release register, with proposed owner roles,
remediation, artifacts, target milestone and GO/CONDITIONAL/BLOCKED disposition.
Corrected platform-health exception, proxy stream/cookie and host binding findings
pass local negatives. Production identity, external connector certification, recovery,
financial/data governance, optional/image audit and independent quality gaps remain.
Passing local or hosted tests cannot confer deployment authorization.

Prompt 14 final evidence: all five hosted jobs PASS on implementation 8e41c2c,
run 44397526897, including full lab/restore/operational capture and fresh OSV
233 packages / zero findings. Java spans verified in-memory without private text.
Production remains BLOCKED per prompt14/RELEASE_RISKS.json; no subsequent stage.
