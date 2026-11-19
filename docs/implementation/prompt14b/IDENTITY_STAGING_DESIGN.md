# Offline staging identity experiment — NOT OIDC integration

StagingJwtValidator is an explicitly constructed, disabled-unless-requested pure
validator. It is NOT a Spring security filter, does not change any route, and
cannot grant enterprise or write authority. No issuer, discovery document, JWK URL,
private certificate or real identity was configured. Current APIs still use the
existing demo capabilities or non-demo deny rule. Do not call these capabilities OIDC.

The experiment pins a deployment-supplied >=2048-bit RSA key/kid and RS256, rejects
untrusted algorithm/key headers and duplicate JSON fields, bounds token size/life,
and validates issuer, audience, signature, integer expiry/issued-at/not-before,
role, scope and server-bound tenant. Only investinator:read with incident-reader
can qualify. It rejects the proposer's same subject and single-use jti replay;
the bounded 1024-entry replay map FAILS CLOSED on capacity and expires entries.
Every returned principal explicitly has production_authority=false.

Tests use an ephemeral generated RSA key and fixture issuer/subjects, never human
identities. Six cases including existing enterprise ingress tests pass. This is
crypto/claim functional evidence, not IdP or independent-human certification.

Before any route integration: identity/security owners must approve a standard
resource-server/JOSE integration, exact issuer/discovery/key rotation, TLS trust,
accepted audience/claim mapping, tenant binding, token revocation/session semantics,
shared durable replay/nonce and real independent subject separation. An access token
is normally reusable; this single-use experiment is only a staging preflight and
must NOT silently become an OAuth access-token policy. Its local replay cache does
not survive restarts or coordinate replicas. OIDC browser code/PKCE/session/CSRF,
enterprise RBAC and change-control integration remain NOT_IMPLEMENTED/BLOCKED.

Rollback: remove unused staging class/tests; no DB, runtime trust or routing change.
Production execution remains hard-denied regardless of validator output. P14-01
cannot become PASS from this experiment or from a coding-agent review.
