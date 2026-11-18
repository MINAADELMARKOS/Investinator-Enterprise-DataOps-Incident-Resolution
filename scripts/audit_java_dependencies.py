#!/usr/bin/env python3
"""Audit a resolved Maven dependency tree with OSV; unavailable service fails explicitly."""
import argparse
import json
import urllib.error
import urllib.request
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tree", type=Path, default=Path("backend-java/target/dependency-tree.json"))
    parser.add_argument("--output", type=Path, default=Path("backend-java/target/dependency-audit.json"))
    args = parser.parse_args()
    dependencies = set()

    def walk(node):
        if node.get("groupId") != "com.investinator":
            dependencies.add((node["groupId"] + ":" + node["artifactId"], node["version"]))
        for child in node.get("children", []):
            walk(child)

    walk(json.loads(args.tree.read_text()))
    packages = sorted(dependencies)
    findings = []
    report = {"scanner": "OSV v1/querybatch", "source": "resolved Maven dependency tree incl test scope",
              "packages": len(packages), "status": "BLOCKED", "findings": findings}
    try:
        for start in range(0, len(packages), 100):
            batch = packages[start:start+100]
            payload = {"queries": [{"package": {"ecosystem": "Maven", "name": name}, "version": version}
                                   for name, version in batch]}
            request = urllib.request.Request("https://api.osv.dev/v1/querybatch",
                                             data=json.dumps(payload).encode(),
                                             headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(request, timeout=30) as response:
                results = json.load(response)["results"]
            if len(results) != len(batch):
                raise ValueError("Incomplete scanner response")
            for package, result in zip(batch, results, strict=True):
                for vuln in result.get("vulns", []):
                    findings.append({"package": package[0], "version": package[1], "id": vuln["id"]})
        report["status"] = "FAIL" if findings else "PASS"
    except (OSError, ValueError, KeyError) as error:
        report.update(reason="OSV query unavailable or incomplete", error_type=type(error).__name__,
                      owner="build/security maintainer", action="Run in permitted CI egress; no clean audit asserted")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({k: v for k, v in report.items() if k != "findings"}))
    return 0 if report["status"] == "PASS" else 1 if report["status"] == "FAIL" else 2


if __name__ == "__main__":
    raise SystemExit(main())
