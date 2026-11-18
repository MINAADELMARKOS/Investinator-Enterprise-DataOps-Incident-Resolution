#!/usr/bin/env python3
"""Read-only backup of this owned demo; restore ONLY into a new disposable database."""
import argparse
import hashlib
import importlib.util
import json
import os
import re
import secrets
import subprocess
import tempfile
import time
from pathlib import Path

spec = importlib.util.spec_from_file_location("cli", Path(__file__).with_name("investinator_cli.py"))
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)
IMAGE = "postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=cli.RUNTIME / "backup-restore.json")
    args = parser.parse_args()
    state = cli.load_state()
    source = cli.compose(state, "ps", "-q", "db", capture=True)
    if not re.fullmatch(r"[a-f0-9]{12,64}", source):
        raise RuntimeError("Owned database is not running")
    target = "investinator-restore-check-" + secrets.token_hex(6)
    env = os.environ.copy()
    env["POSTGRES_PASSWORD"] = secrets.token_urlsafe(32)
    created = False
    started = time.monotonic()

    def sql(container, statement):
        return cli.run([*cli.docker(), "exec", container, "psql", "-U", "investinator_java", "-d", "investinator_java", "-Atc", statement], capture=True)

    def fingerprint(container):
        names = sql(container, "select tablename from pg_tables where schemaname='investinator_java' order by tablename").splitlines()
        result = {}
        for name in names:
            if not re.fullmatch(r"[a-z0-9_]+", name):
                raise RuntimeError("Unexpected fixture table identifier")
            value = sql(container, "select count(*),md5(coalesce(string_agg(row_to_json(t)::text,E'\\n' order by row_to_json(t)::text),'')) from investinator_java.\"" + name + "\" t")
            count, digest = value.split("|")
            result[name] = {"rows": int(count), "md5_consistency_only": digest}
        return result

    try:
        before = fingerprint(source)
        if "flyway_schema_history" not in before:
            raise RuntimeError("Actual Java-owned schema is required")
        with tempfile.TemporaryDirectory(prefix="investinator-private-backup-") as directory:
            backup = Path(directory) / "snapshot.dump"
            with backup.open("xb") as file:
                backup.chmod(0o600)
                subprocess.run([*cli.docker(), "exec", source, "pg_dump", "-U", "investinator_java", "-d", "investinator_java", "-n", "investinator_java", "--no-owner", "--no-acl", "-Fc"], stdout=file, stderr=subprocess.PIPE, check=True, timeout=60)
            cli.run([*cli.docker(), "run", "-d", "--name", target, "-e", "POSTGRES_USER=investinator_java", "-e", "POSTGRES_DB=investinator_java", "-e", "POSTGRES_PASSWORD", IMAGE], env=env, capture=True)
            created = True
            cli.wait(lambda: subprocess.run([*cli.docker(), "exec", target, "pg_isready", "-h", "127.0.0.1", "-U", "investinator_java"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0, 45)
            sql(target, "create table public.alembic_sentinel(value text);insert into public.alembic_sentinel values('PRESERVE_SEPARATE_OWNER')")
            with backup.open("rb") as file:
                subprocess.run([*cli.docker(), "exec", "-i", target, "pg_restore", "-U", "investinator_java", "-d", "investinator_java", "--no-owner", "--no-acl", "--exit-on-error"], stdin=file, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, check=True, timeout=60)
            restored = fingerprint(target)
            after = fingerprint(source)
            assert before == after, "Source changed during check; do not claim snapshot equality"
            assert restored == before, "Restored table content differs"
            assert sql(target, "select value from public.alembic_sentinel") == "PRESERVE_SEPARATE_OWNER"
            report = {"status": "PASS", "provenance": "actual owned PostgreSQL schema dump and disposable restore; SIMULATED demo data", "tables_verified": len(restored), "rows_verified": sum(v['rows'] for v in restored.values()), "backup_sha256": hashlib.sha256(backup.read_bytes()).hexdigest(), "isolated_fixture_elapsed_seconds": round(time.monotonic()-started, 3), "source_writes": 0, "separate_public_owner_preserved": True, "schema_content_equal": True, "production_rto": "NOT_MEASURED", "production_rpo": "NOT_MEASURED", "encrypted_offsite_backup": "NOT_IMPLEMENTED", "temporary_backup_removed": True}
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2)+"\n")
        print(json.dumps(report))
    finally:
        if created:
            cli.run([*cli.docker(), "rm", "-f", "-v", target], capture=True)


if __name__ == "__main__":
    main()
