from __future__ import annotations

import io
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

from buildgraph_agent import ConfigError, load_config, main


class AgentConfigTest(unittest.TestCase):
    def write_config(self, data: dict) -> Path:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "agent-config.json"
        path.write_text(json.dumps(data), encoding="utf-8")
        return path

    def valid_config(self, **overrides: object) -> dict:
        data = {
            "apiBaseUrl": "http://localhost:8080",
            "activationToken": "demo-agent-activation-token",
            "deviceFingerprintHash": "fingerprint-hash",
            "osVersion": "Windows 11",
            "agentVersion": "0.1.0",
            "policyVersion": "policy-v1",
        }
        data.update(overrides)
        return data

    def test_loads_valid_config(self) -> None:
        path = self.write_config(self.valid_config(agentToken="agent-token"))

        config = load_config(path)

        self.assertEqual(config.api_base_url, "http://localhost:8080")
        self.assertEqual(config.activation_token, "demo-agent-activation-token")
        self.assertEqual(config.device_fingerprint_hash, "fingerprint-hash")
        self.assertEqual(config.os_version, "Windows 11")
        self.assertEqual(config.agent_version, "0.1.0")
        self.assertEqual(config.policy_version, "policy-v1")
        self.assertEqual(config.agent_token, "agent-token")

    def test_missing_required_config_field_fails_with_clear_message(self) -> None:
        data = self.valid_config()
        del data["activationToken"]
        path = self.write_config(data)

        with self.assertRaisesRegex(ConfigError, "Missing required config field: activationToken"):
            load_config(path)

    def test_status_without_agent_token_is_unregistered(self) -> None:
        path = self.write_config(self.valid_config())
        output = io.StringIO()

        with redirect_stdout(output):
            exit_code = main(["status", "--config", str(path)])

        self.assertEqual(exit_code, 0)
        self.assertEqual(output.getvalue().strip(), "unregistered")

    def test_status_with_agent_token_is_registered_token_present(self) -> None:
        path = self.write_config(self.valid_config(agentToken="agent-token"))
        output = io.StringIO()

        with redirect_stdout(output):
            exit_code = main(["status", "--config", str(path)])

        self.assertEqual(exit_code, 0)
        self.assertEqual(output.getvalue().strip(), "registered token present")


if __name__ == "__main__":
    unittest.main()
