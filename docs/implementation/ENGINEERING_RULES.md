# Engineering execution contract

Read STATE.md and DECISIONS.md before changing code in every session. The user
now authorizes stages 00–09; later specification prompts are a roadmap,
not permission to implement stages 10 onward. Keep one commit per completed stage and stop at
failed acceptance gates. Never push or merge main without authorization.

Preserve Next.js design, APIs and aliases, data, audit, demo scenarios and approval
semantics. Keep Python until tested contractual parity and approved switchover.
Use a Java 21 Spring Boot modular monolith and incremental strangler routing.
Maintain Infrastructure / Model / Platform / Agent / Application boundaries.
Observation, Evidence, Hypothesis, RootCause, RemediationPlan, Approval and
Verification must be separate typed concepts. Correlation is not causality.

Diagnostics are read-only. GPT may select validated allowlisted diagnostic tools;
it cannot authorize changes or execute commands. Production writes remain disabled
without independently authenticated authorization, allowlisting, time-bound
approval and independent recovery verification. Never silently erase data/volumes.

Pin verified compatible dependencies, Maven Wrapper and images in the scaffold
stage; use reproducible scripts and deterministic fixtures. Never guess SDK methods
or model IDs. Secrets and model settings remain backend-only. Redact before model
transfer; retrieved text is untrusted. Paid provider probes require explicit opt-in.
Report LIVE, SIMULATED, CACHED, NOT_CONFIGURED or NOT_TESTED truthfully.

Each feature needs unit, integration, negative and regression coverage appropriate
to its behavior. Record actual commands, exit codes, failures, skips and provenance.
Do not conceal high/critical vulnerabilities or replace unavailable services with
fake successful health. Every stage updates state, decisions, risks and evidence.
Use the existing isolated checkout; do not create worktrees unless requested.
