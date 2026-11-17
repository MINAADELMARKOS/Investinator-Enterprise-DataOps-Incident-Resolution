# Prompt 09B — Audit of Prompt 05 operational evidence

Status: **PRESENT / VERIFIED AGAINST ORIGINAL COMMIT**, not UNKNOWN.
The actual repository contains Prompt 05's report and all six companion artifacts;
the roadmap's authoring-time statement that its report was not supplied is historical.
[Artifact hashes](../prompt09b/prompt05-artifact-audit.json) bind every file to original
Prompt 05 commit `b14dd6f` and confirm unchanged contents.

| Artifact | Inspection |
|---|---|
| REPORT.md | Actual bounded read-only health engine, CLI/lifecycle, source-mode and production limitations |
| test-summary.json | Historical 36 Java / 10 CLI tests, no Java skips; not presented as today's rerun |
| health-report.schema.json | Original JSON Schema preserved; historical and current doctor reports validate |
| doctor-example.json | Historical actual local probe example with explicit statuses/provenance |
| failure-matrix.json | Eight stopped-service rows, all detected with nonzero exit and no cascading restarts |
| lifecycle-result.json | Historical fresh-volume and unchanged-rerun acceptance, stable container IDs and volume-preserving down |
| smoke.txt | Historical live local API/search/metrics/proxy plus SIMULATED approved recovery |

The seven **external-dependency** stops preserve Java liveness. The intentional
Java-process stop records liveness_independent=false and uses CLI-local fallback;
there is no running Java process to call healthy. An initial audit assertion
incorrectly required true for all eight rows; inspection corrected that assumption
and the explicit per-service check passes. A schema check initially used a virtual
environment without jsonschema; rerunning with the installed system validator
passes. No historical result was edited.

Current 09B [doctor](../prompt09b/doctor.json) and
[health](../prompt09b/health.json) are actual running-lab observations; doctor
success=true, partial=false, original health schema valid. This does not turn
SIMULATED incident/log/span data into production evidence. No outage or fresh-volume
reset was rerun in 09B: the original eight-stop matrix is historical evidence,
and current schema/probe verification is a separate measurement.

The CLI remains loopback/owned-project scoped, serialized, idempotent on unchanged
up, bounded and read-only for diagnostics. Named local demo actions are separate.
No production restart, data reset, volume deletion or paid provider call occurred.
Native Windows and second-host cold-cache operational acceptance remain NOT_TESTED.

## Security advisory reconciliation

Prompt 03, 04, 05, 06 and later original reports accurately describe their earlier
Next.js 16.3.8 audit failure. Those dated records are preserved. Current main at
`ef0268b` contains Next.js **16.4.5**; local apps/web package.json **and lockfile**
match main exactly, including main's security commit `44d18d8`. Fresh 09B
`npm audit --audit-level=high --json` exits 0 with zero vulnerabilities; see
[web-audit.json](../prompt09b/web-audit.json). 09A also verified clean install,
build/type checks and successful hosted frontend CI. The old Next.js finding is
**cleared for this lockfile**, not a current release blocker.

Production identity/RBAC, TLS/role review, private connector certification,
backup/restore, actual provider validation and human-reviewed RCA quality remain
separate gates. A clean npm audit is not a Java CVE audit or production sign-off.
