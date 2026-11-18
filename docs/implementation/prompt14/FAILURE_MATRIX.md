# Failure and isolation evidence

| Scenario | Actual provenance / status | Limit |
|---|---|---|
| Non-demo platform/business/Actuator exposure, demo token reuse | EnterpriseIngressTest PASS | No OIDC or private realm certified |
| Wrong tenant/forged evidence/KB ACL/revocation/approval spoof | Existing actual PG/native API JUnit tests PASS | External service tenant ACLs not certified |
| Request/context/tool/rate budget exhaustion | Official SDK + local WireMock PASS, includes seven-call rejection before tool execution and second-turn rate refusal before provider traffic | MOCKED_PROVIDER, no actual paid budget |
| Provider 429/401/404/503 and timeout | ChatGatewayTest safe taxonomy/no retry PASS | MOCKED_PROVIDER; actual GPT NOT_CONFIGURED |
| Queue saturation/hysteresis/dedupe/stale/late events | MonitoringIntegrationTest and prior 200-delivery lab PASS | SIMULATED events / actual PG |
| DB interruption, outbox rollback, parallel claims/execution races | Monitoring/RemediationIntegrationTest PASS | Actual local PG, simulator effects only |
| OpenSearch 403/corrupt log/service outage | Native evidence pipeline and P13 outage artifacts PASS | Slow-query-specific service experiment NOT_TESTED |
| Eight service interruptions/restarts | P13 actual owned lab PASS; browser revalidated for P14 | Service stop is not a complete host/network partition |
| Snapshot restore / separate owner | P14 actual schema dump + disposable restore PASS | Not production RTO/RPO/PITR/replica proof |
| Extended host/network partition, replica failover/read interruption, long-duration loss/load | NOT_TESTED; release BLOCKED, SRE/data owner | Requires independently scoped staging topology, duration and budget; no compressed simulation called a soak |
| Chunked body, split/invalid UTF-8, secure cookie public origin | Node tests PASS | Direct Java/gateway production perimeter requires separate certification |
| Baseline latency/JVM/queue/HTTP/usage | Bounded local read-only observations + metrics | Not production capacity, cost or availability |

No failure, missing telemetry, unknown usage, seeded score or mock response is
promoted to live success. Extended unavailable cases remain blockers, not waived tests.
