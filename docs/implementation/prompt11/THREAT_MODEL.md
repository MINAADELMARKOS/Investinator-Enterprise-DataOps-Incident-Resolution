# Replay-only remediation threat model

Assessment: local demo safety boundary, not enterprise security certification.
All operations outside the named private ReplayLab model are denied. The model
registry remains diagnostic-only: it cannot plan through a write tool, approve,
execute, supply a URL/SQL/shell argument, sample production state or verify success.

| Threat | Enforced boundary | Evidence |
|---|---|---|
| Forged role / decided_by / missing authentication | Distinct server-configured planner/approver/executor/verifier bearer capabilities; client actor claims ignored; non-demo ingress denied | HTTP missing/wrong-role/body spoof tests |
| Cross-tenant plan/approval/execute | Every query/write scoped by server tenant; foreign references return denial without data | Wrong-tenant approval/execute tests |
| Prompt-injected logs/runbooks | Read-only tool registry unchanged; no remediation tool name or dynamic executor exists | Existing injection/tool tests plus action registry review |
| Unknown action or real target | Two fixed ReplayLab action definitions, strict parameters, environment exactly ReplayLab, owned run/source binding | Unknown action / production / extra shell parameter tests |
| Replayed or expired/revoked approval | Exact immutable content hash, approved hash, expiry, revocation and required state checked before claiming | Expiry/revocation/tamper/repeat tests |
| Changed resource after review | Captured version/value/freshness checked, conditional version update and lease fencing | Stale-precondition / duplicate / DB transaction tests |
| Concurrent requests / UI retry | Per-tenant transaction serialization, unique execution key, one consumed claim and unique effect receipt | Parallel execution/repeated request tests |
| Partial commit / DB outage | Resource update and effect receipt share one transaction; durable plan/outbox claim is separate | SQL-trigger interruption rollback / partial-ack tests |
| Lost ack / process crash | EXECUTING lease expires, explicit recovery inspects durable receipt, no automatic retry | Before-effect/after-effect recovery and real process restart |
| Executor lies about success | Ack does not resolve; receipt plus current independently sampled value/version/time required | No-effect ack, stale samples, client-provided fake checks |
| Rollback inferred by LLM | Separate named restore action, explicit parent receipt, own dry-run/hash/approval/claim/sample | Denied and approved rollback tests |
| Legacy-route bypass | Existing aliases refuse approval/execute/verify for governed incidents | Legacy bypass test; old ungoverned compatibility still covered |
| Audit loss or spoofing | State/audit in transaction; role actors server-bound; plan/evidence/outcome/correlation IDs recorded | Verified audit artifact; HTTP authorization denials have request-correlated security logs |

The planner can create an explicitly isolated ReplayLab sandbox from an existing
owned kafka_disk_pressure run or a triaged/investigating SIMULATED monitored disk
incident with fresh evidence. It never turns that source into a real broker target.
Real LIVE incidents and unsupported faults cannot bind an executor. Pending/failed
samples cannot confirm a cause. Human AI-quality review remains separate.

Bearer capabilities prove possession of separate demo roles, not OIDC users or
independent people. A single operator may own the fixture credentials; this is not
presented as two-person governance. Both allowed actions are LOW_SIMULATION_ONLY;
no high-risk/production action is registered. Any future high-risk action requires
independent identity, a reviewed two-person policy and a separate authorization
implementation before enabling writes. Demo credentials are private and never
placed in model context, repository artifacts or client bundles.

Trusted assumptions: application code/migrations and its private database operator
are not adversarial. A database administrator can rewrite state/audit; this stage
does not claim tamper-proof external audit storage, cryptographic nonrepudiation,
HA/DR, enterprise RBAC or security certification. Production remains BLOCKED.
