#!/usr/bin/env python3
"""Explicit failure injection into this checkout's owned local demo only. Never production."""

import argparse
import importlib.util
import json
import subprocess
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "cli", Path(__file__).resolve().parent / "investinator_cli.py"
)
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)


def snapshot(state):
    result = {}
    for c in cli.parse_containers(
        cli.compose(state, "ps", "--all", "--format", "json", capture=True)
    ):
        identity = c["ID"]
        count = cli.run(
            [*cli.docker(), "inspect", identity, "--format", "{{.RestartCount}}"],
            capture=True,
        )
        result[c["Service"]] = {"id": identity, "restarts": count}
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output",
        type=Path,
        default=cli.ROOT / "docs/implementation/prompt05/failure-matrix.json",
    )
    args = parser.parse_args()
    state = cli.load_state()
    results = []
    with cli.lock():
        initial = snapshot(state)
        assert cli.health(state)["success"]
        for service in cli.SERVICES:
            cli.compose(state, "stop", "--timeout", "3", service, capture=True)
            try:
                p = subprocess.run(
                    [str(cli.ROOT / "investinator"), "doctor", "--json"],
                    cwd="/tmp",
                    text=True,
                    capture_output=True,
                    timeout=35,
                )
                report = json.loads(p.stdout)
                assert p.returncode == 1 and not report["success"], service
                assert any(service in e for e in report["doctor"]["errors"]), service
                alias = {"db": "postgresql", "collector": "otel", "web": "nextjs"}.get(
                    service, service
                )
                checks = [c for c in report["checks"] if c["service"] == alias]
                if service != "forwarder":
                    assert checks and checks[0]["status"] in (
                        "UNHEALTHY",
                        "UNKNOWN",
                        "DEGRADED",
                    ), (service, checks)
                if service != "java":
                    assert (
                        cli.http(cli.api(state), "/actuator/health/liveness")["status"]
                        == "UP"
                    )
                assert snapshot(state) == initial, (
                    "Unexpected container replacement/restart"
                )
                results.append(
                    {
                        "stopped_service": service,
                        "doctor_exit": p.returncode,
                        "detected": True,
                        "check_status": checks[0]["status"]
                        if checks
                        else "UNHEALTHY_CONTAINER",
                        "liveness_independent": service != "java",
                        "cascading_restarts": False,
                    }
                )
            finally:
                cli.compose(state, "start", service, capture=True)
                cli.wait(lambda: cli.health(state)["success"], 65)
        args.output.write_text(json.dumps(results, indent=2) + "\n")
        print(
            "PASS: all eight dependency stops detected; no cascading restart; all recovered"
        )


if __name__ == "__main__":
    main()
