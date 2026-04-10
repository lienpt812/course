from typing import Any

from pydantic import BaseModel


class APIEnvelope(BaseModel):
    data: Any = None
    meta: dict[str, Any] = {}
    errors: list[dict[str, Any]] = []


def success_response(data: Any = None, meta: dict[str, Any] | None = None) -> dict[str, Any]:
    return {"data": data, "meta": meta or {}, "errors": []}


def error_response(message: str, code: str = "BAD_REQUEST", status: int = 400) -> dict[str, Any]:
    return {
        "data": None,
        "meta": {"status": status},
        "errors": [{"code": code, "message": message}],
    }
