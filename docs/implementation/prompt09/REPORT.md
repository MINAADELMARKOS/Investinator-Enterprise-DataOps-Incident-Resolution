# Prompt 09 — Reviewed operational knowledge

Functional gate: PASS. Independent human gold-set quality sign-off: BLOCKED/PENDING.
Implementation date: 2026-11-17. See [review worksheet](GOLD_REVIEW.md).

Versioned Java ingestion accepts runbooks, resolved postmortems, known errors,
topology snapshots, configuration changes and validated corrective actions.
Java-owned Flyway V5 preserves the existing Python schema and default Next.js
routing. Normalized, redacted sources retain owner/time/revision/tenant/ACL/hash;
deterministic overlapping chunks use tenant-specific identities.

BM25 on a fixed OpenSearch index is the retrieval baseline. Server-derived
incident/graph context filters tenant, principal, technology, version family and
KRaft/legacy mode. Bounded retrieval reranks specificity and deduplicates revisions.
Every hit and final citation is checked again against authoritative SQL and chunk
content. Historical advice is labeled separately and cannot alone establish a new
cause. Newest revocation suppresses older versions; policy changes invalidate
private model continuation. No vector/embedding cost is justified by these fixtures.

Writer and reviewer credentials are distinct. Ingestion starts pending review;
injection-like text is quarantined. Publication requires approval; native indexing
is idempotent and separate from SQL approval, exposing pending publication rather
than claiming availability. Readable source endpoints enforce current review and
ACL. Rejected feedback never promotes itself into knowledge. A validated action
requires reviewed successful feedback, a resolved incident and execution evidence.
No model tool writes, approves, executes or deploys anything.

## Verification

- 78 Java tests, zero failures/errors/skips; 14 CLI tests pass.
- Real PostgreSQL and secured OpenSearch 3.4 testcontainers verify migrations,
  tenant/ACL/version/mode filtering, native BM25, repeat publication, least-privilege
  reader/writer denial, replacement/revocation and readable citation membership.
- Negative tests cover injection quarantine, self-review, forged historical/live
  citation types, revoked cached citations, unsupported tenant arguments and
  rejected/unreviewed outcome promotion. Raw JSON credentials/private keys redact.
- Eight simulated cases cover all requested technologies and healthy false positives.
  [Provisional results](provisional-evaluation.json): precision@1 1.0 and causal
  top-1 8/8 against implementation-authored labels. **These are not human-reviewed
  accuracy measurements.** No reviewer or human approval is invented.
- Owned-lab [doctor](doctor.json), [API exercise](lab-result.json) and
  [smoke](smoke.txt) record actual Java/PostgreSQL/OpenSearch behavior with simulated
  incident/knowledge content. Simulated reviewer credentials test authorization
  mechanics; they do not provide independent domain review.
- Ruff, baseline route/schema/document validation and whitespace checks pass.
  Original Python/UI source and default routing remain preserved.

Failures found and corrected: OpenSearch fixture password policy, required bulk
shard permission for create operations, native HTTP 409 exception handling,
legacy KB-001 versus hashed knowledge ID classification, revoked-version fallback,
quoted configuration credential redaction, and verifier zip strictness. Checks
were rerun after fixes. Provider SDK fixtures are explicitly mocked; local
ai-check reports NOT_CONFIGURED and no actual paid OpenAI call was made.

## Remaining gates

Independent qualified human review of cases/sources/labels is required before
calling the evaluation a gold set. Production OIDC/group RBAC, index TLS/security,
retention/key rotation, private connector validation and real provider validation
remain separate gates. The existing critical Next.js advisory remains a release
blocker. No Prompt 10 work, push/merge, environment publication or production
infrastructure change was performed.
