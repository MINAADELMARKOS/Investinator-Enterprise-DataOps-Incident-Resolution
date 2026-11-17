# Safely publish the local Java implementation without losing Codex work

**Verified remote starting state, 2026-11-17:** `main` contains Python + Next.js + GPT/Elasticsearch read-only demo. Java `backend-java/` and `feature/java-ai-audit` are **not on remote GitHub** as of this design update. The user reports Java Prompt 03–09 commits in a separate **local** repository/worktree. A remote agent cannot push work that it cannot access.

## Preparation (run in the checkout that actually contains backend-java)

```bash
pwd
git status --short
git remote -v
git branch -vv
git log --oneline --decorate -15
test -d backend-java && echo "Java worktree present" || echo "WRONG WORKTREE"
git ls-files backend-java | head -30
```

Stop immediately if the checkout lacks the intended Java changes. If uncommitted changes exist, carefully commit them on the feature branch or stash with a descriptive label; never discard them or add secret files.

## Preserve and reconcile

```bash
git fetch origin --prune
git switch feature/java-ai-audit
git branch backup/java-ai-audit-before-remote-sync
git status --short
git log --oneline --left-right --cherry-pick origin/main...HEAD
```

Review pending merge differences and the security patches in `origin/main`.

```bash
git merge --no-ff origin/main
```

If conflicts occur, inspect and resolve **each** intentionally:
- `backend-java/` + Flyway migrations: preserve separately owned schema and Java-specific APIs.
- `src/incident_copilot/` + Alembic: preserve the legacy Python code and its existing externally reachable routes.
- `apps/web/` + `package-lock.json`: keep the verified Next.js 16.4.5 upgrade and do not hand-edit the npm lockfile.
- `docker-compose.yml`: keep Python as default and Java behind opt-in profiles or explicit routing, remove ambiguous port collisions.
- `docs/`: keep both the verified main documentation and local Java Prompt 03–09 reports; accurately mark simulation and provider status.
- `.github/workflows`: keep Python + frontend checks; add real Java + CLI verification without weakening audit steps.

Never run `git reset --hard`, force push, auto-resolve all conflicts using `--ours`/`--theirs`, delete DB volumes, or assume all Python and Java domain definitions can share Flyway migration ownership.

## Verify and publish (commands only after inspection)

```bash
./backend-java/mvnw -B -f backend-java/pom.xml verify
python3 -m pytest -q
python3 -m ruff check src tests
python3 -m mypy src
( cd apps/web && npm ci && npm run build && npm audit --audit-level=high )
docker compose config --quiet
```

Run the local Java CLI's real reported health/doctor/smoke syntax as documented by the Prompt 05 implementation, and rerun tests for the available separate Compose profiles. Preserve any environment-specific Maven proxy/certificate options from `backend-java/README.md`. Do **not** bypass TLS validation or tests to make the build green.

Only when all checks pass (or each missing external dependency is properly categorized):

```bash
git status --short
git diff --check
git push -u origin feature/java-ai-audit
```

Create a GitHub pull request from `feature/java-ai-audit` into `main`, **do not merge automatically**, and verify hosted CI runs the Java Maven/CLI jobs. Add resulting logs and PR details to `docs/implementation/SYNC_REPORT.md`.

## What cannot be recovered by GitHub integration alone

A locally committed branch that has **never been pushed** is not available through the GitHub API. The remote repo can be fully green while this work is still missing. Push from the real Codex workspace or transfer its Git branch/patch into an accessible worktree. Never fabricate a replacement implementation from the stage reports.

## Prerequisite for Prompt 10

Prompt 09A must show a remote `feature/java-ai-audit` head, successful CI for available validations, and a resolved conflict ledger. Prompt 09B's independent human review may remain pending, explicitly blocking measured RCA accuracy claims. Prompt 10 development is allowed only behind the appropriate feature flags and safety boundaries.
