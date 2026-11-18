# Developer entry and verification

```bash
git clone https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution.git
cd Investinator-Enterprise-DataOps-Incident-Resolution
git checkout feature/java-ai-audit
python3 -m venv .venv
. .venv/bin/activate  # Linux/bash path; use the appropriate activation on other shells.
python -m pip install --upgrade pip==26.3
python -m pip install -e '.[dev]'
INVESTINATOR_MONITOR_ENABLED=true ./investinator up
```

Use JDK 21 (javac, not only a JRE), Python 3.11+ for application development (3.12
verified), Node 22.6+ and local Docker/Compose v2. Linux is verified; Windows scripts
are not Windows certification. Keep at least 6 GiB free RAM/10 GiB disk for one lab;
concurrent Testcontainers/labs need more. Images/wrapper/Node lock are pinned. Python
minimum-only declarations are not a complete cross-platform lock. Cold initial
builds require network access; cached local reproduction is not air-gap proof.

## Java iteration / profiles

Maven Wrapper 3.9.11, Boot 4.1.2, Java 21. From root:

```bash
./backend-java/mvnw -B -f backend-java/pom.xml verify
./backend-java/mvnw -B -f backend-java/pom.xml -Dtest=EnterpriseIngressTest,ChatGatewayTest verify
```

`./investinator test` wraps full Maven Verify plus CLI unittest. It does not run npm,
Python pytest or browser tests. Run independent workflows sequentially if they modify
node_modules/.next or the same Maven target; parallel npm ci/build is unsafe.

Default local Java is a DB-free health scaffold. For the full private schema use the
owned lab; do not hand-point Flyway at existing Python data. Profiles demo/integration
keep local behavior; enterprise requires TLS and denies application ingress without
real identity. Never enable enterprise mode as a live demo workaround. Backend image
consumes a built jar; build/test the jar before direct Docker image assembly.

```bash
pytest -q
ruff check src tests scripts tests_cli
mypy src
npm ci --prefix apps/web
npm --prefix apps/web run lint
npm --prefix apps/web test
npm --prefix apps/web run build
npm --prefix apps/web audit --audit-level=high
(cd apps/web && npx playwright install --with-deps chromium)
python scripts/verify_e2e_lab.py --outages
python scripts/verify_backup_restore.py
python scripts/capture_operational_baseline.py
python scripts/generate_handover_inventory.py --check
```

E2E owns a separate project's ports when first setup is empty, verifies unchanged-up,
two causal scenarios, duplicate flood, restart, browser roles, independent simulator
sample and eight owned dependency stop/start cases. It forces provider disabled,
never pays for GPT and cleans up only its fixture resources. See its report before
interpreting a failed run. Private credentials/dumps/cookies are excluded from uploads.
CI runs five jobs, with 25-minute lab and 12-minute security budgets; actual hosted
runs and limitations are in [status evidence](STATUS_MATRIX.md).

## Python coexistence / API compatibility

To use the preserved Python stack, `docker compose up --build -d` keeps Next->Python.
For host development, activate the environment, set DATABASE_URL explicitly (SQLite
local default is supported for Python only), run `alembic upgrade head`, then
`uvicorn incident_copilot.api:app --app-dir src --host 127.0.0.1`. Run Next separately
with API_INTERNAL_URL to that Python API; unset INVESTINATOR_BACKEND or set python.
For a loopback-only host Next development server after npm ci:

```bash
cd apps/web
API_INTERNAL_URL=http://127.0.0.1:8000 INVESTINATOR_BACKEND=python npx next dev --hostname 127.0.0.1 --port 3000
```

The package's generic dev script binds all interfaces; use the explicit host above
for a local-only session. Do not feed Java schema credentials to Alembic. Java database source is PostgreSQL
only, dedicated Flyway V1–V8; [source inventory](source-inventory.json) lists tables.

Compatibility is tested against a captured Python contract plus a 74-step seeded
workflow trace, including aliases and idempotency. This is not universal API parity,
enterprise connector compatibility or schema interchange. Java-native diagnostics,
RAG, correlation, monitor and governed remediation are additive. Static annotation
routes may be conditionally unavailable/denied; see generated bean conditions and
[authentication matrix](../implementation/prompt14/AUTH_MATRIX.md).

## Change and review policy

Read STATE/ADRs and keep public outcomes/evidence separate from private model state.
Secrets stay outside Git. Regenerate inventory after route/config/schema changes.
Preserve Next lock, schemas, old aliases and existing commits. Record test provenance,
unknowns and rollback. No forced push, automatic merge, production routing or executor.
A feature branch/green CI does not substitute for release gates or human AI review.
