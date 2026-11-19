#!/usr/bin/env python3
"""Generate public source inventories only; never reads environment values or runtime secrets."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def generate():
    base = ROOT / "backend-java/src/main"
    modules = {}
    routes = []
    variables = {}
    for path in sorted((base / "java/com/investinator").rglob("*.java")):
        text = path.read_text()
        relative = str(path.relative_to(ROOT))
        parts = path.relative_to(base / "java/com/investinator").parts
        module = parts[0] if len(parts)>1 else "bootstrap"
        modules.setdefault(module, []).append(relative)
        for name in re.findall(r'(?:getProperty|getRequiredProperty)\("([A-Z][A-Z0-9_]+)"', text):
            variables.setdefault(name, set()).add(relative)
        if "@RestController" not in text:
            continue
        header = text.split("public class", 1)[0]
        prefix_match = re.search(r'@RequestMapping\(\s*"([^"]+)"\s*\)', header)
        prefix = prefix_match.group(1) if prefix_match else ""
        condition = re.findall(r'@ConditionalOnProperty\(([^\n]+)\)', header)
        for match in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(([^)]*)\))?', text):
            declared = re.findall(r'"(/[^"\n]*)"', match.group(2) or "")
            for route in declared or ([""] if match.group(2) is None else []):
                routes.append({"method": match.group(1).upper(), "path": prefix+route,
                               "source": relative, "line": text[:match.start()].count("\n")+1,
                               "bean_condition": condition})
    for path in sorted((base / "resources").glob("application*.yaml")):
        for name in re.findall(r'\$\{([A-Z][A-Z0-9_]+)(?::[^}]*)?\}', path.read_text()):
            variables.setdefault(name, set()).add(str(path.relative_to(ROOT)))
    for path in [ROOT/"infra/lab/compose.yaml", ROOT/"docker-compose.yml"]:
        for name in re.findall(r'\$\{([A-Z][A-Z0-9_]+)', path.read_text()):
            variables.setdefault(name, set()).add(str(path.relative_to(ROOT)))
    tables = []
    migrations = sorted((base / "resources/db/migration").glob("V*.sql"))
    for path in migrations:
        for name in re.findall(r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?([a-z_0-9]+)', path.read_text(), re.I):
            tables.append({"table": name, "migration": path.name})
    return {"method": "source inventory, not runtime OpenAPI or endpoint authorization proof",
            "module_files": modules, "routes": routes,
            "framework_routes": ["GET /actuator/health/liveness", "GET /actuator/health/readiness", "GET /actuator/prometheus (scoped token)"],
            "configuration_variable_sources": {k: sorted(v) for k,v in sorted(variables.items())},
            "configuration_inventory_limit": "Static names; dynamic CONNECTOR_<SERVICE>_* families documented separately. No private values read.",
            "schema_owner": "Java Flyway: investinator_java by default; Python Alembic remains separate",
            "migrations": [p.name for p in migrations], "tables": tables,
            "additional_framework_table": "flyway_schema_history"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    target = ROOT / "docs/handover/source-inventory.json"
    content = json.dumps(generate(), indent=2)+"\n"
    if args.check:
        if not target.exists() or target.read_text() != content:
            raise SystemExit("Handover inventory is stale; regenerate from source")
    else:
        target.write_text(content)
    print("PASS source-only handover inventory" + (" freshness" if args.check else " generation"))


if __name__ == "__main__":
    main()
