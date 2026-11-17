# Baseline provenance and reproduction

Date: 2026-11-17. Base main 920fefe1b86e55f038fb0a7e56dd0b42e9d34118.
PR #3 head 67a7328022fa8e918633b7a62bcaf7f48c67f051, open per GitHub HTML.
GitHub REST pull metadata: HTTP 403; HTML and Git ref fetch succeeded.
Python 3.12 / Node 24.22.1 / Java 21.0.13; existing onboarding dependencies reused.
No paid API calls or infrastructure changes. Provider variable presence is recorded
as booleans only. Full Compose functional checks were not performed.

From repository root:

```bash
.venv/bin/python docs/implementation/baseline/run_checks.py
.venv/bin/python docs/implementation/baseline/generate_inventory.py
.venv/bin/python docs/implementation/baseline/validate_artifacts.py
```

checks.json records individual commands and return codes; run_checks.py deliberately
collects all outcomes rather than treating its own completion as a passing suite.
Review each exit_code. web-audit is a failed security gate.
For PR review: fetch refs/pull/3/head, archive the exact SHA into a temporary directory,
run the existing venv Python with PYTHONPATH pointing to the archive's src and pytest
against its tests. Run ruff check and mypy against that archive. Tests use fake OpenAI
and HTTP responses, not live integrations. Never put credentials in archives/logs.

During artifact validation an incorrect assumed path-count assertion (43) failed:
inspection confirmed 42 unique paths and 44 registered routes (GET/POST share paths).
The validator now derives counts from the actual app, validates the full snapshot,
checks every route/table, relative links and the recorded release blocker. Final
artifact-validation.txt records its successful result. This was an audit assertion
mistake, not an application failure.
