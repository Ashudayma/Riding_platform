from pydantic import BaseModel, Field


class BasePredictionResponse(BaseModel):
    use_case: str
    model_version: str
    score: float = Field(ge=0.0, le=1.0)
    label: str
    explanation: list[str]
