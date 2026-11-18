"""Fixed SIMULATED causal streams over real owned Java/PostgreSQL transport."""
import datetime
import json
import re


def create(cli, state, mode):
    name = "kafka-kraft" if mode == "KRAFT" else "kafka-legacy"
    marker = cli.RUNTIME / ("causal-" + name + ".json")
    if marker.exists():
        result = json.loads(marker.read_text())
        cli.http(cli.api(state), "/api/v1/monitor/incidents/" + result["incident_id"],
                 token=state["secrets"]["MONITOR_API_TOKEN"])
        return result
    token = state["secrets"]["MONITOR_API_TOKEN"]
    previous = cli.http(cli.api(state), "/api/v1/monitor/status", token=token)
    if not previous["enabled"]:
        raise RuntimeError("Enable INVESTINATOR_MONITOR_ENABLED=true for the owned lab first")
    settings = previous["settings"]
    if settings.get("disabled_rules"):
        raise RuntimeError("Demo needs its declared rules enabled; preserve and review current overrides")
    base = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(seconds=90)
    stamp = lambda seconds: (base + datetime.timedelta(seconds=seconds)).isoformat()  # noqa: E731
    graph_id = "lab-" + name
    coordination = "controller" if mode == "KRAFT" else "zookeeper"
    nodes = [("network", "NETWORK"), (coordination, "CONTROLLER" if mode == "KRAFT" else "ZOOKEEPER"),
             ("broker", "BROKER"), ("consumer", "CONSUMER_GROUP"), ("stream", "SPARK_JOB"),
             ("dataset", "DATASET"), ("dashboard", "DASHBOARD")]
    graph = {"graph_id": graph_id, "revision": 1, "source_mode": "SIMULATED", "metadata_mode": mode,
             "platform_version": "Kafka 4.3" if mode == "KRAFT" else "Kafka legacy representative protocol fixture",
             "nodes": [{"id": i, "type": t, "name": "SIMULATED " + i} for i, t in nodes], "edges": []}
    for index in range(1, len(nodes)):
        graph["edges"].append({"id": "edge-" + str(index), "from": nodes[index][0], "to": nodes[index-1][0],
                               "type": "DEPENDS_ON", "source": "owned-lab-fixture", "confidence": 1.0,
                               "discovered_at": stamp(-20), "valid_from": stamp(-20), "valid_to": None,
                               "provenance": "SIMULATED declared topology, not discovered enterprise lineage"})
    cli.http(cli.api(state), "/api/v1/dependencies/fixtures", {"graph": graph, "observations": []},
             state["secrets"]["GRAPH_ADMIN_TOKEN"])
    phases = [("network", "network", {"network_loss_percent": 10}),
              (coordination, "kraft-controller" if mode == "KRAFT" else "legacy-session",
               {"controller_unavailable": 1} if mode == "KRAFT" else {"zk_session_expired": 1}),
              ("broker", "lag-isr", {"consumer_lag": 24000, "shrinking_isr": 1}),
              ("stream", "spark-retries", {"spark_retry_count": 12}),
              ("dataset", "freshness", {"freshness_sla_overdue_minutes": 20})]
    events = []
    for index, (resource, rule, measurements) in enumerate(phases):
        for sample in range(2):
            events.append({"event_id": graph_id + "-" + rule + "-" + str(sample), "tenant_id": "demo",
                           "environment": "demo", "cluster_id": graph_id, "graph_id": graph_id,
                           "graph_revision": 1, "resource": resource, "rule_id": rule,
                           "observed_at": stamp(index * 12 + sample * 3), "source_mode": "SIMULATED",
                           "measurements": measurements, "capabilities": list(measurements),
                           "excerpt": "SIMULATED " + rule + "; no physical enterprise fault asserted", "impact": None})
    try:
        cli.http(cli.api(state), "/api/v1/monitor/resume", {}, token)
        for event in events:
            cli.http(cli.api(state), "/api/v1/monitor/events", event, token)
        cli.http(cli.api(state), "/api/v1/monitor/tick", {}, token)
        report = cli.http(cli.api(state), "/api/v1/monitor/status", token=token)
        incidents = [i for i in report["incidents"] if i["graph_id"] == graph_id]
        if len(incidents) != 1:
            raise AssertionError("Declared causal stream must produce one bounded incident")
        result = {"incident_id": incidents[0]["id"], "graph_id": graph_id, "metadata_mode": mode,
                  "source_mode": "SIMULATED", "replay_events": events, "event_count": len(events), "incident_count": len(incidents),
                  "transport": "actual Java HTTP/PostgreSQL; KRaft transport probed separately",
                  "legacy_kind": "REPRESENTATIVE_PROTOCOL_FIXTURE" if mode != "KRAFT" else None,
                  "live_legacy_cluster_tested": False, "production_actions": 0, "paid_provider_calls": 0}
        marker.write_text(json.dumps(result, indent=2) + "\n")
        return result
    finally:
        if previous["paused"]:
            cli.http(cli.api(state), "/api/v1/monitor/pause", {}, token)


def investigate(cli, state, target):
    if target in (None, "kafka-kraft", "kafka-legacy", "legacy-zk"):
        name = "kafka-legacy" if target in ("kafka-legacy", "legacy-zk") else "kafka-kraft"
        marker = cli.RUNTIME / ("causal-" + name + ".json")
        if not marker.exists():
            raise RuntimeError("Run the corresponding named demo first")
        identity = json.loads(marker.read_text())["incident_id"]
    else:
        identity = target
    if not re.fullmatch(r"[A-Za-z0-9_.:-]{1,100}", identity):
        raise ValueError("Invalid incident identifier")
    prefix = "/api/v1/dependencies/incidents/" + identity
    token = state["secrets"]["INVESTIGATION_API_TOKEN"]
    graph = cli.http(cli.api(state), prefix + "/graph", token=token)
    result = cli.http(cli.api(state), prefix + "/investigation?graph=" + graph["graph_id"], token=token)
    return {"investigation": result, "paid_provider_calls": 0, "production_actions": 0,
            "note": "Deterministic read-only correlation; hypotheses are not proven root causes"}
