import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('cli', Path(__file__).resolve().parents[1] / 'scripts/investinator_cli.py')
cli = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cli)

class LocalSafetyTest(unittest.TestCase):
    def test_remote_docker_is_refused(self):
        with patch.dict(os.environ, {'DOCKER_HOST': 'tcp://remote.example:2375'}):
            with self.assertRaisesRegex(RuntimeError, 'remote Docker'):
                cli.docker()

    def test_compose_json_array_and_jsonl(self):
        one = {'Service': 'java', 'State': 'running'}
        self.assertEqual([one], cli.parse_containers(json.dumps([one])))
        self.assertEqual([one, one], cli.parse_containers(json.dumps(one)+'\n'+json.dumps(one)))
        self.assertEqual([], cli.parse_containers(''))

    def test_unconfirmed_reset_cannot_call_compose(self):
        with patch.object(cli, 'compose') as compose:
            with self.assertRaisesRegex(RuntimeError, 'confirm-destroy'):
                cli.reset({}, False)
            compose.assert_not_called()

    def test_unknown_restart_cannot_read_state_or_run_commands(self):
        with patch.object(cli, 'load_state') as state:
            with self.assertRaises(SystemExit) as result:
                cli.main(['restart', 'production'])
            self.assertEqual(2, result.exception.code)
            state.assert_not_called()

    def test_down_never_removes_volumes(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(cli, 'RUNTIME', Path(temp)), patch.object(cli, 'load_state', return_value={'project':'investinator-test'}), patch.object(cli, 'compose') as compose:
            self.assertEqual(0, cli.main(['down']))
            compose.assert_called_once_with({'project':'investinator-test'}, 'down')

    def test_ownership_refuses_cross_checkout(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(cli, 'RUNTIME', Path(temp)):
            (Path(temp)/'lab.json').write_text(json.dumps({'version':1,'root':'/another','project':cli.project()}))
            with self.assertRaisesRegex(RuntimeError, 'ownership marker'):
                cli.load_state()

    def test_unconfigured_ai_never_imports_provider_or_calls_network(self):
        with patch.dict(os.environ, {'OPENAI_API_KEY':'','INVESTINATOR_OPENAI_MODEL':''}), patch.object(cli, 'http') as http:
            self.assertEqual(2, cli.ai_check(False))
            http.assert_not_called()

    def test_configured_ai_no_live_flag_is_non_billing(self):
        with patch.dict(os.environ, {'OPENAI_API_KEY':'test-only','INVESTINATOR_OPENAI_MODEL':'test-model'}), patch.object(cli, 'http') as http:
            self.assertEqual(0, cli.ai_check(False))
            http.assert_not_called()

    def test_wrapper_resolves_own_directory(self):
        root = Path(__file__).resolve().parents[1]
        import subprocess
        result = subprocess.run([root/'investinator', '--help'], cwd='/tmp', capture_output=True, text=True)
        self.assertEqual(0, result.returncode)
        self.assertIn('doctor', result.stdout)

    def test_actual_listener_conflict_and_free_port(self):
        import socket
        with socket.socket() as server:
            server.bind(("127.0.0.1", 0))
            server.listen()
            port = server.getsockname()[1]
            with self.assertRaisesRegex(RuntimeError, "in use"):
                cli.check_free_ports([port])
        cli.check_free_ports([port])
