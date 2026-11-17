# Live GPT Investigator — local developer demo

This implementation upgrades the existing Investigator conversation to the real
**OpenAI Responses API**, with bounded read-only function calls, persisted
conversations, and an optional Elasticsearch/OpenSearch log adapter.

**Not production-ready:** demo-mode identity is fixed, PostgreSQL uses local
trust auth, Elasticsearch is intentionally unauthenticated in the optional
local profile, and ReplayLab events are synthetic. Do not expose any of these
ports to the public internet. Real cluster connectors and OIDC/RBAC remain future work.

## Five-layer architecture

| Layer | Today | Next |
|---|---|---|
| Infrastructure | Docker Compose, PostgreSQL, Kafka KRaft demo, Prometheus, OTel, optional Elasticsearch | Authenticated collectors, HA, DR |
| Model | Real GPT API (when credentials configured); deterministic fallback | Model routing evaluations |
| Platform | FastAPI gateway, stateless Responses tool loop, local memory, read-only tools | Incident correlation/time-series backend, governed RAG |
| Agent | GPT investigates using metrics, log search, runbooks, upstream/downstream graph | Autonomous correlation triggers and candidate scoring |
| Application | Next.js Investigator Chat/Incident Room/ReplayLab | Real-time evidence investigation panels and production operations |

The goal is to discover *originating* failures across service dependencies,
not merely summarize the service reporting the error. This requires logs,
metrics, topology and event timing; no single GPT response proves causality.

## Run with a real GPT API

1. Make a local `.env` from `.env.example`.
2. Configure `OPENAI_API_KEY` and `INVESTINATOR_OPENAI_MODEL` with an
   enabled model from **your OpenAI API project**.
3. Leave `INVESTINATOR_DEMO_MODE=true`.
4. Run:

```bash
docker compose up --build -d
docker compose ps
```

Open http://localhost:3000/investigator and ask a question, e.g.:

> What are the upstream and downstream impacts of the orders.events topic?

The frontend calls `POST /api/v1/investigator/messages` on its own Next.js
proxy. FastAPI calls GPT from the **backend only** (never from the browser),
executes only allowlisted read-only tools, sends tool results back to GPT,
returns the model's actual answer and persists the exchange in PostgreSQL.

The response includes `response_mode=openai` for actual provider responses,
or `deterministic-fallback` when not configured or unavailable. The UI shows
the selected model and each tool's status. It does **not** imply that the
source data are live: ReplayLab metrics are explicitly `mode=simulated`.

The static GitHub Pages UI preview does NOT host this backend and cannot
act as a live GPT demo. Use Docker Compose or deploy a secured API elsewhere.

## Add Elasticsearch logs for a realistic multi-system demo

This is optional. First add this setting to your local `.env`:

```bash
INVESTINATOR_ELASTICSEARCH_URL=http://elasticsearch:9200
INVESTINATOR_ELASTICSEARCH_INDEX=logs-investinator-demo
INVESTINATOR_ELASTICSEARCH_TENANT_FIELD=tenant_id
```

Then:

```bash
docker compose --profile log-search up --build -d
python scripts/seed_elasticsearch_demo.py
```

The seed script uses `http://127.0.0.1:9200` from the host by default.
If running the script from a separate environment, configure its address.

The seed creates **SIMULATED** but timestamped log documents across:
network-monitor -> KRaft controller -> Kafka broker -> Kafka consumer ->
Spark streaming -> delayed dataset. The Elasticsearch search API is real,
while the fault signals are not captured from production.

Example question:

> Investigate Kafka consumer lag. Look at recent Kafka and KRaft logs,
> examine the upstream dependencies, and explain the most likely originating
> failure, with alternative hypotheses and missing checks.

The agent can invoke `search_logs`, `get_upstream_dependencies`,
`get_blast_radius`, `list_current_metrics`, `get_latest_metric` and
`search_knowledge`, within the configured tool-call limit.

For real Elasticsearch/OpenSearch clusters, configure a restricted service
account, server-side URL, index pattern, tenant field mapping, API key and
TLS. Make sure index data includes your tenant key. **Never disable TLS or
authentication in a shared environment.**

For legacy Kafka (before KRaft-only releases), ZooKeeper can be a critical
upstream source of faults. Kafka 4.x is KRaft-only; the demo does not pretend
to find ZooKeeper failures in its Kafka 4.x instance.

## Security and current limitations

- Only known read-only tool names and Pydantic-validated inputs are executed.
- Tool URLs and Elasticsearch DSL are never chosen freely by GPT.
- The log adapter restricts index patterns, request size, response fields,
  lookback interval, timeout and tenant filter.
- Logs, prompts, and tool results are treated as untrusted text and bounded
  before leaving the backend; no production execution tool is registered.
- Model-produced evidence IDs are verified against known observations for
  supported ID formats; this is not a substitute for human expert review.
- Auto-detection, proactive conflict monitoring, streaming log ingestion,
  causal incident graph ranking, true infrastructure topology discovery,
  production authorization and controlled remediation are **not yet built**.
- Fix mode is advisory only and remains subject to the existing separate
  approval/policy workflow.

## Verification

```bash
pytest -q
ruff check src tests
mypy src
cd apps/web && npm run build
```

Tests mock the provider to validate function-call continuation and conversation
history. They **cannot** validate a paid GPT request without the configured
API credential, and do not claim to validate production root-cause accuracy.
