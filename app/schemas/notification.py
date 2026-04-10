from datetime import datetime

from pydantic import BaseModel

from app.models.enums import NotificationType


class NotificationItem(BaseModel):
    id: int
    user_id: int
    type: NotificationType
    title: str
    body: str
    is_read: bool
    ref_id: int | None = None
    ref_type: str | None = None
    created_at: datetime

    class Config:
        from_attributes = True
