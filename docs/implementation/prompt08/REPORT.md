# Prompt 08 — Official Java Responses integration

The existing investigator message contract now delegates to a Java service using
the pinned official com.openai:openai-java SDK and a Spring-managed OpenAIClient.
Original conversation_id/incident_id/answer/evidence_ids remain; additive fields
response_mode, provider_status, source_mode, analysis and tool_trace are explicit.
Default unconfigured replies retain deterministic legacy behavior, never claim GPT.
Original audit payload compatibility remains; actual provider turns have a
separate audit event. Provider request count and token usage are recorded.

Each turn sends the actual redacted question and persisted history. All allowed
Responses output items (messages, function calls, reasoning including encrypted
content) are carried into subsequent stateless requests, along with matching
function_call_output IDs. store=false is explicit and reasoning.encrypted_content
is requested. Private continuation state is AES-256-GCM encrypted in a separate
tenant/conversation-bound table; no reasoning is placed in public messages or logs.
Unknown output kinds are rejected. Permission epoch changes invalidate private
continuation. An incident cannot silently change within an existing conversation.

Named tools perform fixed reads of incident evidence, bound graph/correlation,
explicitly allowed connector health and tenant+incident-filtered search. Native
health uses the bounded probe runner for only the authorized service. No SQL,
URL-fetch, shell, restart, approval or remediation tool exists. Unknown tools and
extra/duplicate/invalid arguments are denied. Captured read evidence remains bound
to tenant/incident with source/time. Every returned structured citation is checked
against authorized stored records, exact source and timestamp; invented citations,
false LIVE labels and fake approval/execution claims are rejected. Historical
advice is distinct from observations. Mixed simulated/live diagnostic data is MIXED,
never silently upgraded to production evidence.

Limits: two chat admissions before taking DB connections; two gateway turns;
20 turns/minute/tenant by default; three provider requests/turn, six tool calls,
20-second turn budget, eight-second SDK requests with no SDK retries; 24 KiB input
transfer bound and 1024 output tokens/request by default (bounded configuration).
Three failures open a 30-second circuit. Budgets, saturation and provider errors
are explicit fallbacks; BAD_KEY, RATE_LIMITED, MODEL_NOT_FOUND,
MODEL_OR_REQUEST_UNSUPPORTED, PROVIDER_UNAVAILABLE and invalid output are separate.
Models come only from backend configuration; optional fast/reasoning IDs route
ask/P1 tasks. No model ID is guessed or embedded in the browser.

Configuration: INVESTINATOR_OPENAI_ENABLED=true, INVESTINATOR_OPENAI_MODEL,
OPENAI_API_KEY and a 32-byte Base64 INVESTINATOR_CHAT_STATE_KEY in backend secret
storage. The local CLI generates the state key privately. Optional
INVESTINATOR_OPENAI_FAST_MODEL / INVESTINATOR_OPENAI_REASONING_MODEL and
INVESTINATOR_AI_ALLOWED_SERVICES grant routing/read scopes. Missing optional
key/model/state key reports NOT_CONFIGURED without breaking core startup. The
provider endpoint is HTTPS on the configured allowlist; loopback HTTP is allowed
only with the explicit test-endpoint setting. JVM CA verification stays enabled;
backend HTTPS_PROXY is supported without echoing values or copying credentials.

Tests use WireMock for provider protocol and negative cases, actual PostgreSQL for
history/idempotency/scope/encryption, and actual Kafka 4.3.1 KRaft for the complete
SDK -> function -> Java connector -> broker -> matching tool result continuation.
That is real read-tool proof with a mocked provider, not a paid OpenAI proof.
Reasoning continuation, actual question, follow-up history, forged approval,
nonexistent citation, provider failures, unknown tool, prompt-injected log,
no selected incident and cross-tenant access are tested. The existing 74-step
Python parity trace still checks legacy fields; only explicitly additive chat
response metadata is projected away, with new API tests covering it.

./investinator ai-check makes no request. Only explicit ai-check --live invokes a
single official Java Responses request, 64 output tokens, store=false, no retries,
and a one-request/minute local rate limit. It returns actual provider text when
successful; provider errors are redacted. No incident/log data is sent by the probe.
No live provider call was made during this stage. On this host key/model are absent:
ai-check returns exit 2 / NOT_CONFIGURED. Live paid-provider proof remains pending;
this is the specification's explicit no-key case, not an end-to-end live claim.
Production identity, private cluster validation and critical Next.js release gates
remain open. No push, merge, publication or production infrastructure action.

Final acceptance: 69 Java tests + 14 CLI tests PASS; no failures/errors/skips.
CLI lint, baseline/source validator and diff checks PASS. Compile test import
overloads and non-AutoCloseable SDK lifecycle handling were corrected before the
final run. Structured redaction preserves JSON rather than applying regexes across
serialized structures. No paid call, push/merge, publication or production change.
