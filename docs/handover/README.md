# InvestiNator handover — Architecture v2 / Prompt 15

This package documents the checked-in local platform, not a production deployment
or independently certified model. Read in this order:

1. [Executive brief](EXECUTIVE_BRIEF.md): value, costs, decision and measurement plan.
2. [As-Is/To-Be and component build status](STATUS_MATRIX.md): implemented versus promised.
3. [Architecture views](ARCHITECTURE.md): context, containers, components, deployment,
   sequences and ownership; [generated source inventory](source-inventory.json).
4. [Developer experience](DEVELOPMENT.md), [configuration/provider guide](CONFIGURATION.md)
   and [operations/troubleshooting](OPERATIONS.md).
5. [Presentation script](DEMO.md): reproducible KRaft investigation plus separately
   scoped disk simulation; fallback and conditional real-provider variants.
6. [Production gates](../implementation/prompt14/RELEASE_RISKS.json) and
   [Prompt 15 acceptance](../implementation/prompt15/REPORT.md).

Provenance vocabulary: VERIFIED_REMOTE = actual published code/CI; LIVE_LOCAL =
actual local protocol/HTTP/JDBC traffic; SIMULATED = workload/fault/effect;
MOCKED = fixture response; NOT_CONFIGURED = required configuration absent;
NOT_TESTED = no applicable execution evidence; BLOCKED = unmet gate. Build status
and provenance are separate columns. An implemented adapter may still be unconfigured
and uncertified. Correlation, demo scores and model fluency are not causal proof.

Historical implementation reports retain failures and older versions. Current
handover/status and STATE supersede their pending statements. No human sign-off,
production accuracy, financial savings or independent approval is manufactured.
