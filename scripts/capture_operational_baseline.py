#!/usr/bin/env python3
"""Bounded read-only local lab observations; no provider call or financial assumption."""
import argparse
import importlib.util
import json
import math
import statistics
import time
import urllib.request
from pathlib import Path

spec = importlib.util.spec_from_file_location("cli", Path(__file__).with_name("investinator_cli.py"))
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--samples", type=int, default=30)
    parser.add_argument("--output", type=Path, default=cli.RUNTIME / "operational-baseline.json")
    args = parser.parse_args()
    if not 5 <= args.samples <= 100:
        parser.error("samples must be 5..100")
    state = cli.load_state()
    observations = {}
    for path in ("/api/v1/health", "/api/v1/incidents"):
        values = []
        for _ in range(args.samples):
            start = time.monotonic()
            cli.http(cli.api(state), path, timeout=4)
            values.append((time.monotonic()-start)*1000)
        values.sort()
        observations[path] = {"requests": len(values), "errors": 0, "median_ms": round(statistics.median(values), 3), "p95_ms": round(values[math.ceil(len(values)*0.95)-1], 3), "max_ms": round(max(values), 3)}
    health = cli.health(state)
    request = urllib.request.Request(cli.api(state)+"/actuator/prometheus", headers={"Authorization":"Bearer "+state["secrets"]["METRICS_TOKEN"]})
    with cli.OPENER.open(request, timeout=5) as response:
        raw = response.read(2000001)
    if len(raw)>2000000:
        raise RuntimeError("Metrics snapshot exceeds safe limit")
    prefixes=("investinator_monitor_", "investinator_chat_", "investinator_provider_", "jvm_gc_pause_seconds_count", "jvm_memory_used_bytes", "hikaricp_connections_active", "system_cpu_usage")
    metrics=[line for line in raw.decode().splitlines() if line.startswith(prefixes)]
    report = {"source_mode": "LIVE_LOCAL_TRANSPORT", "workload": "SIMULATED_DATA", "concurrency": 1,
              "measurements": observations, "metrics_snapshot": metrics, "metrics_scope": "application-lifetime fixed dimensions; missing counters unknown, not financial totals", "core_readiness": health["success"],
              "checks": [{k: c.get(k) for k in ("service", "status", "source_mode", "duration_ms", "checked_at")} for c in health["checks"]],
              "production_availability": "NOT_MEASURED", "production_detection_latency": "NOT_MEASURED",
              "production_mtta_mtti_mttr": "NOT_MEASURED", "business_roi": "NOT_MEASURED", "gpt_cost": None,
              "paid_provider_calls": 0, "production_writes": 0}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2)+"\n")
    print(json.dumps({"core_readiness": report['core_readiness'], "samples_per_route": args.samples, "paid_provider_calls": 0}))


if __name__ == "__main__":
    main()
