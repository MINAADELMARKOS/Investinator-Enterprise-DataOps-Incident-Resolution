# Data-use enforcement scaffold and owner inventory

Java real-provider traffic additionally requires deployment-owned MODEL_TRANSFER_APPROVED
and MODEL_DATA_CLASS=SYNTHETIC or PUBLIC_LICENSED. Missing approval, UNKNOWN/protected/
unlicensed classes and LIVE incident context are denied before provider I/O. Loopback
protocol fixtures remain mock-only. These are administrator configuration claims,
not legal signatures, DLP or proof that user text is synthetic/licensed. Model/tool
answers cannot change them. The preserved Python provider path has no shared Java
ledger/policy integration and MUST NOT be enabled under this Java certificate.

DataUsePolicy also denies every deletion request, including claimed approved
retention; legal hold has an explicit denial reason. There is no purge executor.
This policy function is scaffolding; no deletion API or background retention job
exists. No existing knowledge, logs, incident history or backups are removed.

| Data class | Current handling | Required accountable owner evidence |
|---|---|---|
| Synthetic incident/graph/ReplayLab events | SIMULATED labels, tenant binding, bounded excerpts | SRE fixture provenance; no production claim |
| Normalized logs/metrics/traces | Private scopes, redaction, source/time pointers | Security/privacy classification and collection consent |
| Runbooks and historical citations | Version/hash/ACL/revocation and review | Legal/source owner license and model-transfer permission |
| User prompts and provider continuation | Redaction, private encrypted continuation, bounded transfer | Privacy assessment, provider terms/region/retention approval |
| Conversations/feedback/outcomes | Tenant-scoped storage, reviewed promotion | Retention minima, erasure workflow and hold precedence |
| Approvals/receipts/audit | Private metadata export, no external sink | Change-control owner, separately certified immutable audit |
| Database backups/WAL/index projections | Local fixtures only; no source deletion | SRE/legal encrypted offsite retention, erasure propagation, hold |

Retention minimums are UNAPPROVED, not guessed. Future deletion must enumerate source,
projection, citation, continuation, WAL/backup copies, hold/review and immutable audit
obligations, with dry-run inventory and independently authorized reversible handling.
Do not infer retention authority from a model response, runbook, demo role or policy
boolean. P14-07 remains BLOCKED pending privacy/security/legal owner review.
