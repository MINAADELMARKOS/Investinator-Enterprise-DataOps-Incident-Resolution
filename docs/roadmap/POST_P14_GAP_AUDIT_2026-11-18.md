# InvestiNator — Evidence-based gap audit after Prompts 10–14

**As assessed:** 2026-11-18. **Decision:** local development and demonstration CONDITIONAL GO; production deployment and non-simulated remediation **NO-GO / BLOCKED**.

This document records an engineering **assessment and closure plan**, not a declaration that the listed deployment gaps have been fixed. Changes to executable code must be verified separately. The single coding-agent remediation instruction is [Prompt 14B](PROMPT_14B_SINGLE_REMEDIATION_AND_PUSH.md).

## Source of truth and current code

- Remote main at audit: afb818309f528a843e5be9990b39363dc6daef61, after PR #6 merged earlier Java baseline. Default remains Python + Next.js. Java is opt-in.
- Remote Java branch: feature/java-ai-audit at 15755eca83713411f870153ee6bb072ce4e77857; 14 commits ahead of main, zero behind at audit. This **branch exists and is pushed**, unlike historical v2 documentation.
- Java branch contains Prompt 10, 11, 12, 13 and 14 reports and working modules; the Prompt 14 stage was finished after the user-provided Prompt 13 excerpt.
- Latest GitHub Actions run 44398954163 (Java branch) has five successful jobs: backend, frontend, java-scaffold, local-e2e and dependency-security.
- Prompt 13 implementation run 44391359760 and Prompt 14 implementation run 44397526897 also completed successfully. Check GitHub Actions for up-to-date states; a green CI run does not certify production.
- The latest Prompt 14 code-based corrections include restricted non-demo health, loopback demo binding, bounded Next.js request bodies, secure-cookie origin handling, private metrics, optional privacy-preserving Java spans, local backup/restore and proposed Prometheus rules. Do not open duplicate tasks to reimplement them.

**Important:** Main and the Java feature branch are not the same deployable revision. The 14 newer feature commits are NOT merged into main by the act of publishing this roadmap. Review and merge through normal CI-backed pull requests only.

## Verified findings vs remaining gaps

| Source | Implemented/reported evidence | Remaining issue | Classification | Proposed closure path |
|---|---|---|---|---|
| P10 opt-in monitor | V6 durable outbox/offset, tenant incident uniqueness, event-time rules, dedupe and lifecycle; actual process restart, simulated incident stream | No certified live event ingestion from enterprise services; paused/disabled by default; retention and long-soak not certified | DEVELOPABLE + EXTERNAL_CERT | Build bounded native-adapter ingress behind default-off flag, mode/version/tenant contracts, tests and monitoring; never auto-enable live |
| P10 notifications | Durable LOCAL_FAKE, bounded attempts | No authorized email/webhook integration or delivery configuration | CONDITIONAL | Add contracts and deny-by-default transport; test only fake sink until approved destinations/identity |
| P10 model invocation | No automatic GPT spend; operator-budget gated | Real rate/money caps, model price/usage reconciliation, provider authorization absent | EXTERNAL_GATE + DEVELOPABLE | Durable global/per-tenant reservations and reconciliation with no paid calls; authorized provider smoke only by opt-in |
| P11 guarded remediation | V7 fixed simulation actions, scoped plans/hash/expiry, execution receipts, independent samples, approved sandbox rollback | Demo role secrets are not OIDC, two-person approval, or a real change-control system | P14-01 BLOCKED | Identity integration and separation-of-duties tests; real write executor remains absent |
| P11 failure recovery | Manual escalation for before-effect uncertainty; no replay of ambiguous effects | External tamper-resistant audit, retention, human recovery playbook and HA needed | P14-03/07 BLOCKED | Test exportable append-only audit and recovery runbook; do not auto-retry unknown effects |
| P12 Java opt-in UX | Signed demo sessions, fixed-origin proxy, health/chat/evidence and governed UI; actual browser cases | Enterprise ingress/auth missing; real provider conversation not verified | P14-01/04 BLOCKED | Keep Python default and Java opt-in; add contract/fallback and live opt-in diagnostic coverage |
| P13 regression lab | Fresh checkout, eight owned dependency outages, simulated KRaft/legacy flows, 14 browser cases; five hosted jobs passed | Not a cold-host/Windows test; no optional shaded Hive/container OS security certification | P14-08 BLOCKED | Expand optional-profile SBOM/security verification and OS/container scans, independently label remaining untested platforms |
| P14 security | Next.js 16.4.5 npm audit clean, Python dependencies audited; default Java OSV 233 packages / 0 findings | Enterprise OIDC/RBAC, connector Kerberos/TLS, egress policy, optional profiles/container image CVEs | P14-01/02/06/08 BLOCKED | Release blocker matrix and hardened, tested staging configs; require external evidence |
| P14 recovery | Local 38-table/781-row disposable snapshot/restore; no source writes | No offsite encrypted backups, PITR, HA/DR, extended partition or RPO/RTO certification | P14-03 BLOCKED | Separate owners, staging restore/replica/WAL and signed RPO/RTO; do not imply local snapshot is DR |
| P14 AI evaluation | Reviewed knowledge lifecycle and 8 developer-authored cases; scorer functioning | Human gold-set sign-off and independent holdout absent | P14-05 BLOCKED | Reviewer packet/label audit; only independent authorized reviewer can approve |
| P14 data governance | Redaction, tenant capability checks and local audit | Retention/legal hold/erasure/source licenses/provider transfer and immutable external audit unapproved | P14-07 BLOCKED | Data inventory, policy templates and negative tests; legal/privacy sign-off later |
| P14 observability/SLO | Fixed-cardinality metrics, optional private spans, local 30-request baseline | No staging/production SLO, sustained throughput, MTTA/MTTI/MTTR, real cost/ROI | P14-09 CONDITIONAL | Versioned KPI definitions, staging load/soak test with provenance, no invented economics |

### Known quality issues from Prompt 09B that still matter

1. Eight review cases are **developer-authored simulated fixtures**. The independent human review remains pending; case outcomes do not establish production root-cause accuracy.
2. BM25 evaluation reported one irrelevant healthy-context result (precision 8/9 on fixture outputs), and all eight queries were DEGRADED after SQL stale-projection rejection. Investigate retrieval maintenance, revised candidate bounds, ACL/revision and larger holdout testing; do not optimize against only the development fixtures.
3. UNKNOWN-mode text sometimes mentions Kafka for non-Kafka incidents. Replace with domain-specific uncertainty messages in a separately tested stage.
4. Actual Java OpenAI Responses traffic remains NOT_CONFIGURED; SDK protocol tests were MOCKED_PROVIDER, not a paid model verification.

### Observed documentation drift — fix included in the current GitHub documentation PR

- Original roadmap stated backend-java and feature/java-ai-audit were not yet on GitHub. That is **historical** and now false: the Java baseline was merged through PR #6; later work is pushed to the feature branch.
- Some per-stage reports use old “Next.js critical vulnerability” wording from before the 16.4.5 upgrade. Preserve those as historical evidence, but add a dated current-status note rather than rewriting test history.
- Current main README describes the Java work as only a scaffold and “not pushed”; correct the current overview without implying Prompts 12–14 have been merged.
- Human review, live provider, private cluster certification and enterprise deployment boundaries must remain prominently displayed even with green hosted CI.

## Severity, owner and evidence contract

- **P0: release blockers**, not code-only fixes: P14-01 identity, P14-02 private connector certification, P14-03 HA/PITR and recovery, P14-04 live GPT budget, P14-05 independent reviewer, P14-06 perimeter, P14-07 legal/retention, P14-08 optional/container security.
- **P1: safe engineering tasks**: tighten native collector interfaces and tenant/time/mode provenance; improve BM25 stale projection handling and domain-aware uncertainty; augment no-cost provider budget/handoff diagnostics; extend CI optional-profile and long-run test coverage; deliver bounded recoverable incident monitoring. These must be justified from actual code and retain default-off guards.
- **P2: measurement and handover**: staging SLO load/soak, operator runbooks and executive outcomes; metrics unmeasured until a real baseline and independent target exist.

**Evidence labels:** VERIFIED_REMOTE (GitHub-run source and commit), VERIFIED_LOCAL (actual named process/fixture), MOCKED_PROVIDER, SIMULATED_SOURCE, NOT_CONFIGURED, NOT_TESTED, BLOCKED, HUMAN_REVIEW_PENDING. Distinguish healthy local infrastructure from synthetic faults and generated documents.

**Definition of done:** every P0 has (a) a clear owner and required external evidence, (b) a tested fail-closed condition and (c) a reproduction reference. Only actual independently reviewed evidence can change a BLOCKED status; scaffolding does not certify production. Each safe P1 code fix requires a tested commit and hosted CI evidence. No force-push, production writes or unapproved model billing.

## Recommended execution

Run [Prompt 14B](PROMPT_14B_SINGLE_REMEDIATION_AND_PUSH.md) in the checkout with latest feature/java-ai-audit. Its instructions include a complete preflight, staged code fixes, tests and push/PR without automatically merging the feature into main.

**Final release disposition at this audit:** local demonstration CONDITIONAL PASS; non-simulated production operations BLOCKED. A fresh hosted CI success cannot replace identity, independent gold review, provider spend authorization, real connector testing or security/legal certification.
