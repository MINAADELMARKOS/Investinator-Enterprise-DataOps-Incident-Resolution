#!/usr/bin/env python3
"""Actual isolated Java/PostgreSQL restart, approval, verification and named rollback."""

import argparse
import importlib.util
import json
import os
import secrets
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "fixture",
    Path(__file__).resolve().parents[1] / "backend-java/scripts/verify_persistence.py",
)
fixture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixture)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=fixture.ROOT / "docs/implementation/prompt11/process-acceptance.json")
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    name = "investinator-recovery-check-" + uuid.uuid4().hex[:10]
    tokens = {
        role: secrets.token_urlsafe(24)
        for role in ("PLAN", "APPROVE", "EXECUTE", "VERIFY")
    }
    env = os.environ.copy()
    env.update(
        JAVA_PERSISTENCE_ENABLED="true",
        INVESTINATOR_DEMO_MODE="true",
        INVESTINATOR_MONITOR_ENABLED="false",
        INVESTINATOR_OPENAI_ENABLED="false",
        JAVA_DATABASE_USER="fixture",
        JAVA_DATABASE_PASSWORD=secrets.token_urlsafe(24),
        JAVA_PORT=str(fixture.free_port()),
        JAVA_BIND_ADDRESS="127.0.0.1",
    )
    env.update(
        {"REMEDIATION_" + role + "_TOKEN": token for role, token in tokens.items()}
    )
    process = None
    try:
        fixture.command(
            "run",
            "-d",
            "--name",
            name,
            "-e",
            "POSTGRES_USER=fixture",
            "-e",
            "POSTGRES_DB=fixture",
            "-e",
            "POSTGRES_PASSWORD=" + env["JAVA_DATABASE_PASSWORD"],
            "-p",
            "127.0.0.1::5432",
            fixture.IMAGE,
        )
        for _ in range(60):
            if (
                subprocess.run(
                    [*fixture.DOCKER, "exec", name, "pg_isready", "-U", "fixture"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                ).returncode
                == 0
            ):
                break
            time.sleep(0.5)
        env["JAVA_DATABASE_URL"] = (
            "jdbc:postgresql://127.0.0.1:"
            + fixture.command("port", name, "5432").rsplit(":", 1)[1]
            + "/fixture"
        )
        base = "http://127.0.0.1:" + env["JAVA_PORT"]

        def call(path, body=None, role=None, key=None):
            headers = {"Content-Type": "application/json"}
            if role:
                headers["Authorization"] = "Bearer " + tokens[role]
            if key:
                headers["Idempotency-Key"] = key
            request = urllib.request.Request(
                base + path,
                headers=headers,
                data=json.dumps(body).encode() if body is not None else None,
            )
            with urllib.request.urlopen(request, timeout=15) as response:
                return json.load(response)

        def denied(path, body, role, key=None):
            try:
                call(path, body, role, key)
            except urllib.error.HTTPError as error:
                assert error.code in (403, 409)
            else:
                raise AssertionError("Unauthorized or repeated action crossed policy")

        with tempfile.TemporaryDirectory(
            prefix="investinator-governed-process-"
        ) as directory:

            def start():
                log = open(Path(directory) / (uuid.uuid4().hex + ".log"), "w")
                java = fixture.ROOT.parent / ".cache/jdk-21/bin/java"
                proc = subprocess.Popen(
                    [
                        str(java) if java.exists() else "java",
                        "-jar",
                        str(
                            fixture.ROOT
                            / "backend-java/target/backend-java-0.1.0-SNAPSHOT.jar"
                        ),
                    ],
                    env=env,
                    stdout=log,
                    stderr=subprocess.STDOUT,
                )
                log.close()
                for _ in range(120):
                    if proc.poll() is not None:
                        raise RuntimeError("Owned remediation fixture startup failed")
                    try:
                        call("/api/v1/remediation/registry", role="PLAN")
                        return proc
                    except OSError:
                        time.sleep(0.25)
                proc.terminate()
                proc.wait(timeout=15)
                raise RuntimeError("Owned fixture readiness timeout")

            process = start()
            run = call("/api/v1/replay/runs", {"scenario": "kafka_disk_pressure"})
            proposal = {
                "incident_id": run["incident_id"],
                "action": "REPLAY_DISK_RECOVER_V1",
                "environment": "ReplayLab",
                "parameters": {"disk_percent": 60},
                "parent_plan": None,
            }
            denied("/api/v1/remediation/plans", proposal, None)
            p = call("/api/v1/remediation/plans", proposal, "PLAN")
            path = "/api/v1/remediation/plans/" + p["plan_id"]
            review = {"plan_hash": p["plan_hash"]}
            denied(path + "/execute", review, "EXECUTE", "owned-execution")
            denied(path + "/approval", {**review, "decision": "APPROVE"}, "PLAN")
            call(
                path + "/approval",
                {**review, "decision": "APPROVE", "decided_by": "ignored-client-claim"},
                "APPROVE",
            )
            assert (
                call(path + "/execute", review, "EXECUTE", "owned-execution")["state"]
                == "EXECUTED"
            )
            assert not call(path + "/verify", review, "VERIFY")["passed"]
            process.terminate()
            process.wait(timeout=20)
            process = start()
            assert call(path, role="PLAN")["state"] == "EXECUTED"
            denied(path + "/execute", review, "EXECUTE", "owned-execution")
            denied(
                "/api/v1/incidents/" + run["incident_id"] + "/verify",
                {"checks": {}},
                None,
            )
            call(path + "/sample", {}, "VERIFY")
            assert call(path + "/verify", review, "VERIFY")["passed"]
            rollback = {
                **proposal,
                "action": "REPLAY_DISK_RESTORE_V1",
                "parameters": {},
                "parent_plan": p["plan_id"],
            }
            assert call("/api/v1/remediation/dry-run", rollback, "PLAN")["dry_run"]
            r = call("/api/v1/remediation/plans", rollback, "PLAN")
            rollback_path = "/api/v1/remediation/plans/" + r["plan_id"]
            reviewed = {"plan_hash": r["plan_hash"]}
            denied(rollback_path + "/execute", reviewed, "EXECUTE", "owned-rollback")
            call(
                rollback_path + "/approval",
                {**reviewed, "decision": "APPROVE"},
                "APPROVE",
            )
            call(rollback_path + "/execute", reviewed, "EXECUTE", "owned-rollback")
            assert not call(rollback_path + "/verify", reviewed, "VERIFY")["passed"]
            call(rollback_path + "/sample", {}, "VERIFY")
            assert call(rollback_path + "/verify", reviewed, "VERIFY")["passed"]
            assert call(path, role="PLAN")["state"] == "ROLLED_BACK"
            assert (
                call("/api/v1/incidents/" + run["incident_id"])["status"]
                == "closed_unresolved"
            )
            result = {
                "status": "PASS",
                "source_mode": "SIMULATED",
                "transport": "actual disposable PostgreSQL/Java HTTP",
                "real_java_process_restart": True,
                "independent_post_state_required": True,
                "unapproved_and_wrong_role_denied": True,
                "repeat_after_restart_denied": True,
                "legacy_bypass_denied": True,
                "named_rollback_separately_approved_and_verified": True,
                "production_actions": 0,
                "paid_provider_calls": 0,
            }
            args.output.write_text(json.dumps(result, indent=2) + "\n")
            print(
                "PASS governed ReplayLab recovery/restart, independent verification and named rollback"
            )
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            process.wait(timeout=20)
        fixture.command("rm", "-f", name)


if __name__ == "__main__":
    main()
