import uuid
import logging
from time import perf_counter

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

from app.core.logging_config import correlation_id_context


logger = logging.getLogger("app.request")


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        correlation_id = request.headers.get("X-Correlation-Id", str(uuid.uuid4()))
        request.state.correlation_id = correlation_id
        token = correlation_id_context.set(correlation_id)
        started_at = perf_counter()
        try:
            response = await call_next(request)
            response.headers["X-Correlation-Id"] = correlation_id
            logger.info(
                "request_completed method=%s path=%s status_code=%s duration_ms=%.2f",
                request.method,
                request.url.path,
                response.status_code,
                (perf_counter() - started_at) * 1000,
            )
            return response
        finally:
            correlation_id_context.reset(token)
