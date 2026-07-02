from __future__ import annotations

import argparse
import json
import random
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Sequence

try:
    import psutil
except Exception:  # pragma: no cover - optional for prototype environments
    psutil = None

KST = timezone(timedelta(hours=9))
DEFAULT_CONFIG_PATH = Path("agent-config.json")


class ConfigError(ValueError):
    pass


@dataclass(frozen=True)
class AgentConfig:
    api_base_url: str
    activation_token: str
    device_fingerprint_hash: str
    os_version: str
    agent_version: str
    policy_version: str
    agent_token: str | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "AgentConfig":
        return cls(
            api_base_url=required_config_text(data, "apiBaseUrl"),
            activation_token=required_config_text(data, "activationToken"),
            device_fingerprint_hash=required_config_text(data, "deviceFingerprintHash"),
            os_version=required_config_text(data, "osVersion"),
            agent_version=required_config_text(data, "agentVersion"),
            policy_version=required_config_text(data, "policyVersion"),
            agent_token=optional_config_text(data, "agentToken"),
        )

    def registration_status(self) -> str:
        if self.agent_token:
            return "registered token present"
        return "unregistered"


def required_config_text(data: dict[str, Any], field: str) -> str:
    if field not in data:
        raise ConfigError(f"Missing required config field: {field}")
    value = data[field]
    if not isinstance(value, str) or not value.strip():
        raise ConfigError(f"Config field must be a non-empty string: {field}")
    return value.strip()


def optional_config_text(data: dict[str, Any], field: str) -> str | None:
    if field not in data or data[field] is None:
        return None
    value = data[field]
    if not isinstance(value, str):
        raise ConfigError(f"Config field must be a string when provided: {field}")
    value = value.strip()
    return value or None


def load_config(path: Path) -> AgentConfig:
    if not path.exists():
        raise ConfigError(f"Config file not found: {path}")
    try:
        with path.open("r", encoding="utf-8") as file:
            data = json.load(file)
    except json.JSONDecodeError as exception:
        raise ConfigError(f"Config file is not valid JSON: {path}: {exception.msg}") from exception
    if not isinstance(data, dict):
        raise ConfigError("Config file root must be a JSON object.")
    return AgentConfig.from_dict(data)


def print_status(config_path: Path) -> None:
    config = load_config(config_path)
    print(config.registration_status())


def print_doctor(config_path: Path) -> None:
    config = load_config(config_path)
    print("config: ok")
    print(f"apiBaseUrl: {config.api_base_url}")
    print(f"registration: {config.registration_status()}")


def metric_snapshot(ts: datetime, index: int) -> dict:
    if psutil:
        cpu_usage = psutil.cpu_percent(interval=0.05)
        ram_usage = psutil.virtual_memory().percent
        disk_usage = psutil.disk_usage("/").percent
    else:
        cpu_usage = 38 + index * 3 + random.random() * 8
        ram_usage = 62 + index * 2 + random.random() * 6
        disk_usage = 49 + random.random()

    return {
        "timestamp": ts.isoformat(),
        "cpuUsage": round(cpu_usage, 1),
        "ramUsage": round(ram_usage, 1),
        "gpuUsage": round(min(98, 64 + index * 4 + random.random() * 8), 1),
        "vramUsage": round(min(95, 58 + index * 3 + random.random() * 5), 1),
        "gpuTemp": round(min(91, 70 + index * 1.8 + random.random() * 3), 1),
        "cpuTemp": round(min(86, 62 + index * 1.2 + random.random() * 2), 1),
        "diskUsage": round(disk_usage, 1),
        "osErrorEvent": None if index % 7 else "Display driver warning",
        "topCpuProcess": "game.exe" if index % 2 else "ide64.exe",
        "topRamProcess": "game.exe",
    }


def write_sample(out: Path, count: int, interval_seconds: int) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    start = datetime.now(KST) - timedelta(seconds=count * interval_seconds)
    with out.open("w", encoding="utf-8") as file:
        for index in range(count):
            row = metric_snapshot(start + timedelta(seconds=index * interval_seconds), index)
            file.write(json.dumps(row, ensure_ascii=False) + "\n")


def export_recent(source: Path, out: Path, minutes: int) -> None:
    cutoff = datetime.now(KST) - timedelta(minutes=minutes)
    rows: list[dict] = []
    with source.open("r", encoding="utf-8") as file:
        for line in file:
            if not line.strip():
                continue
            row = json.loads(line)
            try:
                ts = datetime.fromisoformat(row["timestamp"])
            except ValueError:
                continue
            if ts >= cutoff:
                rows.append(row)

    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as file:
        for row in rows:
            file.write(json.dumps(row, ensure_ascii=False) + "\n")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="BuildGraph AI PC Agent prototype CLI")
    sub = parser.add_subparsers(dest="command", required=True)

    sample = sub.add_parser("sample", help="generate sample JSONL hardware metrics")
    sample.add_argument("--out", type=Path, default=Path("sample-agent-log.jsonl"))
    sample.add_argument("--count", type=int, default=24)
    sample.add_argument("--interval-seconds", type=int, default=5)

    export = sub.add_parser("export", help="export recent JSONL rows")
    export.add_argument("--source", type=Path, required=True)
    export.add_argument("--out", type=Path, default=Path("recent-30m.jsonl"))
    export.add_argument("--minutes", type=int, default=30)

    status = sub.add_parser("status", help="read config and print registration state")
    status.add_argument("--config", type=Path, default=DEFAULT_CONFIG_PATH)

    doctor = sub.add_parser("doctor", help="validate config without registering or uploading")
    doctor.add_argument("--config", type=Path, default=DEFAULT_CONFIG_PATH)

    args = parser.parse_args(argv)

    try:
        if args.command == "sample":
            write_sample(args.out, args.count, args.interval_seconds)
            print(f"wrote {args.out}")
        elif args.command == "export":
            export_recent(args.source, args.out, args.minutes)
            print(f"exported {args.out}")
        elif args.command == "status":
            print_status(args.config)
        elif args.command == "doctor":
            print_doctor(args.config)
    except ConfigError as exception:
        print(f"config error: {exception}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
