from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.enums import RegistrationStatus, UserRole
from app.models.registration_log import RegistrationLog
from app.models.user import User
from app.schemas.common import success_response
from app.schemas.registration import (
    RegistrationBulkApproveRequest,
    RegistrationCancelRequest,
    RegistrationCreateRequest,
    RegistrationDecisionRequest,
    RegistrationItem,
)
from app.services.registration_service import (
    bulk_approve_pending,
    cancel_registration,
    create_registration,
    list_registrations,
    process_registration,
)

router = APIRouter(prefix="/registrations", tags=["registrations"])


def _registration_to_item_payload(registration):
    payload = RegistrationItem.model_validate(registration).model_dump()
    payload["user_name"] = registration.user.name if registration.user else None
    payload["user_email"] = registration.user.email if registration.user else None
    payload["course_title"] = registration.course.title if registration.course else None
    payload["course_slug"] = registration.course.slug if registration.course else None
    return payload


@router.post("")
def register_course(
    payload: RegistrationCreateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    registration = create_registration(db, current_user, payload.course_id)
    return success_response(data=_registration_to_item_payload(registration))


@router.post("/{registration_id}/approve")
def approve_registration(
    registration_id: int,
    payload: RegistrationDecisionRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    registration = process_registration(db, registration_id, current_user, approve=True, reason=payload.reason)
    return success_response(data=_registration_to_item_payload(registration))


@router.post("/{registration_id}/reject")
def reject_registration(
    registration_id: int,
    payload: RegistrationDecisionRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    registration = process_registration(db, registration_id, current_user, approve=False, reason=payload.reason)
    return success_response(data=_registration_to_item_payload(registration))


@router.post("/bulk-approve")
def bulk_approve(
    payload: RegistrationBulkApproveRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    result = bulk_approve_pending(
        db,
        actor=current_user,
        course_id=payload.course_id,
        reason=payload.reason,
    )
    return success_response(data=result)


@router.post("/{registration_id}/cancel")
def cancel(
    registration_id: int,
    payload: RegistrationCancelRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    registration = cancel_registration(db, registration_id, current_user, reason=payload.reason)
    return success_response(data=_registration_to_item_payload(registration))


@router.get("")
def list_items(
    current_user: Annotated[User, Depends(get_current_user)],
    course_id: int | None = Query(default=None),
    status: RegistrationStatus | None = Query(default=None),
    db: Session = Depends(get_db),
):
    items = list_registrations(db, course_id=course_id, status=status)
    if current_user.role not in [UserRole.ADMIN, UserRole.INSTRUCTOR]:
        items = [x for x in items if x.user_id == current_user.id]

    return success_response(data=[_registration_to_item_payload(x) for x in items])


@router.get("/{registration_id}/logs")
def registration_logs(
    registration_id: int,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    logs = db.query(RegistrationLog).filter(RegistrationLog.registration_id == registration_id).order_by(RegistrationLog.created_at.asc()).all()
    return success_response(
        data=[
            {
                "id": x.id,
                "registration_id": x.registration_id,
                "from_status": x.from_status,
                "to_status": x.to_status,
                "actor_id": x.actor_id,
                "reason": x.reason,
                "created_at": x.created_at,
            }
            for x in logs
        ]
    )
