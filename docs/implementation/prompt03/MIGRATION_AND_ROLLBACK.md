# Persistence ownership and rollback

Python remains the default backend and Alembic remains its migration owner. Java
uses a separate PostgreSQL database/schema (`investinator_java` by default), not
Python's existing SQLite file or public/Alembic tables. V1 derives the 13 operational
tables/indexes/foreign keys from bf50bd87cb88 and the inspected ORM PostgreSQL DDL.
V2 adds request_keys and execution_records, both additive. JSON incident payloads
retain the existing API shape; source, time, tenant and simulation metadata are
preserved in payloads and operational records.

Flyway validates version checksums, refuses baseline-on-migrate, disables clean and
refuses a nonempty schema without Flyway history, an Alembic-owned schema, public or
system schemas. Do not bypass these checks to attach existing Python data. Only
fresh isolated databases are used by automated tests. Existing customer-data import
and schema ownership transfer are NOT performed or authorized by this stage.

## Forward/backward compatibility

V1 contains the Python-compatible columns/types. V2 is additive and does not remove
or rename V1 fields. JDBC targets its configured schema. Each command, audit record,
approval, verification and idempotency response commits in one transaction. Tenant
advisory locks serialize mutations; unique keys reject duplicate approval/execution.
Idempotency-Key is optional, bounded and scoped to tenant plus route; changed payload
under the same key returns 409. Stored response reuse survives process restarts.

For a future real data handover: inventory the source revision; snapshot/verify a
logical backup and restore into a disposable database; compare schema and row/tenant
counts, foreign keys and payloads; validate migration ownership with one writer;
rehearse forward/backward contracts and failure recovery; review the concrete import
and cutover before running it. No destructive rollback SQL is supplied.

## Routing and rollback

Next.js is unchanged and defaults to Python. To evaluate only the complete local
deterministic Java demo on a dedicated dataset, set backend-only API_INTERNAL_URL to
the Java address when starting Next.js. Do not mix Java incident IDs with Python
chat/action routes: they own separate databases. A provider-enabled Python workflow
must remain on Python until the Java provider stage is accepted.

Rollback is a routing change: restore API_INTERNAL_URL to Python and restart only
the Next.js process you started; stop only java-api if needed. Preserve both databases
and the `java_postgres_data` volume. Never run Alembic and Flyway against the same
schema; never use `down -v`. Do not delete Flyway versions to undo code. Restoring
actual user data needs a separately reviewed backup/recovery action.

## Local commands

Build with `./backend-java/mvnw -B -f backend-java/pom.xml verify` (JDK21 and Docker).
Run the disposable restart/outage gate with
`python3 backend-java/scripts/verify_persistence.py`. This uses the local managed
Docker socket and removes only its own randomly named disposable test container.
No existing services, volumes or application database are touched.

For retained local demo data, supply JAVA_DATABASE_PASSWORD securely via the process
environment or an ignored local env file, then:

```bash
docker compose -f docker-compose.yml -f compose.java.yaml -f compose.persistence.yaml --profile java --profile java-persistence up --build --wait --wait-timeout 90 java-db java-api
```

The database has no published host port. Java publishes loopback only. This is a
local demo with fixed server identity, not authenticated enterprise tenancy. No
production infrastructure executor exists. All ACT-DEMO writes modify simulated
telemetry only after a stored approval and require metric-based verification.
