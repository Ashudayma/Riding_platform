from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

import joblib

from app.core.config import get_settings
from app.core.exceptions import ModelNotReadyError, RegistryError


class ModelRegistry:
    def __init__(self, registry_path: Path) -> None:
        self.registry_path = registry_path
        self._registry = self._load_registry()

    def _load_registry(self) -> dict[str, Any]:
        if not self.registry_path.exists():
            return {"models": {}}
        with self.registry_path.open("r", encoding="utf-8") as handle:
            return json.load(handle)

    def describe(self) -> dict[str, Any]:
        return self._registry

    def resolve_model(self, use_case: str, version: str | None = None) -> tuple[dict[str, Any], Any]:
        model_entry = self._registry.get("models", {}).get(use_case)
        if not model_entry:
            raise ModelNotReadyError(f"No model registered for use case '{use_case}'")

        selected_version = version or model_entry["default_version"]
        version_entry = model_entry["versions"].get(selected_version)
        if not version_entry:
            raise RegistryError(f"Version '{selected_version}' is not registered for use case '{use_case}'")

        artifact_path = Path(version_entry["artifact_path"])
        if not artifact_path.exists():
            raise ModelNotReadyError(f"Artifact '{artifact_path}' is missing for use case '{use_case}'")
        return version_entry, joblib.load(artifact_path)


@lru_cache(maxsize=1)
def get_model_registry() -> ModelRegistry:
    settings = get_settings()
    return ModelRegistry(settings.registry_file)
