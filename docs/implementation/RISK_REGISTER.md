# Risk register

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R1 | Critical / frontend maintainer | web audit exit 1, Next 16.3.8 | patched lockfile + build/regression/audit before release |
| R2 | Critical / security lead | fixed demo actor, PG trust | OIDC/RBAC, TLS/credentials, network isolation before enterprise exposure |
| R3 | High / AI lead | PR regex citations, no total deadline | typed membership, prompt injection and budget tests before port acceptance |
| R4 | High / SRE | platform missing metrics default healthy | freshness/UNKNOWN semantics and dependency failure tests |
| R5 | High / data owner | Alembic/Flyway and JSON/schema parity | one migration owner, backup/restore and compatibility tests before writes |
| R6 | High / platform lead | Prompt 02 pins, JDK21 wrapper verify and container smoke pass; live connector compatibility remains unverified | retain per-connector integration gates; JUnit6 compatibility decision in ADR-009 |
| R7 | High / integration lead | no live big-data/search proof | version-aware credential-scoped functional tests; label unconfigured |
| R8 | High / security + change owner | demo approvals are not independent auth | production executor stays absent; separate authorization required |
| R9 | Medium / UI/API lead | generic dict contracts, proxy drops identity/request headers | explicit contracts and routing/auth propagation tests |
| R10 | Medium / AI/data lead | deterministic confidence/runbooks, synthetic eval | human-reviewed causal gold set; no accuracy/business claims |
| R11 | Medium / build owner | Python minimum-only dependencies | reproducible lock strategy and Python dependency audit before release |

Owner roles are proposed responsibilities, not assignments to named people.
