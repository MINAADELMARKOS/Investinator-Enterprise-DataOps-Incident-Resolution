# Knowledge API and operating contract

All routes are under `/api/v1/knowledge`; tenant/actor identities are server bound.
Demo capability credentials are not production identity. Non-demo exposure remains
fail-closed until separately implemented enterprise authentication is available.

| Operation | Route | Credential |
|---|---|---|
| Stage source | POST `/revisions` | KNOWLEDGE_WRITE_TOKEN |
| Inspect draft | GET `/revisions/{id}/{revision}` | writer or reviewer |
| Approve/reject/revoke | POST `/revisions/{id}/{revision}/review` | KNOWLEDGE_REVIEW_TOKEN |
| Publish approved chunks | POST `/revisions/{id}/{revision}/publish` | KNOWLEDGE_WRITE_TOKEN |
| Scoped BM25 | GET `/search?q=...&incident_id=...` | KNOWLEDGE_READ_TOKEN |
| Current readable source | GET `/documents/{id}/{revision}` | KNOWLEDGE_READ_TOKEN |
| Submit outcome | POST `/feedback` | KNOWLEDGE_FEEDBACK_TOKEN |
| Read outcomes | GET `/feedback/{incident}` | KNOWLEDGE_READ_TOKEN |
| Review outcome | POST `/feedback/{id}/review` | KNOWLEDGE_REVIEW_TOKEN |

Writer and feedback tokens must differ from reviewer. Credentials never appear in
model context. Corpus revision/hash is immutable; review/publish are explicit
transitions. Quarantined sources cannot be approved. Revocation leaves immutable
index documents present but SQL rejects them, including an older revision when
the newest approved revision was revoked. A new reviewed revision is required.

Sources are at most 48k characters, chunks 2k with 200-character overlap, at most
32 chunks. Search is plain text at most 200 characters, 30 index hits, at most
five deduplicated sources, a two-second native query timeout. Unavailable or stale
results report partial/unavailable explicitly; there is no fabricated fallback.

The writer can create on `investinator-knowledge` but cannot search/delete/change
settings. The reader cannot write. `scripts/knowledge_bootstrap.py` establishes
fixed schema/roles only against owned loopback lab infrastructure during explicit
`./investinator up`; the application does not create infrastructure or mappings.
Private generated tokens remain under ignored `.runtime/`.

Model capabilities add only `search_knowledge` and `get_incident_feedback`.
Retrieved text is untrusted historical advice. Tenant/principal/incident cannot
be supplied by the model. Final references require current SQL approval, ACL,
platform/mode compatibility and exact chunk/source/time membership. Legacy seeded
runbook IDs remain compatible. Rejected claims and feedback remain user reports,
never automatically authoritative causes, training data or execution approval.

Rollback: keep Python/default routing; disable Java knowledge consumers. V5 is
additive to the private Java schema; do not drop tables/indexes or erase reviews
as rollback. No existing data was migrated or deleted.
