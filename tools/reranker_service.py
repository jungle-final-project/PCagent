#!/usr/bin/env python3
"""Optional local scorer for BuildGraph recommendation shadow scoring.

The API is intentionally tiny:
  POST /score
  { "candidates": [{ "candidateId": "...", "features": {...} }] }

Run with a trained model:
  python tools/reranker_service.py --model artifacts/recommendation/model/xgb-....json
"""

from __future__ import annotations

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any


FEATURES = [
    "rank_position",
    "part_price",
    "build_total_price",
    "part_benchmark_score",
    "part_tool_ready",
    "part_has_image",
    "part_has_offer",
    "part_price_age_days",
    "part_has_fps_coverage",
]


class Scorer:
    def __init__(self, model_path: str | None):
        self.model_path = model_path
        self.model = None
        if model_path:
            try:
                from xgboost import XGBRegressor

                self.model = XGBRegressor()
                self.model.load_model(model_path)
            except ImportError as exc:
                raise SystemExit("Install xgboost to serve a trained reranker model.") from exc

    @property
    def model_version(self) -> str:
        if not self.model_path:
            return "baseline-shadow"
        return Path(self.model_path).stem

    @property
    def artifact_path(self) -> str | None:
        return self.model_path

    def score(self, candidate: dict[str, Any]) -> float:
        features = candidate.get("features") or {}
        if self.model is None:
            price = number(features.get("part_price")) or number(features.get("totalPrice")) or 0
            rank_position = number(features.get("rank_position")) or candidate.get("rankPosition") or 0
            benchmark = number(features.get("part_benchmark_score")) or number(features.get("benchmark_score")) or 0
            has_image = 1 if truthy(features.get("part_has_image")) or truthy(features.get("has_image")) else 0
            has_offer = 1 if truthy(features.get("part_has_offer")) or truthy(features.get("has_offer")) else 0
            return float(max(0, 10 - rank_position) + benchmark / 20 + has_image + has_offer - (price / 10_000_000))
        row = [[number(features.get(name)) or 0 for name in FEATURES]]
        return float(self.model.predict(row)[0])


def number(value: Any) -> float | None:
    try:
        if value is None or value == "":
            return None
        return float(value)
    except (TypeError, ValueError):
        return None


def truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).strip().lower() in {"1", "true", "yes", "y"}


def make_handler(scorer: Scorer):
    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):  # noqa: N802 - stdlib callback name
            if self.path != "/score":
                self.send_error(404)
                return
            length = int(self.headers.get("Content-Length", "0"))
            payload = json.loads(self.rfile.read(length).decode("utf-8") or "{}")
            candidates = payload.get("candidates") or []
            scores = []
            for candidate in candidates:
                scores.append({
                    "candidateId": candidate.get("candidateId"),
                    "partId": candidate.get("partId"),
                    "score": scorer.score(candidate),
                })
            response = {
                "modelName": "xgboost-reranker" if scorer.model is not None else "baseline-shadow-reranker",
                "modelVersion": scorer.model_version,
                "artifactPath": scorer.artifact_path,
                "scores": scores,
                "metrics": {},
                "featureSchema": {"features": FEATURES},
            }
            body = json.dumps(response, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, format, *args):  # noqa: A003,N802 - stdlib callback name
            return

    return Handler


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8091)
    parser.add_argument("--model", default=None)
    args = parser.parse_args()

    scorer = Scorer(args.model)
    server = ThreadingHTTPServer((args.host, args.port), make_handler(scorer))
    print(f"reranker_service listening on http://{args.host}:{args.port}/score modelVersion={scorer.model_version}")
    server.serve_forever()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
