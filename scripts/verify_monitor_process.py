#!/usr/bin/env python3
"""Disposable PostgreSQL/Java-process monitoring acceptance; SIMULATED input only."""

import importlib.util
import json
import os
import secrets
import signal
import subprocess
import tempfile
import time
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "persistence",
    Path(__file__).resolve().parents[1] / "backend-java/scripts/verify_persistence.py",
)
persistence = importlib.util.module_from_spec(spec)
spec.loader.exec_module(persistence)
ROOT = persistence.ROOT


def main():
    name = "investinator-monitor-check-" + uuid.uuid4().hex[:10]
    token = secrets.token_urlsafe(24)
    graph_token = secrets.token_urlsafe(24)
    metrics_token = secrets.token_urlsafe(24)
    env = os.environ.copy()
    env.update(
        JAVA_PERSISTENCE_ENABLED="true",
        INVESTINATOR_DEMO_MODE="true",
        INVESTINATOR_MONITOR_ENABLED="true",
        MONITOR_API_TOKEN=token,
        GRAPH_ADMIN_TOKEN=graph_token,
        METRICS_TOKEN=metrics_token,
        INVESTINATOR_OPENAI_ENABLED="false",
        JAVA_DATABASE_USER="fixture",
        JAVA_DATABASE_PASSWORD=secrets.token_urlsafe(24),
        JAVA_PORT=str(persistence.free_port()),
        JAVA_BIND_ADDRESS="127.0.0.1",
    )
    env["JAVA_TOOL_OPTIONS"] = (
        env.get("JAVA_TOOL_OPTIONS", "") + " -Dinvestinator.monitor.tick-ms=3600000"
    )
    process = None
    try:
        persistence.command(
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
            persistence.IMAGE,
        )
        for _ in range(60):
            if (
                subprocess.run(
                    [*persistence.DOCKER, "exec", name, "pg_isready", "-U", "fixture"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                ).returncode
                == 0
            ):
                break
            time.sleep(0.5)
        port = persistence.command("port", name, "5432").rsplit(":", 1)[1]
        env["JAVA_DATABASE_URL"] = f"jdbc:postgresql://127.0.0.1:{port}/fixture"
        import urllib.request

        base = "http://127.0.0.1:" + env["JAVA_PORT"]

        def call(path, body=None, admin=False):
            headers = {"Content-Type": "application/json"}
            headers["Authorization"] = "Bearer " + (graph_token if admin else token)
            request = urllib.request.Request(
                base + path,
                headers=headers,
                data=json.dumps(body).encode() if body is not None else None,
            )
            with urllib.request.urlopen(request, timeout=15) as response:
                return json.load(response)

        with tempfile.TemporaryDirectory(
            prefix="investinator-monitor-process-"
        ) as directory:

            def start():
                log = open(Path(directory) / (uuid.uuid4().hex + ".log"), "w")
                binary = ROOT.parent / ".cache/jdk-21/bin/java"
                proc = subprocess.Popen(
                    [
                        str(binary) if binary.exists() else "java",
                        "-jar",
                        str(
                            ROOT / "backend-java/target/backend-java-0.1.0-SNAPSHOT.jar"
                        ),
                    ],
                    env=env,
                    stdout=log,
                    stderr=subprocess.STDOUT,
                )
                log.close()
                for _ in range(120):
                    if proc.poll() is not None:
                        raise RuntimeError(
                            "Owned Java fixture startup failed; no credentials echoed"
                        )
                    try:
                        call("/api/v1/monitor/status")
                        return proc
                    except OSError:
                        time.sleep(0.25)
                proc.terminate()
                proc.wait(timeout=10)
                raise RuntimeError("Owned fixture readiness timeout")

            process = start()
            call("/api/v1/monitor/resume", {})
            now = datetime.now(timezone.utc) - timedelta(seconds=180)
            graph = {
                "graph_id": "owned-process-check",
                "revision": 1,
                "source_mode": "SIMULATED",
                "metadata_mode": "KRAFT",
                "platform_version": "Kafka 4.3",
                "nodes": [
                    {"id": "network", "type": "NETWORK", "name": "SIMULATED network"}
                ],
                "edges": [],
            }
            call(
                "/api/v1/dependencies/fixtures",
                {"graph": graph, "observations": []},
                admin=True,
            )

            def event(identity, seconds):
                return {
                    "event_id": identity,
                    "tenant_id": "demo",
                    "environment": "demo",
                    "cluster_id": "owned-process",
                    "graph_id": graph["graph_id"],
                    "graph_revision": 1,
                    "resource": "network",
                    "rule_id": "network",
                    "observed_at": (now + timedelta(seconds=seconds)).isoformat(),
                    "source_mode": "SIMULATED",
                    "measurements": {"network_loss_percent": 10},
                    "capabilities": ["network_loss_percent"],
                    "excerpt": "SIMULATED",
                    "impact": None,
                }

            first = event("owned-one", 0)
            for _ in range(100):
                call("/api/v1/monitor/events", first)
            call("/api/v1/monitor/events", event("owned-two", 10))
            assert call("/api/v1/monitor/tick", {})["processed"] == 2
            before = call("/api/v1/monitor/status")
            assert len(before["incidents"]) == 1
            process.send_signal(signal.SIGTERM)
            process.wait(timeout=20)
            process = start()
            after = call("/api/v1/monitor/status")
            assert (
                before["offset"] == after["offset"]
                and before["incidents"] == after["incidents"]
            )
            assert call("/api/v1/monitor/tick", {})["processed"] == 0
            request = urllib.request.Request(
                base + "/actuator/prometheus",
                headers={"Authorization": "Bearer " + metrics_token},
            )
            with urllib.request.urlopen(request, timeout=15) as response:
                metrics = response.read().decode()
            assert (
                "investinator_monitor_active" in metrics
                and "investinator_monitor_events" in metrics
            )
            samples = 0
            for _ in range(80):
                samples = int(
                    persistence.command(
                        "exec",
                        name,
                        "psql",
                        "-U",
                        "fixture",
                        "-d",
                        "fixture",
                        "-Atc",
                        "select count(*) from investinator_java.monitor_health_samples",
                    )
                )
                if samples:
                    break
                time.sleep(0.25)
            assert samples > 0
            result = {
                "status": "PASS",
                "actual_health_samples": samples,
                "prometheus_gauges": "PASS",
                "source_mode": "SIMULATED",
                "actual_transport": "local PostgreSQL + restarted Java process",
                "duplicate_deliveries": 100,
                "unique_processed": 2,
                "logical_incidents": 1,
                "offset_survives_restart": True,
                "duplicate_after_restart": call("/api/v1/monitor/events", first)[
                    "duplicate"
                ],
                "external_action": False,
                "paid_provider_calls": 0,
            }
            (ROOT / "docs/implementation/prompt10/process-restart.json").write_text(
                json.dumps(result, indent=2) + "\n"
            )
            print(
                "PASS monitoring process restart, retained offset, one deduplicated SIMULATED incident"
            )
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            process.wait(timeout=20)
        persistence.command("rm", "-f", name)


if __name__ == "__main__":
    main()
