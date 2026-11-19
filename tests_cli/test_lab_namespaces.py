import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import Mock

spec = importlib.util.spec_from_file_location("scenarios", Path(__file__).parents[1] / "scripts/lab_scenarios.py")
scenarios = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scenarios)


class ScenarioNamespaceTest(unittest.TestCase):
    def test_cached_historical_marker_is_not_rewritten_or_upgraded(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            old = root / "causal-kafka-kraft.json"
            old.write_text(json.dumps({"incident_id": "old-operator-case"}))
            before = old.read_bytes()
            cli = SimpleNamespace(RUNTIME=root, api=lambda state: "http://fixture.invalid", http=Mock())
            result = scenarios.create(cli, {"secrets": {"MONITOR_API_TOKEN": "fixture-only"}}, "KRAFT")
            self.assertEqual("old-operator-case", result["incident_id"])
            self.assertNotIn("replay_events", result)
            self.assertEqual(before, old.read_bytes())

    def test_namespace_and_mode_cannot_escape_fixed_fixture_paths(self):
        for namespace, mode in [("../outside", "KRAFT"), ("acceptance-v2", "UNKNOWN"), ("A" * 100, "KRAFT")]:
            with self.assertRaises(ValueError):
                scenarios.create(None, None, mode, namespace)
