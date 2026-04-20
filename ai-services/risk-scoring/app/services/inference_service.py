from __future__ import annotations

from functools import lru_cache
from typing import Any

import pandas as pd
from fastapi import Depends

from app.core.config import Settings, get_settings
from app.core.model_registry import ModelRegistry, get_model_registry
from app.schemas.inference import (
    DemandHotspotPredictionRequest,
    HotspotPredictionItem,
    HotspotPredictionResponse,
    ModelCatalogEntry,
    ModelCatalogResponse,
    ModelVersionInfo,
    PredictionEnvelope,
)

POSITIVE_LABELS = {
    "fraud_risk": "HIGH_RISK",
    "ride_cancellation_probability": "LIKELY_CANCELLED",
    "driver_acceptance_probability": "LIKELY_ACCEPTED",
    "pooled_ride_compatibility": "COMPATIBLE",
}


class InferenceService:
    def __init__(self, settings: Settings, registry: ModelRegistry) -> None:
        self.settings = settings
        self.registry = registry

    def catalog(self) -> ModelCatalogResponse:
        registry = self.registry.describe()
        models = []
        for use_case, payload in registry.get("models", {}).items():
            versions = [
                ModelVersionInfo(
                    version=version_name,
                    problem_type=version_payload["problem_type"],
                    target_name=version_payload["target_name"],
                    trained_at=version_payload["trained_at"],
                    feature_names=version_payload["feature_names"],
                    threshold=version_payload.get("threshold"),
                )
                for version_name, version_payload in payload["versions"].items()
            ]
            models.append(ModelCatalogEntry(
                use_case=use_case,
                default_version=payload["default_version"],
                versions=versions,
            ))
        return ModelCatalogResponse(service=self.settings.service_name, models=models)

    def predict(self, use_case: str, request: Any) -> PredictionEnvelope:
        metadata, artifact = self.registry.resolve_model(use_case)
        frame = pd.DataFrame([request.model_dump()])
        probability = float(artifact.predict_proba(frame)[0][1])
        threshold = float(metadata.get("threshold", 0.5))
        label = POSITIVE_LABELS[use_case] if probability >= threshold else "LOW"
        explanation = self._explain_classification(use_case, request.model_dump(), probability)
        return PredictionEnvelope(
            use_case=use_case,
            model_version=metadata["version"],
            score=round(probability, 6),
            label=label,
            threshold=threshold,
            accepted=probability >= threshold,
            explanation=explanation,
        )

    def predict_hotspots(self, request: DemandHotspotPredictionRequest) -> HotspotPredictionResponse:
        metadata, artifact = self.registry.resolve_model("demand_hotspot_prediction")
        frame = pd.DataFrame([zone.model_dump() for zone in request.zones])
        probabilities = artifact.predict_proba(frame)[:, 1]
        predictions = []
        for zone, probability in zip(request.zones, probabilities, strict=True):
            predictions.append(HotspotPredictionItem(
                zone_id=zone.zone_id,
                hotspot_score=round(float(probability), 6),
                label="HOTSPOT" if probability >= float(metadata.get("threshold", 0.5)) else "NORMAL",
                explanation=self._explain_hotspot(zone.model_dump(), float(probability)),
            ))
        predictions.sort(key=lambda item: item.hotspot_score, reverse=True)
        return HotspotPredictionResponse(
            use_case="demand_hotspot_prediction",
            model_version=metadata["version"],
            predictions=predictions,
        )

    def _explain_classification(self, use_case: str, payload: dict[str, Any], probability: float) -> list[str]:
        if use_case == "fraud_risk":
            reasons = []
            if payload.get("failed_payments_24h", 0) > 1:
                reasons.append("failed payment activity is elevated")
            if payload.get("gps_spoofing_signals_24h", 0) > 0:
                reasons.append("GPS spoofing indicators were observed recently")
            if payload.get("promo_abuse_count_30d", 0) > 2:
                reasons.append("promo usage pattern is anomalous")
            return reasons or [f"fraud score computed at {probability:.2f} from baseline operational features"]

        if use_case == "ride_cancellation_probability":
            reasons = []
            if payload.get("estimated_eta_minutes", 0) >= 8:
                reasons.append("pickup ETA is relatively high")
            if payload.get("surge_multiplier", 1.0) > 1.3:
                reasons.append("surge multiplier is likely increasing cancellation pressure")
            if payload.get("stop_count", 0) > 1:
                reasons.append("multi-stop rides historically cancel more often")
            return reasons or ["cancellation probability derived from ETA, fare pressure, and behavior features"]

        if use_case == "driver_acceptance_probability":
            reasons = []
            if payload.get("distance_to_pickup_km", 0) > 3:
                reasons.append("pickup is relatively far from the driver")
            if payload.get("surge_multiplier", 1.0) > 1.2:
                reasons.append("surge pricing improves acceptance likelihood")
            if payload.get("idle_time_minutes", 0) > 15:
                reasons.append("driver idle time supports higher acceptance")
            return reasons or ["acceptance probability derived from supply economics and driver behavior"]

        reasons = []
        if payload.get("compatibility_rule_score", 0.0) >= 0.6:
            reasons.append("rule-based compatibility baseline is already strong")
        if payload.get("detour_ratio_rider_a", 0.0) < 0.25 and payload.get("detour_ratio_rider_b", 0.0) < 0.25:
            reasons.append("detours are within an acceptable pooled range")
        if payload.get("seat_utilization_ratio", 0.0) >= 0.5:
            reasons.append("seat utilization improves pooling efficiency")
        return reasons or ["compatibility score blends detour, overlap, and seat utilization features"]

    def _explain_hotspot(self, payload: dict[str, Any], probability: float) -> list[str]:
        reasons = []
        if payload.get("ride_requests_last_15m", 0) > payload.get("active_drivers", 0):
            reasons.append("recent requests exceed available drivers")
        if payload.get("avg_eta_minutes", 0.0) > 7:
            reasons.append("average ETA is rising in the zone")
        if payload.get("event_intensity_score", 0.0) > 0.5:
            reasons.append("local event intensity suggests additional demand")
        return reasons or [f"hotspot probability computed at {probability:.2f} from zone pressure features"]


@lru_cache(maxsize=1)
def get_inference_service(
    settings: Settings = Depends(get_settings),
    registry: ModelRegistry = Depends(get_model_registry),
) -> InferenceService:
    return InferenceService(settings=settings, registry=registry)
