# Durable provider budget — development control, not live spend approval

Flyway V9 adds three tables only in the private Java schema: USD micro-unit global
and tenant/model accounts, expiring approved model prices, and immutable-reservation
receipts. It seeds NO quota, price, approval or credentials. Alembic/Python ownership
is untouched. It also indexes tenant/plan audit export metadata.

Real backend requests require key/model/provider enablement plus deployment-owned
PAID_OPT_IN, COST_APPROVED and nonblank APPROVAL_REF configuration. These flags are
configuration claims, NOT proof of independent human approval. Separate owner review
and approved DB policy provisioning are mandatory before actual enablement. No policy
write API exists; retrieved text, tenant request bodies and models cannot set them.
Do not copy a key into the repository or browser. Approved prices and quotas are
operator-managed infrastructure, not silently assumed current market prices.

Before each real SDK request, a separate committed transaction locks a global ledger,
validates both account expiries and model price/approval reference, and reserves a
conservative text/output token cost. Input uses UTF-8 bytes of instructions, history
and tool schemas plus framing allowance; output uses the hard configured token cap.
Failure to reserve denies before network I/O. Reconciliation charges only actual SDK
usage with the reserved price snapshot, releases the unused hold, and is idempotent
for identical receipts; changed receipts/foreign tenants are denied. Timeout/429/
missing usage holds the ENTIRE reservation as UNKNOWN, never zero. Process/service
restart does not clear reservations. No automatic timeout release, daily reset or
billing-invoice reconciliation is claimed. Unexpected overrun is charged as actual,
recorded OVERDRAWN, and prevents subsequent spend when the quota is exhausted.

This controls reservations under an approved price/token model, not the provider's
billing system. Model-specific tokenization, hidden provider framing, rate changes,
invoice reconciliation, multiple provider organizations/currencies and real spend
certification remain owner gates. Unknown prices fail closed. Database admins remain
trusted; separate migration/runtime roles are a production requirement.

Explicit loopback HTTP protocol fixtures require BOTH the test-endpoint flag and a
non-provider `fixture-` credential. They are MOCKED_PROVIDER and do not reserve real
money. HTTPS/provider hosts cannot use that fixture exemption. Existing SDK failures,
429/timeouts/tool/rate/context tests remain. No test here makes a paid provider call.

The standalone OpenAiLiveProbe is now budget-only, including --live intent: it cannot
reserve in the backend ledger and therefore never bills. CLI ai-check reports
NOT_CONFIGURED without all presence/approval configuration, then ledger-required;
it does not verify credential validity or provider entitlement. Approved operators
must use the separately reviewed ledger-backed backend for a live conversation.

Rollback: disable provider flags first. Keep V9 and reservation data; rolling back
application code must not bypass unresolved holds or restore the former standalone
billing probe. No destructive down migration or policy seeding is supplied.
