# Prompt 11 — Governed ReplayLab remediation acceptance

2026-11-18. Local simulation safety gate **PASS**. Production writes remain
**HARD-DENIED**. Prompt 10 checkpoint `1422665` was verified/committed before this
stage. No Prompt 12 work, paid provider call, default-route cutover or publication.

## Implemented boundary

The model remains a read-only recommender. A separate deterministic policy validates
fixed definitions, tenant, target, platform/version, environment, parameter schema,
risk/owner/required role, exact immutable hash, approval expiry, fresh versioned
preconditions, idempotency key, execution lease, receipt and independent post-state.

Two named actions exist: REPLAY_DISK_RECOVER_V1 and REPLAY_DISK_RESTORE_V1. Their only
executor updates private SIMULATED ReplayLab rows. There is no shell, arbitrary SQL,
URL, topic deletion, real restart/failover or native write connector. All enterprise
requests and unknown operations fail closed. Real-time bearer capabilities separate
planner, approver, executor and verifier; duplicate role secrets fail startup.
These are demo identities, not independently authenticated people or enterprise
change-control certification. No high-risk/two-person production policy is simulated.

Flyway V7 adds plans, durable execution outbox/leases, sandbox resource versions,
unique effect receipts, independent samples and review-pending postmortems to the
private Java schema. Python/Alembic data ownership and Next.js/default Python routing
are preserved. Existing ungoverned aliases still pass the compatibility suite;
once an incident has a governed plan, legacy approval/execute/verify cannot bypass it.
Original ReplayLab payload/steps are preserved when governed results are added.

An explicitly triaged/investigating SIMULATED monitored disk incident may bind a
fresh isolated named sandbox; no real broker target is inferred. The monitored
lifecycle advances awaiting approval -> mitigating -> verifying and resolves only
with independently sampled post-state. Native LIVE/unsupported incidents cannot
bind this executor. Postmortems are inspectable and PENDING_KNOWLEDGE_REVIEW, never
automatically promoted into authoritative RAG knowledge.

## Required artifacts

- [Threat model](THREAT_MODEL.md): forged identity, injection, replay, tenant/parameter
  changes, leases, DB failure, partial commit, verification and alias bypass.
- [State machine](STATE_MACHINE.md), [fixed registry and schemas](action-registry.json)
  and [policy matrix](POLICY_MATRIX.md).
- [Recovery manual](RECOVERY_MANUAL.md): approval, ambiguous completion, receipt
  recovery, independent samples and separately authorized named rollback.
- [Policy failure matrix](failure-matrix.json), [verification/audit example](verified.json),
  [real process acceptance](process-acceptance.json) and [test summary](test-summary.json).

## Actual validation

| Check | Result |
|---|---|
| Full `./investinator test` | 104 Java, zero failures/errors/skips; 26 CLI tests pass |
| Final focused policy verification/package | 14 tests pass; includes correlated missing-auth denial after audit-header update |
| Fresh physical-process acceptance | PASS: actual disposable PostgreSQL + Java HTTP, terminate/start Java, persisted EXECUTED state, repeated action denial, independent sampling, separate rollback approval/verification |
| Policy negatives | Expired/revoked approval, changed hash/parameters, unknown/production action, missing/wrong role, role spoofing, wrong tenant, parallel/repeated execute, stale resource/sample and false success denied |
| Failure recovery | Before-effect interruption remains manual; after-effect receipt recovery still needs fresh verification; partial acknowledgement and DB failure preserve correct atomic effect semantics |
| Prompt 10 integration | SIMULATED monitored disk -> governed sandbox -> audited lifecycle -> independent verified resolution |
| Legacy compatibility | Original 74-step differential trace and ungoverned approvals/replay behavior pass; governed bypasses denied |
| Python / static checks | 12 pytest, Ruff, mypy 17 files, baseline contract/link validation and diff checks pass |
| BM25 evaluation replay | Functional diagnostics pass; human quality approval still pending |
| Frontend dependency audit | Fresh npm audit zero vulnerabilities; patched Next.js lock unchanged |
| Compose / secret configuration | Base profiles + owned lab config pass; four distinct private generated capability secrets; no secret values committed |

Provider tests in the regression suite remain MOCKED where declared. The printed
paid_call_made=true CLI fixture is a stub, not actual traffic. The new process
fixture explicitly disables OpenAI and monitoring side effects; production_actions
and paid_provider_calls are both zero. It only removes its own named disposable
container. No production restart, data reset or real connector write was attempted.

The initial 13-test policy checkpoint passed; a fourteenth test added monitored
lifecycle/sandbox integration and postmortem review state. Final full/focused/process
checks pass after preserving original replay payload and adding correlated security
denial logs. No failed acceptance criterion was waived. Actual hosted CI for these
local commits is NOT_RUN; workflow steps now include both process acceptance scripts
and their lint checks. No push/merge or environment publication is claimed.

## Limits and readiness

GO for this isolated simulation gate; **BLOCKED for production execution/deployment**.
Demo bearer capabilities are not OIDC, real human independence or enterprise RBAC.
A future real executor requires independently authorized change control, two-person
policy where risk demands it, certified targets, native read/write separation,
TLS/secret review, durable external audit, HA/DR, retention and reviewed recovery.
Database administrators remain trusted; audit is not externally tamper-proof.

Execution is intentionally at-most-one attempt and does not auto-retry ambiguous
writes. Recovery without a known effect remains manual; an operator-abandon/reset
workflow is not invented. The verifier samples actual *simulator* state, not real
Kafka health, and legacy telemetry charts are not used as a success oracle. Native
live monitoring, remote notifications, approved automatic model spending, human
RCA review, provider credentials/budget and Java-backed UI adaptation remain the
previously recorded gaps. The 09A GitHub PR/API access issue also remains unresolved.

Prompt 12 may be considered only on a new user request. Stop after the local commit.
Rollback of this implementation is a reviewed revert/disable of the new API; retain
V6/V7 state/evidence. Never reset schemas, delete shared volumes or infer reverse
commands as a rollback. Named simulator rollback is itself approved and verified.
