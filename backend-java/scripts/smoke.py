#!/usr/bin/env python3
"""Read-only HTTP acceptance checks for the local Java scaffold; never calls OpenAI."""
import argparse
import json
import urllib.error
import urllib.parse
import urllib.request


def check(base_url: str) -> None:
    parsed = urllib.parse.urlsplit(base_url)
    if parsed.hostname not in {"localhost", "127.0.0.1", "::1"} or parsed.scheme != "http":
        raise ValueError("This scaffold smoke targets local HTTP only")
    for group in ("liveness", "readiness"):
        with urllib.request.urlopen(f"{base_url}/actuator/health/{group}", timeout=5) as response:
            assert json.load(response) == {"status": "UP"}
    request = urllib.request.Request(f"{base_url}/api/v1/platform/health", headers={"X-Request-ID": "java-smoke"})
    with urllib.request.urlopen(request, timeout=5) as response:
        assert response.headers["X-Request-ID"] == "java-smoke"
        health = json.load(response)
        assert health["status"] == "NOT_IMPLEMENTED"
        assert health["source_mode"] == "NOT_TESTED"
        assert health["overall_health"]["score"] is None
    for path in ("/actuator/env", "/actuator/prometheus", "/api/v1/incidents"):
        try:
            urllib.request.urlopen(base_url + path, timeout=5)
        except urllib.error.HTTPError as error:
            assert error.code == 403, (path, error.code)
        else:
            raise AssertionError(f"Unauthenticated access unexpectedly allowed: {path}")
    print("PASS: local liveness, readiness, truthful scaffold contract, correlation, and denied diagnostics")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8081")
    check(parser.parse_args().base_url.rstrip("/"))
