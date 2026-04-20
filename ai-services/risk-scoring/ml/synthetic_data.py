from __future__ import annotations

import math
from dataclasses import dataclass

import numpy as np
import pandas as pd


CITY_CODES = np.array(["DEL", "BLR", "MUM", "HYD", "PUN"])
RIDE_TYPES = np.array(["STANDARD", "SHARED"])
VEHICLE_TYPES = np.array(["SEDAN", "HATCHBACK", "SUV", "AUTO"])
SUBJECT_TYPES = np.array(["RIDER", "DRIVER"])


@dataclass(frozen=True)
class DatasetBundle:
    frame: pd.DataFrame
    description: str


def generate_dataset(use_case: str, rows: int = 5000, random_state: int = 42) -> DatasetBundle:
    rng = np.random.default_rng(random_state)
    if use_case == "fraud_risk":
        frame = _fraud_risk_frame(rng, rows)
        return DatasetBundle(frame=frame, description="Synthetic fraud risk supervision data")
    if use_case == "ride_cancellation_probability":
        frame = _cancellation_frame(rng, rows)
        return DatasetBundle(frame=frame, description="Synthetic cancellation behavior dataset")
    if use_case == "driver_acceptance_probability":
        frame = _driver_acceptance_frame(rng, rows)
        return DatasetBundle(frame=frame, description="Synthetic driver acceptance dataset")
    if use_case == "demand_hotspot_prediction":
        frame = _hotspot_frame(rng, rows)
        return DatasetBundle(frame=frame, description="Synthetic demand hotspot dataset")
    if use_case == "pooled_ride_compatibility":
        frame = _pooling_frame(rng, rows)
        return DatasetBundle(frame=frame, description="Synthetic pooled ride compatibility dataset")
    raise ValueError(f"Unsupported use case '{use_case}'")


def _fraud_risk_frame(rng: np.random.Generator, rows: int) -> pd.DataFrame:
    frame = pd.DataFrame({
        "completed_trips_30d": rng.integers(0, 120, size=rows),
        "cancellation_rate_7d": rng.uniform(0.0, 0.9, size=rows),
        "failed_payments_24h": rng.poisson(0.4, size=rows),
        "gps_spoofing_signals_24h": rng.poisson(0.2, size=rows),
        "route_deviation_ratio": rng.uniform(0.0, 1.5, size=rows),
        "repeated_short_trip_ratio": rng.uniform(0.0, 1.0, size=rows),
        "promo_abuse_count_30d": rng.poisson(1.2, size=rows),
        "device_account_count_30d": rng.integers(1, 6, size=rows),
        "average_trip_distance_km": rng.uniform(0.8, 22.0, size=rows),
        "account_age_days": rng.integers(1, 1200, size=rows),
        "current_hour_local": rng.integers(0, 24, size=rows),
        "city_code": rng.choice(CITY_CODES, size=rows),
        "subject_type": rng.choice(SUBJECT_TYPES, size=rows),
    })
    signal = (
        frame["cancellation_rate_7d"] * 1.4
        + frame["failed_payments_24h"] * 0.9
        + frame["gps_spoofing_signals_24h"] * 1.8
        + frame["route_deviation_ratio"] * 0.8
        + frame["promo_abuse_count_30d"] * 0.4
        + (frame["device_account_count_30d"] - 1) * 0.5
        - np.log1p(frame["account_age_days"]) * 0.15
    )
    probability = 1 / (1 + np.exp(-(signal - 2.8)))
    frame["is_high_risk"] = rng.binomial(1, np.clip(probability, 0.02, 0.98))
    return frame


def _cancellation_frame(rng: np.random.Generator, rows: int) -> pd.DataFrame:
    frame = pd.DataFrame({
        "ride_type": rng.choice(RIDE_TYPES, size=rows, p=[0.7, 0.3]),
        "estimated_fare": rng.uniform(60.0, 1100.0, size=rows),
        "estimated_eta_minutes": rng.uniform(1.0, 18.0, size=rows),
        "surge_multiplier": rng.uniform(1.0, 2.2, size=rows),
        "rider_cancellation_rate_30d": rng.uniform(0.0, 0.8, size=rows),
        "driver_cancellation_rate_30d": rng.uniform(0.0, 0.5, size=rows),
        "pickup_hour_local": rng.integers(0, 24, size=rows),
        "stop_count": rng.integers(0, 4, size=rows),
        "weather_severity_score": rng.uniform(0.0, 1.0, size=rows),
        "demand_pressure_index": rng.uniform(0.2, 3.0, size=rows),
        "city_code": rng.choice(CITY_CODES, size=rows),
    })
    peak_penalty = ((frame["pickup_hour_local"] >= 18) & (frame["pickup_hour_local"] <= 22)).astype(int)
    signal = (
        frame["estimated_eta_minutes"] * 0.18
        + (frame["surge_multiplier"] - 1.0) * 1.2
        + frame["rider_cancellation_rate_30d"] * 2.0
        + frame["weather_severity_score"] * 1.1
        + frame["stop_count"] * 0.25
        + peak_penalty * 0.3
        + np.where(frame["ride_type"] == "SHARED", 0.35, 0.0)
        - frame["demand_pressure_index"] * 0.08
    )
    probability = 1 / (1 + np.exp(-(signal - 2.5)))
    frame["will_cancel"] = rng.binomial(1, np.clip(probability, 0.03, 0.96))
    return frame


def _driver_acceptance_frame(rng: np.random.Generator, rows: int) -> pd.DataFrame:
    frame = pd.DataFrame({
        "distance_to_pickup_km": rng.uniform(0.1, 8.0, size=rows),
        "eta_to_pickup_minutes": rng.uniform(1.0, 20.0, size=rows),
        "offered_fare": rng.uniform(50.0, 1600.0, size=rows),
        "surge_multiplier": rng.uniform(1.0, 2.5, size=rows),
        "driver_acceptance_rate_30d": rng.uniform(0.2, 0.98, size=rows),
        "driver_cancellation_rate_30d": rng.uniform(0.0, 0.4, size=rows),
        "idle_time_minutes": rng.uniform(0.0, 90.0, size=rows),
        "active_hours_today": rng.uniform(0.0, 12.0, size=rows),
        "ride_type": rng.choice(RIDE_TYPES, size=rows, p=[0.75, 0.25]),
        "vehicle_type": rng.choice(VEHICLE_TYPES, size=rows),
        "pickup_hour_local": rng.integers(0, 24, size=rows),
        "demand_pressure_index": rng.uniform(0.3, 3.5, size=rows),
        "city_code": rng.choice(CITY_CODES, size=rows),
    })
    signal = (
        frame["offered_fare"] / 400.0
        + (frame["surge_multiplier"] - 1.0) * 1.4
        + frame["driver_acceptance_rate_30d"] * 2.2
        + frame["idle_time_minutes"] * 0.03
        - frame["distance_to_pickup_km"] * 0.55
        - frame["eta_to_pickup_minutes"] * 0.09
        - frame["driver_cancellation_rate_30d"] * 1.1
        - np.maximum(frame["active_hours_today"] - 9.0, 0.0) * 0.25
        + np.where(frame["ride_type"] == "SHARED", -0.2, 0.15)
    )
    probability = 1 / (1 + np.exp(-(signal - 1.8)))
    frame["will_accept"] = rng.binomial(1, np.clip(probability, 0.05, 0.97))
    return frame


def _hotspot_frame(rng: np.random.Generator, rows: int) -> pd.DataFrame:
    zones = np.array([f"ZONE-{index:03d}" for index in range(1, 101)])
    frame = pd.DataFrame({
        "zone_id": rng.choice(zones, size=rows),
        "city_code": rng.choice(CITY_CODES, size=rows),
        "hour_of_day": rng.integers(0, 24, size=rows),
        "day_of_week": rng.integers(0, 7, size=rows),
        "active_drivers": rng.integers(5, 250, size=rows),
        "ride_requests_last_15m": rng.integers(0, 320, size=rows),
        "avg_eta_minutes": rng.uniform(1.0, 18.0, size=rows),
        "surge_multiplier": rng.uniform(1.0, 2.8, size=rows),
        "weather_severity_score": rng.uniform(0.0, 1.0, size=rows),
        "event_intensity_score": rng.uniform(0.0, 1.0, size=rows),
    })
    rush_hour = ((frame["hour_of_day"] >= 8) & (frame["hour_of_day"] <= 10) | (frame["hour_of_day"] >= 17) & (frame["hour_of_day"] <= 21)).astype(int)
    signal = (
        (frame["ride_requests_last_15m"] - frame["active_drivers"]) / 40.0
        + frame["avg_eta_minutes"] * 0.12
        + (frame["surge_multiplier"] - 1.0) * 0.9
        + frame["event_intensity_score"] * 1.5
        + rush_hour * 0.4
        + frame["weather_severity_score"] * 0.5
    )
    probability = 1 / (1 + np.exp(-(signal - 0.8)))
    frame["is_hotspot"] = rng.binomial(1, np.clip(probability, 0.04, 0.97))
    return frame


def _pooling_frame(rng: np.random.Generator, rows: int) -> pd.DataFrame:
    frame = pd.DataFrame({
        "rider_a_wait_minutes": rng.uniform(1.0, 14.0, size=rows),
        "rider_b_wait_minutes": rng.uniform(1.0, 14.0, size=rows),
        "merged_trip_distance_km": rng.uniform(1.0, 32.0, size=rows),
        "merged_trip_duration_minutes": rng.uniform(6.0, 70.0, size=rows),
        "detour_minutes_rider_a": rng.uniform(0.0, 18.0, size=rows),
        "detour_minutes_rider_b": rng.uniform(0.0, 18.0, size=rows),
        "detour_ratio_rider_a": rng.uniform(0.0, 0.7, size=rows),
        "detour_ratio_rider_b": rng.uniform(0.0, 0.7, size=rows),
        "overlap_distance_ratio": rng.uniform(0.0, 1.0, size=rows),
        "seat_utilization_ratio": rng.uniform(0.25, 1.0, size=rows),
        "rider_rating_gap": rng.uniform(0.0, 2.0, size=rows),
        "compatibility_rule_score": rng.uniform(0.0, 1.0, size=rows),
        "city_code": rng.choice(CITY_CODES, size=rows),
    })
    signal = (
        frame["compatibility_rule_score"] * 2.3
        + frame["overlap_distance_ratio"] * 1.5
        + frame["seat_utilization_ratio"] * 1.1
        - frame["detour_ratio_rider_a"] * 1.4
        - frame["detour_ratio_rider_b"] * 1.4
        - frame["rider_rating_gap"] * 0.25
        - (frame["rider_a_wait_minutes"] + frame["rider_b_wait_minutes"]) * 0.04
    )
    probability = 1 / (1 + np.exp(-(signal - 1.6)))
    frame["is_compatible"] = rng.binomial(1, np.clip(probability, 0.03, 0.98))
    return frame
