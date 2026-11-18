#!/usr/bin/env python3
"""Local-only evidence lab: secure bootstrap or disposable collector/index/Java/Prometheus acceptance."""
import argparse
import base64
import json
import os
import secrets
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DOCKER = ["docker", "--host=unix:///var/run/docker.sock"]
SEARCH_IMAGE = "opensearchproject/opensearch:3.4.0@sha256:1647ce2e07371f1ac8bbad28890a64ef77c5a694a61f656120f0ace09e66bf48"
FLUENT_IMAGE = "fluent/fluent-bit:4.2.2@sha256:7d727245767ae632eb296c2ff4d206bf2e205b5f244c1f37b8fdd61f9fb33985"
OTEL_IMAGE = "otel/opentelemetry-collector-contrib:0.121.0@sha256:789689988e379c58ac12b07718dbcf4b23c2214bd804173c1c77af346d381c15"
PROM_IMAGE = "prom/prometheus:v2.55.1@sha256:2659f4c2ebb718e7695cb9b25ffa7d6be64db013daba13e05c875451cf51b0d3"
INDEX = "investinator-evidence"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def docker(*args, **kwargs):
    return subprocess.check_output([*DOCKER, *args], text=True, **kwargs).strip()


def port(name, internal):
    return docker("port", name, str(internal)).rsplit(":", 1)[1]


def request(base, path, body=None, user=None, password=None, token=None, method=None, timeout=10):
    headers = {"Content-Type": "application/json"}
    if user is not None:
        headers["Authorization"] = "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode()
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(base + path, data=None if body is None else json.dumps(body).encode(), headers=headers, method=method)
    with OPENER.open(req, timeout=timeout) as response:
        return json.load(response)


def wait_for(probe, seconds=90):
    deadline = time.monotonic() + seconds
    last = None
    while time.monotonic() < deadline:
        try:
            result = probe()
            if result:
                return result
        except (OSError, ValueError, AssertionError) as error:
            last = type(error).__name__
        time.sleep(.5)
    raise TimeoutError(f"Local fixture readiness deadline exceeded ({last})")


def bootstrap(base, admin_password, writer_password, reader_password, apply_retention=False):
    if urllib.parse.urlsplit(base).hostname != "127.0.0.1":
        raise ValueError("Bootstrap is restricted to the local demo endpoint")

    def admin(path, body=None, method=None):
        try:
            return request(base, path, body, "admin", admin_password, method=method)
        except urllib.error.HTTPError as error:
            if error.code == 400 and path.startswith("/_ingest/"):
                diagnostic = error.read().decode()
                for secret in (admin_password, writer_password, reader_password):
                    diagnostic = diagnostic.replace(secret, "[REDACTED]")
                destination = ROOT / "backend-java/target/evidence-bootstrap-error.json"
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_text(diagnostic)
                raise RuntimeError("Search validation pipeline was rejected; inspect redacted target/evidence-bootstrap-error.json") from error
            raise

    info = admin("/")
    if info["version"]["number"] != "3.4.0":
        raise RuntimeError("Use the verified compatible OpenSearch 3.4.0 server / official Java 3.x client")
    admin("/_ingest/pipeline/investinator-evidence-validation", json.loads((ROOT / "infra/evidence/pipeline.json").read_text()), "PUT")
    try:
        existing = admin(f"/{INDEX}/_mapping")
        if existing[INDEX]["mappings"].get("_meta", {}).get("schema_version") != 1:
            raise RuntimeError("Existing evidence schema needs a reviewed migration")
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        admin(f"/{INDEX}", json.loads((ROOT / "infra/evidence/index.json").read_text()), "PUT")
    try:
        admin("/investinator-traces/_mapping")
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        admin("/investinator-traces", json.loads((ROOT / "infra/evidence/trace-index.json").read_text()), "PUT")
    for role, privileges in (("evidence_writer", ["indices:data/write/bulk*", "indices:data/write/index*"]), ("evidence_reader", ["read", "indices:admin/mappings/get", "indices:admin/get"])):
        cluster = ["cluster_monitor"] + (["indices:data/write/bulk"] if role == "evidence_writer" else [])
        admin(f"/_plugins/_security/api/roles/{role}", {"cluster_permissions": cluster, "index_permissions": [{"index_patterns": [INDEX, "investinator-traces"], "allowed_actions": privileges}]}, "PUT")
    for user, password in (("evidence_writer", writer_password), ("evidence_reader", reader_password)):
        admin(f"/_plugins/_security/api/internalusers/{user}", {"password": password}, "PUT")
        admin(f"/_plugins/_security/api/rolesmapping/{user}", {"users": [user]}, "PUT")
    # Definition alone deletes nothing; attachment requires an explicit operator flag.
    try:
        admin("/_plugins/_ism/policies/investinator-demo-7d")
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        admin("/_plugins/_ism/policies/investinator-demo-7d", json.loads((ROOT / "infra/evidence/retention-policy.json").read_text()), "PUT")
    if apply_retention:
        print("Applying explicit seven-day deletion retention to the local demo evidence index")
        admin(f"/_plugins/_ism/add/{INDEX}", {"policy_id": "investinator-demo-7d"}, "POST")
    return admin


def verify():
    suffix = secrets.token_hex(5)
    network = f"investinator-evidence-test-{suffix}"
    names = {name: f"{network}-{name}" for name in ("es", "collector", "fluent", "prom")}
    env = os.environ.copy()
    for key in ("EVIDENCE_ADMIN_PASSWORD", "EVIDENCE_WRITER_PASSWORD", "EVIDENCE_READER_PASSWORD", "EVIDENCE_API_TOKEN"):
        env[key] = "Aa1!" + secrets.token_urlsafe(24)
    env["OPENSEARCH_INITIAL_ADMIN_PASSWORD"] = env["EVIDENCE_ADMIN_PASSWORD"]
    for key in ("no_proxy", "NO_PROXY"):
        env[key] = env.get(key, "") + ",evidence-search,evidence-collector,localhost,127.0.0.1"
    java = str(Path(env["JAVA_HOME"]) / "bin/java") if env.get("JAVA_HOME") else "java"
    process = None
    try:
        docker("network", "create", network)
        docker("run", "-d", "--name", names["es"], "--network", network, "--network-alias", "evidence-search", "-p", "127.0.0.1::9200", "-e", "OPENSEARCH_INITIAL_ADMIN_PASSWORD", "-e", "discovery.type=single-node", "-e", "plugins.security.ssl.http.enabled=false", "-e", "node.store.allow_mmap=false", "-e", "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m", SEARCH_IMAGE, env=env)
        es = "http://127.0.0.1:" + port(names["es"], 9200)
        wait_for(lambda: request(es, "/_cluster/health?wait_for_status=yellow&timeout=1s", user="admin", password=env["EVIDENCE_ADMIN_PASSWORD"])["status"] in ("green", "yellow"), 120)
        admin = bootstrap(es, env["EVIDENCE_ADMIN_PASSWORD"], env["EVIDENCE_WRITER_PASSWORD"], env["EVIDENCE_READER_PASSWORD"])
        with tempfile.TemporaryDirectory(prefix="investinator-evidence-fixture-") as directory:
            directory = Path(directory)
            directory.chmod(0o755)
            fixture = directory / "fixtures"
            fixture.mkdir(mode=0o755)
            classpath = ROOT / "backend-java/target/classes"
            # Json encoding requires dependency jars; use Maven's captured runtime classpath.
            cp = str(classpath) + os.pathsep + (ROOT / "backend-java/target/runtime-classpath.txt").read_text().strip()
            subprocess.run([java, "-cp", cp, "com.investinator.evidence.EvidenceFixtureGenerator", str(fixture)], check=True, stdout=subprocess.DEVNULL)
            for file in fixture.iterdir():
                file.chmod(0o644)
            exports = directory / "exports"
            exports.mkdir(mode=0o777)
            # Only synthetic local fixture traces live here. No credentials are written to mounts.
            exports.chmod(0o777)
            docker("run", "-d", "--name", names["collector"], "--network", network, "--network-alias", "evidence-collector", "-p", "127.0.0.1::4318", "-p", "127.0.0.1::13133", "--mount", f"type=bind,src={ROOT / 'infra/evidence/collector.yaml'},dst=/etc/otelcol/config.yaml,readonly", "--mount", f"type=bind,src={exports},dst=/exports", OTEL_IMAGE, "--config=/etc/otelcol/config.yaml")
            diagnostic_config = directory / "forwarder.conf"
            diagnostic_config.write_text((ROOT / "infra/evidence/fluent-bit.conf").read_text().replace("Index              investinator-traces", "Index              investinator-traces\n    Trace_Error        On"))
            diagnostic_config.chmod(0o644)
            docker("run", "-d", "--name", names["fluent"], "--network", network, "-e", "EVIDENCE_WRITER_PASSWORD", "-e", "NO_PROXY", "-e", "no_proxy", "--mount", f"type=bind,src={diagnostic_config},dst=/fluent-bit/etc/fluent-bit.conf,readonly", "--mount", f"type=bind,src={ROOT / 'infra/evidence/evidence-parsers.conf'},dst=/fluent-bit/etc/evidence-parsers.conf,readonly", "--mount", f"type=bind,src={fixture},dst=/fixtures,readonly", "--mount", f"type=bind,src={exports},dst=/exports,readonly", "--tmpfs", "/buffers", FLUENT_IMAGE, env=env)
            health = "http://127.0.0.1:" + port(names["collector"], 13133)
            wait_for(lambda: request(health, "/")["status"] == "Server available", 30)
            wait_for(lambda: admin(f"/{INDEX}/_count")["count"] == 4, 60)
            admin(f"/{INDEX}/_refresh", {}, "POST")
            # Reader is genuinely read-only; writer is not permitted to search.
            for user, password, path, body in (("evidence_reader", env["EVIDENCE_READER_PASSWORD"], f"/{INDEX}/_doc/not-allowed", {"message": "denied"}), ("evidence_writer", env["EVIDENCE_WRITER_PASSWORD"], f"/{INDEX}/_search", {})):
                try:
                    request(es, path, body, user, password)
                except urllib.error.HTTPError as error:
                    assert error.code == 403
                else:
                    raise AssertionError("Index role exceeded its intended privileges")
            otlp = "http://127.0.0.1:" + port(names["collector"], 4318)
            now = time.time_ns()
            request(otlp, "/v1/traces", {"resourceSpans": [{"resource": {"attributes": [{"key": "service.name", "value": {"stringValue": "fixture-producer"}}, {"key": "source_mode", "value": {"stringValue": "SIMULATED"}}]}, "scopeSpans": [{"scope": {"name": "investinator-fixture"}, "spans": [{"traceId": "0123456789abcdef0123456789abcdef", "spanId": "1111111111111111", "name": "synthetic-observation", "kind": 1, "startTimeUnixNano": str(now - 1000000000), "endTimeUnixNano": str(now)}]}]}]})
            request(otlp, "/v1/metrics", {"resourceMetrics": [{"resource": {"attributes": [{"key": "service.name", "value": {"stringValue": "fixture-producer"}}]}, "scopeMetrics": [{"scope": {"name": "investinator-fixture"}, "metrics": [{"name": "evidence_fixture_records", "gauge": {"dataPoints": [{"timeUnixNano": str(now), "asInt": "4", "attributes": [{"key": "source_mode", "value": {"stringValue": "SIMULATED"}}, {"key": "tenant_id", "value": {"stringValue": "demo"}}]}]}}]}]}]})
            wait_for(lambda: admin("/investinator-traces/_count")["count"] >= 1, 45)
            docker("run", "-d", "--name", names["prom"], "--network", network, "-p", "127.0.0.1::9090", "--mount", f"type=bind,src={ROOT / 'infra/evidence/prometheus.yaml'},dst=/etc/prometheus/prometheus.yml,readonly", PROM_IMAGE)
            prom = "http://127.0.0.1:" + port(names["prom"], 9090)
            query = urllib.parse.urlencode({"query": '{__name__=~"evidence_fixture_records.*"}'})
            metrics = wait_for(lambda: request(prom, "/api/v1/query?" + query)["data"]["result"], 45)
            assert float(metrics[0]["value"][1]) == 4
            with socket.socket() as sock:
                sock.bind(("127.0.0.1", 0))
                api_port = sock.getsockname()[1]
            env.update(EVIDENCE_SEARCH_ENABLED="true", EVIDENCE_SEARCH_URL=es, EVIDENCE_SEARCH_USER="evidence_reader", EVIDENCE_SEARCH_PASSWORD=env["EVIDENCE_READER_PASSWORD"], JAVA_PORT=str(api_port), JAVA_BIND_ADDRESS="127.0.0.1", JAVA_PERSISTENCE_ENABLED="false", INVESTINATOR_DEMO_MODE="true", INVESTINATOR_OPENAI_ENABLED="false")
            java_log = ROOT / "backend-java/target/evidence-lab-java.log"
            log = open(java_log, "w")
            process = subprocess.Popen([java, "-jar", str(ROOT / "backend-java/target/backend-java-0.1.0-SNAPSHOT.jar")], env=env, stdout=log, stderr=subprocess.STDOUT)
            api = f"http://127.0.0.1:{api_port}"
            wait_for(lambda: request(api, "/actuator/health/readiness")["status"] == "UP", 45)
            token = env["EVIDENCE_API_TOKEN"]
            for credential in (None, "wrong-token"):
                try:
                    request(api, "/api/v1/evidence/search", token=credential)
                except urllib.error.HTTPError as error:
                    assert error.code == 403
                else:
                    raise AssertionError("Unauthenticated evidence access was allowed")
            health_result = request(api, "/api/v1/evidence/health", token=token)
            assert health_result["status"] == "HEALTHY"
            result = request(api, "/api/v1/evidence/search", token=token)
            assert result["status"] == "HEALTHY" and result["retrieval_mode"] == "LIVE" and len(result["items"]) == 3
            assert all(item["tenant_id"] == "demo" and item["source_mode"] == "SIMULATED" and item["evidence_id"].startswith("EV-") for item in result["items"])
            assert all(item["trace_id"] == "0123456789abcdef0123456789abcdef" for item in result["items"])
            encoded = json.dumps(result)
            assert "fixture-secret" not in encoded and "sre@example.com" not in encoded and "FOREIGN_TENANT_RECORD" not in encoded
            assert "[REDACTED]" in encoded
            pages = [request(api, f"/api/v1/evidence/search?size=1&page={i}", token=token)["items"][0]["evidence_id"] for i in range(3)]
            assert len(set(pages)) == 3
            filtered = request(api, "/api/v1/evidence/search?service=kraft-controller&q=heartbeat", token=token)
            assert len(filtered["items"]) == 1
            for suffix in ("?tenant_id=other", "?url=http://example.com", "?q=%7B%22match_all%22%3A%7B%7D%7D", "?size=51", "?page=99999", "?from=not-a-time"):
                try:
                    request(api, "/api/v1/evidence/search" + suffix, token=token)
                except urllib.error.HTTPError as error:
                    assert error.code == 422
                else:
                    raise AssertionError("Unsafe/unbounded search input was accepted")
            assert admin(f"/{INDEX}/_count")["count"] == 4
            docker("stop", names["es"])
            started = time.monotonic()
            try:
                request(api, "/api/v1/evidence/search", token=token)
            except urllib.error.HTTPError as error:
                assert error.code == 503
                assert json.load(error)["status"] == "UNAVAILABLE"
            else:
                raise AssertionError("Unavailable search returned success")
            assert time.monotonic() - started < 10
            assert request(api, "/actuator/health/liveness")["status"] == "UP"
            process.terminate()
            process.wait(timeout=20)
            log.close()
        print(json.dumps({"status": "PASS", "source_mode": "SIMULATED", "retrieval_mode": "LIVE", "normalized_documents": 4, "visible_tenant_documents": 3, "deduplication": "PASS", "redaction": "PASS", "tenant_filter": "PASS", "read_only_roles": "PASS", "pagination_and_filters": "PASS", "unsafe_inputs_denied": "PASS", "trace_arrival": "PASS", "prometheus_query": "PASS", "unavailable_search": "PASS", "no_paid_model_call": True}))
    except Exception:
        destination = ROOT / "backend-java/target/evidence-lab-logs"
        destination.mkdir(parents=True, exist_ok=True)
        java_log = ROOT / "backend-java/target/evidence-lab-java.log"
        if java_log.exists():
            text = java_log.read_text()
            for key in ("EVIDENCE_ADMIN_PASSWORD", "EVIDENCE_WRITER_PASSWORD", "EVIDENCE_READER_PASSWORD", "EVIDENCE_API_TOKEN"):
                text = text.replace(env[key], "[REDACTED]")
            (destination / "java.log").write_text(text)
        for key, name in names.items():
            output = subprocess.run([*DOCKER, "logs", name], capture_output=True, text=True)
            (destination / f"{key}.log").write_text(output.stdout + output.stderr)
        raise
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            process.wait(timeout=20)
        for name in names.values():
            subprocess.run([*DOCKER, "rm", "-f", "-v", name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([*DOCKER, "network", "rm", network], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["verify", "bootstrap"], default="verify", nargs="?")
    parser.add_argument("--port", type=int, default=19200)
    parser.add_argument("--confirm-seven-day-retention-deletion", action="store_true")
    args = parser.parse_args()
    if args.command == "verify":
        verify()
    else:
        for name in ("EVIDENCE_ADMIN_PASSWORD", "EVIDENCE_WRITER_PASSWORD", "EVIDENCE_READER_PASSWORD"):
            if not os.getenv(name):
                raise SystemExit(f"Set {name} securely in the local process environment")
        bootstrap(f"http://127.0.0.1:{args.port}", os.environ["EVIDENCE_ADMIN_PASSWORD"], os.environ["EVIDENCE_WRITER_PASSWORD"], os.environ["EVIDENCE_READER_PASSWORD"], args.confirm_seven_day_retention_deletion)
        print("Local evidence index, validation pipeline and scoped identities prepared; no credentials printed")
