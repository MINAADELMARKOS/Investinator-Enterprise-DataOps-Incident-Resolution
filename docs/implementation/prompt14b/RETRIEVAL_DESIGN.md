# Current-revision retrieval, no destructive repair

Baseline at 1ca09a1 reproduced 8/8 partial/degraded queries, each rejecting an
obsolete projection. SQL remains authoritative; OpenSearch is a projection.

The search path now materializes at most 129 current, compatible, principal-readable
SQL revision headers, with a 1.5-second local statement timeout. Only the first
128 contribute at most 4096 deterministic chunk IDs to the BM25 filter. Latest
APPROVED or REVOKED revision invalidates older approved projections, including
unpublished replacements and ACL removals. Candidate limits, unpublished current
revisions, timeout/shard/termination/candidate-hit limits, post-query rejects and
policy-epoch changes are not hidden. An epoch change clears results. SQL failures
fail the request rather than returning a fabricated healthy corpus.

Post-search canonical text/hash/ACL/version/mode checks and citation-time checks
remain. Sort ties use chunk ID. No approval/source revision, index record, volume
or live knowledge is deleted. Existing explicit publish is repeatable for an
approved immutable revision; conflicting projected content still requires reviewed
repair. No unreviewed index rewrite, retention policy or automatic purging is added.

This bounds per-request materialization, not lifetime storage: historical projection
retention and a source-table execution plan at enterprise scale still need review.
The candidate bound can omit relevant documents at high cardinality; the response
then explicitly says partial/candidate_budget_exhausted, never complete recall.
A SQL/index race or misconfigured/missing projected record can require operator
republish/rebuild; no zero-hit response proves corpus completeness independently.
The healthy-context extra hit remains reported; no score or fixture labels are tuned.

Rollback: revert the retrieval commit; retained source/index data remain intact.
Do not drop schemas or remove approved source records. Production quality and
independent human signoff stay BLOCKED/PENDING.
