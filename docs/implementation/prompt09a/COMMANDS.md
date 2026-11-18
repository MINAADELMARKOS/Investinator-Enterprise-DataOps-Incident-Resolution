# Prompt 09A verification commands

All commands ran locally after merging remote main. JDK21/cache/proxy options are
managed by `./investinator` as documented in backend-java/README.md. No TLS/test
bypass or real paid provider call. See test-summary.json and adjacent evidence.

| Command | Result |
|---|---|
| `./investinator test` | exit 0; Maven verify 78 tests, 0 failures/errors/skips; CLI 14 tests |
| `.venv/bin/python -m pytest -q` | exit 0; 12 passed |
| `.venv/bin/ruff check src tests scripts/investinator_cli.py scripts/verify_health_lab.py scripts/verify_connectors_lab.py scripts/knowledge_bootstrap.py scripts/verify_knowledge_lab.py tests_cli` | exit 0 |
| `.venv/bin/mypy src` | exit 0; 17 files |
| `.venv/bin/python -m incident_copilot.evaluation eval/test_incidents.jsonl` | exit 0; 7/7 fixture cases |
| `npm ci --cache /workspace/.cache/npm` in apps/web | exit 0; initial default-cache path was unavailable, corrected to writable cache |
| `npm run build` / `npm run lint` in apps/web | both exit 0; Next.js 16.4.5, TypeScript |
| `npm audit --audit-level=high --json --cache /workspace/.cache/npm` in apps/web | exit 0; zero vulnerabilities |
| `docker compose --profile '*' config --quiet` | exit 0; includes Python's optional log-search |
| `docker compose -f docker-compose.yml -f compose.java.yaml --profile java config --quiet` | exit 0 |
| Java + persistence + evidence overlay with `--profile '*' config --quiet` | exit 0 after all required placeholders supplied |
| `./investinator up` / `doctor --json` / `health --all --json` / `smoke` | all exit 0; owned local lab only |
| `.venv/bin/python backend-java/scripts/verify_persistence.py --with-web` | exit 0; actual PostgreSQL/process restart/Next proxy/DB outage/non-demo denial |
| `JAVA_HOME=/workspace/.cache/jdk-21 .venv/bin/python backend-java/scripts/evidence_lab.py verify` | exit 0; actual collector/search/metrics/tenant/role/failure transport |
| `.venv/bin/python scripts/verify_knowledge_lab.py` | exit 0; actual scoped BM25, review, publish, feedback, fallback/history |
| `.venv/bin/python docs/implementation/baseline/validate_artifacts.py` | exit 0; historical contract + synchronized description-only snapshot |
| `git diff --check` and staged credential checks | pass |

Overlay config uses nonsecret `09a-validation-placeholder` values for
JAVA_DATABASE_PASSWORD, EVIDENCE_ADMIN_PASSWORD, EVIDENCE_WRITER_PASSWORD,
EVIDENCE_READER_PASSWORD and EVIDENCE_API_TOKEN. These placeholders are used for
syntax validation only; actual lab runtime credentials remain ignored/private.
The first combined config invocation omitted required values and was correctly
rejected; the complete invocation passed. No config guard was weakened.

Java tests include actual PostgreSQL/OpenSearch/Kafka/ZooKeeper/Thrift fixtures,
provider protocol mocks and negative connector/citation/review cases. Source
incident content is SIMULATED; no enterprise-live claim. Unit-test printed
`paid_call_made:true` is a stubbed CLI subprocess result, not a real provider call.
