# Gaps and readiness

| Capability | Status | Required next evidence |
|---|---|---|
| API, durable records, UI, audit | implemented; SQLite tests pass | snapshot/differential Java contracts |
| GPT structured investigation | partial on main | question/history/tool loop migration |
| GPT function calling and log search | partial, PR #3 only | safety fixes and live opt-in evidence |
| Detection and incident correlation | implemented deterministic; seeded graph | causal counterexamples, skew/freshness, dedup/alert budgets |
| Replay recovery | SIMULATED | preserve approval and independent metric verification |
| Real broker workload | code implemented; NOT_TESTED this stage | live broker smoke and semantic probes |
| Search / RAG | absent main; PR HTTP search | collection pipeline, BM25, tenant ACLs, ingestion proof |
| Multi-system connectors / discovery | not implemented | least-privilege endpoints + integration fixtures |
| Enterprise authorization | not implemented | OIDC/RBAC, per-resource identity, independent approval |
| Production execution | disabled / absent | separately authorized controlled change system |
| Java backend / safe CLI | planned | Prompts 02 and 05 gates |
| Dependency security | release BLOCKED | patched locked Next.js, rebuild/regression/audit |

Prompt 01 can complete as an audit with documented failures; it does not certify
application or production readiness. Prompt 02 has not been started. No critical
finding is waived. Obtain a separate security fix task before release and do not
claim Java dependency compatibility until measured. Missing live credentials do not
block fixture-based development, but prevent live integration readiness claims.
