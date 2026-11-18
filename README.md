# InvestiNator — distributed-systems incident investigation

InvestiNator connects incident symptoms with inspectable logs, metrics, dependency
history and competing root-cause hypotheses. It has a Java 21/Spring Boot modular
monolith, the preserved Python/FastAPI backend and the existing Next.js interface.
**This is a verified local laboratory, not a production-ready service.**

Java work is published on `feature/java-ai-audit`; earlier Java migration reached
main through owner-merged PR #6. Python remains the default backend. Only the
explicitly owned Java lab selects Java. Production identity, private connector
certification, independent AI-quality review and release hardening remain blocked.
See the [handover](docs/handover/README.md), [build/status matrix](docs/handover/STATUS_MATRIX.md)
, [post-P14 audit](docs/roadmap/POST_P14_GAP_AUDIT_2026-11-18.md),
[Prompt 14B scope](docs/roadmap/PROMPT_14B_SINGLE_REMEDIATION_AND_PUSH.md)
and [production release gates](docs/implementation/prompt14/RELEASE_RISKS.json).

## One-command Java laboratory

Prerequisites: local Docker Engine/Desktop with Compose v2, Python 3.11+, a **JDK
21**, Node 22.6+ and npm; allow at least 6 GiB free RAM and 10 GiB disk for one lab.
Initial setup needs package/image network access. Linux is verified; native Windows
wrapper availability is not a Windows certification. From a clean feature checkout:

```bash
git checkout feature/java-ai-audit
INVESTINATOR_MONITOR_ENABLED=true ./investinator up
```

This builds and starts PostgreSQL, authenticated OpenSearch, Kafka KRaft, collector,
forwarder, Prometheus, Java and Next. It preserves data on repeated up. Defaults:
[UI](http://127.0.0.1:13000), [Java liveness](http://127.0.0.1:18081/actuator/health/liveness),
[Prometheus](http://127.0.0.1:19090). Ports bind loopback. Select alternate LAB_* ports
**before first setup** if occupied. Private generated credentials and ownership live
in ignored `.runtime/lab.json`; never commit, paste or project that file.

Open `/operator/login` and use the private credential for the selected demo role.
The health view distinguishes actual local probes from SIMULATED incident data;
missing optional services remain NOT_CONFIGURED. Model configuration is independent
of telemetry source. A visible SIMULATED DEMO banner does not turn GPT into a mock.

```bash
./investinator doctor
./investinator health --all
./investinator demo kafka-kraft
./investinator investigate kafka-kraft
./investinator smoke
./investinator logs api
./investinator restart api
./investinator down
```

`doctor` and health are read-only diagnostics. `smoke` performs approved **simulated**
workflow writes. `restart api` can restart only the owned Java container. `down`
removes containers/network and **retains volumes**. Only an explicitly intended
`./investinator reset-demo --confirm-destroy-demo-data` erases owned demo volumes.
Do not use generic volume deletion as troubleshooting. Preserve runtime credentials
with their data. See [operations/config/troubleshooting](docs/handover/OPERATIONS.md).

## Presentation and provider configuration

Follow the [presentation script](docs/handover/DEMO.md) for causal KRaft/network
investigation and a **separate** governed disk ReplayLab recovery. No network
remediation executor exists. Planner, approver, executor and verifier capabilities
are separate demo roles, not proof of independent people. Executor acknowledgement
cannot resolve an incident without independently sampled simulator post-state.

The official OpenAI Responses SDK implements real read-only function calling and
history when explicitly configured. Without credentials/model it returns a visibly
labeled deterministic fallback. Current actual GPT validation is NOT_CONFIGURED;
SDK fixture tests do not prove live model quality. OpenAI API billing is separate
from a ChatGPT subscription. Only with a valid key, permitted model and independently
approved test budget and durable quota policy may an operator enable model-backed
chat. `./investinator ai-check` is a zero-call budget preflight; standalone probes
(including `--live` intent) cannot bypass the backend reservation ledger. See [provider/billing/preflight](docs/handover/CONFIGURATION.md).
Never put a key in a browser variable, screenshot, prompt, Git commit or Pages build.

## Developer verification

```bash
python3 -m venv .venv
. .venv/bin/activate  # Linux/bash; use shell-appropriate activation elsewhere.
python -m pip install --upgrade pip==26.3
python -m pip install -e '.[dev]'
./investinator test
npm ci --prefix apps/web
npm --prefix apps/web run lint
npm --prefix apps/web test
npm --prefix apps/web run build
npm --prefix apps/web audit --audit-level=high
pytest -q
ruff check src tests scripts tests_cli
mypy src
```

`./investinator test` runs Maven Verify and CLI tests; it does **not** implicitly run
frontend/Python/browser suites. For the complete isolated regression after installing
Playwright Chromium, use `python scripts/verify_e2e_lab.py --outages`. See the
[developer guide](docs/handover/DEVELOPMENT.md) for JDK profiles, direct Maven commands,
API/schema inventories, backup restore, test costs and clean-checkout reproduction.

[Hosted CI run 44397526897](https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/actions/runs/44397526897)
passed all five jobs on `8e41c2c`, including Java/Testcontainers, frontend, Python/CLI,
full laboratory/restore and dependency security. OSV checked 233 default Maven
packages with zero findings. This does not certify container OS images, the optional
shaded Hive driver, enterprise identity, production SLOs or independent AI accuracy.
[Prompt 14 acceptance](docs/implementation/prompt14/REPORT.md) records scope and limits.

## Preserved Python runtime and static preview

`docker compose up --build -d` runs the **Python** demo with Next on port 3000 and
FastAPI on 8000; it does not perform a Java cutover. That database uses demo trust
inside its private network, so keep it local. [Python/API guide](docs/user-guide.md)
and [Python model integration](docs/live-investigator.md) describe that separate path.
Java Flyway never adopts Python/Alembic schemas. Selecting Python on a Next server
requires its existing Python API URL; it does not roll back or delete Java data.

The [GitHub Pages preview](https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/)
is an explicitly synthetic static showcase (`apps/showcase`), without a backend or
live GPT. Its source link points to this repository; feature work does not publish it.
[Design system](docs/design-system.md) · [versioned architecture](docs/handover/ARCHITECTURE.md)
· [executive brief](docs/handover/EXECUTIVE_BRIEF.md) · [implementation state](docs/implementation/STATE.md).

Created by **Mina Adel Markos**, Senior Big Data Engineer.
[GitHub](https://github.com/MINAADELMARKOS) · [LinkedIn](https://www.linkedin.com/in/mina-markos-343b8b171).
