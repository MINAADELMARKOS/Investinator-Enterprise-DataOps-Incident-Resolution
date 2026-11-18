import importlib.util
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "cli", Path(__file__).resolve().parents[1] / "scripts/investinator_cli.py"
)
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)


class LocalSafetyTest(unittest.TestCase):
    def test_remote_docker_is_refused(self):
        with patch.dict(os.environ, {"DOCKER_HOST": "tcp://remote.example:2375"}):
            with self.assertRaisesRegex(RuntimeError, "remote Docker"):
                cli.docker()

    def test_compose_json_array_and_jsonl(self):
        one = {"Service": "java", "State": "running"}
        self.assertEqual([one], cli.parse_containers(json.dumps([one])))
        self.assertEqual(
            [one, one], cli.parse_containers(json.dumps(one) + "\n" + json.dumps(one))
        )
        self.assertEqual([], cli.parse_containers(""))

    def test_unconfirmed_reset_cannot_call_compose(self):
        with patch.object(cli, "compose") as compose:
            with self.assertRaisesRegex(RuntimeError, "confirm-destroy"):
                cli.reset({}, False)
            compose.assert_not_called()

    def test_unknown_restart_cannot_read_state_or_run_commands(self):
        with patch.object(cli, "load_state") as state:
            with self.assertRaises(SystemExit) as result:
                cli.main(["restart", "production"])
            self.assertEqual(2, result.exception.code)
            state.assert_not_called()

    def test_down_never_removes_volumes(self):
        with (
            tempfile.TemporaryDirectory() as temp,
            patch.object(cli, "RUNTIME", Path(temp)),
            patch.object(
                cli, "load_state", return_value={"project": "investinator-test"}
            ),
            patch.object(cli, "compose") as compose,
        ):
            self.assertEqual(0, cli.main(["down"]))
            compose.assert_called_once_with({"project": "investinator-test"}, "down")

    def test_ownership_refuses_cross_checkout(self):
        with (
            tempfile.TemporaryDirectory() as temp,
            patch.object(cli, "RUNTIME", Path(temp)),
        ):
            (Path(temp) / "lab.json").write_text(
                json.dumps({"version": 1, "root": "/another", "project": cli.project()})
            )
            with self.assertRaisesRegex(RuntimeError, "ownership marker"):
                cli.load_state()

    def test_unconfigured_ai_never_imports_provider_or_calls_network(self):
        with (
            patch.dict(
                os.environ, {"OPENAI_API_KEY": "", "INVESTINATOR_OPENAI_MODEL": ""}
            ),
            patch.object(cli, "http") as http,
        ):
            self.assertEqual(2, cli.ai_check(False))
            http.assert_not_called()

    def test_configured_ai_no_live_flag_is_non_billing(self):
        with (
            patch.dict(
                os.environ,
                {
                    "OPENAI_API_KEY": "test-only",
                    "INVESTINATOR_OPENAI_MODEL": "test-model",
                },
            ),
            patch.object(cli, "http") as http,
        ):
            self.assertEqual(0, cli.ai_check(False))
            http.assert_not_called()

    def test_wrapper_resolves_own_directory(self):
        root = Path(__file__).resolve().parents[1]
        import subprocess

        result = subprocess.run(
            [root / "investinator", "--help"],
            cwd="/tmp",
            capture_output=True,
            text=True,
        )
        self.assertEqual(0, result.returncode)
        self.assertIn("doctor", result.stdout)

    def test_actual_listener_conflict_and_free_port(self):
        import socket

        with socket.socket() as server:
            server.bind(("127.0.0.1", 0))
            server.listen()
            port = server.getsockname()[1]
            with self.assertRaisesRegex(RuntimeError, "in use"):
                cli.check_free_ports([port])
        cli.check_free_ports([port])

    def test_smoke_refuses_unrelated_active_run_without_writes(self):
        state = {"ports": {"LAB_API_PORT": 18081}}
        active = [
            {
                "id": "foreign-run",
                "incident_id": "FOREIGN",
                "scenario": "kafka_disk_pressure",
                "status": "awaiting_approval",
            }
        ]
        with (
            tempfile.TemporaryDirectory() as temp,
            patch.object(cli, "RUNTIME", Path(temp)),
            patch.object(cli, "http", return_value=active) as http,
        ):
            with self.assertRaisesRegex(RuntimeError, "unrelated ReplayLab"):
                cli.smoke(state)
            http.assert_called_once_with(cli.api(state), "/api/v1/replay/runs")

    def _resume_owned_smoke(self, approved):
        import urllib.error

        state = {
            "ports": {"LAB_API_PORT": 18081, "LAB_WEB_PORT": 13000},
            "secrets": {"EVIDENCE_API_TOKEN": "test-only"},
        }
        calls = []
        executed = False

        def response(base, path, body=None, token=None):
            nonlocal executed
            calls.append((path, body))
            if path == "/api/v1/replay/runs":
                self.assertIsNone(body)
                return [
                    {
                        "id": "owned-run",
                        "incident_id": "OWNED",
                        "scenario": "kafka_disk_pressure",
                        "status": "awaiting_approval",
                    }
                ]
            if path.endswith("/verify"):
                return {"passed": False}
            if path == "/api/v1/incidents/OWNED":
                return {
                    "recovery_actions": [
                        {
                            "id": "ACT-DEMO",
                            "status": "approved" if approved else "proposed",
                        }
                    ]
                }
            if path.endswith("/execute"):
                if not approved and not executed:
                    executed = True
                    raise urllib.error.HTTPError(
                        base + path, 403, "Approval required", {}, None
                    )
                return {"verification": {"passed": True}}
            if path.endswith("/approval"):
                return {"status": "approved"}
            if path == "/api/v1/evidence/search":
                return {"status": "HEALTHY"}
            if path == "/api/v1/health":
                return {"product": "InvestiNator"}
            self.fail(path)

        with (
            tempfile.TemporaryDirectory() as temp,
            patch.object(cli, "RUNTIME", Path(temp)),
            patch.object(cli, "http", side_effect=response),
            patch.object(cli, "health", return_value={"success": True}),
        ):
            (Path(temp) / "demo-kafka-kraft.json").write_text(
                json.dumps({"incident_id": "OWNED"})
            )
            self.assertEqual(0, cli.smoke(state))
            marker = json.loads((Path(temp) / "smoke.json").read_text())
            self.assertEqual("owned-run", marker["replay_id"])
            approvals = [path for path, body in calls if path.endswith("/approval")]
            self.assertEqual(0 if approved else 1, len(approvals))
            self.assertTrue(
                all(
                    body is None
                    for path, body in calls
                    if path == "/api/v1/replay/runs"
                )
            )

    def test_smoke_resumes_own_demo_and_checks_approval_before_recovery(self):
        self._resume_owned_smoke(False)

    def test_smoke_resumes_after_approval_without_duplicate_approval(self):
        self._resume_owned_smoke(True)

    def test_java_live_probe_requires_explicit_opt_in_and_rate_limits_repeats(self):
        state_env = {
            "OPENAI_API_KEY": "test-only",
            "INVESTINATOR_OPENAI_MODEL": "fixture-model",
        }
        with (
            tempfile.TemporaryDirectory() as temp,
            patch.dict(os.environ, state_env),
            patch.object(cli, "RUNTIME", Path(temp)),
            patch.object(
                cli, "java_env", return_value={"JAVA_HOME": "/fixture/jdk", **state_env}
            ),
            patch.object(cli, "classpath", return_value="fixture-classpath"),
            patch.object(cli, "maven"),
            patch.object(
                cli,
                "run",
                return_value='{"status":"HEALTHY","paid_call_made":true,"text":"OK"}',
            ) as run,
        ):
            self.assertEqual(0, cli.ai_check(False))
            run.assert_not_called()
            self.assertEqual(0, cli.ai_check(True))
            self.assertIn(
                "com.investinator.model.OpenAiLiveProbe", run.call_args.args[0]
            )
            self.assertIn("--live", run.call_args.args[0])
            self.assertNotIn("test-only", str(run.call_args.args[0]))
            with self.assertRaisesRegex(RuntimeError, "one request per minute"):
                cli.ai_check(True)
            self.assertEqual(1, run.call_count)
