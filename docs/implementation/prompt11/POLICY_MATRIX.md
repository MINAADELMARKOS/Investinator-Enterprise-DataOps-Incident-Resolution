# Capability and policy decisions

All endpoints below are under `/api/v1/remediation`, demo-only and server-tenant
bound. No enterprise endpoint is enabled without independent identity/change control.

| Operation | Required backend capability | Additional requirements |
|---|---|---|
| GET registry/plan/postmortem | Any configured remediation role | Own tenant; source remains SIMULATED |
| POST plans / dry-run | REMEDIATION_PLAN_TOKEN | Fixed registered action, ReplayLab, bounded args, owned run or eligible simulated disk binding |
| POST plans/{id}/approval | REMEDIATION_APPROVE_TOKEN | APPROVE/REVOKE; exact reviewed hash; unexpired proposal; no in-flight revocation fiction |
| POST plans/{id}/execute | REMEDIATION_EXECUTE_TOKEN | Matching approval/hash, unique Idempotency-Key, fresh unchanged resource, fenced lease |
| POST plans/{id}/recover | REMEDIATION_EXECUTE_TOKEN | Expired uncertain lease; inspect receipt; never automatic re-execution |
| POST plans/{id}/sample | REMEDIATION_VERIFY_TOKEN | Independent read of scoped simulator row; caller cannot supply measurements |
| POST plans/{id}/verify | REMEDIATION_VERIFY_TOKEN | EXECUTED, valid receipt, sample after ack, <=60s age, exact current version/value |
| Named rollback | Planner -> approver -> executor -> verifier | Parent effect receipt, separate plan/hash/key/approval, independent restoration sample |
| Real service/topic/filesystem/SQL action | None | Hard denied; no write connector or arbitrary interpreter exists |

The four secrets must be pairwise distinct when configured; sharing one fails
startup. `decided_by`, role labels and requested tenant cannot become identity.
Read-only mode `investinator.remediation.read-only=true` denies all mutations.
The default named ReplayLab simulator is available only with explicit role tokens;
missing tokens do not fall back to the unprotected generic API rule.

Dry-run has no plan/resource/outbox writes and confers no execution authority.
A persisted proposal returns a policy decision that is eligible for review but
execution_authorized=false. The registered platform/version is the simulated
Kafka 4.3 scenario, not certification of the user's actual cluster version.
