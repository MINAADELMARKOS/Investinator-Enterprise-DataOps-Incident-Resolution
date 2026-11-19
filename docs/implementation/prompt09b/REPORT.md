# Prompt 09B — Independent knowledge review and GPT preflight acceptance

Functional evaluation/review-package gate: **PASS**. Human AI-quality approval:
**PENDING**. Actual OpenAI provider: **NOT_CONFIGURED**. Production release: **BLOCKED**.
No Prompt 10 implementation, deployment, production write or paid inference.

## Scope and provenance

Read remote main `ef0268b108b4c7a09c7d32ddc82f0a71712d5460` architecture v2 and
post-09 prompts, and remote feature state at `6e981b9fab5a2af7134c6aa5128bb626aec6470c`.
Local branch is feature/java-ai-audit; starting tree clean. Reports 03–09, original
Prompt 05 artifacts, graph/correlation, SDK loop/history and reviewed knowledge
sources were inspected. Production code, routes, private Flyway/Alembic ownership
and default Python UI are unchanged. No embeddings, training or vector retrieval.

## Required deliverables

- [QUALITY_REVIEW_PACKAGE.md](../prompt09/QUALITY_REVIEW_PACKAGE.md): eight proposed
  cases, exact evidence IDs/time ordering, typed graph/revision, scope/version/mode,
  originating and competing hypotheses, contradictions, blind spots, uncertainty,
  inspectable source text, immutable runbook revisions/chunk IDs/hashes and blank
  independent reviewer fields. Source references point to the actual fixture file.
- [EVAL_REPORT.json](../prompt09/EVAL_REPORT.json): numerator/denominator diagnostics;
  fixture, expert-reviewed and live transport observations are separate sections.
- [GPT_LIVE_REPORT.md](../prompt09/GPT_LIVE_REPORT.md): actual eligibility and precise
  protocol test matrix; no-key/no-budget is NOT_CONFIGURED, never live PASS.
- [PROMPT05_AUDIT.md](../prompt09/PROMPT05_AUDIT.md): all historical artifacts verified,
  eight-stop scope checked, current health/schema observed, old security finding reconciled.

[Exact input fixture](../../../eval/knowledge_review_cases.json) preserves the original
Prompt 09 dataset by SHA256; [raw observations](raw-observations.json) retain actual
native BM25/SQL outputs and corpus revisions. The scorer rejects hash drift, omitted
or duplicate cases, fabricated human approval, live/wrong-incident inputs, corrupt
corpus identities and confirmed-cause assertions. Citation/ACL/staleness or missing
safety diagnostics fail its operational CLI gate. It does not convert a fixture
ranking score into independent quality approval.

## Measured results — proposed labels only

| Metric | Numerator / denominator | Meaning |
|---|---|---|
| Detection precision / recall | 7/7 each | Nonempty deterministic correlation candidates; not autonomous monitoring |
| Upstream top-1 / top-3 | 7/7 each | Fault cases only, excludes healthy case |
| Healthy false-positive rate | 0/1 | One healthy fixture; no production rate estimate |
| Retrieval precision@1 / recall@1 | 8/8 each | One proposed relevant source/case |
| Retrieval precision@3 / recall@3 | 8/24 / 8/8 | Precision includes unfilled rank slots |
| Retrieval precision@5 / recall@5 | 8/40 / 8/8 | Same denominator convention |
| Returned-result precision | 8/9 | One extra healthy-baseline runbook matched the KRaft query |
| Evidence/citation membership | 70/70 | Correlation reference occurrences plus readable historical chunks, source/time/scope checked |
| Unauthorized knowledge | 0/9 returned | Foreign tenant/private ACL distractors indexed but excluded |
| Stale/incompatible knowledge | 0/9 returned | Old revision and wrong-version distractors excluded |
| Missing-data abstention | 2/2 | Empty observations and downstream-only evidence |
| Injection quarantine/approval denial | 2/2 | Untrusted instructions cannot promote themselves |

All eight retrievals correctly report DEGRADED/partial after SQL discards stale
index revisions (one or two rejected records/query). These are actual recorded
statuses, not silently healthy results. Stale projection accumulation and fixed
30-hit candidate bounds can starve approved chunks at larger scale; reviewed
retention/reindex and load validation remain gaps. The current fixture still
retrieves all proposed sources; no relevance or confidence weights were tuned.
The generic UNKNOWN-mode warning mentions Kafka even for non-Kafka fixtures;
domain-specific uncertainty wording remains a presentation gap.

No independent human or held-out evaluation exists. Corpus/query overlap is
intentional and optimistic. Seven faults plus one healthy case are far too small
to infer production accuracy, calibrated confidence or business value. Every
cause remains HYPOTHESIS; confirmed_cause is null. Expert metrics stay null.
The data do not justify adding embeddings or claiming vector/hybrid gains.

## Tests and commands

- `./investinator test`: 81 Java tests, zero failure/error/skip; 22 CLI tests at
  the initial full-run checkpoint. New tests cover multi-call pairing and bounded
  eight-second SDK timeout/no retry, plus native reviewed RAG/evaluation capture.
- After changing only fixture source references to a real tracked file, focused
  `KnowledgeIntegrationTest`: 5/5 pass on actual PostgreSQL/OpenSearch. No production
  Java source or schema was changed. Existing mocked provider and real Kafka read
  integration cases passed in the full suite.
- Final `python -m unittest discover -s tests_cli`: 24/24 pass, including ten scorer
  corruption/denominator/isolation tests. Ruff passes on source/tests/new scripts.
- Python pytest 12/12, mypy 17 files, original seven fixture evaluations pass.
- Current doctor and health exit 0, original JSON schema validates. ai-check exits
  2 / NOT_CONFIGURED. Fresh npm audit exits 0 / zero vulnerabilities; package+lock
  exactly match patched main. Baseline contract/document validation and diff checks pass.
- [test-summary.json](test-summary.json) records exact commands/provenance.

Reproduce the evaluation after Maven verify:

```bash
python3 scripts/evaluate_knowledge_quality.py \
  --review-package docs/implementation/prompt09/QUALITY_REVIEW_PACKAGE.md
```

For a no-Docker review replay, use the checked-in raw artifact:

```bash
python3 scripts/evaluate_knowledge_quality.py \
  --raw docs/implementation/prompt09b/raw-observations.json \
  --output /tmp/investinator-quality-replay.json
```

CI is extended to run the scorer after Java verify and lint the new script; CLI
unit discovery already includes scorer tests. These 09B changes are committed
locally only; new hosted CI is NOT_RUN. Previous 09A hosted success is not evidence
that hosted CI ran 09B. Existing GitHub PR creation/API access remains blocked by
the environment's api.github.com destination policy.

Failures/corrections: initial historical audit incorrectly treated intentional
Java-process stop as an external dependency; corrected to explicit per-service
liveness semantics without editing artifacts. The venv lacked jsonschema; the
installed system validator passes. Final quality sources use real fixture file
references, not nonexistent runbook filenames. No failed acceptance assertion was
waived; no scorer or correlation thresholds were fitted to improve results.

## Remaining blockers and Prompt 10 readiness

**READY for explicitly authorized, feature-flagged, read-only Prompt 10 development**
under v2: diagnostics/evidence/correlation/retrieval and bounded provider fallback
are operational; missing provider or expert review must never create GPT floods,
claims of causation, automatic external notifications or changes. Prompt 10 is
NOT_STARTED and this report does not authorize it.

Human sign-off and a held-out dataset remain required for AI-quality claims.
Actual key/model, approved budget and live full-loop validation remain absent.
Production OIDC/RBAC/TLS, private connector certification, retention/key rotation,
security/DR and default-route cutover approval remain deployment blockers. 09A's
PR access issue is still an administrative follow-up. Main's new Python chat UI
uses openai/tool_calls whereas Java uses gpt/tool_trace; provider-backed Java UI
alignment remains Prompt 12 work. Current npm audit clears the old Next.js issue.

Rollback is a normal revert of this stage's evaluation/test/documentation commit;
there is no database migration, data deletion, route cutover or production action
in this stage. Keep original 03–09 reports and pending-review records.
