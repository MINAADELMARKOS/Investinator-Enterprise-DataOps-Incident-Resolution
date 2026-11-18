# InvestiNator — AI DataOps Incident Investigation Copilot

**AI-powered DataOps Observability, Investigation & Controlled Remediation**

See the signal. Find the cause. Fix with confidence.

**Observe → Detect → Investigate → Fix → Verify → Learn**

InvestiNator follows a signal across infrastructure, pipelines, and datasets, gathers inspectable evidence, proposes a recovery path, records human approval, and verifies symptoms before closing an incident. This repository is a working **local demo foundation** for that loop, centered on an orders pipeline. It is not a production monitoring or remediation service.

| Capability | What runs today | Boundary |
| --- | --- | --- |
| Telemetry and Pulse | API ingestion, seeded metrics/logs/events, threshold alerts, Prometheus API scrape, optional OpenTelemetry HTTP traces | ReplayLab fault signals are simulated; no live Spark, HDFS, NiFi, or Kudu connector |
| Kafka | Real local producer, consumer, and read-only broker health in Compose | Broker disk pressure and lag failure scenarios are injected demo signals |
| Correlation and NerveMap | Persistent topology, correlated alerts, incident evidence, blast radius, dataset freshness | Seed lineage graph; no automatic discovery |
| Investigator and memory | Read-only tool registry, saved multi-turn chat, real GPT Responses tool-calling when configured, plus deterministic fallback | Requires an OpenAI API key and configured model; source metrics can still be simulated |
| Fix and verification | Deterministic policy, recorded approvals, one allowlisted simulated recovery, metric-based verification, audit trail | No production shell or infrastructure write executor |
| ReplayLab and evaluation | Three replay scenarios, saved run/evaluation records, offline regression dataset | Scenario results describe demo behavior only |
| Interface | Next.js Command Center, Pulse, Incident Room, Investigator, NerveMap, ReplayLab, Approvals, Knowledge, AI Observability and more | Demo identity is fixed server-side; OIDC/RBAC is not implemented |

For a **real GPT-backed local chatbot** and optional Elasticsearch log retrieval, follow the [Live Investigator setup](docs/live-investigator.md). The public GitHub Pages UI is a static preview, not an API-connected live demo.

The dark interface uses a shared [cyberpunk design system](docs/design-system.md) with accessible neon controls, terminal panels, and responsive layouts.

Explore the [public GitHub Pages UI preview](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/) with synthetic data, or follow the [detailed user guide](docs/user-guide.md) to run the complete API-backed demo locally. The preview source and deployment workflow live in `apps/showcase` and `.github/workflows/pages.yml`.


## Engineering evidence

- **Author:** [Mina Markos](https://minaadelmarkos.github.io/MinaMarkos/). See the [architecture](docs/architecture.md) for the service and evidence flow.
- **Reproduce a case:** Run the documented [Kafka disk-pressure replay](docs/user-guide.md) with synthetic orders. The [API smoke script](scripts/smoke_demo.py) exercises investigation, approval and verification.
- **Evaluate safely:** The [offline fixtures](eval/test_incidents.jsonl) and [workflow tests](tests/test_workflow.py) cover demo behavior. Use the [security notes](docs/security.md) to understand the demo identity and remediation boundaries.

## Java AI architecture — next implementation stages

The Java 21 / Spring Boot foundation from Prompts 02–09 was merged into `main` through PR #6. Later Java monitoring, governed ReplayLab remediation, opt-in Next.js operator workflows, E2E lab and security hardening through Prompt 14 are **pushed to `feature/java-ai-audit` but are not yet merged into `main`**. Python/FastAPI remains the default backend; Java uses its own Flyway schema and opt-in routing. Production release remains **BLOCKED** by the documented security and certification requirements.

- [Five-layer Java architecture v2](docs/roadmap/JAVA_AI_ARCHITECTURE_V2.md): Infrastructure, Model, Platform, Agent, Application, including verified-vs-reported status.
- [Version 2 execution prompts (09A–16)](docs/roadmap/POST_P09_PROMPTS_V2.md): Sync and evaluation first, then monitoring, remediation, frontend, E2E, security, handover and optional cutover.
- [Prompt 10–14 gap audit and current remote evidence](docs/roadmap/POST_P14_GAP_AUDIT_2026-11-18.md): what has passed, what remains blocked, and where to verify it.
- [Consolidated Prompt 14B — remediation, full verification and push](docs/roadmap/PROMPT_14B_SINGLE_REMEDIATION_AND_PUSH.md): one coding-agent instruction to close code-addressable gaps without bypassing production gates.
- [Historical synchronization of the local Codex Java worktree](docs/roadmap/SYNC_JAVA_WORKTREE.md): recover local commits and incorporate main without force-pushing or deleting work.

The public Pages experience remains an explicitly synthetic static preview; it does not host the Java API or live GPT connections.

### Java v2 operator interface on GitHub Pages

The [public Java operator experience](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/) mirrors the newer Java-backed UI **without exposing an API**. Use the hash routes to explore:

- [System Health](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/system-health): semantic capabilities, source mode, UNKNOWN and NOT_CONFIGURED.
- [Monitoring](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/monitoring): versioned rules, bounded controls and incident deduplication concepts.
- [Root Cause Explorer](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/correlation): KRaft/network, legacy ZooKeeper and NiFi/HDFS timestamped causal scenarios.
- [Governed Recovery](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/governance): role separation, named simulated actions and independent verification.
- [Operator Access](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/operator/login), [AI Architecture](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/architecture) and [Release Readiness](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/release-readiness).

The new pages are implemented under `apps/showcase/src/JavaPreview.tsx` and `java-preview-data.ts`, not the Java-backed Next.js runtime in `apps/web`. All examples are **SIMULATED**, entirely browser-local and free of model, backend, authorization and infrastructure calls. To use the functional Java operator UI instead, follow the isolated lab's [Prompt 12 operations guide](docs/implementation/prompt12/OPERATIONS.md) on `feature/java-ai-audit` until that development work is reviewed and merged.

## Run the full demo

Requires Docker Desktop with its engine running. From the repository root:

```bash
docker compose up --build -d
```

Open [InvestiNator](http://localhost:3000), [API docs](http://localhost:8000/docs), and [Prometheus](http://localhost:9090). Run `docker compose ps` to check containers. The Kafka workload creates `orders.events` and continually produces and consumes demo orders. PostgreSQL stores incidents, telemetry, approvals, audit records, and replay results; Alembic applies the schema at API startup. Docker Compose uses a trusted PostgreSQL connection on its private demo network. Do not expose the stack to the internet.

In ReplayLab, choose **Kafka broker disk pressure** and start a run. Open its Incident Room to review five correlated alerts, evidence, and blast radius. Try **Verify** before recovery to see the failing checks. Approve the named demo action and execute it. The demo adapter writes recovery telemetry and resolves the incident only when thresholds pass. The replay does **not** fill an actual Kafka disk or restart a broker. Other scenarios cover Spark executor pressure and dataset freshness.

Run `python scripts/smoke_demo.py` after the stack starts to exercise the same path through the live API. It creates and resolves one demo incident.

To stop the stack, run `docker compose down`. Add `-v` only when you intend to erase the demo PostgreSQL volume.

## Run backend and frontend separately

Use Python 3.11+ and Node 22+:

```bash
python -m venv .venv
# Activate .venv for your shell.
python -m pip install -e '.[dev]'
alembic upgrade head
uvicorn incident_copilot.api:app --app-dir src --reload
```

The default local database is `investinator.db` (SQLite) and demo identity is enabled. In a second terminal:

```bash
cd apps/web
npm ci
npm run dev
```

Copy `.env.example` to `.env` to configure optional models or a different database. Environment files are ignored by Git. The web app proxies API calls server-side; the OpenAI key stays in the API environment.

## Optional Java backend

The Java 21 / Spring Boot service is an **opt-in** implementation with its own persistence and governed local ReplayLab. See [Java setup and smoke checks](backend-java/README.md), [normalized evidence lab](infra/evidence/README.md), and [current Prompt 14 status](docs/roadmap/POST_P14_GAP_AUDIT_2026-11-18.md). The normal Compose flow and Python default remain supported. Do not enable Java production routing or enterprise actions without a separately reviewed cutover.

## Verify and evaluate

```bash
pytest -q
ruff check src tests
mypy src
python -m incident_copilot.evaluation eval/test_incidents.jsonl
cd apps/web && npm ci && npm run build && npm audit --audit-level=high
```

The offline evaluation checks classification, evidence or explicit uncertainty, and approval requirements. Model quality, retrieval relevance, and live connector behavior require separate evaluations; no offline score is presented as a production accuracy claim.

## API and design

The stable API namespace is `/api/v1`. Useful endpoints include `/platform/health`, `/telemetry/metrics`, `/topology`, `/alerts`, `/incidents`, `/replay/scenarios`, `/replay/runs`, `/integrations/kafka/health`, `/ai/usage`, `/audit`, and `/evaluations`. The API docs list request schemas and action routes. Older V0.2 routes remain as aliases where practical.

See [architecture](docs/architecture.md), [AI design](docs/ai-design.md), [security](docs/security.md), [ReplayLab](docs/replaylab.md), [demo guide](docs/demo.md), [integrations](docs/integrations.md), and [migration plan](docs/migration-plan.md). Source runbooks live in [`runbooks/`](runbooks/).

## Creator

Created by **Mina Adel Markos**, Senior Big Data Engineer. [GitHub](https://github.com/MINAADELMARKOS) · [LinkedIn](https://www.linkedin.com/in/mina-markos-343b8b171).

## Local Java integration operations

Run `./investinator up && ./investinator doctor && ./investinator smoke` for the
isolated Java/PostgreSQL/Kafka/search/telemetry/Next.js lab. Windows uses
`./investinator.ps1`. See [operations](docs/operations.md) for prerequisites,
JSON health reports, safe teardown and explicit demo reset. This optional profile
preserves the existing Python deployment; local samples remain SIMULATED.
