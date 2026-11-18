#!/usr/bin/env python3
"""Disposable PostgreSQL WAL recovery to a named restore point; never touches the retained lab."""
import argparse
import json
import os
import secrets
import subprocess
import time
from pathlib import Path

IMAGE = "postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if os.getenv("DOCKER_HOST", "unix:///var/run/docker.sock") != "unix:///var/run/docker.sock":
        raise RuntimeError("Only local disposable Docker is permitted")
    prefix = "investinator-pitr-fixture-" + secrets.token_hex(6)
    source, target = prefix + "-source", prefix + "-target"
    volumes = [prefix + suffix for suffix in ("-source-data", "-archive", "-backup")]
    made_volumes, made_containers = [], []
    env = {**os.environ, "POSTGRES_PASSWORD": secrets.token_urlsafe(32)}
    start = time.monotonic()
    report = {"status": "FAIL", "source_mode": "SIMULATED_DISPOSABLE_DATABASE",
              "production_source_writes": 0, "retained_lab_touched": False,
              "offsite_encryption_ha_certificate": "NOT_TESTED", "rpo_rto_certificate": "NOT_APPROVED"}

    def run(argv, timeout=60):
        p = subprocess.run(argv, env=env, text=True, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE, timeout=timeout, check=False)
        if p.returncode:
            # No raw database output, process environment or backup content in artifacts.
            raise RuntimeError("Owned fixture command failed: " + argv[0])
        return p.stdout.strip()

    def sql(container, statement):
        return run(["docker", "exec", container, "psql", "-U", "pitr_fixture", "-d", "pitr_fixture", "-Atc", statement])

    def wait_ready(container, recovered=False):
        deadline = time.monotonic() + 40
        while time.monotonic() < deadline:
            try:
                # The final TCP listener avoids PostgreSQL init's temporary Unix server.
                run(["docker", "exec", container, "pg_isready", "-h", "127.0.0.1", "-U", "pitr_fixture"])
                if not recovered or sql(container, "select pg_is_in_recovery()") == "f":
                    return
            except RuntimeError:
                pass
            time.sleep(0.5)
        raise RuntimeError("Owned fixture recovery readiness deadline")

    try:
        for volume in volumes:
            run(["docker", "volume", "create", "--label", "investinator.scope=disposable-pitr-fixture", volume])
            made_volumes.append(volume)
        run(["docker", "run", "-d", "--network", "none", "--name", source,
             "--label", "investinator.scope=disposable-pitr-fixture", "-e", "POSTGRES_PASSWORD",
             "-e", "POSTGRES_USER=pitr_fixture", "-e", "POSTGRES_DB=pitr_fixture",
             "--mount", f"type=volume,src={volumes[0]},dst=/var/lib/postgresql/data",
             "--mount", f"type=volume,src={volumes[1]},dst=/archive",
             "--mount", f"type=volume,src={volumes[2]},dst=/basebackup", IMAGE,
             "postgres", "-c", "wal_level=replica", "-c", "archive_mode=on",
             "-c", "archive_command=cp -n %p /archive/%f", "-c", "max_wal_senders=3"])
        made_containers.append(source)
        wait_ready(source)
        run(["docker", "exec", "-u", "root", source, "chown", "postgres:postgres", "/archive", "/basebackup"])
        sql(source, "create schema python_owner; create table python_owner.sentinel(id int primary key); insert into python_owner.sentinel values(7); create schema java_owner; create table java_owner.events(id int primary key); insert into java_owner.events values(1)")
        run(["docker", "exec", "-u", "postgres", source, "pg_basebackup", "-h", "127.0.0.1", "-U", "pitr_fixture", "-D", "/basebackup", "-Fp", "-X", "stream", "-c", "fast"], timeout=90)
        sql(source, "insert into java_owner.events values(2)")
        sql(source, "select pg_create_restore_point('p14b_fixture_restore')")
        sql(source, "insert into java_owner.events values(3)")
        sql(source, "select pg_switch_wal()")
        deadline = time.monotonic() + 30
        archived = 0
        while time.monotonic() < deadline:
            archived = int(sql(source, "select archived_count from pg_stat_archiver"))
            if archived >= 2:
                break
            time.sleep(0.5)
        if archived < 2:
            raise RuntimeError("Owned WAL archival deadline")
        # Static recovery configuration only; no client/model command or SQL is accepted.
        recovery = "restore_command = 'cp /archive/%f %p'\nrecovery_target_name = 'p14b_fixture_restore'\nrecovery_target_action = 'promote'\n"
        run(["docker", "exec", "-u", "postgres", source, "sh", "-c",
             "printf '%s' \"$1\" >> /basebackup/postgresql.auto.conf; touch /basebackup/recovery.signal", "fixture", recovery])
        run(["docker", "run", "-d", "--network", "none", "--name", target,
             "--label", "investinator.scope=disposable-pitr-fixture",
             "--mount", f"type=volume,src={volumes[2]},dst=/var/lib/postgresql/data",
             "--mount", f"type=volume,src={volumes[1]},dst=/archive,readonly", IMAGE,
             "postgres", "-c", "archive_mode=off"])
        made_containers.append(target)
        wait_ready(target, recovered=True)
        restored = sql(target, "select string_agg(id::text,',' order by id) from java_owner.events")
        sentinel = sql(target, "select id from python_owner.sentinel")
        original = sql(source, "select string_agg(id::text,',' order by id) from java_owner.events")
        if (restored, sentinel, original) != ("1,2", "7", "1,2,3"):
            raise RuntimeError("WAL target or separate-owner preservation failed")
        report.update(status="PASS_FUNCTIONAL_FIXTURE_ONLY", restored_event_ids=[1, 2],
                      excluded_later_event=3, separate_owner_sentinel=7,
                      archived_segments=archived, recovery_in_progress=False)
    except Exception as error:
        report["failure_class"] = type(error).__name__
        raise
    finally:
        cleanup_ok = True
        for name in reversed(made_containers):
            cleanup_ok &= subprocess.run(["docker", "rm", "-f", name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0
        for name in reversed(made_volumes):
            cleanup_ok &= subprocess.run(["docker", "volume", "rm", name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0
        report.update(elapsed_seconds=round(time.monotonic() - start, 3),
                      cleanup_test_owned_only=cleanup_ok, backups_or_credentials_exported=False)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2) + "\n")
        if not cleanup_ok:
            raise RuntimeError("Owned fixture cleanup incomplete; inspect named test resources")
    print("PASS disposable WAL restore point; not production HA/RPO/RTO certification")


if __name__ == "__main__":
    main()
