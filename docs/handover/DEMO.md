# Presentation script — 12–15 minutes, truthful branches

Use a clean owned lab for a fresh story. Existing named causal commands intentionally
reuse stored incident/event IDs and timestamps; they do not fabricate a new fault on
rerun. Never reset shared data to make a presentation look fresh. Reserve setup and
image-download time before the session. The rehearsal runner below performs simulator
writes only and disables paid provider use. Do not project private runtime files.

## Preflight (before the audience)

```bash
INVESTINATOR_MONITOR_ENABLED=true ./investinator up
./investinator doctor --json
./investinator health --service kafka
./investinator ai-check
```

Require actual core readiness before continuing. Keep NOT_CONFIGURED optional rows
visible. Provider absent -> announce the **fallback** branch, not a live GPT demo.
Provider live branch requires an account owner's valid backend key/model, independent
approved budget and explicit enablement/preflight from CONFIGURATION.md. Never enter
secrets on a shared screen. No actual paid provider call occurred in handover testing.

Open the local UI (default 127.0.0.1:13000), sign in as viewer with its private role
credential, then close the login form. Explain: “These are actual local service
transports. The incident signals and recovery effects are SIMULATED. Model mode and
telemetry source are separate facts.” Show System Health timestamps and missing
connectors. A low seeded score or an open port is not semantic health.

## 1. Symptoms and upstream investigation (minutes 1–5)

```bash
./investinator demo kafka-kraft
./investinator investigate kafka-kraft
```

On first run, ten events produce one causal incident: network instability -> KRaft
controller -> broker metadata/lag -> Spark retries -> dataset freshness. Save the
returned incident_id. Do not describe this as physically damaging a Kafka broker;
actual Kafka Admin health may remain healthy. A repeat uses the same durable case.
Open NerveMap, select that monitored incident, then Incident Room. Show graph revision,
KRAFT mode (no ZooKeeper), event timestamps, supporting and contradictory evidence,
ranked network/controller/broker alternatives and “no confirmed cause.” Explain the
local fixture's 10:1 grouping is not measured business alert reduction or accuracy.

Show Explore/Pulse/Incident evidence with their source labels. The native normalized
OpenSearch logs and the causal stream are distinct evidence sets: do not claim an
unbound log belongs to this incident. Where a scoped search/knowledge tool returns
nothing, show missing evidence rather than silently substituting a seeded record.
Historical runbook guidance is not a fresh observation or human-approved diagnosis.

For an actual bounded normalized-log retrieval from this owned lab, run from root
without printing the private configuration (do not project any secret editor):

```bash
python - <<'READ_ONLY'
import json, sys
from datetime import datetime, timedelta, timezone
from urllib.parse import urlencode
sys.path.insert(0, "scripts")
import investinator_cli as cli
state = cli.load_state()
end = datetime.now(timezone.utc)
query = urlencode({"size": 3, "from": (end-timedelta(hours=4)).isoformat(), "to": end.isoformat()})
result = cli.http(cli.api(state), "/api/v1/evidence/search?"+query, token=state["secrets"]["EVIDENCE_API_TOKEN"])
print(json.dumps({"status": result["status"], "retrieval_mode": result["retrieval_mode"], "items": result["items"]}, indent=2))
READ_ONLY
```

The default native search window is one hour; this explicit query uses the allowed
four-hour window without changing record timestamps. If it is empty, report no hits
or prepare a fresh owned lab before the presentation. These are native OpenSearch results, not automatically bound causal-incident logs.
In Runbooks/Knowledge inspect the named repository guidance (for example
runbooks/kafka_disk_pressure.md). The legacy catalog/repository file is historical
advice, not proof of an approved versioned RAG hit. The conditional real-provider
branch may retrieve approved incident-scoped knowledge; a missing/revoked result is
shown honestly. Do not auto-review/publish a document to manufacture human approval.

## 2. Investigator conversation (minutes 5–8)

Select the causal incident in Investigator. Ask:

> “Why is Kafka failing in this simulated case? Compare network, KRaft controller
> and broker explanations. Cite timestamped supporting and contradictory evidence,
> name missing checks, and propose only a read-only next check.”

Follow up: “What observation would falsify the leading hypothesis?”

**Current unconfigured branch:** display deterministic-fallback / NOT_CONFIGURED.
It is a working availability path, not evidence of model quality. Do not narrate an
LLM/tool call that did not happen. Use the actual correlation view for alternatives.

**Conditional real-provider branch:** require response_mode=gpt/provider_status=OK
and inspect the actual per-turn tool trace/citation IDs. If no tool was chosen, say
so; never pretend a call. Click an authorized citation, inspect source/time and reload
history. Show unknown usage/cost honestly; only measured tokens/latency appear.
Fault data remains SIMULATED even when the provider traffic is real. If 429/outage/
timeout occurs, show safe status and stop model claims. Model advice cannot approve.

## 3. Separate controlled disk recovery (minutes 8–12)

The network/KRaft case has **no registered recovery executor**. Announce the change
of scope: “We now demonstrate governance in a separate owned disk ReplayLab sandbox;
this does not repair the preceding network case or a real Kafka broker.”

1. Sign in as operator and start the named Kafka disk-pressure ReplayLab scenario
   (`kafka_disk_pressure`); open its returned Incident Room. Record the different ID.
2. Switch to planner. In **Governed ReplayLab action**, dry-run recovery (e.g. disk
   target 60%), inspect eligibility, then create the governed proposal. Show immutable
   target/hash/version, SIMULATED environment, risk/owner, evidence freshness, expiry
   and independent verification requirement. Do not use legacy approval controls.
3. Switch to approver, load the same plan, review the exact definition and check the
   review box. Approve before the <=120-second authorization expiry. If expired,
   refresh/review policy; do not bypass it. Roles are credentials, not independent people.
4. Switch to executor, review the same hash and execute the approved simulation.
   Acknowledge receipt; **do not claim resolved yet**. No real restart is sent.
5. Switch to verifier. Verify before sampling: expect failure/unresolved. Sample
   current simulator state, review again and verify: only the independent fresh
   matching sample can pass. The resulting metrics are simulator post-state, not
   native Kafka health or the earlier network-case recovery.
6. Optional: planner dry-runs/proposes named rollback, then repeat separate approval,
   execution, sample and verification. Restoring the fault closes unresolved; it is
   not successful business recovery. Review the audit/postmortem; knowledge promotion
   remains pending independent review.

Do the long explanation before starting the disk scenario. Once proposing/approving,
complete the short execution sequence within freshness/approval limits; do not pause
for a long audience Q&A. Expired preconditions require fresh owned evidence/a new
explicit demo case, not a silent timestamp or approval refresh.

Stop on a failed criterion. Do not invent a success screen, skip approvals or issue
production commands. Plan expiry, changed hash, role denial or lost acknowledgement
are expected safe failure paths; refresh durable status before an explicit retry.

## 4. Business close and cleanup (minutes 12–15)

Show the affected dataset/dashboard **simulation** and distinguish it from measured
customer impact. Present the executive brief: potential investigation-time/risk value,
provider/infrastructure/operating costs, and the study needed for verified ROI.
Production MTTA/MTTI/MTTR, accuracy and financial savings are NOT_MEASURED. Show the
blocked release gates, not a production-ready claim.

```bash
./investinator doctor
./investinator down
```

Down retains data and credentials; do not destroy volumes as a closing flourish.

## Automated rehearsal / evidence

With dev dependencies and Playwright Chromium installed:

```bash
python scripts/verify_e2e_lab.py --outages
```

This exercises first/repeated up, both causal modes, duplicate storm, API restart,
14 browser checks with the named causal graph and **separate** governed disk recovery/
rollback, wrong role/tenant, default Python and eight owned dependency failures.
It does not make a paid provider request. Reports and screenshots live under .runtime
and sanitized hosted artifacts; no cookie/credential capture. For a shorter existing
lab rehearsal: `python scripts/verify_frontend_compat.py` (creates only owned simulated
replay data and disposable Python/Next processes). Actual live GPT remains a separate
budget-authorized test, never a fixture-based acceptance claim.
