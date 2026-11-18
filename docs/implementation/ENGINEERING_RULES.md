# Engineering execution contract

Read STATE.md and DECISIONS.md before changing code in every session. The user
authorizes completed stages through 11 and Architecture v2 Prompts 12 and 13
sequentially in this session. Finish and commit 12 acceptance before 13. Read docs/roadmap/JAVA_AI_ARCHITECTURE_V2.md and POST_P09_PROMPTS_V2.md;
the v2 sequence supersedes the original chain from 09A onward. Prompt 14 onward requires a subsequent request. No live provider request without
configured credentials and an independently approved testing budget. Prompt 09 human review remains pending
and blocks measured AI-quality claims, not repository synchronization. Keep one commit per completed stage and stop at
failed acceptance gates. The user explicitly authorizes pushing feature/java-ai-audit and opening its PR
after verification for 09A and 13. Never force-push or merge main without authorization.

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
