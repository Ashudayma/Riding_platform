from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field

from app.schemas.common import BasePredictionResponse


class FraudRiskRequest(BaseModel):
    rider_id: str | None = None
    driver_id: str | None = None
    completed_trips_30d: int = Field(ge=0)
    cancellation_rate_7d: float = Field(ge=0.0, le=1.0)
    failed_payments_24h: int = Field(ge=0)
    gps_spoofing_signals_24h: int = Field(ge=0)
    route_deviation_ratio: float = Field(ge=0.0)
    repeated_short_trip_ratio: float = Field(ge=0.0)
    promo_abuse_count_30d: int = Field(ge=0)
    device_account_count_30d: int = Field(ge=0)
    average_trip_distance_km: float = Field(ge=0.0)
    account_age_days: int = Field(ge=0)
    current_hour_local: int = Field(ge=0, le=23)
    city_code: str
    subject_type: str = Field(pattern="^(RIDER|DRIVER)$")


class CancellationProbabilityRequest(BaseModel):
    ride_type: str = Field(pattern="^(STANDARD|SHARED)$")
    estimated_fare: float = Field(gt=0.0)
    estimated_eta_minutes: float = Field(ge=0.0)
    surge_multiplier: float = Field(ge=1.0)
    rider_cancellation_rate_30d: float = Field(ge=0.0, le=1.0)
    driver_cancellation_rate_30d: float = Field(ge=0.0, le=1.0)
    pickup_hour_local: int = Field(ge=0, le=23)
    stop_count: int = Field(ge=0)
    weather_severity_score: float = Field(ge=0.0, le=1.0)
    demand_pressure_index: float = Field(ge=0.0)
    city_code: str


class DriverAcceptanceProbabilityRequest(BaseModel):
    driver_id: str | None = None
    distance_to_pickup_km: float = Field(ge=0.0)
    eta_to_pickup_minutes: float = Field(ge=0.0)
    offered_fare: float = Field(gt=0.0)
    surge_multiplier: float = Field(ge=1.0)
    driver_acceptance_rate_30d: float = Field(ge=0.0, le=1.0)
    driver_cancellation_rate_30d: float = Field(ge=0.0, le=1.0)
    idle_time_minutes: float = Field(ge=0.0)
    active_hours_today: float = Field(ge=0.0)
    ride_type: str = Field(pattern="^(STANDARD|SHARED)$")
    vehicle_type: str
    pickup_hour_local: int = Field(ge=0, le=23)
    demand_pressure_index: float = Field(ge=0.0)
    city_code: str


class DemandZoneFeature(BaseModel):
    zone_id: str
    city_code: str
    hour_of_day: int = Field(ge=0, le=23)
    day_of_week: int = Field(ge=0, le=6)
    active_drivers: int = Field(ge=0)
    ride_requests_last_15m: int = Field(ge=0)
    avg_eta_minutes: float = Field(ge=0.0)
    surge_multiplier: float = Field(ge=1.0)
    weather_severity_score: float = Field(ge=0.0, le=1.0)
    event_intensity_score: float = Field(ge=0.0)


class DemandHotspotPredictionRequest(BaseModel):
    requested_at: datetime
    zones: list[DemandZoneFeature] = Field(min_length=1, max_length=500)


class PooledRideCompatibilityRequest(BaseModel):
    rider_a_wait_minutes: float = Field(ge=0.0)
    rider_b_wait_minutes: float = Field(ge=0.0)
    merged_trip_distance_km: float = Field(ge=0.0)
    merged_trip_duration_minutes: float = Field(ge=0.0)
    detour_minutes_rider_a: float = Field(ge=0.0)
    detour_minutes_rider_b: float = Field(ge=0.0)
    detour_ratio_rider_a: float = Field(ge=0.0)
    detour_ratio_rider_b: float = Field(ge=0.0)
    overlap_distance_ratio: float = Field(ge=0.0, le=1.0)
    seat_utilization_ratio: float = Field(ge=0.0, le=1.0)
    rider_rating_gap: float = Field(ge=0.0)
    compatibility_rule_score: float = Field(ge=0.0, le=1.0)
    city_code: str


class PredictionEnvelope(BasePredictionResponse):
    threshold: float = Field(ge=0.0, le=1.0)
    accepted: bool


class HotspotPredictionItem(BaseModel):
    zone_id: str
    hotspot_score: float = Field(ge=0.0, le=1.0)
    label: str
    explanation: list[str]


class HotspotPredictionResponse(BaseModel):
    use_case: str
    model_version: str
    predictions: list[HotspotPredictionItem]


class ModelVersionInfo(BaseModel):
    version: str
    problem_type: str
    target_name: str
    trained_at: str
    feature_names: list[str]
    threshold: float | None = None


class ModelCatalogEntry(BaseModel):
    use_case: str
    default_version: str
    versions: list[ModelVersionInfo]


class ModelCatalogResponse(BaseModel):
    service: str
    models: list[ModelCatalogEntry]
