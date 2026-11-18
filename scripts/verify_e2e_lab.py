#!/usr/bin/env python3
"""Bounded acceptance for this checkout's owned lab. No production targets or paid calls."""
import argparse
import hashlib
import importlib.util
import json
import os
import subprocess
import sys
import time
import urllib.error
from pathlib import Path

spec = importlib.util.spec_from_file_location("cli", Path(__file__).with_name("investinator_cli.py"))
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--outages", action="store_true", help="Stop/start only this checkout's eight owned containers")
    parser.add_argument("--output", type=Path, default=cli.RUNTIME / "e2e.json")
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    report = {"status": "RUNNING", "commands": [], "production_actions": 0, "paid_provider_calls": 0,
              "provenance": "actual owned local transport; all faults/incident content SIMULATED"}
    env = os.environ.copy()
    env.update(INVESTINATOR_MONITOR_ENABLED="true", OPENAI_API_KEY="",
               INVESTINATOR_OPENAI_MODEL="", INVESTINATOR_OPENAI_ENABLED="false")

    def run(*arguments, expected=0, timeout=900):
        start = time.monotonic()
        result = subprocess.run([str(cli.ROOT / "investinator"), *arguments], cwd=cli.ROOT, env=env,
                                capture_output=True, text=True, timeout=timeout)
        report["commands"].append({"args": list(arguments), "exit_code": result.returncode,
                                   "expected": expected, "seconds": round(time.monotonic()-start, 3),
                                   "stdout_sha256": hashlib.sha256(result.stdout.encode()).hexdigest()})
        if result.returncode != expected:
            raise AssertionError("Owned CLI check failed: " + " ".join(arguments))
        return result.stdout

    def snapshot(state):
        return {c["Service"]: c["ID"] for c in cli.parse_containers(cli.compose(state, "ps", "--format", "json", capture=True))}

    try:
        report["initial_commit"] = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=cli.ROOT, text=True).strip()
        run("up")
        state = cli.load_state()
        before = snapshot(state)
        assert set(before) == set(cli.SERVICES)
        run("up")
        assert before == snapshot(state), "Unchanged up replaced containers"
        report["idempotent_up"] = True
        run("doctor", "--json")
        run("health", "--all", "--json")
        run("smoke")
        cases = {}
        for name in ("kafka-kraft", "kafka-legacy"):
            fixture = json.loads(run("demo", name))
            repeated = json.loads(run("demo", name))
            assert repeated["incident_id"] == fixture["incident_id"]
            investigation = json.loads(run("investigate", name))["investigation"]
            assert investigation["candidate_causes"][0]["entity_id"] == "network"
            assert investigation["metadata_mode"] == fixture["metadata_mode"]
            assert len(investigation["observed_symptoms"]) == 10
            if name == "kafka-kraft":
                assert "ZOOKEEPER" not in json.dumps(investigation)
            cases[name] = {"incident_count": fixture["incident_count"], "source_mode": fixture["source_mode"],
                           "event_count": fixture["event_count"], "metadata_mode": fixture["metadata_mode"],
                           "origin_hypothesis": investigation["candidate_causes"][0],
                           "alternatives": investigation["candidate_causes"][1:], "human_accuracy": "NOT_EVALUATED"}
            # Identical deliveries are durable deduplications, not new incidents.
            token = state["secrets"]["MONITOR_API_TOKEN"]
            for _ in range(100):
                reply = cli.http(cli.api(state), "/api/v1/monitor/events", fixture["replay_events"][0], token)
                assert reply["duplicate"]
            status = cli.http(cli.api(state), "/api/v1/monitor/status", token=token)
            assert sum(i["graph_id"] == fixture["graph_id"] for i in status["incidents"]) == 1
        report["golden_cases"] = cases
        report["duplicate_deliveries"] = 200
        run("restart", "api")
        cli.wait(lambda: cli.health(state)["success"], 90)
        assert json.loads(run("investigate", "kafka-kraft"))["investigation"]["candidate_causes"][0]["entity_id"] == "network"
        report["api_restart_retains_incident_and_correlation"] = True
        run("ai-check", "--live", expected=2)  # No configured budget/key: no paid request permitted.
        for path, body, token, expected in [
            ("/api/v1/remediation/plans", {}, None, 403),
            ("/api/v1/monitor/events", {**fixture["replay_events"][0], "tenant_id": "other"}, state["secrets"]["MONITOR_API_TOKEN"], 403),
        ]:
            try:
                cli.http(cli.api(state), path, body, token)
            except urllib.error.HTTPError as error:
                assert error.code == expected
            else:
                raise AssertionError("Negative authorization check unexpectedly succeeded")
        report["native_auth_and_tenant_negatives"] = "PASS"
        subprocess.run([sys.executable, "scripts/verify_frontend_compat.py"], cwd=cli.ROOT, env=env, check=True, timeout=300)
        report["browser"] = json.loads((cli.RUNTIME / "ui-acceptance/results.json").read_text())
        if args.outages:
            subprocess.run([sys.executable, "scripts/verify_health_lab.py", "--output", str(args.output.with_name("outages.json"))],
                           cwd=cli.ROOT, env=env, check=True, timeout=900)
            report["eight_owned_service_outages"] = "PASS"
        report["status"] = "PASS"
    except (OSError, ValueError, AssertionError, subprocess.SubprocessError) as error:
        report.update(status="FAIL", error_type=type(error).__name__)
        raise
    finally:
        args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"status": report["status"], "report": str(args.output)}))


if __name__ == "__main__":
    main()
