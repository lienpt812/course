from sqlalchemy.orm import Session

from app.models.enums import NotificationType
from app.models.notification import Notification


def create_notification(
    db: Session,
    user_id: int,
    title: str,
    body: str,
    type: NotificationType = NotificationType.SYSTEM,
    ref_id: int | None = None,
    ref_type: str | None = None,
) -> Notification:
    item = Notification(
        user_id=user_id,
        type=type,
        title=title,
        body=body,
        ref_id=ref_id,
        ref_type=ref_type,
    )
    db.add(item)
    return item
