from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AI_SERVICE_", env_file=".env", extra="ignore")

    service_name: str = "risk-scoring-service"
    service_version: str = "0.1.0"
    artifacts_root: Path = Field(default=Path("artifacts"))
    registry_file: Path = Field(default=Path("artifacts/registry.json"))
    default_timeout_ms: int = 800
    log_level: str = "INFO"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
