# Five-layer threat model — 2026-11-18

Scope: Java modular monolith, Python compatibility API, Next console and owned lab.
This is an engineering review, not an independent penetration-test attestation.
No production environment, identity provider or private connector has been certified.

| Layer / boundary | Threat | Current control / verification | Residual production gate |
|---|---|---|---|
| Infrastructure: collector -> search | Spoofed tenant/time, corrupt payload, log secrets, malicious telemetry | Java normalization/redaction and pipeline required fields; actual corrupt-log rejection, scoped reader/writer 403 and tenant negatives | Authenticate source identities; tenant from trusted ingestion principal; retention, encrypted backups, network policy |
| Infrastructure: network/DB | HTTP interception, public listeners, shared schema owner, data loss | Loopback host ports; private Flyway schema; refuse adopting public/Alembic schema; dump/restore fixture | TLS/mTLS, least-privilege runtime vs migrator roles, PITR, encrypted offsite backup, DR and restore drills |
| Model: provider egress | Key leakage, arbitrary endpoints, runaway spend, private reasoning exposure | Backend-only key, HTTPS configured host allowlist, no redirect capability, redaction, encrypted continuation, store=false; bounded requests/output/context/time/tools/concurrency/rate tests | Distributed durable financial caps, key rotation proof, privacy/legal review, approved prices and live budget |
| Model: response -> tools | Invented approval, wrong citation/source mode, repeated tool IDs | Strict response/argument schemas, exact scoped citations and current ACL/revocation; SDK MOCKED adversarial/429/timeout tests | Independent held-out human quality review; real provider preflight remains NOT_CONFIGURED |
| Platform: connector | SSRF, caller-chosen query/SQL/URL, Kerberos delegation, stale credentials | Fixed GET/RPC reads, configured endpoints, HTTPS outside loopback fixtures, redirects denied, bounded responses, fixed SQL, mutual SPNEGO/no delegation | Configured endpoint is a trusted-admin boundary, not a network firewall; real realms/certs/refresh and per-target principals unverified |
| Platform: API/metrics | Tenant spoofing, demo token mistaken for enterprise identity, data exposed via health/Actuator | Server-bound demo tenant; non-demo business/platform routes denied even with demo capabilities; only minimal probes public, metrics bearer-scoped | OIDC/RBAC not implemented: enterprise application ingress HARD-BLOCKED; direct Java JSON body/rate perimeter still needs gateway validation |
| Agent: retrieval -> investigation | Runbook injection, stale/revoked advice promoted to fact | Untrusted knowledge, quarantine/review separation, BM25 tenant/version/ACL revalidation, historical-advice labels | Licensing/source approval and qualified reviewer sign-off pending; no production causal-accuracy claim |
| Agent: incident -> recovery | Forged actor, replayed approval, modified target, race/partial acknowledgement | Fixed ReplayLab actions, immutable hash/version, separate role capabilities, expiry, receipt/fencing, independent sample, separate rollback; integration/restart negatives | Possession of tokens is not independent people; no enterprise executor registered; external change control required |
| Application: browser -> Next | CSRF, session forgery, XSS, client secret leak, oversized chunked bodies | Exact configured origin, HttpOnly SameSite Strict 15-minute signed cookie, HTTPS-public-origin Secure flag, React escaping, fixed proxy roles, byte-counting stream cancellation; tests | Real identity/session revocation, TLS ingress headers and rotation must be verified before exposure |
| Cross-layer audit | Sensitive logs, tampered audit, excess retention/cardinality | Structured/redacted logging, public metadata only, low-cardinality metrics; no prompt/tenant/incident/model labels | DB admin remains trusted; no immutable external audit, approved retention/erasure or legal-hold policy |

Trust assumptions: local Docker daemon and deployment configuration administrators
are trusted; demo credentials are private, distinct and generated. A malicious admin
can alter host/network configuration. TLS syntax/config validation is not certificate
or Kerberos realm certification. Loopback does not isolate hostile local processes.
Python's demo APIs and trust-auth database remain demo-only; do not expose the base
Compose network as enterprise infrastructure. No tool-selected production command,
SQL write, arbitrary network destination or automatic paid investigation is enabled.
