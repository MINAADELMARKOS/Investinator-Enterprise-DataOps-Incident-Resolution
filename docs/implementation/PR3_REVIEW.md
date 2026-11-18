# PR #3 review

Reviewed exact fetched head 67a7328022fa8e918633b7a62bcaf7f48c67f051 against
main 920fefe1b86e55f038fb0a7e56dd0b42e9d34118. Open at audit time (GitHub HTML).
13 files, 889 insertions, 42 deletions. Read chat_runtime.py, log_search.py,
tools/api/services/UI/Compose diffs and test_live_chat.py. Archive tests: 12 passed.

## Retain concepts, refactor implementation

PR adds genuine Responses create calls, named function definitions, JSON argument
validation, matching call_id tool results, response output continuation including
encrypted reasoning, bounded history, usage and explicit fallback. It uses the actual
question and supports no selected incident. Log search fixes destination/index in
server settings, bounds time/results and enforces tenant filter. Retrieval can be
LIVE while indexed fault data is SIMULATED; this distinction is valuable.
Port these behaviors and tests through official Java SDK interfaces, not Python code
or guessed API methods. Do not blindly cherry-pick. Keep response additions compatible.

## Findings to resolve before port acceptance

1. Citation checking uses a narrow regex for ES-/METRIC-/LOG-/KB- patterns. Stored
   metrics/knowledge IDs may be UUIDs and invented citations outside the regex escape
   validation. Replace with structured citation fields and tenant/incident membership.
2. Round limits are not a total wall-clock/token budget: max_ai_calls + 1 provider
   requests, up to six tools each, provider retries/timeouts can exceed web proxy's
   20-second timeout. Add cancellation, total deadline, token and tenant cost budgets.
3. Tools use strict=false and Pydantic default extra-field handling. Require strict
   argument schemas, authorization per tool and explicit unknown-field rejection.
4. Invalid evidence / empty response can still have response_mode=openai. Distinguish
   provider invoked from accepted investigation; never label validation failure as success.
5. Elasticsearch/OpenSearch generic HTTP compatibility is not proven for both products;
   no real ingestion/query integration test. Source mode defaults to external, not
   authenticated provenance. Verify mapping, index ACLs and source freshness.
6. Tests cover fake multi-round tools, history, unconfigured fallback, tenant query filter,
   simulated provenance and upstream seed graph. Missing negative coverage includes
   malicious approval, prompt injection, unknown tool, bad citation, provider failure,
   cross-tenant conversations and encrypted reasoning continuation.

The seed topology and synthetic search seeder are not discovery or real root-cause
validation. Demo identity remains unsuitable for enterprise access. Production execution
is absent and must stay separate from the model tool registry.

Additional PR #3 static checks: ruff PASS (exit 0), mypy PASS (17 files, exit 0), using the isolated fetched source archive.
