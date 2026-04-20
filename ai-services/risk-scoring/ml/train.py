from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import train_test_split

from ml.feature_schemas import FEATURE_SCHEMAS
from ml.pipelines import build_classification_pipeline
from ml.synthetic_data import generate_dataset


def main() -> None:
    parser = argparse.ArgumentParser(description="Train baseline ride-platform ML models")
    parser.add_argument("--use-case", choices=sorted(FEATURE_SCHEMAS.keys()), required=True)
    parser.add_argument("--version", required=True, help="Semantic or timestamp-like model version")
    parser.add_argument("--rows", type=int, default=8000)
    parser.add_argument("--artifacts-root", default="artifacts")
    args = parser.parse_args()

    schema = FEATURE_SCHEMAS[args.use_case]
    bundle = generate_dataset(args.use_case, rows=args.rows)
    frame = bundle.frame

    features = schema["numeric"] + schema["categorical"]
    target = schema["target"]
    train_frame, test_frame = train_test_split(frame, test_size=0.2, random_state=42, stratify=frame[target])

    pipeline = build_classification_pipeline(schema["numeric"], schema["categorical"])
    pipeline.fit(train_frame[features], train_frame[target])
    probabilities = pipeline.predict_proba(test_frame[features])[:, 1]
    auc = roc_auc_score(test_frame[target], probabilities)

    version_dir = Path(args.artifacts_root) / args.use_case / args.version
    version_dir.mkdir(parents=True, exist_ok=True)
    artifact_path = version_dir / "model.joblib"
    metrics_path = version_dir / "metrics.json"
    joblib.dump(pipeline, artifact_path)

    metadata = {
        "version": args.version,
        "use_case": args.use_case,
        "artifact_path": str(artifact_path.resolve()),
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "problem_type": schema["problem_type"],
        "target_name": target,
        "feature_names": features,
        "threshold": schema["threshold"],
        "metrics": {
            "roc_auc": round(float(auc), 6),
            "rows": args.rows,
        },
    }

    metrics_path.write_text(json.dumps(metadata["metrics"], indent=2), encoding="utf-8")
    update_registry(Path(args.artifacts_root) / "registry.json", args.use_case, metadata)
    print(json.dumps(metadata, indent=2))


def update_registry(registry_path: Path, use_case: str, metadata: dict) -> None:
    registry = {"models": {}}
    if registry_path.exists():
        registry = json.loads(registry_path.read_text(encoding="utf-8"))

    registry["models"].setdefault(use_case, {"default_version": metadata["version"], "versions": {}})
    registry["models"][use_case]["default_version"] = metadata["version"]
    registry["models"][use_case]["versions"][metadata["version"]] = metadata
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps(registry, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
