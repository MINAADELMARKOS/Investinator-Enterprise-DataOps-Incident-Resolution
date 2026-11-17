# Gaps and readiness

## Current assessment — Prompt 09B

Functional evaluation harness PASS: inspect [review package](prompt09/QUALITY_REVIEW_PACKAGE.md),
[diagnostics](prompt09/EVAL_REPORT.json) and [acceptance](prompt09b/REPORT.md).
Expert review/held-out data absent, live GPT key/model/budget absent; no production
accuracy claim. Native BM25 returns one extra irrelevant source (precision 8/9);
all eight queries are degraded/partial after stale-revision SQL rejection. Fixed
30-hit retrieval plus stale projection accumulation needs reviewed maintenance/load
coverage. UNKNOWN metadata-mode warnings are Kafka-specific even on non-Kafka
fixtures. No embeddings or threshold tuning. Current Next.js audit clean.
09B local only: new hosted CI NOT_RUN; 09A PR/API access issue remains. Prompt 10
is ready for an explicit read-only/flagged development request, not started here.

## Previous synchronized assessment — Prompt 09A

Local Java 00–09 is preserved and passes 78 Java/14 CLI tests. Remote-main Python
GPT/search and Next.js 16.4.5 are reconciled; Python 12 tests and frontend
install/build/types/audit pass, zero npm vulnerabilities. Java topology, bounded
SDK loop, BM25/review/feedback and CLI are implemented. See [sync report](SYNC_REPORT.md).
Remote feature branch is pushed and SHA verified; Java/backend/CLI/frontend hosted
jobs pass (run 44102365986 for 08cc387). PR creation/authenticated logs are blocked by API
destination policy; private enterprise services/provider remain
NOT_CONFIGURED. Independent human review is pending; no measured RCA quality claim.
Production auth/cutover/security gates remain; Prompt 09B/10 have not started.
New Python chat metadata (`openai`, `tool_calls`) differs from Java (`gpt`,
`tool_trace`). Default Python UI is intact; opt-in Java provider-label/trace
rendering needs the explicitly scoped Prompt 12 adapter, not an implicit cutover.

## Historical audit and stage-by-stage gaps (before 09A)


| Capability | Status | Required next evidence |
|---|---|---|
| API, durable records, UI, audit | implemented; SQLite tests pass | snapshot/differential Java contracts |
| GPT structured investigation | partial on main | question/history/tool loop migration |
| GPT function calling and log search | partial, PR #3 only | safety fixes and live opt-in evidence |
| Detection and incident correlation | implemented deterministic; seeded graph | causal counterexamples, skew/freshness, dedup/alert budgets |
| Replay recovery | SIMULATED | preserve approval and independent metric verification |
| Real broker workload | code implemented; NOT_TESTED this stage | live broker smoke and semantic probes |
| Search / RAG | Prompt 04 log collection/search gate passes locally; RAG not implemented | enterprise ingestion, retrieval evaluation and knowledge stage |
| Multi-system connectors / discovery | not implemented | least-privilege endpoints + integration fixtures |
| Enterprise authorization | not implemented | OIDC/RBAC, per-resource identity, independent approval |
| Production execution | disabled / absent | separately authorized controlled change system |
| Java backend / safe CLI | Prompt 03 persistence/workflows and Prompt 04 evidence implemented; universal CLI planned | reports 03/04; Prompt 05 CLI gate |
| Dependency security | release BLOCKED | patched locked Next.js, rebuild/regression/audit |

Prompt 01 can complete as an audit with documented failures; it does not certify
application or production readiness. Prompt 02 is now implemented as a health-only scaffold; see prompt02/REPORT.md. No critical
finding is waived. Obtain a separate security fix task before release and do not
claim Java dependency compatibility until measured. Missing live credentials do not
block fixture-based development, but prevent live integration readiness claims.

Prompts 03/04 now have local acceptance evidence. Java demo restart durability,
74 Python differential requests and real OpenSearch/collector/Prometheus forwarding
are verified. No live provider, existing-data cutover, enterprise deployment or
production writes are certified. Default Python/Next.js behavior is preserved.
