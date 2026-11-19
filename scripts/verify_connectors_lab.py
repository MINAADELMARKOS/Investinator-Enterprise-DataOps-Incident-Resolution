#!/usr/bin/env python3
"""Owned local lab authorization/persistence acceptance. Never contacts a private cluster."""

import argparse
import importlib.util
import json
import re
import urllib.error
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "cli", Path(__file__).resolve().parent / "investinator_cli.py"
)
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)


def kafka_verify(state):
    values = cli.java_env()
    values.update(
        KAFKA_BOOTSTRAP_SERVERS=f"127.0.0.1:{state['ports']['LAB_KAFKA_PORT']}",
        KAFKA_SECURITY_PROTOCOL="SASL_PLAINTEXT",
        KAFKA_SASL_MECHANISM="PLAIN",
        KAFKA_SASL_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="health" password="'
        + state["secrets"]["KAFKA_DIAGNOSTIC_PASSWORD"]
        + '";',
    )
    binary = (
        str(Path(values["JAVA_HOME"]) / "bin/java")
        if values.get("JAVA_HOME")
        else "java"
    )
    output = cli.run(
        [
            binary,
            "-cp",
            cli.classpath(),
            "com.investinator.health.KafkaReadOnlyVerifier",
            "--owned-local-fixture",
            state["cluster_id"],
        ],
        env=values,
        capture=True,
        timeout=30,
    )
    match = re.search(r"DATA: (\d+):(\d+)", output)
    assert match and "PASS: diagnostic principal" in output
    return [int(v) for v in match.groups()]


def expect_http(state, path, status, body=None, token=None):
    try:
        cli.http(cli.api(state), path, body, token)
    except urllib.error.HTTPError as e:
        assert e.code == status, (path, e.code)
    else:
        raise AssertionError("Unexpected permission success")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    state = cli.load_state()
    token = state["secrets"]["CONNECTOR_API_TOKEN"]
    with cli.lock():
        expect_http(state, "/api/v1/connectors", 403)
        catalog = cli.http(cli.api(state), "/api/v1/connectors", token=token)
        assert (
            catalog["scope"] == "diagnostics.read" and len(catalog["connectors"]) == 14
        )
        report = cli.http(
            cli.api(state), "/api/v1/connectors/kafka/health", token=token
        )
        assert (
            report["status"] == "HEALTHY"
            and report["details"]["metadata_mode"] == "KRAFT"
        )
        assert (
            report["details"]["lag_status"] == "OBSERVED"
            and report["details"]["max_consumer_lag"] == 0
        )
        expect_http(
            state,
            "/api/v1/connectors/kafka/health?url=http://outside.invalid",
            422,
            token=token,
        )
        expect_http(state, "/api/v1/connectors/kafka/health", 403, body={}, token=token)
        before = kafka_verify(state)
        cli.compose(state, "down", capture=True)
    cli.up()
    state = cli.load_state()
    with cli.lock():
        after = kafka_verify(state)
        assert before == after, "Kafka records/offsets changed across down/up"
        assert cli.health(state)["success"]
        encoded = json.dumps(catalog) + json.dumps(report)
        assert all(secret not in encoded for secret in state["secrets"].values())
        result = {
            "kafka_source": "LIVE local SASL KRaft; data SIMULATED",
            "diagnostic_create_topic": "DENIED",
            "diagnostic_produce": "DENIED",
            "fixture_end_and_committed_before": before,
            "fixture_end_and_committed_after": after,
            "down_up_data_retained": True,
            "connector_api_auth_and_parameter_boundary": "PASS",
            "private_live_connectors": "NOT_CONFIGURED",
            "paid_openai_call": False,
        }
        folder = cli.ROOT / "docs/implementation/prompt06"
        folder.mkdir(exist_ok=True)
        (args.output or folder / "lab-result.json").write_text(json.dumps(result, indent=2) + "\n")
        print(
            "PASS: scoped connector API, denied Kafka writes, persisted data/offsets across down/up"
        )


if __name__ == "__main__":
    main()
