import importlib.util
import json
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("sbom", Path(__file__).parents[1] / "scripts/generate_java_sbom.py")
sbom = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sbom)


class SbomTest(unittest.TestCase):
    def test_shaded_classifier_and_duplicate_edges_keep_explicit_limits(self):
        child = {"groupId": "org.fixture", "artifactId": "driver", "version": "1.0", "classifier": "standalone"}
        tree = {"groupId": "com.investinator", "artifactId": "backend-java", "version": "0.1.0", "children": [child, child]}
        result = sbom.generate(tree, json.dumps(tree).encode(), "a" * 40)
        self.assertEqual(1, len(result["components"]))
        self.assertIn("classifier=standalone", result["components"][0]["purl"])
        self.assertTrue(any(p["value"] == "NOT_PERFORMED_NOT_CERTIFIED" for p in result["metadata"]["properties"]))
        self.assertEqual(1, len(next(e["dependsOn"] for e in result["dependencies"] if "backend-java" in e["ref"])))

    def test_unresolved_tree_or_nonexact_source_is_rejected(self):
        for tree, sha in [({}, "a" * 40), ({"groupId": "org.fixture", "artifactId": "driver", "version": "1.0"}, "main")]:
            with self.assertRaises(ValueError):
                sbom.generate(tree, b"{}", sha)
