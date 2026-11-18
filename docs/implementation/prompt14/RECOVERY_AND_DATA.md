# Data, retention and recovery review

Python/Alembic and Java/Flyway have separate schema ownership. Java rejects public,
reserved, Alembic-owned or unidentified existing schemas; Flyway clean is disabled.
The backup test reads only the owned local demo and restores all Java-owned tables
into a newly created disposable PostgreSQL instance. Counts and canonical row
fingerprints match; an existing public sentinel representing another owner survives.
The private temporary custom-format dump is deleted; reports expose only SHA256,
counts and elapsed fixture time. Actual source writes: zero. This is not a production
backup, PITR proof, production RTO/RPO, HA replica failover or disaster-recovery sign-off.

Production remains BLOCKED until encrypted offsite/versioned backups, key ownership,
WAL/archive/PITR, immutable retention, restoration of both independently owned schemas,
search reindex/rebuild, attachment/projection consistency and regular timed restore
exercises are independently authorized and verified. Snapshot equality requires a
quiescent source for comparison; the dump itself is transactionally consistent.

Review-only retention: evidence seven-day deletion policy requires explicit operator
confirmation in the evidence bootstrap; do not activate deletion automatically.
Conversations, audit, citations, monitor queues/health samples, postmortems and
knowledge revision retention/erasure/legal hold are not implemented as enterprise
policies. Database administrators can alter stored audit. Logs/redaction reduce
accidental exposure but are not a DLP or licensing review. Knowledge source licensing,
PII classification/consent and external model transfer/retention require data/legal
approval; store=false and encrypted continuation alone are insufficient.

Lab base Compose still uses demo PostgreSQL trust and internal plaintext transports.
P14 restricts host binds to loopback; this is not TLS/mTLS or database-user isolation.
Do not use base Compose for enterprise ingress. Enterprise Java profile requires
an external TLS bundle and disables demo mode; no supplied bundle or identity is
certified. Do not rotate keys or perform infrastructure failover in production from
this assessment. Named ReplayLab rollback is separately approved simulation only.

Reproduce only in the owned local lab after `./investinator up`:

```
python scripts/verify_backup_restore.py --output .runtime/backup-restore.json
python scripts/capture_operational_baseline.py --samples 30 --output .runtime/operational-baseline.json
```

The restore target name is generated internally; no production/source override is
accepted. The source ownership marker must match this checkout. Private dumps
never leave the temporary directory or enter CI artifacts. CI also checks these
operations after its isolated laboratory and before volume-preserving down.
