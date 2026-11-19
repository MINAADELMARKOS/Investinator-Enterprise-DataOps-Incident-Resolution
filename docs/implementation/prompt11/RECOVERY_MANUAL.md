# ReplayLab action and crash recovery manual

Use the Java demo's generated backend capabilities from ignored `.runtime/` secret
storage. The CLI provisions them on explicit `up`; never paste values into source,
model requests, screenshots or a PR. No additional software/service restart is a
model tool. Python remains the default frontend backend.

1. Inspect GET `/api/v1/remediation/registry`. Only REPLAY_DISK_RECOVER_V1 and
   REPLAY_DISK_RESTORE_V1 exist. Start the existing owned Kafka disk ReplayLab run,
   or triage/investigate a fresh SIMULATED monitored disk incident. A proposal may
   explicitly bind that incident to a new isolated sandbox; it is not a real broker.
2. With planner capability, POST `/dry-run` then `/plans` with incident_id, action,
   environment=ReplayLab, parameters={disk_percent:60}, parent_plan=null. Inspect
   the returned immutable target, platform/version, owner, risk, preconditions,
   postconditions and plan_hash. Never treat dry-run as approval.
3. The distinct approver posts {plan_hash,decision:APPROVE} to `/plans/{id}/approval`.
   Approval expires within 120 seconds and cannot exceed the plan's ten-minute life.
   REVOKE denies pending/authorized execution. In-flight work cannot be fictionally
   revoked after the fact: inspect receipts and use governed rollback if needed.
4. The executor posts {plan_hash} to `/plans/{id}/execute` with a fresh
   Idempotency-Key. This claims a ten-second lease and makes at most one private
   simulator resource version update plus its receipt. Retry/parallel execution is
   denied. EXECUTED means acknowledged, not healthy, resolved or a confirmed cause.
5. The independent verifier posts `/plans/{id}/sample`; it reads actual current
   simulator state and records SIMULATED provenance/version/time. Then POST
   `/plans/{id}/verify` with {plan_hash}. A pre-action, stale, wrong-version or
   mismatched value cannot pass; body-provided success/checks are never trusted.
6. Only a verified post-state resolves the incident. Inspect the protected
   postmortem endpoint and audit records. The postmortem remains
   PENDING_KNOWLEDGE_REVIEW; it is not automatically a runbook or a human gold label.

## Crash, timeout, database failure and partial success

Read plan/outbox/effect state before issuing another action. Wait for an uncertain
lease to expire, then explicitly POST `/plans/{id}/recover` using executor scope.
A matching durable receipt permits EXECUTED recovery but still needs a NEW verifier
sample after recovery acknowledgement. No receipt leaves
REQUIRES_MANUAL_INTERVENTION; never reuse the consumed execution key or infer a
retry was harmless. This implementation does not retry unknown writes. Database
faults roll back resource/effect together; a lost acknowledgement after a committed
effect is deliberately treated differently. No operator should edit leases or SQL
state to make approval or verification pass; tests do so only in disposable fixtures.

Recover only the named sandbox. If state cannot be established, retain evidence,
close unresolved through the applicable operator workflow and escalate. Manual
abandon/reinitialization of uncertain runs is a future governed operation; do not
drop tables or reset shared volumes as a workaround. Retention/backup/HA and native
production executor recovery are outside this stage and remain blocked.

## Explicit rollback

Inspect the original action's receipt and immutable pre-state. With planner scope,
create/dry-run REPLAY_DISK_RESTORE_V1, parameters={}, parent_plan=original_plan_id.
It has its own hash, resource version, approval expiry, execution key and lease.
The restore operation writes only the prior private simulator value; no inferred
reverse command is accepted. The verifier must sample/verify the restoration
independently. Both plans then show ROLLED_BACK and the incident is
closed_unresolved because the original fault has been restored.

Reproduction: `./investinator test`, then
`python3 scripts/verify_remediation_process.py` with built JDK21 jar/Docker available.
The script creates only a named disposable PostgreSQL container and local Java
processes, proves real process restart and HTTP role/approval/post-state behavior,
and cleans up only its own container. Fixtures are SIMULATED; no paid provider or
production connector action occurs. See process-acceptance.json for actual results.
