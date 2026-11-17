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

Prompt 04 updates: search/trace/metric transport is verified locally with simulated
inputs, so R7 is partially addressed for the lab only. OpenSearch 3.4.0 / official
Java 3.11.0 replaces unavailable Elasticsearch on this kernel (ADR-011). Reactive
transport dependencies and enabled Spring context are regression tested.

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R12 | High / SRE | bounded file buffers/retries can drop data on prolonged outage; create-only duplicate conflicts retry whole batches | monitor errors/drop counters and add production quarantine/replay coverage |
| R13 | High / security lead | local demo uses bundled OpenSearch security/certificates and private plaintext HTTP; no enterprise authenticated ingestion | identity/TLS/certificate/role/secret review before exposure; enterprise evidence mode disabled |
| R14 | Medium / data owner | native trace batches are stored separately but have no tenant-aware query API | keep index credentials private; add scoped trace adapters before exposing queries |

Critical Next.js R1 remains a release blocker. No Java dependency CVE audit or
production security signoff was performed; passing compatibility tests is not that audit.


Prompt 05/06 updates: local health/CLI, eight dependency outages, real KRaft
metadata/lag/auth and read-only authorization/persistence are verified. R7 is
further reduced for the isolated lab, not for private or production clusters.

| ID | Severity / owner role | Evidence and risk | Required mitigation / gate |
|---|---|---|---|
| R15 | High / connector owner | private live credentials/Kerberos/mTLS, full Kudu quorum/version and advanced service semantics unverified; optional Ranger unsupported | scope adapters to declared capabilities; independent live version/role/TLS validation before deployment |
| R16 | Medium / build owner | native Windows and cold-cache second-host CLI not run; optional Hive driver changes dependency surface | host-specific acceptance and dependency/security audit before release |
| R17 | High / SRE/security lead | demo Kafka controller listener remains private plaintext with lab-only authorization; search bundled demo security persists | no production exposure; provision separate authenticated/TLS controller and broker identities/ACLs through authorized deployment |

Prompt 06 fixes the Kafka image log-directory/named-volume mismatch and verifies
retained records/offsets across teardown. This is local fixture durability, not a
backup/restore or production disaster-recovery certification.
