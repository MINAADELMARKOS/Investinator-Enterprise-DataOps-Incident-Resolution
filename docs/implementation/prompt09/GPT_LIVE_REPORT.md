# Prompt 09B — GPT live preflight and protocol acceptance

Actual OpenAI status: **NOT_CONFIGURED**. Paid requests made: **0**.
Human evaluation approval: **PENDING**, not inferred from any provider test.

On this environment OPENAI_API_KEY, INVESTINATOR_OPENAI_MODEL and enabled-provider
settings are absent. No explicit testing budget/approval is configured. Credential
validity and live endpoint/model availability are NOT_TESTED; configuration absence
is not a successful provider test. `./investinator ai-check` returned exit 2 and
[the exact non-paid presence result](../prompt09b/provider-preflight.json).
No `--live` command was invoked, and no incident content was sent externally.

## Protocol and integration evidence

| Requirement | Actual verification | Provenance |
|---|---|---|
| Two user turns/history reload | ChatPersistenceTest.officialSdkBeanPersistsRedactedHistoryEncryptedContinuationAndIdempotentFollowups | Official SDK, WireMock provider, real PostgreSQL; MOCKED inference |
| Scoped read tool | ChatPersistenceTest.functionLoopExecutesActualReadOnlyKafkaAdminAgainstRealKraftBroker | Real local Kafka KRaft metadata; provider MOCKED, server-bound demo tenant |
| Authentication / tenant rejection | ConnectorApiSecurityTest plus ChatPersistenceTest.toolsRejectCrossTenantArgumentsNoIncidentAndUnsupportedApproval | Missing/wrong bearer and tenant arguments denied; not enterprise OIDC |
| Complete continuation/reasoning | ChatGatewayTest.actualSdkLoopReturnsToolResultByCallIdAndCarriesAllReasoningItems | MOCKED provider protocol |
| Multi-call batch | ChatGatewayTest.multiCallBatchPreservesEveryCallIdAndReturnsBothReadResults | Two distinct call IDs and matching outputs; MOCKED provider |
| Invalid citation | ChatGatewayTest.nonexistentOrWrongTimestampCitationAndFakeLiveModeFail; KnowledgeIntegrationTest.historicalCitationsAreRecheckedAfterAclOrReviewChangesAndCannotBecomeFreshObservations | Invented/source-mode/revoked membership denied |
| Provider outage/auth/rate/model errors | ChatGatewayTest.providerFailuresHaveSafeDistinctStatus | MOCKED 503/401/429/404/400, sanitized bounded fallback |
| Timeout | ChatGatewayTest.providerTimeoutIsBoundedHasNoRetryAndNeverReturnsFabricatedModelSuccess | Real eight-second SDK timeout against delayed WireMock; one request, no retry, PROVIDER_UNAVAILABLE fallback |
| Missing configuration | MissingProviderConfigurationTest, CLI and persistence fallback coverage | Core starts without client; no fabricated GPT reply |
| Prompt injection / fake approval | ChatGatewayTest.unknownToolIsDeniedAndFakeApprovalInFinalOutputIsRejected | Unknown tool never executes; POLICY_VIOLATION fallback |

None of these mocked provider results constitutes a live OpenAI tool-loop pass.
`paid_call_made:true` printed by one CLI unit test is a stubbed subprocess result;
it is not actual traffic. Live-provider coverage for every row remains NOT_TESTED.

## Future opt-in live session prerequisites

Use backend secret storage for a valid key and an explicitly configured authorized
model; do not guess IDs. Record a human-approved request/token/cost ceiling and
allowed synthetic input, with a budget owner and expiry, before sending requests.
First verify egress/TLS/authorization without disabling verification. Enable only
scoped read capabilities; never add approval, execution, shell, SQL or arbitrary URL
tools. Production ingress remains denied without real identity.

Current Java bounds: three requests/turn by default (hard cap ten), six tool calls,
20-second turn, eight seconds per SDK request, zero transport retries, 24 KiB input
and 1024 output tokens/request by default (hard cap 2048), two active turns,
20 turns/minute/tenant and circuit cooldown after repeated failures. Encrypted
private history uses AES-GCM with tenant/conversation binding; store=false and
reasoning.encrypted_content are explicit. Configured optional model IDs route tasks.

The existing `./investinator ai-check --live` is only a single capped 64-output-token
presence probe with one/minute rate limit; it does not by itself validate the full
multi-turn/tool matrix. A future approved live matrix must record actual call IDs,
redacted results, measured usage/cost, matched tool outputs and replay/tenant/error
cases. Do not claim that it has run here or auto-run it on a later environment
merely because a key appears. No new production/live execution capability is added.
