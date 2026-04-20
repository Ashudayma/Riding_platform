# AI Risk Scoring Service

Production-style FastAPI service for baseline inference across the first five AI/ML use cases in the ride platform:

- fraud risk scoring
- ride cancellation probability prediction
- driver acceptance probability prediction
- demand hotspot prediction
- pooled ride compatibility scoring

This service is intentionally designed as a separate deployable unit so the Java/Spring backend can call it over HTTP today and later evolve to async feature/event pipelines without changing the inference contract.

## Project Structure

```text
ai-services/risk-scoring/
|-- app/
|   |-- api/
|   |   `-- routes.py
|   |-- core/
|   |   |-- config.py
|   |   |-- exceptions.py
|   |   |-- middleware.py
|   |   `-- model_registry.py
|   |-- schemas/
|   |   |-- common.py
|   |   `-- inference.py
|   |-- services/
|   |   `-- inference_service.py
|   `-- main.py
|-- ml/
|   |-- feature_schemas.py
|   |-- pipelines.py
|   |-- synthetic_data.py
|   |-- train.py
|   `-- train_all.py
|-- artifacts/
|   `-- .gitkeep
|-- Dockerfile
|-- requirements.txt
`-- README.md
```

## Endpoints

- `GET /api/v1/health`
- `GET /api/v1/models`
- `POST /api/v1/inference/fraud-risk`
- `POST /api/v1/inference/ride-cancellation-probability`
- `POST /api/v1/inference/driver-acceptance-probability`
- `POST /api/v1/inference/demand-hotspot-prediction`
- `POST /api/v1/inference/pooled-ride-compatibility`

## Local Setup

Create a virtual environment and install dependencies:

```bash
python -m venv .venv
. .venv/Scripts/activate
pip install -r requirements.txt
```

Train all baseline models with synthetic data:

```bash
python -m ml.train_all
```

Run the API:

```bash
uvicorn app.main:app --reload --port 8090
```

Open Swagger UI at `http://localhost:8090/docs`.

## Training and Inference Pipeline

1. `ml.synthetic_data` generates local synthetic supervised datasets per use case.
2. `ml.train` builds a preprocessing + model pipeline with scikit-learn.
3. Artifacts are written to `artifacts/<use-case>/<version>/model.joblib`.
4. `artifacts/registry.json` stores the active versions and metadata.
5. FastAPI loads models lazily through `ModelRegistry`.
6. Inference endpoints validate payloads with Pydantic and return typed responses.

## Model Versioning Strategy

The service uses explicit versioned artifacts and a simple registry:

- every trained model is stored under `artifacts/<use-case>/<version>/`
- `artifacts/registry.json` tracks all known versions
- each use case has a `default_version`
- Spring Boot should passively depend on the active default version unless a per-call override is introduced later

This gives us safe rollback capability:

- train `baseline-20260418...`
- promote it in `registry.json`
- roll back by switching `default_version`

## Feature Schema Design

Feature definitions live in `ml/feature_schemas.py`. Each use case declares:

- numeric features
- categorical features
- target column
- problem type
- decision threshold

That keeps training, registry metadata, and inference expectations aligned.

## Spring Boot Integration Contract

Base URL example:

- `http://risk-scoring-service:8090/api/v1`

Recommended backend call pattern:

1. Spring Boot builds a feature DTO from its own validated domain state.
2. Calls the FastAPI endpoint with a tight timeout, usually `300-800ms`.
3. Sends `X-Correlation-Id` from the current request trace.
4. Treats the ML response as advisory for the first production phase.
5. Falls back to deterministic business rules if the ML service times out or returns non-2xx.

Recommended safety controls from Java:

- use internal network-only exposure
- protect with mTLS or service-to-service auth at gateway/service mesh level
- apply client-side timeouts and circuit breaking
- never block booking or dispatch solely on ML unavailability
- log the request/response metadata, not raw PII-heavy payloads
- cache low-volatility responses only when the feature set is immutable for that decision

Suggested Java client behavior:

```text
timeout: 500ms
retries: 0 or 1 for idempotent read-style inference
fallback: rule-based score already available in Spring Boot
audit: store model version, score, accepted label, correlation id
```

## Example Fraud Inference Payload

```json
{
  "completed_trips_30d": 12,
  "cancellation_rate_7d": 0.42,
  "failed_payments_24h": 2,
  "gps_spoofing_signals_24h": 1,
  "route_deviation_ratio": 0.31,
  "repeated_short_trip_ratio": 0.26,
  "promo_abuse_count_30d": 4,
  "device_account_count_30d": 3,
  "average_trip_distance_km": 4.8,
  "account_age_days": 18,
  "current_hour_local": 23,
  "city_code": "DEL",
  "subject_type": "RIDER"
}
```

## Docker

Build:

```bash
docker build -t riding-platform-risk-scoring .
```

Run:

```bash
docker run --rm -p 8090:8090 riding-platform-risk-scoring
```

## Notes for Future Expansion

- replace synthetic data with offline feature snapshots from PostgreSQL, Kafka, or a feature store
- add XGBoost or LightGBM when better non-linear performance is needed
- add async batch scoring jobs for nightly recalculation
- add shadow deployment support for candidate models
- add explainability and drift metrics per use case
