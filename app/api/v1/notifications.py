from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.notification import Notification
from app.models.user import User
from app.schemas.common import success_response
from app.schemas.notification import NotificationItem

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.get("/me")
def list_my_notifications(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    items = db.scalars(
        select(Notification).where(Notification.user_id == current_user.id).order_by(Notification.created_at.desc())
    ).all()
    return success_response(data=[NotificationItem.model_validate(x).model_dump() for x in items])


@router.post("/{notification_id}/read")
def mark_read(
    notification_id: int,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    item = db.get(Notification, notification_id)
    if not item:
        raise HTTPException(status_code=404, detail="Notification not found")
    if item.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not allowed")

    item.is_read = True
    db.commit()
    db.refresh(item)
    return success_response(data=NotificationItem.model_validate(item).model_dump())
