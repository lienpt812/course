from datetime import datetime

from pydantic import BaseModel, Field

from app.models.enums import RegistrationStatus


class RegistrationCreateRequest(BaseModel):
    course_id: int


class RegistrationDecisionRequest(BaseModel):
    reason: str | None = Field(default=None, max_length=500)


class RegistrationBulkApproveRequest(BaseModel):
    course_id: int | None = None
    reason: str | None = Field(default=None, max_length=500)


class RegistrationBulkApproveResult(BaseModel):
    approved_count: int
    rejected_count: int
    message: str
    rejected_items: list[dict] = Field(default_factory=list)


class RegistrationCancelRequest(BaseModel):
    reason: str | None = Field(default=None, max_length=500)


class RegistrationItem(BaseModel):
    id: int
    user_id: int
    course_id: int
    status: RegistrationStatus
    waitlist_position: int | None = None
    user_name: str | None = None
    user_email: str | None = None
    course_title: str | None = None
    course_slug: str | None = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
