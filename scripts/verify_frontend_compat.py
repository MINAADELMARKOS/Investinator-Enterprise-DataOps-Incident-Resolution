#!/usr/bin/env python3
"""Own disposable Python/Next processes; execute browser runner against both backends."""
import json
import os
import socket
import subprocess
import tempfile
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def main():
    processes = []
    with tempfile.TemporaryDirectory(prefix="investinator-ui-python-") as directory:
        env = os.environ.copy()
        for key in list(env):
            if "OPENAI" in key or key.startswith("KAFKA") or key.startswith("OTEL"):
                env.pop(key)
        env.pop("INVESTINATOR_BACKEND", None)  # Prove the actual default.
        env.update(DATABASE_URL="sqlite:///" + directory + "/fixture.db",
                   INVESTINATOR_DEMO_MODE="true", WEB_DEMO_ENABLED="false",
                   OTEL_SDK_DISABLED="true", NEXT_TELEMETRY_DISABLED="1")
        api_port, web_port = free_port(), free_port()
        env["API_INTERNAL_URL"] = f"http://127.0.0.1:{api_port}"
        try:
            for name, command, cwd, url in [
                ("python", [str(ROOT / ".venv/bin/python"), "-m", "uvicorn",
                            "incident_copilot.api:app", "--host", "127.0.0.1", "--port", str(api_port)],
                 ROOT, env["API_INTERNAL_URL"] + "/api/v1/health"),
                ("next", ["npm", "run", "start", "--", "--hostname", "127.0.0.1", "--port", str(web_port)],
                 ROOT / "apps/web", f"http://127.0.0.1:{web_port}/api/v1/health"),
            ]:
                with open(Path(directory) / (name + ".log"), "w") as log:
                    process = subprocess.Popen(command, cwd=cwd, env=env, stdout=log, stderr=log)
                processes.append(process)
                for _ in range(120):
                    if process.poll() is not None:
                        raise RuntimeError(name + " owned process exited")
                    try:
                        with urllib.request.urlopen(url, timeout=2) as response:
                            if response.status == 200:
                                break
                    except OSError:
                        time.sleep(0.25)
                else:
                    raise RuntimeError(name + " readiness timeout")
            browser_env = os.environ.copy()
            browser_env["PYTHON_WEB_URL"] = f"http://127.0.0.1:{web_port}"
            subprocess.run(["node", "scripts/verify_operator_ui.mjs"], cwd=ROOT,
                           env=browser_env, check=True, timeout=300)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait(timeout=5)
    print(json.dumps({"owned_python_and_next_processes": "cleaned", "default_python_browser": "PASS"}))


if __name__ == "__main__":
    main()
