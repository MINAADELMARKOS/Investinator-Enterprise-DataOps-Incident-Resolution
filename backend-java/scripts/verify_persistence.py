#!/usr/bin/env python3
"""Disposable local PostgreSQL + Java process restart acceptance test. Never adopts existing data."""
import argparse
import json
import os
import secrets
import signal
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DOCKER = ["docker", "--host=unix:///var/run/docker.sock"]
IMAGE = "postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e"


def command(*args, **kwargs):
    return subprocess.check_output([*DOCKER, *args], text=True, **kwargs).strip()


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def main(with_web=False):
    name = "investinator-persistence-test-" + secrets.token_hex(5)
    env = os.environ.copy()
    env["POSTGRES_PASSWORD"] = secrets.token_urlsafe(24)
    java = str(Path(env["JAVA_HOME"]) / "bin/java") if env.get("JAVA_HOME") else "java"
    process = None
    web = None
    try:
        command("run", "-d", "--rm", "--name", name, "-e", "POSTGRES_PASSWORD", "-e", "POSTGRES_USER=investinator", "-e", "POSTGRES_DB=investinator_test", "-p", "127.0.0.1::5432", IMAGE, env=env)
        deadline = time.monotonic() + 45
        while subprocess.run([*DOCKER, "exec", name, "pg_isready", "-U", "investinator"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode:
            if time.monotonic() > deadline:
                raise TimeoutError("Disposable PostgreSQL did not become ready")
            time.sleep(.2)
        db_port = command("port", name, "5432").rsplit(":", 1)[1]
        api_port = free_port()
        base = f"http://127.0.0.1:{api_port}"
        env.update(JAVA_DATABASE_URL=f"jdbc:postgresql://127.0.0.1:{db_port}/investinator_test", JAVA_DATABASE_USER="investinator", JAVA_DATABASE_PASSWORD=env["POSTGRES_PASSWORD"], JAVA_PERSISTENCE_ENABLED="true", JAVA_PORT=str(api_port), JAVA_BIND_ADDRESS="127.0.0.1", INVESTINATOR_DEMO_MODE="true", INVESTINATOR_OPENAI_ENABLED="false")
        for key in ("KAFKA_BOOTSTRAP_SERVERS", "OPENAI_API_KEY", "INVESTINATOR_OPENAI_MODEL"):
            env.pop(key, None)

        def request(path, body=None):
            data = None if body is None else json.dumps(body).encode()
            req = urllib.request.Request(base + path, data=data, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=10) as response:
                return json.load(response)

        with tempfile.TemporaryDirectory(prefix="investinator-java-restart-") as directory:
            def start(index):
                nonlocal process
                log = open(Path(directory) / f"java-{index}.log", "w")
                process = subprocess.Popen([java, "-jar", str(ROOT / "backend-java/target/backend-java-0.1.0-SNAPSHOT.jar")], env=env, stdout=log, stderr=subprocess.STDOUT)
                deadline = time.monotonic() + 45
                while True:
                    try:
                        assert request("/actuator/health/readiness")["status"] == "UP"
                        return log
                    except (OSError, AssertionError) as error:
                        if process.poll() is not None or time.monotonic() > deadline:
                            raise RuntimeError("Java persistence startup failed; inspect local test logs") from error
                        time.sleep(.2)

            log = start(1)
            replay = request("/api/v1/replay/runs", {"scenario": "kafka_disk_pressure"})
            incident = replay["incident_id"]
            request(f"/api/v1/incidents/{incident}/actions/ACT-DEMO/approval", {"decision": "approve", "decided_by": "client-is-not-authority"})
            before = request(f"/api/v1/incidents/{incident}")
            process.terminate()
            process.wait(timeout=20)
            log.close()
            log = start(2)
            assert request(f"/api/v1/incidents/{incident}") == before
            if with_web:
                web_port = free_port()
                web_env = env.copy()
                web_env["API_INTERNAL_URL"] = base
                web_log = open(Path(directory) / "next.log", "w")
                web = subprocess.Popen(["node", "node_modules/next/dist/bin/next", "start", "--hostname", "127.0.0.1", "--port", str(web_port)], cwd=ROOT / "apps/web", env=web_env, stdout=web_log, stderr=subprocess.STDOUT, start_new_session=True)
                web_base = f"http://127.0.0.1:{web_port}"
                deadline = time.monotonic() + 30
                while True:
                    try:
                        with urllib.request.urlopen(web_base + "/api/v1/health", timeout=5) as response:
                            assert json.load(response)["mode"] == "simulated-demo"
                        break
                    except (OSError, AssertionError) as error:
                        if web.poll() is not None or time.monotonic() > deadline:
                            raise RuntimeError("Next.js proxy startup failed") from error
                        time.sleep(.2)
                with urllib.request.urlopen(web_base, timeout=20) as response:
                    assert "InvestiNator" in response.read().decode()
                proxy = urllib.request.Request(web_base + f"/api/v1/incidents/{incident}/actions/ACT-DEMO/execute", data=b"{}", headers={"Content-Type": "application/json"})
                with urllib.request.urlopen(proxy, timeout=10) as response:
                    result = json.load(response)
                os.killpg(web.pid, signal.SIGTERM)
                web.wait(timeout=20)
                web_log.close()
                print("PASS: unchanged Next.js page and GET/POST API proxy against the Java demo")
            else:
                result = request(f"/api/v1/incidents/{incident}/actions/ACT-DEMO/execute", {})
            assert result["verification"]["passed"] is True
            assert request(f"/api/v1/incidents/{incident}")["status"] == "resolved"
            assert request("/api/v1/approvals")[0]["actor"] == "demo.senior_sre"
            assert any(a["event"] == "action_executed" for a in request("/api/v1/audit"))
            process.terminate()
            process.wait(timeout=20)
            log.close()
            env["INVESTINATOR_DEMO_MODE"] = "false"
            log = start(3)
            try:
                request("/api/v1/incidents")
            except urllib.error.HTTPError as error:
                assert error.code == 503
            else:
                raise AssertionError("Non-demo identity was incorrectly accepted")
            # A required DB outage affects readiness, not process liveness.
            command("stop", name)
            assert request("/actuator/health/liveness")["status"] == "UP"
            try:
                request("/actuator/health/readiness")
            except urllib.error.HTTPError as error:
                assert error.code == 503
            else:
                raise AssertionError("Missing DB was incorrectly ready")
            process.terminate()
            process.wait(timeout=20)
            log.close()
        print("PASS: PostgreSQL Flyway, persisted incident/approval across Java process restart, approved simulated recovery/audit; non-demo access denied; DB outage gives readiness 503 while liveness stays UP")
    finally:
        if web is not None and web.poll() is None:
            os.killpg(web.pid, signal.SIGTERM)
            web.wait(timeout=20)
        if process is not None and process.poll() is None:
            process.terminate()
            process.wait(timeout=20)
        subprocess.run([*DOCKER, "rm", "-f", name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--with-web", action="store_true", help="Also test the unchanged installed Next.js production server")
    main(parser.parse_args().with_web)
