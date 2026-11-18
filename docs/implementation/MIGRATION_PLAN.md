# Stage dependencies, acceptance and rollback

No stage after 01 is executed in this task. Complete each gate before moving on.
Health and safe operational tooling are the first operational priority after the
required scaffold, contract and evidence prerequisites. Keep scope small and local.

| Stage | Depends on / deliverable | Required gate | Rollback point |
|---|---|---|---|
| 00 | repository and PR inspection; rules/state | branch, rules and state versioned | documentation-only commit |
| 01 | 00; inventory, snapshots, ADRs/business case | grounded baseline, reviewable decisions, failures named | documentation/artifact commit |
| Security fix | separately authorized dependency maintenance | patched Next lock, build, UI regressions, audit | prior lock only in isolated dev; never release vulnerable build |
| 02 | 01; Java21 modular monolith + Maven Wrapper | official compatible pins, verify, Actuator + container, Python regression | no traffic routed to Java |
| 03 | 02; persistence/API parity | fresh DB migration, tenant/approval/audit tests, differential responses | Python route ownership; single schema owner |
| 04 | 03; normalized evidence + search ingestion | synthetic event through actual collector/index, tenant and unavailable tests | disable new ingest profile, preserve existing data |
| 05 | 04; health engine + safe Bash/PS CLI | up/doctor/smoke, reruns, stop-each-dependency negative matrix | stop local new services, never delete volumes |
| 06 | 05; read-only versioned connectors | auth/timeout/capability tests per configured adapter | disable individual connector |
| 07 | 06; graph and causal correlation | KRaft and legacy fixtures, counterexamples | retain previous deterministic hypotheses |
| 08 | 07; official SDK tool loop | multi-turn/negative/history/citation tests; opt-in live when configured | explicit deterministic fallback, retain Python |
| 09 | 08; BM25 knowledge and feedback | tenant/version ACLs, injection/citation tests | disable new retrieval source |
| 10 | 09; continuous incident lifecycle | dedup/cooldown/failure/cost bounds | disable scheduler, preserve manual investigations |
| 11 | 10; deterministic policy + simulation executor | expiry/tamper/repeat/cross-tenant/verify tests | disable executor; production writes remain disabled |
| 12 | 11; Java-backed existing Next.js | UI/proxy/fallback/evidence flows + build/audit | restore route config to Python |
| 13 | 12; repeatable Compose E2E lab | new clone, failure matrix, CI artifacts | isolated test stack only |
| 14 | 13; production security/operability review | identity, data handling, HA/DR, load, dependency gates | no production deployment absent signoff |
| 15 | 14; handover and measured impact | reproducible demo and factual status | no main merge without user approval |

For every endpoint: capture Python success/error fixtures, add Java contract tests,
compare seeded outputs accounting explicitly for IDs/timestamps, shadow reads only
where safe, then route deliberately. Record exact owner and rollback before switching.
For writes: prove transactional/idempotent semantics, negative tenant/approval tests,
forward/backward schema compatibility and backup/restore. No automatic schema rollback
against real data. No concurrent Alembic/Flyway ownership or approval by GPT text.

Implementation readiness: ready for review of scaffold design; not Java runtime-ready,
not live-integration-ready, not release-ready. Spring Boot/SDK/search pins still require
official verification in 02. Release remains blocked by R1; no next prompt is assumed.
