# Prompt 01 factual assessment

Audit date: 2026-11-17. Main and local baseline: `920fefe1b86e55f038fb0a7e56dd0b42e9d34118`.
PR #3 head: `67a7328022fa8e918633b7a62bcaf7f48c67f051`. GitHub HTML showed **Open**;
REST metadata request returned HTTP 403. Git fetch and HTML review succeeded.
No PR was merged, cherry-picked, pushed or modified.

## As-is inventory

| Area | Code evidence | Actual implementation and boundary |
|---|---|---|
| HTTP | api.py, schemas.py | 44 registered routes including legacy aliases; FastAPI/Pydantic; many dict responses are weakly typed |
| Domain | agents.py, services.py | keyword classification, regex signatures, runbook snippets, metric thresholds, 30-minute graph-connected grouping; hypotheses are not proven causality |
| Persistence | db.py, migrations/versions/bf50bd87cb88* | SQLAlchemy, Alembic; SQLite local, PostgreSQL Compose; 13 tables; JSON payloads carry evidence/actions/timelines, no separate Evidence table |
| UI | apps/web | working Next.js App Router, server fetches plus GET/POST proxy, cyberpunk design; API_INTERNAL_URL permits backend switch |
| Showcase | apps/showcase, pages.yml | Vite static synthetic preview, not connected operational evidence |
| GPT on main | ai_gateway.py | actual optional responses.parse call with typed analysis, citation membership check, usage, fallback; tools preselected by Python, not model function calling |
| Chat on main | api.py chat_message | persists user/assistant messages; model is given incident context but not actual question/history; ask/fix use deterministic text |
| Tools | tools.py | three tenant-scoped read-only handlers: metric, knowledge, downstream impact; no model tool loop on main |
| Replay | services.py SCENARIOS | disk pressure, executor memory and freshness simulation; updates stored metrics, alerts and graph impact |
| Actions | policy.py, api.py execute_action | deterministic string policy helper; route separately permits only approved ACT-DEMO simulation; no shell/production executor |
| Telemetry | api.py, observability.py | metric ingestion, stored logs/events/traces reads; optional HTTP spans; no continuous multi-system collection |
| Kafka | kafka_adapter.py, demo_workload.py | optional read-only broker metadata/topics; real producer/consumer path exists; quorum, ISR, lag and version/auth semantics absent |
| Tests | tests/, eval/ | 8 main tests, 7 deterministic evaluation cases; not a live causal accuracy benchmark |
| Operations | scripts/smoke_demo.py | live Compose smoke requires Kafka; no universal doctor/health CLI or Windows wrapper |

SQL inventory in baseline/schema-postgresql.sql is compiled from ORM metadata without
connecting to or mutating a live database. Migration source was inspected. It is not
proof of PostgreSQL deployment/schema parity. Tables: incidents, alerts, telemetry,
entities, relationships, audit_events, approvals, replay_runs, conversations,
messages, knowledge_documents, ai_usage, evaluations. Foreign keys link alerts to
incidents and messages to conversations; tenant filters are in application queries,
not database row-level security. Approvals have unique tenant/incident/action index.
Relationships have no entity foreign keys. SQLite and PostgreSQL behavior needs parity tests.

## Docker and CI

Compose declares db (postgres:16-alpine), api, web, broker (apache/kafka:4.3.1,
KRaft), demo-producer, demo-consumer, prometheus:v2.55.1 and OTel collector:0.121.0.
API waits for healthy DB; web waits for healthy API; other dependencies largely use
startup ordering. PostgreSQL uses trust authentication on the demo network. Broker
and web/API ports are published. Do not deploy this configuration to production.
Prometheus scrapes /metrics; the collector exports only to debug, not a retained
trace/log search backend. Syntax validation does not prove image availability or health.

CI backend: Python 3.12 editable dev install, pytest, ruff, mypy, JSONL evaluation,
Compose validation. Frontend: Node 22 npm ci, build, high-level audit. Pages builds
the separate showcase. No Java, container functional tests or real provider tests in CI.
Python dependencies have minimum bounds without lock; frontend has npm locks.
Current machine: Python 3.12, Node 24, Java 21.0.13; Maven not on PATH. Java scaffold,
wrapper and dependency compatibility have NOT been implemented or tested.

## Secrets and safety

config.py reads DATABASE_URL, demo flags/tenant, model/default/fast/reasoning IDs,
provider storage and max-call settings, CORS and creator links. ai_gateway.py reads
OPENAI_API_KEY backend-only. Kafka and OTel read their optional endpoints.
API_INTERNAL_URL is server-side. .env.example contains empty placeholders; Compose
passes optional provider settings. baseline/config-presence.json records booleans,
never values. No paid call was made. Fixed demo.senior_sre is not authentication;
protected endpoints return 503 outside demo mode. Approval decided_by from the
request does not establish independent authorization. Production writes remain disabled.

## Baseline results and reproduction

Run commands in baseline/checks.json from their recorded directories. Outputs and
exit codes are versioned alongside OpenAPI and SQL inventory.

- PASS: pytest 8/8, ruff, mypy (15 files), offline evaluation 7/7,
  docker compose config --quiet, Next.js production build.
- FAIL: web npm audit --audit-level=high, exit 1; locked Next.js 16.3.8 has
  one critical dependency finding with critical/high/moderate underlying advisories.
  See web-audit.txt, including GHSA-2464-fhr4-w9xw, GHSA-673q-mjm6-r5xr,
  GHSA-p3m5-59mp-jv5r and GHSA-mfr8-8qww-v76h. Release BLOCKED; no automatic audit fix.
- PASS: showcase dependency audit, exit 0 (does not establish runtime coverage).
- PASS: PR #3 source archive tests 12/12 with fake provider/HTTP; isolated
  PYTHONPATH pointed at the fetched PR archive. No paid OpenAI/real search proof.
- NOT_TESTED this stage: full Compose startup, PostgreSQL runtime, live Kafka,
  Elasticsearch/OpenSearch, Prometheus scrape arrival, trace arrival, production clusters.

No business logic, dependencies, UI or schema were changed by this audit.

Additional PR #3 static checks: ruff PASS (exit 0), mypy PASS (17 files, exit 0), using the isolated fetched source archive.
