# Opt-in operator console

The base Compose and unconfigured Next server keep Python routing. No database
owner, Pages content or production route changes. The owned eight-service lab
alone explicitly selects Java. `./investinator up` retains volumes and generates
private role credentials in `.runtime/lab.json`; never copy that file into reports.

For an explicitly configured Java demo Next server:

```
INVESTINATOR_BACKEND=java
JAVA_API_INTERNAL_URL=http://java:8081
WEB_DEMO_ENABLED=true
UI_PUBLIC_ORIGIN=http://127.0.0.1:13000
```

Set a random UI_SESSION_SECRET (at least 32 characters), six distinct
UI_{VIEWER,OPERATOR,PLANNER,APPROVER,EXECUTOR,VERIFIER}_LOGIN_TOKEN values (at least
24 characters), and native HEALTH_API_TOKEN, CONNECTOR_API_TOKEN,
INVESTIGATION_API_TOKEN, KNOWLEDGE_READ_TOKEN, MONITOR_API_TOKEN and the four
REMEDIATION_{PLAN,APPROVE,EXECUTE,VERIFY}_TOKEN capabilities on the **Next server**.
No NEXT_PUBLIC credential exists. Graph import and knowledge-write capabilities
are deliberately absent from the web server. UI_PUBLIC_ORIGIN must exactly match
the externally accessed origin; untrusted forwarded headers cannot override it.

Visit /operator/login. The password field is cleared after login, and a signed
15-minute HttpOnly, SameSite=Strict role cookie is issued (Secure under HTTPS).
This is isolated demo possession, not OIDC, independent humans or enterprise RBAC.
The lab binds loopback. Do not expose it to enterprise ingress. Each role has a
fixed server allowlist; client role text, Authorization and tenant query parameters
cannot grant capabilities. Reads and writes remain tenant-bound by the backend.

System Health reads native diagnostic timestamps rather than seeded platform
scores. Refresh failure retains old timestamps; stale checks become UNKNOWN.
Optional connectors remain NOT_CONFIGURED. LIVE means a local transport was probed;
incident fixtures and simulator effects remain SIMULATED. Refresh is explicit.

Chat stores only a conversation identifier in localStorage. Persisted assistant
metadata includes public tool provenance and measured usage when available, never
private model continuation. Clicking a citation rechecks incident scope and current
knowledge ACL/revocation. Missing provider configuration produces labeled fallback,
not a fake model turn. Unknown token/cost fields remain unknown. Same-turn retry
reuses the original request identity after ambiguous failure.

Monitoring controls allow operator-only pause/resume, bounded cadence, named rule
disables and time-limited notification suppression. Suppression never deletes
incidents. Governed actions require planner dry-run/proposal, exact approver review,
executor acknowledgement and a separate verifier sample. Acknowledgement alone does
not resolve an incident. Rollback is a separate named approved plan. No real restart,
arbitrary shell or enterprise action is exposed. Roles are capabilities, not proof
of different people. Existing legacy simulator controls cannot bypass governed plans.

Rollback the frontend route by unsetting INVESTINATOR_BACKEND (or selecting python),
setting API_INTERNAL_URL to the existing Python API and restarting only the owned
Next server. Retain Java's private schema and evidence; no reverse migration, data
reset or volume deletion is needed. Disable WEB_DEMO_ENABLED where no Java demo
session is needed. Do not point Python at Java-owned schemas.

Browser reproduction after the lab is ready, npm ci/build and Python dev install:

```
# Install the pinned Playwright Chromium, or explicitly set CHROMIUM_PATH.
npx --prefix apps/web playwright install chromium
python scripts/verify_frontend_compat.py
```

The runner starts disposable Python/Next processes and exercises actual browser
flows against both backends. It mutates only named SIMULATED ReplayLab fixtures in
the owned local lab, restores paused monitor state and leaves audit evidence intact.
It writes screenshots and sanitized results to .runtime/ui-acceptance. It never
records HAR, credential fields, cookies or provider traffic. MOCKED browser outages
are labeled separately from actual native traffic. No paid GPT call occurs.
