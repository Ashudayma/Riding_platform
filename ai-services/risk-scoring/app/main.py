from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.routes import router as inference_router
from app.core.config import get_settings
from app.core.logging_config import configure_logging
from app.core.middleware import CorrelationIdMiddleware


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging(settings.log_level)
    app = FastAPI(
        title="Riding Platform AI Risk Scoring Service",
        version=settings.service_version,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
    )
    app.add_middleware(CorrelationIdMiddleware)
    app.include_router(inference_router)
    Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)
    return app


app = create_app()
