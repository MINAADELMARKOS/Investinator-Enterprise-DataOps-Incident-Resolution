# InvestiNator — executive product brief

**Decision:** Continue controlled laboratory/staging development; production release
is **BLOCKED**. The platform can demonstrate an evidence-led incident workflow with
real local protocols and clearly simulated faults. It is not a certified autonomous
remediation product, and no production accuracy or savings have been established.

Distributed-system incidents are expensive to investigate because operators must
connect a visible symptom—Kafka lag or stale datasets—to earlier network, controller,
storage and pipeline observations. Symptoms can be downstream effects. Prematurely
restarting the component that complained can increase risk without fixing the origin.

InvestiNator brings scoped logs/metrics, a versioned dependency graph, event-time
correlation, competing hypotheses and retrievable historical guidance into one
operator workspace. Its optional OpenAI SDK loop selects bounded read tools and
returns inspectable citations. Approval and independent verification are separate
from model output. Current execution changes only an isolated simulator. This
separation is a concrete safety benefit, not a claim that an AI diagnosis is correct.

Verified engineering evidence includes Java/Python compatibility, bounded incident
deduplication, actual local Kafka/OpenSearch/PostgreSQL transport, scoped/negative
security tests, durable restart/receipt behavior, Java-backed Next workflows, and
hosted end-to-end checks. A 38-table/781-row local restore and small serial HTTP
latency sample exist. They do not establish production RTO/RPO or capacity. Provider
credentials/testing budget and independent human gold-set sign-off remain absent.

Cost drivers are deployment/retention/HA, indexing and observability volume,
connector identity/certification and maintenance, model requests/tokens, incident
review, security and ongoing operations. No provider price, run rate, avoided loss,
customer count or ROI is assumed. Per-process technical limits are not durable
organization-level financial controls. ChatGPT subscriptions do not fund API traffic.

Build-versus-buy should compare this transparent, customizable evidence/policy model
with managed observability and incident products. Building provides ownership of
schema/workflows and integration semantics but also creates long-term operational,
security, connector and evaluation obligations. Vendor-managed capabilities may
reduce that burden; neither option removes the need for reviewed evidence, identity
and change control. No completed vendor benchmark is claimed.

Phased roadmap: (1) independent held-out incident/knowledge review and measured
operator study; (2) real staging principals, connector TLS/Kerberos/rotation and
tenant certification; (3) OIDC/RBAC, immutable audit/retention, durable cost controls,
encrypted offsite/PITR/HA and sustained failure/load tests; (4) only after independently
verified gates and explicit user approval, an optional cutover. No automatic rollout.

Measure impact with comparable cohorts and raw timestamps: time to acknowledge and
accepted cause, independently verified recovery, false positives/missed incidents,
citation support/abstention, actual model/infrastructure cost and change failures.
Use blinded expert review and a held-out set; report sample size and uncertainty.
Potential annual value = independently verified hours saved × approved loaded labor
rate, less measured incremental costs. All inputs remain unmeasured until that study;
fixture compression, model fluency and simulator recovery are not financial return.
