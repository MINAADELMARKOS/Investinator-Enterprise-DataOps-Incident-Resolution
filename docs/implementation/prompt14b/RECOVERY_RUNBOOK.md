# ReplayLab recovery and audit export

Only REPLAY_DISK_RECOVER_V1 and separately approved REPLAY_DISK_RESTORE_V1 exist.
No live/native/production executor is introduced. Existing independent capability,
plan hash, resource version, expiry, fencing and sampler checks remain mandatory.

| Observed state | Operator procedure | Forbidden inference |
|---|---|---|
| Before-effect interruption, no receipt | Wait for lease expiry, inspect protected plan, invoke recover once with executor capability; preserve REQUIRES_MANUAL_INTERVENTION and escalate | No receipt is not permission to execute again |
| After-effect interruption, durable matching receipt | Recover acknowledgment; obtain NEW independent sample and verify exact current version | A recovered receipt is not healthy or resolved |
| DB unavailable | Stop operations, preserve IDs and external evidence, restore connectivity through authorized SRE process, inspect receipt/outbox before recovery | No automatic retry, SQL editing or volume reset |
| Failed/false-success verification | Keep incident unresolved; obtain fresh independent sample, investigate mismatch; propose named rollback only if confirmed effect exists | Executor response text cannot be a verifier sample |
| Disaster restore/replay | Restore into a separately owned disposable/staging target under recovery approval; compare schema ownership, plans, receipts and resource versions; quarantine ambiguous plans for operator review | Restoring a backup must not automatically replay an execution outbox |
| Named rollback | Separate RESTORE proposal tied to confirmed parent effect, independent capability approval, execution, fresh sample, verification | Rejected recovery or model suggestion cannot authorize rollback |

GET /api/v1/remediation/plans/{id}/audit (existing read capabilities) exports at most
100 metadata envelopes with a validated opaque cursor. Tenant is server-bound;
plan ownership is checked first. Payloads, response text, arbitrary actors and raw
logs are omitted. The fixed envelope is AUDIT_ONLY_NO_APPROVAL_AUTHORITY. An
external sink is NOT_CONFIGURED. Source audit writes are append-only by application
convention, but the database administrator can mutate them. This is NOT an
externally immutable or tamper-proof audit service. Downstream sinks must dedupe
by tenant/event ID and retain their own approved cursor, identity and retention.
No remote transport, notification, retention deletion or automatic delivery exists.

Export rollback: remove the read endpoint; existing audit/source data remain.
Enterprise authorization, independent humans, legal retention and externally
certified audit/DR remain BLOCKED. See the historical Prompt 11 recovery manual
and Prompt 14 release inventory; neither is superseded as past evidence.
