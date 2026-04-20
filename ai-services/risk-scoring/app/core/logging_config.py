import json
import logging
from contextvars import ContextVar
from datetime import datetime, timezone


correlation_id_context: ContextVar[str] = ContextVar("correlation_id", default="-")


class JsonLogFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "correlationId": correlation_id_context.get(),
        }
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=True)


def configure_logging(level: str = "INFO") -> None:
    root_logger = logging.getLogger()
    root_logger.handlers.clear()
    handler = logging.StreamHandler()
    handler.setFormatter(JsonLogFormatter())
    root_logger.addHandler(handler)
    root_logger.setLevel(level.upper())
