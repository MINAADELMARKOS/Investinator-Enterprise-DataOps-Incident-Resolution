# Prompt 12 — Java-backed operator console

2026-11-18. Explicitly opt-in local demo; no main merge, production cutover, Pages
publication or paid provider request. Prompt 11 checkpoint: `97a5f83`.

## Implementation

Preserved the existing Next.js design and Python default. A typed fixed-route
adapter selects Python unless INVESTINATOR_BACKEND=java is explicitly configured.
Only the owned eight-service lab opts in. Signed expiring role sessions, a fixed
same-origin server proxy and native capability binding keep credentials off the
client. No graph-admin, knowledge-write, arbitrary URL/command or tenant override
is exposed. Public liveness remains available; operational pages require a session.
The native web probe uses the public login page plus the actual backend proxy,
so authenticated dashboard access does not falsely fail local readiness.

Added System Health with actual timestamps, semantic capability, source and
uncertainty; durable chat history/public tool provenance, current scoped evidence
inspection and provider/fallback distinction; bound graph/correlation/feedback
views; operator-only bounded monitoring overrides; immutable governed plan review,
approval, execution, independently sampled verification and separate rollback.
Flyway V8 adds bounded private monitor controls; Python/Alembic ownership is unchanged.
Missing observations, usage/cost, graphs, provider configuration or connector
certification are not inferred from seeded platform scores.

## Acceptance evidence

The browser runner exercises actual Java/Next/PostgreSQL and a disposable actual
Python API/Next pair with backend selection unset. The named ReplayLab fault/effects
are SIMULATED. Only the browser diagnostic-outage response is MOCKED, explicitly
labeled. No live model response or production accuracy is claimed.

- Full Java/CLI: 107 Java tests, no failures/errors/skips; 26 CLI tests pass.
- New scoped UI contracts: 3 tests pass, including knowledge inspection/revocation,
  cross-tenant/invalid evidence and bounded monitor/suppression semantics.
- Frontend: npm ci, production build, TypeScript lint, 8 adapter/session/contract
  tests pass; npm audit reports zero vulnerabilities. Next.js remains 16.4.5.
- Python: 12 pytest, Ruff and mypy (17 files) pass; baseline artifact validation passes.
- Actual browser: unauthorized access, role/CSRF/query boundaries, native diagnostics,
  fallback chat and malicious-message escaping, history/stale ID handling, scoped
  evidence inspection, client-bundle secret isolation, monitor pause/resume,
  distinct capability recovery + rollback and independent samples, tampered cookie,
  and actual default Python rendering/chat. See results.json and screenshots.
- Native local up/doctor/health/smoke final verification is recorded in test-summary.json.

Initial focused test correctly failed on historical knowledge citation inspection:
lookup did not reload captured citations. Fixed the scoped lookup; current
revocation is still checked, and the test passes. Browser testing found the Docker
internal/public-origin mismatch; added a configured exact public origin without
trusting caller-forwarded headers. The native web probe initially failed after
login became required; it now checks the public login page and real proxy. Browser
assertions were scoped to their own alert instead of Next's empty route announcer.
An overlapping local build/CLI npm install caused a missing-file build failure;
the sequential final CLI build passed. No failed check was waived.

## Operations, contracts and limits

See [operations and rollback](OPERATIONS.md), [contract snapshot](contract-snapshot.json),
[results](results.json) and [test summary](test-summary.json). Existing 74-step Java/
Python compatibility and private migration ownership remain covered by Maven Verify.
Static showcase files are untouched and continue to describe a simulated demo.

Demo role possession does not establish OIDC, independent humans, enterprise RBAC,
TLS ingress or change-control certification. Enterprise executors remain absent/
hard-denied. Actual OpenAI key/model/budget and independent human gold-set sign-off
are still absent. No cost, model accuracy or business-outcome claim is fabricated.
Hosted CI and the 09A PR/API issue remain for Prompt 13; this stage is local only.

Rollback routing by selecting Python and its existing API URL; retain Java schemas,
evidence and volume data. No destructive reverse migration is necessary. GO is
limited to isolated demo development, never production release.
