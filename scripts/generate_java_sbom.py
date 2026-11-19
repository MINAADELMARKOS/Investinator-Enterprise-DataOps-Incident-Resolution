#!/usr/bin/env python3
"""CycloneDX from resolved Maven metadata. Does NOT inspect classes nested in shaded/vendor jars."""
import argparse
import hashlib
import json
import re
import uuid
from pathlib import Path
from urllib.parse import quote


def generate(tree, tree_bytes, sha):
    if not re.fullmatch(r"[a-f0-9]{40}", sha):
        raise ValueError("Exact source SHA required")
    components, edges = {}, {}

    def walk(node, root=False):
        group, name, version = (node.get(k, "") for k in ("groupId", "artifactId", "version"))
        if not all(isinstance(v, str) and re.fullmatch(r"[A-Za-z0-9_.+-]+", v) for v in (group, name, version)):
            raise ValueError("Resolved public Maven coordinates required")
        reference = f"pkg:maven/{quote(group, safe='')}/{quote(name, safe='')}@{quote(version, safe='')}"
        classifier = node.get("classifier")
        if classifier:
            reference += "?classifier=" + quote(classifier, safe="")
        component = {"type": "application" if root else "library", "bom-ref": reference,
                     "group": group, "name": name, "version": version, "purl": reference}
        components[reference] = component
        children = {walk(child) for child in node.get("children", [])}
        edges.setdefault(reference, set()).update(children)
        return reference

    root = walk(tree, root=True)
    digest = hashlib.sha256(tree_bytes).hexdigest()
    return {"bomFormat": "CycloneDX", "specVersion": "1.6", "version": 1,
            "serialNumber": "urn:uuid:" + str(uuid.uuid5(uuid.NAMESPACE_URL, digest + sha)),
            "metadata": {"component": components[root], "properties": [
                {"name": "investinator:source-sha", "value": sha},
                {"name": "investinator:resolved-tree-sha256", "value": digest},
                {"name": "investinator:shaded-class-inspection", "value": "NOT_PERFORMED_NOT_CERTIFIED"},
                {"name": "investinator:license-validation", "value": "NOT_PERFORMED"}]},
            "components": [components[k] for k in sorted(components) if k != root],
            "dependencies": [{"ref": k, "dependsOn": sorted(v)} for k, v in sorted(edges.items())]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tree", type=Path, required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    raw = args.tree.read_bytes()
    bom = generate(json.loads(raw), raw, args.sha)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(bom, indent=2) + "\n")
    print(f"SBOM metadata: {len(bom['components'])} components; shaded internals NOT_CERTIFIED")


if __name__ == "__main__":
    main()
