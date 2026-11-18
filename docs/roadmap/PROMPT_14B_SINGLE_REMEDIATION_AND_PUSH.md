# PROMPT 14B — One consolidated gap-closure, verification and GitHub publication task

**This is the single master prompt to execute in Codex.** It continues the already pushed Java work after Prompts 10–14. It is intentionally staged: perform feasible safety-preserving engineering improvements; accurately record anything requiring independent human review, credentials, deployment approval or private infrastructure. **Do not falsely label a production gate PASS.**

Read the companion [current gap audit](POST_P14_GAP_AUDIT_2026-11-18.md) and the existing roadmap [architecture v2](JAVA_AI_ARCHITECTURE_V2.md).

---

## PASTE THIS ENTIRE PROMPT INTO THE CODING AGENT

Act as the **Principal Java/Spring Boot Architect, Distributed Systems SRE, AI Evaluation Engineer, Enterprise Security Reviewer and Git Release Engineer** for InvestiNator.

**Repository:** https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution

**Expected initial remote state as of 2026-11-18:**
- main: afb818309f528a843e5be9990b39363dc6daef61 (Java foundation merged through PR #6; default Python).
- feature/java-ai-audit: 15755eca83713411f870153ee6bb072ce4e77857 (Prompt 14 reports and code pushed; 14 commits ahead, 0 behind at audit).
- Latest Java CI: 44398954163, all five jobs PASS.
- Prompt 13 implementation CI: 44391359760 PASS; Prompt 14 implementation CI: 44397526897 PASS.
- Prompt 14 production decision: BLOCKED. No default Java cutover, production write executor, verified live GPT call, enterprise identity or independent human gold-set approval.

**These SHAs and results are checkpoints, NOT authority to overwrite newer work. Fetch the current state first; if changed, record differences.**

### Non-negotiable engineering contract

1. Work in the actual repository that contains backend-java, Python/FastAPI, Next.js, PostgreSQL Flyway/Alembic ownership, evidence collection, read-only adapters, correlation, GPT SDK, knowledge/RAG, monitoring, governed ReplayLab and system-health tooling. **Never rebuild completed Prompts 10–14 from scratch.**
2. Preserve default Python routing. Java remains opt-in. Flyway owns only the private Java schema, Alembic only the Python schema; do not merge schemas or delete data. Never change production routing or external ingress.
3. Keep monitoring disabled/paused for new tenants; real data ingestion remains off until capability and security certification. KRaft **must not** query ZooKeeper; legacy ZooKeeper is only for verified compatible legacy deployments.
4. Keep production actions HARD-DENIED; named ReplayLab simulation is the only executor. Never run a shell/SQL/system command supplied by GPT, and never grant policy authority to a model answer or retrieved knowledge.
5. No paid provider calls without an actual configured key, enabled model, approved budget and explicit opt-in. A MOCKED_PROVIDER test and a CLI fixture field do not count as live model traffic.
6. Do not invent human reviewer identity/signature, enterprise Kerberos/TLS/OIDC configuration, approval, private-service health, monetary savings, SLA/SLO achievement or business ROI. Use NOT_CONFIGURED / NOT_TESTED / BLOCKED with evidence.
7. Never leak .env, bearer tokens, session secrets, private certificates, protected logs, database dumps or prompts. Keep minimal public liveness public, and private API/metric/evidence surfaces authenticated and tenant-scoped.
8. Preserve every existing test gate, npm audit, OSV/dependency-security checks, negative authorization cases and data provenance fields. Do not change thresholds merely to green CI or mark failing tests skipped. No force-push/reset --hard or destructive volume cleanup.
9. Execute in **small, reviewable, sequential commits**. Commit each logical fix only after its focused regression checks. Prefer a modular monolith, bounded adapters and existing infrastructure over introducing unneeded agents/queues/microservices.
10. Finish with remote branch PUSH and PR for review; **do not merge into main automatically**. If API access is blocked, push the branch if allowed, provide an actual compare URL and mark PR_CREATION_BLOCKED; never claim a PR exists when it doesn't.

### Phase 0 — Audit real source, protect changes, reproduce baseline

- Inspect working tree, remotes, branch heads, open PRs, latest GitHub Actions, current Java/Python and frontend code, docs/implementation/STATE.md, GAPS.md, RISK_REGISTER.md, HEALTH_MATRIX.md.
- Read all exact reports and JSON artifacts for Prompts 09B and 10–14, particularly prompt14/RELEASE_RISKS.json, prompt14/REPORT.md, prompt13/hosted-ci.json and prompt09/GOLD_REVIEW.md.
- Confirm the current state from git; create a non-destructive backup ref, make a fresh feature branch such as feature/p14b-gap-closure from the latest feature/java-ai-audit, fetch origin and reconcile newer main only with an explicit normal merge and conflict ledger.
- Produce initial matrix with: gap ID, existing implementation, reproduction, failure if any, security owner, scope, priority, proposed patch, required evidence, external dependency, acceptance, safe rollback. Do not assume an old report's warnings remain current; current Next.js version is 16.4.5 with recent green audits.

**Gate:** exact source and baseline checks known; no untracked secrets or ambiguous branch ownership. If critical baseline checks already fail, fix/report them before adding capability.

### Phase 1 — Fix repository-closable Prompt 09B and knowledge reliability defects

- Reproduce the **BM25 degraded/stale projection** condition where all eight developer queries were partially degraded and returned one unrelated healthy-context hit (fixture precision 8/9). Inspect KnowledgeIndex/KnowledgeService SQL bounds, projection revision invalidation, tenant ACL and source versions.
- Implement **bounded, repeatable** stale-index cleanup/reindex/reconciliation or a safe revision-filtered retrieval strategy. Never delete live knowledge without an approved retention policy; prevent revoked/stale records from appearing. Use time/size limits and explicit partial/UNKNOWN when the index cannot be repaired.
- Correct domain-agnostic UNKNOWN wording that incorrectly names Kafka for non-Kafka findings. Keep causes explicitly hypotheses, not numeric confidence probabilities.
- Add coverage for stale versions, tenant and document ACL, revocation, healthy false-positive, empty corpus, high-cardinality sources, score ties, retrieval budget exhaustion, contradictory evidence, wrong platform mode and injection text.
- Report retrieval recall@k, precision@k, citation validity, abstention and unauthorized/stale return separately. Developer-authored fixtures may be used for functional regression only; **never declare human-verified RCA quality**.
- Build the independent reviewer handoff for all eight cases, including version/mode, timeline, alternative explanations, negative controls, contradictory evidence, citation sources, corpus revision and label fields. Only an actual independent reviewer may sign GOLD_REVIEW.md.

**Gate:** no ACL leak, no stale citation asserted current, no degradation hidden, reproducible fixture outcomes. Human RCA GOLD_SIGNOFF stays BLOCKED/PENDING.

### Phase 2 — Close safe Prompt 10 monitoring integration gaps without enabling production

- Review MonitoringScheduler, MonitoringService, DetectionRules, DiagnosticConnector SPI, Flyway V6 and existing pause/resume/smoke CLI; preserve all opt-in + per-tenant incident constraints.
- Implement only **capability-gated, fixed read-only native signal ingestion contracts** that can be validated with local Kafka/health protocol fixtures; source must carry tenant, cluster, service/version, KRaft/legacy mode, observed_at, ingested_at, evidence ID, live/simulated provenance and freshness. Reject anonymous/client-asserted LIVE and foreign tenant/source.
- Make event-time watermark, out-of-order buffering, duplicate event IDs, backpressure/drop tracking, bounded queue retention and restart checkpointing explicit. Avoid unbounded full-table scans and log storms.
- Preserve existing 1000-duplicate/parallel-worker/incident fingerprint tests, hysteresis, unknown semantics, cooldown, dedupe, fake notification outage and zero automatic paid calls.
- Extend failure/soak tests with long duplicate bursts, delayed observations, missing health, clock skew, version mismatch, paused tenant and service restart; report actual test duration and scope.
- Default native ingestion to DISABLED unless every required connector capability, auth and tenancy control has been independently certified. No automatic remote email/webhook or model call.

**Gate:** one logical tenant-specific incident per bounded correlated cause in isolated lab; no invented signal or production read claim. Real enterprise ingestion stays NOT_CERTIFIED.

### Phase 3 — Close safe Prompt 11 remediation gaps, preserve hard-denial

- Inspect Flyway V7, RemediationController, fixed action registry, policy, lease/fencing, independent samples, review-pending postmortem and old compatibility aliases.
- Extend the **manual** recovery runbook and API/status explanations for before-effect ambiguity, after-effect receipt recovery, operator intervention, failed verification and disaster replay. No automatic retry of an ambiguous action.
- Add an exportable, redacted, tenant-filtered append-only audit envelope or staging sink contract if code review supports it; do not call it externally tamper-proof without a certified separate audit service. Ensure no mutable client role/decided_by or response text can approve/execute.
- Explicitly test revoked/expired approval, simultaneous execution, changed params/hash, wrong tenant, forged role, model prompt injection, duplicate result/receipt, stale preconditions, interrupted Java process, DB outage, false-success sample, legacy alias bypass, named separately approved rollback, and native LIVE/production hard denial.
- The two existing fixed simulator actions and their independently verified result remain the ONLY executable actions. Do not add live Kafka/HDFS/Kudu/NiFi/Spark executor or destructive cleanup.

**Gate:** existing simulations and independent verification pass; enterprise authorization remains BLOCKED until actual OIDC + independent humans + approved change control.

### Phase 4 — Prompt 12 UI correctness, privacy and model integration

- Compare Java and Python API contracts, ChatService, ConversationController, Next.js fixed-origin proxy, bounded-body handling, signed demo sessions and existing Investigator/Health/NerveMap/Approval screens.
- Unify the **presentation adapter**, not database ownership: support provider status, tool result provenance, evidence citation freshness, reversible conversation state, missing usage UNKNOWN and actual token counters only when returned. Preserve the Python and Java response envelopes through typed mapping.
- Make live-model unavailable/fallback visible; never label MOCKED_PROVIDER, Java SDK protocol test or static GitHub Pages chatbot as actual GPT traffic.
- Test model timeout, malformed response, unavailable OpenSearch, stale evidence, wrong tenant, invalid signature, body over-limit including chunked/no Content-Length, invalid UTF-8, CSRF, disallowed proxy route, secure-cookie HTTPS origin, simulated-vs-live label, and browser accessibility.
- Add a **budget-only preflight** for live GPT (backend-only, key/model validation and explicit cost approval). If credentials and approval are absent, return NOT_CONFIGURED and execute ZERO real calls. Never request the user to put a key into the repository.
- Keep Next.js 16.4.5 and security audit clean. If a new patched release is verified by the dependency scanner, upgrade the manifest + lock together without downgrading security.

**Gate:** both local backends and browser flows pass, Python default works, no secret leaks, no evidence mode confusion. Live provider proof remains separately conditional.

### Phase 5 — Prompt 13 + 14 reproducibility, security and release-hardening work

Use docs/implementation/prompt14/RELEASE_RISKS.json as the immutable **starting release blocker inventory**. Make a per-risk closure disposition and add tests where feasible:

- **P14-01 Identity:** design/implement fail-closed OIDC/JWT validation **only** behind explicit off-by-default staging config. Tests must check issuer, audience, signature, token expiry, role/scope, tenant, replay and separation of duties; don't pretend demo capability tokens are OIDC or authorize enterprise writes. Without IdP integration evidence, BLOCKED.
- **P14-02 Connectors:** add an automated version/auth/capability certification harness with fixtures and negative tests for Spark/YARN/HDFS/Hive/Kudu/NiFi/Kafka/KRaft/legacy ZK/Airflow and optional Impala/CM/Atlas/Ranger; fail with NOT_CONFIGURED when Kerberos, TLS or representative private target is unavailable. Never claim a mocked endpoint proves private cluster behavior.
- **P14-03 DR:** add bounded disposable WAL/PITR, schema-owner collision, startup/restore and search-index rebuild rehearsal when feasible. Keep dump secrets out of CI artifacts. Offsite-encrypted HA/replica certification remains NOT_TESTED without infrastructure and RPO/RTO approval.
- **P14-04 FinOps:** build durable tenant/model reservation, global quota and post-response token/cost reconciliation with rate-limit and timeout tests; preserve unknown price/usage as UNKNOWN, not zero. Fail closed on budget exhaustion. NO paid call unless explicitly approved.
- **P14-05 AI quality:** preserve independently unsigned gold set; produce reviewer-ready package, holdout partition proposal, quality metrics and failure modes. Never self-sign.
- **P14-06 Perimeter:** review loopback Compose binds, TLS public-origin cookies, private Actuator/metrics, bounded body parsing, auth/egress allowlists and no arbitrary remote URLs. Add only default-deny, locally testable controls. Real ingress network certification remains BLOCKED.
- **P14-07 Governance:** inventory data classes, logging/citations, model transfer, runbook licenses, minimum retention, legal hold and deletion architecture; implement safe policy-enforcement scaffolding and denial tests, **not** unapproved destructive purging. Approval belongs to security/privacy/legal owners.
- **P14-08 Dependencies/images:** preserve existing Python pip/npm/Java OSV gates. Extend SBOM/optional shaded Hive driver and Docker image OS vulnerability scans where the runner permits. Never suppress high/critical advisories or substitute a scanner network failure for a PASS. Keep missing scans NOT_TESTED/BLOCKED and an explicit accountable owner.
- **P14-09 SLOs:** verify fixed-cardinality, bounded metrics and RED/USE/queue health; test stale/NaN/unavailable conditions and long-running memory/queue budget. Supply staging load/soak harness with separate real/simulated labels; no invented 30-day availability or business ROI.
- **P14-10 Completed corrections:** keep regressions for no non-demo health bypass, strict Next.js request-stream bound, HTTPS secure cookie, loopback-only demo, safe spans and redacted telemetry.

**Gate:** All preexisting high/critical findings require either tested remediation or explicit release BLOCKED. No blanket “all gaps fixed” status. Production release cannot be enabled by an automated coding agent.

### Phase 6 — Execute full regression and independently verify repository outputs

Run the actual repository's commands and capture the exit codes and source revision:

- JDK 21 Maven Verify with Testcontainers, including schema ownership, API parity, graph/time/correlation, read-only native protocol fixtures, health/monitor/failure matrices, RAG and authorized simulator.
- Existing CLI unit tests, lint, doctor/health/smoke and named local process restart tests.
- Python pytest, Ruff, mypy, baseline API/link/source validation.
- Frontend npm ci, production build, TypeScript/lint, frontend contract tests and npm audit --audit-level=high.
- Full Docker Compose configuration for base and named optional profiles; isolation-safe up/unchanged-up/down/restart and **only** explicitly test-owned disposable volume cleanup.
- Hosted CI jobs for backend, frontend, java-scaffold, local-e2e, dependency-security. Run new code-specific negative tests and scans; don't mark jobs skipped or green from an older SHA.
- Record 8 owned dependency outages where feasible, KRaft vs legacy guard, 1000-duplicate storm, audit/approval/rollback, browser session/CSRF and telemetry redaction.
- If full host Docker/Java prerequisites are absent, record NOT_TESTED with exact blocker and STOP before declaring stage PASS. Keep test fixtures and actual source modes visible.

### Phase 7 — Produce deliverables, commit and push (required)

Write or update:

- docs/implementation/prompt14b/REPORT.md — actual fixes vs remaining blocked items, baseline and end-state.
- docs/implementation/prompt14b/GAP_CLOSURE_MATRIX.json — one row per P10/P11/P12/P13/P14 risk, source ref, owner, status, changed code, tests, evidence, blocker.
- docs/implementation/prompt14b/VALIDATION.json — command, SHA, exit status, test number, source/provenance, CI run link; no fabricated counts.
- docs/implementation/prompt14b/RELEASE_DECISION.md — local-demo and production dispositions; release remains BLOCKED until actual owner review.
- docs/implementation/prompt14b/PR_DESCRIPTION.md — precise PR summary, impacted modules, DB migration/rollback and risk disclosures.
- docs/implementation/STATE.md, GAPS.md, RISK_REGISTER.md, HEALTH_MATRIX.md, DECISIONS.md — current authoritative summary at TOP; preserve older checkpoint evidence as historical and explicitly label older Next.js/09A blocked status as superseded when confirmed.
- Update README and architecture diagrams only for functionality backed by tested code; keep static GitHub Pages explicitly simulated.

Perform a secret scan and git diff --check; no unrelated refactors or uncommitted changes. Commit in logical groups with signed-off test output if available. Push the new topic branch using **normal** git push; no force/rewrite. Create PR to main only after checking main divergence and actual CI. A green branch pipeline is not automatically an approval to merge. If GitHub API for PR creation is unavailable, report the pushed branch URL and honest manual compare link. Do NOT merge the pull request or deploy to production automatically.

### Required final response from Codex — do not stop with “done”

Show:

1. **Starting revisions:** main SHA, source feature SHA, new feature SHA, branch status, conflict ledger.
2. **Gap changes:** one table per P10–P14 grouped as FIXED_AND_TESTED / PARTIALLY_IMPLEMENTED / BLOCKED_EXTERNAL / NOT_CONFIGURED / NOT_TESTED, with code paths and exact tests.
3. **Security and safety:** live writes = disabled, provider paid calls = zero unless explicitly authorized, tenant isolation and approval denials, secret scan.
4. **Build:** Maven/CLI/Python/Next.js/Browsers/Compose/security run results with actual counts and source mode.
5. **GitHub:** commit links, actual pushed SHA, PR URL or concrete API blocker, run IDs and latest conclusions. Do not confuse a remote branch with code merged into main.
6. **Enterprise disposition:** production GO/NO-GO, unresolved P14 risk IDs, evidence/owner needed to resolve each; no invented sign-offs.
7. **Next action:** one concise recommended next stage, such as Prompt 15 handover after safe P1 fixes and independent owner review, or a blocked gate requiring explicit external setup.

**STOP after producing and pushing this verified work.** No subsequent prompt, production cutover, release, credential setup, remote notification, destructive cleanup or live model billing is authorized.

---

## Expected output template (illustrative structure, not claimed results)

| Area | Expected deliverable | Acceptable end state |
|---|---|---|
| P09B knowledge | Tenant-scoped versioned retrieval, domain-aware UNKNOWN, independent reviewer packet | Local fix PASS; human review PENDING |
| P10 monitoring | Safe native-ingestion contract + soak/replay + missing data handling | Local fixture PASS; enterprise native DISABLED |
| P11 remediation | Governed simulation, manual ambiguity response, audit contract | ReplayLab PASS; production writes HARD-DENIED |
| P12 app | Safe provider/fallback display + role/tenant/CSRF tests | Java opt-in + Python default PASS; live provider may be NOT_CONFIGURED |
| P13 E2E | Fresh clone, CI, optional scans, outage/regression suite | Actual hosted run PASS or explicit failure |
| P14 security | Per-risk closure evidence and maintained hard-deny | Development CONDITIONAL; production BLOCKED until outside approvals |
| GitHub | Branch, commits, PR, CI links and no force-push | Only actual observed artifacts reported |

**Success means measurable reduction in unresolved engineering gaps, not removal of required security and human-governance controls.**
