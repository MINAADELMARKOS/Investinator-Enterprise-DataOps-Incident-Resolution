#!/usr/bin/env python3
"""Bounded read-only owned-lab workload; not production SLO or long-duration certification."""
import argparse
import importlib.util
import json
import math
import re
import statistics
import time
import urllib.request
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait
from pathlib import Path

spec = importlib.util.spec_from_file_location("cli", Path(__file__).with_name("investinator_cli.py"))
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seconds", type=int, default=30)
    parser.add_argument("--max-requests", type=int, default=500)
    parser.add_argument("--heap-budget-mib", type=int, default=1024)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not 5 <= args.seconds <= 300 or not 20 <= args.max_requests <= 5000 or not 64 <= args.heap_budget_mib <= 4096:
        parser.error("Bounded seconds/requests/lab heap budget required")
    state = cli.load_state()
    start = time.monotonic()
    observations, errors, metric_samples = [], [], []
    submitted = 0
    next_submission = start
    metrics_token = state["secrets"].get("METRICS_TOKEN")
    if not metrics_token:
        raise RuntimeError("Owned lab metrics capability NOT_CONFIGURED")

    def sample_metrics():
        request = urllib.request.Request(cli.api(state) + "/actuator/prometheus", headers={"Authorization": "Bearer " + metrics_token})
        with cli.OPENER.open(request, timeout=4) as response:
            raw = response.read(2000001)
        if len(raw) > 2000000:
            raise RuntimeError("Metrics bound exceeded")
        heaps, queues = [], []
        for line in raw.decode("utf-8", errors="strict").splitlines():
            match = re.fullmatch(r'(jvm_memory_used_bytes|investinator_monitor_events)(\{[^}]*\})? (\S+)', line)
            if not match:
                continue
            name, tags, text = match.groups()
            value = float(text)
            if not math.isfinite(value) or value < 0:
                continue
            if name == "jvm_memory_used_bytes" and 'area="heap"' in (tags or ""):
                heaps.append(value)
            if name == "investinator_monitor_events" and 'state="QUEUED"' in (tags or ""):
                queues.append(value)
        return {"elapsed_seconds": round(time.monotonic() - start, 3),
                "heap_bytes": sum(heaps) if heaps else None,
                "queued": sum(queues) if queues else None}

    def request_once(sequence):
        began = time.monotonic()
        # Fixed owned loopback GETs only. No chat, writes, remote destination or arbitrary URL.
        path = "/api/v1/health" if sequence % 2 else "/api/v1/incidents"
        try:
            with cli.OPENER.open(cli.api(state) + path, timeout=4) as response:
                data = response.read(2000001)
                if response.status != 200 or len(data) > 2000000:
                    raise RuntimeError("Read failed or exceeded body bound")
            return round((time.monotonic() - began) * 1000, 3), None
        except Exception as error:
            return None, type(error).__name__

    metric_samples.append(sample_metrics())
    next_sample = time.monotonic() + 2
    with ThreadPoolExecutor(max_workers=4) as pool:
        pending = set()
        while pending or submitted < args.max_requests and time.monotonic() - start < args.seconds:
            while len(pending) < 4 and submitted < args.max_requests and time.monotonic() - start < args.seconds and time.monotonic() >= next_submission:
                pending.add(pool.submit(request_once, submitted))
                submitted += 1
                next_submission += args.seconds / args.max_requests
            if not pending:
                time.sleep(min(0.02, max(0, next_submission - time.monotonic())))
                continue
            done, pending = wait(pending, timeout=0.25, return_when=FIRST_COMPLETED)
            for future in done:
                elapsed, error = future.result()
                if error:
                    errors.append(error)
                else:
                    observations.append(elapsed)
            if time.monotonic() >= next_sample:
                metric_samples.append(sample_metrics())
                next_sample = time.monotonic() + 2
    metric_samples.append(sample_metrics())
    known = all(v["heap_bytes"] is not None and v["queued"] is not None for v in metric_samples)
    within = known and all(v["heap_bytes"] <= args.heap_budget_mib * 1024 * 1024 and v["queued"] <= 1000 for v in metric_samples)
    observations.sort()
    report = {"status": "PASS_BOUNDED_LOCAL_ONLY" if not errors and within else "FAIL_OR_UNKNOWN",
              "source_mode": "LIVE_LOCAL_HTTP_ON_SIMULATED_DATA", "maximum_concurrency": 4, "pacing": "evenly spaced over requested duration",
              "duration_limit_seconds": args.seconds, "elapsed_seconds": round(time.monotonic() - start, 3),
              "submitted": submitted, "successes": len(observations), "errors": len(errors),
              "error_classes": sorted(set(errors)), "heap_budget_mib": args.heap_budget_mib,
              "queue_budget": 1000, "metric_samples": metric_samples,
              "sampled_memory_only_not_continuous_peak": True, "long_duration_soak": "NOT_TESTED",
              "p50_ms": statistics.median(observations) if observations else None,
              "p95_ms": observations[math.ceil(len(observations) * 0.95) - 1] if observations else None,
              "paid_calls": 0, "writes": 0, "production_slo": "NOT_MEASURED", "business_roi": "NOT_MEASURED"}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({k: report[k] for k in ("status", "submitted", "errors", "elapsed_seconds")}))
    if report["status"] != "PASS_BOUNDED_LOCAL_ONLY":
        raise RuntimeError("Bounded local read workload failed or metrics UNKNOWN")


if __name__ == "__main__":
    main()
