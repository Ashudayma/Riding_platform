from fastapi import APIRouter, Depends

from app.schemas.inference import (
    CancellationProbabilityRequest,
    DemandHotspotPredictionRequest,
    DriverAcceptanceProbabilityRequest,
    FraudRiskRequest,
    HotspotPredictionResponse,
    ModelCatalogResponse,
    PooledRideCompatibilityRequest,
    PredictionEnvelope,
)
from app.services.inference_service import InferenceService, get_inference_service

router = APIRouter(prefix="/api/v1", tags=["model-inference"])


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@router.get("/models", response_model=ModelCatalogResponse)
def list_models(service: InferenceService = Depends(get_inference_service)) -> ModelCatalogResponse:
    return service.catalog()


@router.post("/inference/fraud-risk", response_model=PredictionEnvelope)
def score_fraud_risk(
    request: FraudRiskRequest,
    service: InferenceService = Depends(get_inference_service),
) -> PredictionEnvelope:
    return service.predict("fraud_risk", request)


@router.post("/inference/ride-cancellation-probability", response_model=PredictionEnvelope)
def predict_ride_cancellation(
    request: CancellationProbabilityRequest,
    service: InferenceService = Depends(get_inference_service),
) -> PredictionEnvelope:
    return service.predict("ride_cancellation_probability", request)


@router.post("/inference/driver-acceptance-probability", response_model=PredictionEnvelope)
def predict_driver_acceptance(
    request: DriverAcceptanceProbabilityRequest,
    service: InferenceService = Depends(get_inference_service),
) -> PredictionEnvelope:
    return service.predict("driver_acceptance_probability", request)


@router.post("/inference/demand-hotspot-prediction", response_model=HotspotPredictionResponse)
def predict_demand_hotspots(
    request: DemandHotspotPredictionRequest,
    service: InferenceService = Depends(get_inference_service),
) -> HotspotPredictionResponse:
    return service.predict_hotspots(request)


@router.post("/inference/pooled-ride-compatibility", response_model=PredictionEnvelope)
def score_pooled_compatibility(
    request: PooledRideCompatibilityRequest,
    service: InferenceService = Depends(get_inference_service),
) -> PredictionEnvelope:
    return service.predict("pooled_ride_compatibility", request)
