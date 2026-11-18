#!/usr/bin/env python3
"""Safe local-only InvestiNator operations. Never targets production or accepts shell commands."""

import argparse
import base64
import contextlib
import datetime
import hashlib
import importlib.util
import json
import os
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / ".runtime"
SERVICES = (
    "db",
    "search",
    "collector",
    "forwarder",
    "prometheus",
    "kafka",
    "java",
    "web",
)
VOLUMES = (
    "postgres_data",
    "search_data",
    "trace_data",
    "forwarder_state",
    "kafka_data",
)
KNOWLEDGE_SECRET_KEYS = (
    "GRAPH_ADMIN_TOKEN",
    "INVESTIGATION_API_TOKEN",
    "KNOWLEDGE_WRITE_TOKEN",
    "KNOWLEDGE_REVIEW_TOKEN",
    "KNOWLEDGE_READ_TOKEN",
    "KNOWLEDGE_FEEDBACK_TOKEN",
    "MONITOR_API_TOKEN",
    "UI_SESSION_SECRET",
    "UI_VIEWER_LOGIN_TOKEN",
    "UI_OPERATOR_LOGIN_TOKEN",
    "UI_PLANNER_LOGIN_TOKEN",
    "UI_APPROVER_LOGIN_TOKEN",
    "UI_EXECUTOR_LOGIN_TOKEN",
    "UI_VERIFIER_LOGIN_TOKEN",
    "REMEDIATION_PLAN_TOKEN",
    "REMEDIATION_APPROVE_TOKEN",
    "REMEDIATION_EXECUTE_TOKEN",
    "REMEDIATION_VERIFY_TOKEN",
    "KNOWLEDGE_WRITER_PASSWORD",
)
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))
PORTS = {
    "LAB_API_PORT": 18081,
    "LAB_WEB_PORT": 13000,
    "LAB_SEARCH_PORT": 19200,
    "LAB_PROM_PORT": 19090,
    "LAB_OTLP_PORT": 14318,
    "LAB_OTEL_HEALTH_PORT": 13134,
    "LAB_KAFKA_PORT": 19092,
}


def run(args, *, env=None, capture=False, timeout=300, cwd=ROOT):
    result = subprocess.run(
        [str(v) for v in args],
        cwd=cwd,
        env=env,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        timeout=timeout,
    )
    if result.returncode:
        # Do not echo command arguments/environment (may contain credentials).
        raise RuntimeError(
            f"{Path(str(args[0])).name} failed (exit {result.returncode}); inspect the local service/build logs"
        )
    return result.stdout.strip() if capture else ""


def docker():
    host = os.getenv("DOCKER_HOST", "")
    if host and not host.startswith(("unix://", "npipe://")):
        raise RuntimeError(
            "Lifecycle commands require a local Docker socket; remote Docker is refused"
        )
    if sys.platform == "win32":
        context = run(
            ["docker", "context", "inspect", "--format", "{{.Endpoints.docker.Host}}"],
            capture=True,
        )
        if not context.startswith("npipe://"):
            raise RuntimeError(
                "Windows lifecycle commands require the local Docker named pipe"
            )
        return ["docker", "--host=" + context]
    return ["docker", "--host=unix:///var/run/docker.sock"]


def project():
    value = os.getenv(
        "INVESTINATOR_PROJECT",
        "investinator-" + hashlib.sha256(str(ROOT).encode()).hexdigest()[:10],
    )
    import re

    if not re.fullmatch(r"investinator-[a-z0-9-]{1,40}", value):
        raise ValueError("Project must be an investinator- prefixed local lab name")
    return value


def load_state(create=False):
    if RUNTIME.is_symlink():
        raise RuntimeError("Runtime directory cannot be a symlink")
    path = RUNTIME / "lab.json"
    if path.exists():
        if path.is_symlink():
            raise RuntimeError("Local lab configuration cannot be a symlink")
        state = json.loads(path.read_text())
        if (
            state.get("project") != project()
            or state.get("root") != str(ROOT)
            or state.get("version") != 1
        ):
            raise RuntimeError(
                "Local lab ownership marker differs; preserve it and use its owning checkout/project"
            )
        if create:
            changed = False
            for key in (
                "CONNECTOR_API_TOKEN",
                "KAFKA_DIAGNOSTIC_PASSWORD",
                *KNOWLEDGE_SECRET_KEYS,
            ):
                if key not in state["secrets"]:
                    state["secrets"][key] = "Aa1!" + secrets.token_urlsafe(24)
                    changed = True
            if "INVESTINATOR_CHAT_STATE_KEY" not in state["secrets"]:
                state["secrets"]["INVESTINATOR_CHAT_STATE_KEY"] = base64.b64encode(
                    secrets.token_bytes(32)
                ).decode()
                changed = True
            if changed:
                with tempfile.NamedTemporaryFile(
                    mode="w", dir=RUNTIME, delete=False
                ) as output:
                    json.dump(state, output)
                os.replace(output.name, path)
        return state
    if not create:
        raise RuntimeError("Local lab is NOT_CONFIGURED; run ./investinator up")
    RUNTIME.mkdir(mode=0o700, exist_ok=True)
    if RUNTIME.is_symlink():
        raise RuntimeError("Runtime directory cannot be a symlink")
    state = {
        "version": 1,
        "root": str(ROOT),
        "project": project(),
        "ports": {key: int(os.getenv(key, default)) for key, default in PORTS.items()},
        "secrets": {
            key: "Aa1!" + secrets.token_urlsafe(24)
            for key in (
                "JAVA_DATABASE_PASSWORD",
                "EVIDENCE_ADMIN_PASSWORD",
                "EVIDENCE_WRITER_PASSWORD",
                "EVIDENCE_READER_PASSWORD",
                "EVIDENCE_API_TOKEN",
                "HEALTH_API_TOKEN",
                "LAB_KAFKA_PASSWORD",
                "CONNECTOR_API_TOKEN",
                "KAFKA_DIAGNOSTIC_PASSWORD",
                *KNOWLEDGE_SECRET_KEYS,
            )
        },
        "cluster_id": base64.urlsafe_b64encode(uuid.uuid4().bytes).decode().rstrip("="),
    }
    state["secrets"]["INVESTINATOR_CHAT_STATE_KEY"] = base64.b64encode(
        secrets.token_bytes(32)
    ).decode()
    if any(value < 1024 or value > 65535 for value in state["ports"].values()) or len(
        set(state["ports"].values())
    ) != len(PORTS):
        raise ValueError("Local lab ports must be distinct nonprivileged ports")
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as file:
        json.dump(state, file)
    return state


def environment(state):
    values = os.environ.copy()
    values.update(state["secrets"])
    values.update({key: str(value) for key, value in state["ports"].items()})
    values["LAB_KAFKA_CLUSTER_ID"] = state["cluster_id"]
    values["LAB_JAVA_IMAGE"] = state["project"] + "-java:local"
    values["LAB_WEB_IMAGE"] = state["project"] + "-web:local"
    return values


def compose(state, *args, capture=False, timeout=300):
    return run(
        [
            *docker(),
            "compose",
            "--project-name",
            state["project"],
            "--file",
            ROOT / "infra/lab/compose.yaml",
            *args,
        ],
        env=environment(state),
        capture=capture,
        timeout=timeout,
    )


def http(base, path, body=None, token=None, timeout=15):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(
        base + path,
        data=None if body is None else json.dumps(body).encode(),
        headers=headers,
    )
    with OPENER.open(req, timeout=timeout) as response:
        return json.load(response)


def api(state):
    return f"http://127.0.0.1:{state['ports']['LAB_API_PORT']}"


def wait(probe, timeout=120):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        try:
            value = probe()
            if value:
                return value
        except (OSError, RuntimeError, ValueError, AssertionError):
            pass
        time.sleep(0.5)
    raise TimeoutError(
        "Local service did not become functionally ready before its deadline"
    )


@contextlib.contextmanager
def lock():
    RUNTIME.mkdir(mode=0o700, exist_ok=True)
    if RUNTIME.is_symlink() or (RUNTIME / "operation.lock").is_symlink():
        raise RuntimeError("Runtime/lock path cannot be a symlink")
    with (RUNTIME / "operation.lock").open("a+") as file:
        if sys.platform == "win32":
            import msvcrt

            file.seek(0)
            file.write("0")
            file.flush()
            file.seek(0)
            msvcrt.locking(file.fileno(), msvcrt.LK_NBLCK, 1)
        else:
            import fcntl

            fcntl.flock(file, fcntl.LOCK_EX | fcntl.LOCK_NB)
        try:
            yield
        finally:
            if sys.platform == "win32":
                file.seek(0)
                msvcrt.locking(file.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(file, fcntl.LOCK_UN)


def java_env():
    values = os.environ.copy()
    candidate = Path(values.get("JAVA_HOME", "/workspace/.cache/jdk-21"))
    if (candidate / "bin/javac").exists():
        values["JAVA_HOME"] = str(candidate)
    elif not shutil.which("javac"):
        raise RuntimeError(
            "A Java 21 JDK with javac is required; a JRE alone is insufficient"
        )
    values["MAVEN_USER_HOME"] = str(RUNTIME / "maven-user")
    return values


def maven(*goals):
    values = java_env()
    args = [ROOT / "backend-java/mvnw", "-B", "-f", ROOT / "backend-java/pom.xml"]
    if sys.platform == "win32":
        args = [
            "cmd.exe",
            "/d",
            "/c",
            ROOT / "backend-java/mvnw.cmd",
            "-B",
            "-f",
            ROOT / "backend-java/pom.xml",
        ]
    # Adapt only local Maven proxy settings, preserving inherited routing and CA verification.
    proxy = os.getenv("HTTPS_PROXY", "")
    if proxy:
        from urllib.parse import urlsplit

        target = urlsplit(proxy)
        if target.username or target.password:
            raise RuntimeError(
                "Use secure Maven settings for an authenticated proxy; credentials are not copied"
            )
        root = ET.Element("settings")
        item = ET.SubElement(ET.SubElement(root, "proxies"), "proxy")
        for key, value in {
            "id": "cloud",
            "active": "true",
            "protocol": "http",
            "host": target.hostname,
            "port": str(target.port or 80),
            "nonProxyHosts": "localhost|127.*",
        }.items():
            ET.SubElement(item, key).text = value
        config = RUNTIME / "maven-settings.xml"
        ET.ElementTree(root).write(config, encoding="unicode")
        args += ["-s", config]
        trust = Path("/etc/ssl/certs/java/cacerts")
        if trust.exists():
            values["MAVEN_OPTS"] = (
                values.get("MAVEN_OPTS", "") + f" -Djavax.net.ssl.trustStore={trust}"
            )
    args += ["-Dmaven.repo.local=" + str(RUNTIME / "m2"), *goals]
    # Reuse this managed machine's existing verified dependency cache when present.
    if Path("/workspace/.cache/m2").is_dir():
        args[-len(goals) - 1] = "-Dmaven.repo.local=/workspace/.cache/m2"
    run(args, env=values, timeout=900)


def fingerprint(paths):
    digest = hashlib.sha256()
    for path in sorted(paths):
        if path.is_file():
            digest.update(str(path.relative_to(ROOT)).encode())
            digest.update(path.read_bytes())
    return digest.hexdigest()


def build(state):
    values = environment(state)
    stamp = RUNTIME / "build-fingerprints.json"
    previous = json.loads(stamp.read_text()) if stamp.exists() else {}
    java_paths = (
        list((ROOT / "backend-java/src/main").rglob("*"))
        + list((ROOT / "backend-java/.mvn").rglob("*"))
        + [ROOT / "backend-java/pom.xml", ROOT / "backend-java/Dockerfile"]
    )
    web_paths = [
        p
        for p in (ROOT / "apps/web").rglob("*")
        if not any(
            part in ("node_modules", ".next", ".git")
            for part in p.relative_to(ROOT / "apps/web").parts
        )
        and not p.name.endswith(".tsbuildinfo")
    ]
    web_paths += [
        ROOT / "infra/lab/Dockerfile.web",
        ROOT / "infra/lab/Dockerfile.web.dockerignore",
    ]
    current = {"java": fingerprint(java_paths), "web": fingerprint(web_paths)}

    def image_exists(name):
        result = subprocess.run(
            [*docker(), "image", "inspect", name],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return result.returncode == 0

    if (
        previous.get("java") != current["java"]
        or not image_exists(values["LAB_JAVA_IMAGE"])
        or not (ROOT / "backend-java/target/runtime-classpath.txt").exists()
    ):
        maven("-DskipTests", "package")
        run(
            [*docker(), "build", "-t", values["LAB_JAVA_IMAGE"], ROOT / "backend-java"],
            timeout=300,
        )
    node = shutil.which("node")
    if (
        not node
        or int(run([node, "--version"], capture=True).lstrip("v").split(".")[0]) < 22
    ):
        raise RuntimeError(
            "Node 22+ and npm are required for the preserved Next.js app"
        )
    if previous.get("web") != current["web"] or not image_exists(
        values["LAB_WEB_IMAGE"]
    ):
        values["npm_config_cache"] = str(RUNTIME / "npm-cache")
        run(
            ["npm", "ci", "--no-audit", "--no-fund"],
            env=values,
            cwd=ROOT / "apps/web",
            timeout=600,
        )
        run(["npm", "run", "build"], env=values, cwd=ROOT / "apps/web", timeout=600)
        run(
            [
                *docker(),
                "build",
                "-f",
                ROOT / "infra/lab/Dockerfile.web",
                "-t",
                values["LAB_WEB_IMAGE"],
                ROOT / "apps/web",
            ],
            timeout=300,
        )
    stamp.write_text(json.dumps(current))


def evidence_module():
    spec = importlib.util.spec_from_file_location(
        "investinator_evidence_lab", ROOT / "backend-java/scripts/evidence_lab.py"
    )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def classpath():
    return (
        str(ROOT / "backend-java/target/classes")
        + os.pathsep
        + (ROOT / "backend-java/target/runtime-classpath.txt").read_text().strip()
    )


def kafka_seed(state, access_only=False):
    values = java_env()
    values.update(
        KAFKA_BOOTSTRAP_SERVERS=f"127.0.0.1:{state['ports']['LAB_KAFKA_PORT']}",
        KAFKA_SECURITY_PROTOCOL="SASL_PLAINTEXT",
        KAFKA_SASL_MECHANISM="PLAIN",
        KAFKA_SASL_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="lab" password="'
        + state["secrets"]["LAB_KAFKA_PASSWORD"]
        + '";',
    )
    binary = (
        str(Path(values["JAVA_HOME"]) / "bin/java")
        if values.get("JAVA_HOME")
        else "java"
    )
    run(
        [
            binary,
            "-cp",
            classpath(),
            "com.investinator.health.KafkaFixtureSeeder",
            "--owned-local-fixture",
            state["cluster_id"],
            *(["--access-only"] if access_only else []),
        ],
        env=values,
        timeout=45,
        capture=True,
    )


def signals(state):
    marker = RUNTIME / "signals.json"
    if marker.exists():
        payload = json.loads(marker.read_text())
    else:
        now = time.time_ns()
        attributes = [{"key": "source_mode", "value": {"stringValue": "SIMULATED"}}]
        payload = {
            "traces": {
                "resourceSpans": [
                    {
                        "resource": {
                            "attributes": [
                                {
                                    "key": "service.name",
                                    "value": {
                                        "stringValue": "investinator-local-fixture"
                                    },
                                },
                                *attributes,
                            ]
                        },
                        "scopeSpans": [
                            {
                                "scope": {"name": "investinator-local-fixture"},
                                "spans": [
                                    {
                                        "traceId": "0123456789abcdef0123456789abcdef",
                                        "spanId": "1111111111111111",
                                        "name": "explicit-local-smoke",
                                        "kind": 1,
                                        "startTimeUnixNano": str(now - 1000000),
                                        "endTimeUnixNano": str(now),
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
            "metrics": {
                "resourceMetrics": [
                    {
                        "scopeMetrics": [
                            {
                                "scope": {"name": "investinator-local-fixture"},
                                "metrics": [
                                    {
                                        "name": "evidence_fixture_records",
                                        "gauge": {
                                            "dataPoints": [
                                                {
                                                    "timeUnixNano": str(now),
                                                    "asInt": "4",
                                                    "attributes": attributes,
                                                }
                                            ]
                                        },
                                    }
                                ],
                            }
                        ]
                    }
                ]
            },
        }
        marker.write_text(json.dumps(payload))
    base = f"http://127.0.0.1:{state['ports']['LAB_OTLP_PORT']}"
    for kind in ("traces", "metrics"):
        http(base, "/v1/" + kind, payload[kind])


def check_free_ports(ports):
    for port in ports:
        with socket.socket() as sock:
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                sock.bind(("127.0.0.1", port))
            except OSError as error:
                raise RuntimeError(
                    f"Port {port} is in use; choose LAB_* ports before first setup"
                ) from error


def up():
    with lock():
        state = load_state(True)
        run([*docker(), "info", "--format", "{{.ServerVersion}}"], capture=True)
        # Refuse unrelated listeners before creating/reconfiguring resources.
        known = parse_containers(
            compose(state, "ps", "--all", "--format", "json", capture=True)
        )
        if isinstance(known, dict):
            known = [known]
        if not known:
            check_free_ports(state["ports"].values())
        build(state)
        fixture = RUNTIME / "fixtures"
        if not (fixture / "evidence.ndjson").exists():
            values = java_env()
            binary = (
                str(Path(values["JAVA_HOME"]) / "bin/java")
                if values.get("JAVA_HOME")
                else "java"
            )
            run(
                [
                    binary,
                    "-cp",
                    classpath(),
                    "com.investinator.evidence.EvidenceFixtureGenerator",
                    fixture,
                ],
                env=values,
            )
        # Renew only generated SIMULATED evidence, at most once/hour. Preserve old files/data.
        newest = max(
            (path.stat().st_mtime for path in fixture.glob("*.ndjson")), default=0
        )
        refresh = fixture / f"refresh-{int(time.time() // 3600)}.ndjson"
        if time.time() - newest > 3600 and not refresh.exists():
            with tempfile.TemporaryDirectory(dir=RUNTIME) as folder:
                values = java_env()
                binary = (
                    str(Path(values["JAVA_HOME"]) / "bin/java")
                    if values.get("JAVA_HOME")
                    else "java"
                )
                run(
                    [
                        binary,
                        "-cp",
                        classpath(),
                        "com.investinator.evidence.EvidenceFixtureGenerator",
                        folder,
                    ],
                    env=values,
                    capture=True,
                )
                with refresh.open("xb") as output:
                    output.write((Path(folder) / "evidence.ndjson").read_bytes())
        fixture.chmod(0o755)
        for path in fixture.glob("*.ndjson"):
            path.chmod(0o644)
        for file in (
            "collector.yaml",
            "fluent-bit.conf",
            "evidence-parsers.conf",
            "prometheus.yaml",
        ):
            (ROOT / "infra/evidence" / file).chmod(0o644)
        compose(state, "config", "--quiet")
        compose(
            state,
            "up",
            "-d",
            "--wait",
            "--wait-timeout",
            "150",
            "db",
            "search",
            "kafka",
            timeout=210,
        )
        module = evidence_module()
        base = f"http://127.0.0.1:{state['ports']['LAB_SEARCH_PORT']}"
        module.bootstrap(
            base,
            state["secrets"]["EVIDENCE_ADMIN_PASSWORD"],
            state["secrets"]["EVIDENCE_WRITER_PASSWORD"],
            state["secrets"]["EVIDENCE_READER_PASSWORD"],
        )
        import knowledge_bootstrap

        knowledge_bootstrap.bootstrap(
            module,
            base,
            state["secrets"]["EVIDENCE_ADMIN_PASSWORD"],
            state["secrets"]["KNOWLEDGE_WRITER_PASSWORD"],
        )
        compose(
            state,
            "up",
            "-d",
            "--wait",
            "--wait-timeout",
            "90",
            "collector",
            "forwarder",
            "prometheus",
            "java",
            "web",
            timeout=150,
        )
        wait(
            lambda: (
                http(
                    f"http://127.0.0.1:{state['ports']['LAB_OTEL_HEALTH_PORT']}", "/"
                ).get("status")
                == "Server available"
            ),
            30,
        )
        kafka_seed(state, access_only=(RUNTIME / "kafka-seeded").exists())
        (RUNTIME / "kafka-seeded").write_text(state["cluster_id"])
        signals(state)
        wait(lambda: health(state)["success"], 90)
        print(
            "Local lab is functionally ready; sources are SIMULATED and production execution is disabled."
        )


def health(state):
    try:
        return http(
            api(state),
            "/api/v1/health/integrations?refresh=true",
            token=state["secrets"]["HEALTH_API_TOKEN"],
            timeout=20,
        )
    except (OSError, ValueError):
        now = datetime.datetime.now(datetime.timezone.utc).isoformat()
        return {
            "report_id": str(uuid.uuid4()),
            "checked_at": now,
            "success": False,
            "partial": True,
            "delivery_mode": "LOCAL_FALLBACK",
            "checks": [
                {
                    "check_id": str(uuid.uuid4()),
                    "service": "java",
                    "capability": "integration-report",
                    "status": "UNHEALTHY",
                    "source_mode": "LIVE",
                    "evidence_source": "local HTTP integration endpoint",
                    "checked_at": now,
                    "duration_ms": 0,
                    "diagnosis": "Integration health API unavailable",
                    "next_safe_action": "Inspect local Java logs and scoped health token",
                    "error_type": "API_UNAVAILABLE",
                    "required_for_readiness": True,
                    "details": {},
                }
            ],
        }


def display(report, as_json=False):
    if as_json:
        print(json.dumps(report, indent=2))
        return
    print(f"{'SERVICE':20} {'STATUS':16} {'SOURCE':16} {'MS':>6}  FINDING")
    for check in report["checks"]:
        print(
            f"{check['service']:20} {check['status']:16} {check['source_mode']:16} {check['duration_ms']:6}  {check['diagnosis']}"
        )


def parse_containers(raw):
    return (
        json.loads(raw or "[]")
        if raw.lstrip().startswith("[")
        else [json.loads(line) for line in raw.splitlines() if line.strip()]
    )


def doctor(state, as_json=False):
    report = health(state)
    errors = []
    tools = {
        name: bool(shutil.which(name)) for name in ("docker", "node", "npm", "python3")
    }
    try:
        java_env()
        tools["jdk21"] = True
    except RuntimeError:
        tools["jdk21"] = False
    try:
        compose(state, "config", "--quiet", capture=True)
        config_ok = True
    except RuntimeError:
        config_ok = False
    raw = compose(state, "ps", "--all", "--format", "json", capture=True)
    containers = parse_containers(raw)
    states = {
        c["Service"]: {"state": c.get("State"), "health": c.get("Health", "")}
        for c in containers
    }
    missing = [
        name
        for name in SERVICES
        if name not in states
        or states[name]["state"] != "running"
        or states[name]["health"] == "unhealthy"
    ]
    if missing:
        errors.append("Local services not running/healthy: " + ", ".join(missing))
    if not all(tools.values()) or not config_ok:
        errors.append("Local prerequisites or Compose syntax failed")
    free = shutil.disk_usage(ROOT).free
    if free < 512 * 1024 * 1024:
        errors.append("Less than 512MiB workspace free")
    listeners = {}
    for key, port in state["ports"].items():
        with socket.socket() as sock:
            sock.settimeout(0.3)
            listeners[key] = sock.connect_ex(("127.0.0.1", port)) == 0
    if not all(listeners.values()):
        errors.append(
            "A configured local service port is unavailable; inspect services and port collisions"
        )
    available_memory = None
    memory = Path("/proc/meminfo")
    if memory.exists():
        available_memory = next(
            (
                int(line.split()[1]) * 1024
                for line in memory.read_text().splitlines()
                if line.startswith("MemAvailable:")
            ),
            None,
        )
        if available_memory is not None and available_memory < 512 * 1024 * 1024:
            errors.append("Less than 512MiB available host memory")
    report.update(
        doctor={
            "available_memory_bytes": available_memory,
            "local_port_listeners": listeners,
            "tools": tools,
            "compose_valid": config_ok,
            "free_bytes": free,
            "services": states,
            "secret_presence": {
                key: bool(value) for key, value in state["secrets"].items()
            },
            "errors": errors,
        },
        release_ready=False,
        release_blockers=[
            "Enterprise identity, independent quality review and production connector gates incomplete; dependency status requires a current audit"
        ],
    )
    report["success"] = report["success"] and not errors
    (RUNTIME / "last-health.json").write_text(json.dumps(report, indent=2))
    display(report, as_json)
    if not as_json:
        for error in errors:
            print(error, file=sys.stderr)
        print(
            "Release blocked by incomplete production gates; consult the current dependency audit."
        )
    return 0 if report["success"] else 1


def smoke(state):
    # Explicit local demo mutations, not production writes: independent stored approval is still required.
    marker = RUNTIME / "smoke.json"
    active = [
        run
        for run in http(api(state), "/api/v1/replay/runs")
        if run["status"] == "awaiting_approval"
    ]
    if active:
        owned = []
        for candidate in (marker, RUNTIME / "demo-kafka-kraft.json"):
            if candidate.exists():
                owned.append(json.loads(candidate.read_text())["incident_id"])
        if (
            len(active) != 1
            or active[0]["incident_id"] not in owned
            or active[0].get("scenario") != "kafka_disk_pressure"
        ):
            raise RuntimeError(
                "An unrelated ReplayLab run is active; resolve or reject it through its approved workflow before smoke"
            )
        replay = active[0]
    else:
        replay = http(
            api(state), "/api/v1/replay/runs", {"scenario": "kafka_disk_pressure"}
        )
    incident = replay["incident_id"]
    marker.write_text(json.dumps({"incident_id": incident, "replay_id": replay["id"]}))
    assert (
        http(api(state), f"/api/v1/incidents/{incident}/verify", {"checks": {}})[
            "passed"
        ]
        is False
    )
    current = http(api(state), f"/api/v1/incidents/{incident}")
    action = next(
        action for action in current["recovery_actions"] if action["id"] == "ACT-DEMO"
    )
    if action["status"] != "approved":
        try:
            http(
                api(state), f"/api/v1/incidents/{incident}/actions/ACT-DEMO/execute", {}
            )
        except urllib.error.HTTPError as error:
            assert error.code == 403
        else:
            raise AssertionError("Unapproved local simulation unexpectedly executed")
        http(
            api(state),
            f"/api/v1/incidents/{incident}/actions/ACT-DEMO/approval",
            {
                "decision": "approve",
                "decided_by": "explicit-local-smoke",
                "reason": "User invoked the isolated local smoke for this CLI-owned simulation",
            },
        )
    assert http(
        api(state), f"/api/v1/incidents/{incident}/actions/ACT-DEMO/execute", {}
    )["verification"]["passed"]
    assert (
        http(
            api(state),
            "/api/v1/evidence/search",
            token=state["secrets"]["EVIDENCE_API_TOKEN"],
        )["status"]
        == "HEALTHY"
    )
    proxy = f"http://127.0.0.1:{state['ports']['LAB_WEB_PORT']}"
    assert http(proxy, "/api/v1/health")["product"] == "InvestiNator"
    if not health(state)["success"]:
        raise RuntimeError("Local integration health failed")
    print(
        "PASS: live local API/DB/search/metrics/proxy and approved SIMULATED recovery; no production action"
    )
    return 0


def ai_check(live):
    key, model = os.getenv("OPENAI_API_KEY"), os.getenv("INVESTINATOR_OPENAI_MODEL")
    if not key or not model:
        print(
            json.dumps(
                {
                    "status": "NOT_CONFIGURED",
                    "key_present": bool(key),
                    "model_present": bool(model),
                    "paid_call_made": False,
                }
            )
        )
        return 2
    if not live:
        print(
            json.dumps(
                {"status": "NOT_TESTED", "configured": True, "paid_call_made": False}
            )
        )
        return 0
    # The rate gate precedes every invocation; no provider call during builds/health.
    with lock():
        rate = RUNTIME / "ai-check-last-request"
        if rate.exists() and time.time() - float(rate.read_text()) < 60:
            raise RuntimeError(
                "Paid AI health probe limited to one request per minute per checkout"
            )
        if not (
            ROOT
            / "backend-java/target/classes/com/investinator/model/OpenAiLiveProbe.class"
        ).exists():
            maven("-DskipTests", "package")
        rate.write_text(str(time.time()))
        values = java_env()
        trust = Path("/etc/ssl/certs/java/cacerts")
        if trust.exists():
            values["JAVA_TOOL_OPTIONS"] = (
                values.get("JAVA_TOOL_OPTIONS", "")
                + f" -Djavax.net.ssl.trustStore={trust}"
            )
        binary = (
            str(Path(values["JAVA_HOME"]) / "bin/java")
            if values.get("JAVA_HOME")
            else "java"
        )
        raw = run(
            [
                binary,
                "-cp",
                classpath(),
                "com.investinator.model.OpenAiLiveProbe",
                "--live",
            ],
            env=values,
            capture=True,
            timeout=15,
        )
        try:
            report = json.loads(raw.splitlines()[-1])
        except (ValueError, IndexError):
            raise RuntimeError(
                "Java live probe returned an invalid diagnostic; no provider output was echoed"
            ) from None
        print(json.dumps(report))
        return 0 if report["status"] == "HEALTHY" else 1


def reset(state, confirmed):
    if not confirmed:
        raise RuntimeError("reset-demo requires --confirm-destroy-demo-data")
    names = [state["project"] + "_" + name for name in VOLUMES]
    print("Confirmed destruction of this local demo's volumes only:")
    for name in names:
        print(name)
    compose(state, "down")
    for name in names:
        result = subprocess.run(
            [
                *docker(),
                "volume",
                "inspect",
                name,
                "--format",
                '{{index .Labels "com.docker.compose.project"}}',
            ],
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            if result.stdout.strip() != state["project"]:
                raise RuntimeError("Volume ownership marker does not match")
            run([*docker(), "volume", "rm", name], capture=True)
    for name in (
        "kafka-seeded",
        "smoke.json",
        "signals.json",
        "demo-kafka-kraft.json",
        "legacy-zk-demo.json",
        "causal-kafka-kraft.json",
        "causal-kafka-legacy.json",
    ):
        (RUNTIME / name).unlink(missing_ok=True)
    fixture = RUNTIME / "fixtures"
    if fixture.is_symlink():
        raise RuntimeError("Fixture path cannot be a symlink")
    if fixture.exists():
        shutil.rmtree(fixture)
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "command",
        choices=[
            "up",
            "down",
            "status",
            "doctor",
            "health",
            "smoke",
            "test",
            "logs",
            "restart",
            "demo",
            "ai-check",
            "reset-demo",
            "monitor",
            "investigate",
        ],
    )
    parser.add_argument("target", nargs="?")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--all", action="store_true")
    parser.add_argument(
        "--service",
        choices=[
            *SERVICES,
            "java",
            "nextjs",
            "postgresql",
            "otel",
            "gpt",
            "spark",
            "yarn",
            "hdfs",
            "hive",
            "hive-metastore",
            "kudu",
            "nifi",
            "airflow",
            "zookeeper",
            "impala",
            "cloudera-manager",
            "atlas",
            "ranger",
        ],
    )
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--demo", action="store_true")
    parser.add_argument("--confirm-destroy-demo-data", action="store_true")
    args = parser.parse_args(argv)
    if args.command == "monitor":
        if not args.demo or args.target not in {"status", "pause", "resume"}:
            parser.error("Use monitor status|pause|resume --demo")
        state = load_state()
        token = state["secrets"].get("MONITOR_API_TOKEN")
        if not token:
            raise RuntimeError(
                "Configure an owned demo MONITOR_API_TOKEN and enable monitoring explicitly"
            )
        print(
            json.dumps(
                http(
                    api(state),
                    "/api/v1/monitor/" + args.target,
                    None if args.target == "status" else {},
                    token,
                )
            )
        )
        return 0

    if args.command in ("logs", "restart") and args.target == "api":
        args.target = "java"
    if args.command in ("logs", "restart") and args.target not in SERVICES:
        parser.error("Select an allowlisted local service: " + ", ".join(SERVICES))
    if args.command == "ai-check":
        return ai_check(args.live)
    if args.command == "up":
        up()
        return 0
    if args.command == "test":
        with lock():
            maven("verify")
            run([sys.executable, "-m", "unittest", "discover", "-s", "tests_cli"])
        return 0
    state = load_state()
    if args.command == "investigate":
        import lab_scenarios

        print(json.dumps(lab_scenarios.investigate(argparse.Namespace(http=http, RUNTIME=RUNTIME, api=api), state, args.target), indent=2))
        return 0
    if args.command == "doctor":
        return doctor(state, args.json)
    if args.command == "health":
        report = health(state)
        if args.service:
            aliases = {"db": "postgresql", "web": "nextjs", "collector": "otel"}
            target = aliases.get(args.service, args.service)
            report["checks"] = [c for c in report["checks"] if c["service"] == target]
            report["success"] = bool(report["checks"]) and all(
                c["status"] == "HEALTHY" for c in report["checks"]
            )
        display(report, args.json)
        return 0 if report["success"] else 1
    if args.command == "smoke":
        with lock():
            return smoke(state)
    if args.command == "status":
        print(compose(state, "ps", "--all", capture=True))
        return 0
    if args.command == "logs":
        compose(state, "logs", "--no-color", "--tail", "100", args.target)
        return 0
    with lock():
        if args.command == "down":
            compose(state, "down")
        elif args.command == "restart":
            compose(state, "restart", args.target)
        elif args.command == "reset-demo":
            return reset(state, args.confirm_destroy_demo_data)
        elif args.command == "demo":
            if args.target not in {"kafka-kraft", "kafka-legacy", "legacy-zk"}:
                parser.error("Choose kafka-kraft or kafka-legacy (representative protocol fixture)")
            import lab_scenarios

            mode = "KRAFT" if args.target == "kafka-kraft" else "LEGACY_ZOOKEEPER"
            print(json.dumps(lab_scenarios.create(argparse.Namespace(http=http, RUNTIME=RUNTIME, api=api), state, mode), indent=2))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (
        OSError,
        RuntimeError,
        ValueError,
        AssertionError,
        subprocess.SubprocessError,
    ) as error:
        # Never print provider exception bodies, credentials, or the process environment.
        message = (
            str(error)
            if isinstance(error, (RuntimeError, ValueError, TimeoutError))
            else type(error).__name__
        )
        print("InvestiNator: " + message, file=sys.stderr)
        raise SystemExit(1) from None
