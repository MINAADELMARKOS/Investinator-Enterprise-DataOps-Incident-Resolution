# Configuration and provider preflight

Never copy real values into Git, tickets, screenshots, command transcripts or this
package. The [generated inventory](source-inventory.json) records names/source paths
only. Inspect the owning service configuration; a Python setting is not automatically
a Java setting. The native health JSON is authoritative, not the pasted illustrative
contract or seeded platform score. No production secret store or identity is configured.

| Group | Names / effect |
|---|---|
| Routing | INVESTINATOR_BACKEND unset/python -> Python; explicit java -> Java. API_INTERNAL_URL defaults Python:8000; JAVA_API_INTERNAL_URL defaults Java:8081 on host. Owned lab injects internal service URLs. |
| Java profiles | SPRING_PROFILES_ACTIVE local (default), demo, integration, enterprise. JAVA_BIND_ADDRESS=127.0.0.1, JAVA_PORT=8081 outside containers. Enterprise disables demo and requires JAVA_TLS_BUNDLE; no real TLS/OIDC certificate is verified. |
| Ownership | JAVA_PERSISTENCE_ENABLED=false by default; lab=true. JAVA_DATABASE_URL, JAVA_DATABASE_USER, JAVA_DATABASE_PASSWORD and JAVA_DATABASE_SCHEMA (investinator_java). Never target public/Alembic data. |
| Next demo | WEB_DEMO_ENABLED=true only for an owned demo; UI_PUBLIC_ORIGIN exact externally accessed origin; UI_SESSION_SECRET >=32 characters; distinct UI_{VIEWER,OPERATOR,PLANNER,APPROVER,EXECUTOR,VERIFIER}_LOGIN_TOKEN >=24. Server-only capabilities remain private. HTTPS origin requires Secure cookies even behind TLS termination. |
| Diagnostics | HEALTH_API_TOKEN, CONNECTOR_API_TOKEN, INVESTIGATION_API_TOKEN, GRAPH_ADMIN_TOKEN, METRICS_TOKEN. Lab generates them; graph-admin is not passed to Next. |
| Knowledge | KNOWLEDGE_{WRITE,REVIEW,READ,FEEDBACK}_TOKEN; reader principal server-bound; index writer credentials separate. Next receives read capability only. Current review state/ACL is rechecked. |
| Recovery | REMEDIATION_{PLAN,APPROVE,EXECUTE,VERIFY}_TOKEN; distinct roles, ReplayLab only. These are possession capabilities, not independent human identities. |
| Search | EVIDENCE_SEARCH_ENABLED, EVIDENCE_SEARCH_URL, EVIDENCE_SEARCH_USER/PASSWORD, EVIDENCE_API_TOKEN, EVIDENCE_INDEX. Java lab uses authenticated OpenSearch 3.4.0, not the older optional Python Elasticsearch profile. |
| Monitoring | INVESTINATOR_MONITOR_ENABLED=false by default; set true when starting the demo lab. Worker is initially paused. ./investinator monitor status/pause/resume --demo; overrides bounded in UI. No automatic paid investigation or external notification transport. |
| Ports | LAB_API_PORT=18081, LAB_WEB_PORT=13000, LAB_SEARCH_PORT=19200, LAB_PROM_PORT=19090, LAB_KAFKA_PORT=19092, LAB_OTLP_PORT=14318, LAB_OTEL_HEALTH_PORT=13134; choose before first up, then persisted privately. |
| Provider | OPENAI_API_KEY, INVESTINATOR_OPENAI_MODEL, INVESTINATOR_OPENAI_ENABLED=false, optional FAST_MODEL/REASONING_MODEL, INVESTINATOR_MAX_AI_CALLS=3, INVESTINATOR_CHAT_STATE_KEY (base64 32-byte key). Lab generates continuation key; never share it. |
| Provider egress | Default https://api.openai.com/v1; configured HTTPS allowed hosts only, no arbitrary caller URL. Loopback TEST_ENDPOINT is fixture-only. SDK retries=0; no credentials in URL/proxy. |
| Tracing | Default SDK/exporters disabled. Existing OTel starter and bounded Java investigation spans support reviewed configuration; certify sampling/context/PII/exporter/receiver before enabling staging tracing. In-memory span tests and synthetic OTLP transport are separate evidence. |

## Connector onboarding — read-only first

CONNECTOR_ALLOWED_SERVICES restricts the registry; enabled services use
CONNECTOR_<UPPERCASE_SERVICE_WITH_UNDERSCORES>_* settings. REST families use URL,
AUTH (BEARER/BASIC/MTLS/SPNEGO), TOKEN or USER/PASSWORD and bounded/version settings.
Use only deployment-owned HTTPS endpoints; redirects are denied. Loopback HTTP
requires explicit LOCAL_FIXTURE. SPNEGO uses configured JVM login context, mutual
authentication and no delegation; keystores/trust stores belong in approved secret
mounts. Static environment credentials do not implement token refresh or enterprise
principal certification. Never pass passwords in SQL/URLs/model messages.

Native Kafka uses KAFKA_BOOTSTRAP_SERVERS, KAFKA_SECURITY_PROTOCOL,
KAFKA_SASL_MECHANISM and KAFKA_SASL_JAAS_CONFIG from the backend environment. Only
owned lab setup seeds records/ACLs; diagnostic identity cannot produce/create topics.
Metadata mode determines KRaft versus legacy; do not infer ZooKeeper for Kafka 4.x.
Hive JDBC is optional `-Phive-jdbc`, with fixed reads and a separately reviewed
standalone driver. Real realm/cert/refresh/multi-tenant negatives are still blocked.
See [certification matrix](../implementation/prompt14/AUTH_MATRIX.md) and
[connector-specific configuration](../implementation/prompt06/CONNECTORS.md).

## OpenAI billing and honest live status

A ChatGPT subscription does not configure or pay for this application's OpenAI API.
An account owner must provide a valid backend-only key, a permitted model ID and an
independently approved testing budget. No prices, savings or provider access are
assumed. The [FinOps model](../implementation/prompt14/FINOPS.json) leaves rates/cost
unknown until approved and measured; per-process bounds are not a durable monetary cap.

Prompt 14B adds a [durable USD quota/reservation ledger](../implementation/prompt14b/FINOPS.md).
It seeds no prices, quota or approval. Missing approval/opt-in/price/global or tenant
quota fails closed before a real request. Timeout/unknown usage retains the full hold.
The earlier per-process-only limitation is historical for Java; invoice and actual
provider/owner certification remain blocked.

Safe first check: `./investinator ai-check` inspects presence and cost-approval
configuration only, with ZERO provider calls. Standalone `--live` intent also cannot
bill: it reports NOT_CONFIGURED or BLOCKED_DURABLE_LEDGER_REQUIRED. Credential
validity remains NOT_TESTED. Separately approved real chat must use the backend
ledger and owner-provisioned policy, not a CLI fixture field or a standalone probe.

For approved Java chat, inject key/model, explicitly enable the provider on the Java
service, and restart only the owned lab API through normal up. Do not project your
terminal while exporting secrets. Ask two incident-scoped turns in the UI, inspect
read-tool names/citations, reload history, and verify mode=gpt with provider_status=OK.
Fault source must still say SIMULATED. Missing key/model produces deterministic
fallback; 429/outage/timeouts produce safe distinct statuses, not pretend model text.
Never relabel an SDK WireMock test as a live provider call. Fix mode cannot approve.

Compose reads its environment/.env; generic Python/Java processes do not automatically
load a copied .env. Use trusted shell/service secret injection. The CLI private lab
file is not a general .env file and never contains a provider key generated by setup.
