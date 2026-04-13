from datetime import datetime, timedelta, timezone

from fastapi import HTTPException
from sqlalchemy import and_, asc, func, select
from sqlalchemy.orm import Session

from app.models.course import Course
from app.models.enums import CourseStatus, NotificationType, RegistrationStatus, UserRole, UserStatus
from app.models.registration import Registration
from app.models.registration_log import RegistrationLog
from app.models.user import User
from app.services.notification_service import create_notification


def _is_course_open(course: Course) -> bool:
    now = datetime.now(timezone.utc)
    if course.status != CourseStatus.PUBLISHED:
        return False
    # Make timezone-aware comparison
    open_at = course.registration_open_at
    close_at = course.registration_close_at
    if open_at is not None:
        if open_at.tzinfo is None:
            open_at = open_at.replace(tzinfo=timezone.utc)
        if now < open_at:
            return False
    if close_at is not None:
        if close_at.tzinfo is None:
            close_at = close_at.replace(tzinfo=timezone.utc)
        if now > close_at:
            return False
    return True


def _add_log(
    db: Session,
    registration: Registration,
    from_status: RegistrationStatus | None,
    to_status: RegistrationStatus,
    actor_id: int | None,
    reason: str | None,
) -> None:
    db.add(
        RegistrationLog(
            registration_id=registration.id,
            from_status=from_status,
            to_status=to_status,
            actor_id=actor_id,
            reason=reason,
        )
    )


def _active_confirmed_count(db: Session, course_id: int) -> int:
    return int(
        db.scalar(
            select(func.count(Registration.id)).where(
                Registration.course_id == course_id,
                Registration.status == RegistrationStatus.CONFIRMED,
            )
        )
        or 0
    )


def _next_waitlist_position(db: Session, course_id: int) -> int:
    max_pos = db.scalar(
        select(func.max(Registration.waitlist_position)).where(
            Registration.course_id == course_id,
            Registration.status == RegistrationStatus.WAITLIST,
        )
    )
    return (max_pos or 0) + 1


def _pull_waitlist(db: Session, course: Course, actor_id: int | None, reason: str) -> list[Registration]:
    promoted: list[Registration] = []

    while _active_confirmed_count(db, course.id) < course.max_capacity:
        candidate = db.scalar(
            select(Registration)
            .where(
                Registration.course_id == course.id,
                Registration.status == RegistrationStatus.WAITLIST,
            )
            .order_by(asc(Registration.waitlist_position), asc(Registration.created_at))
            .with_for_update(skip_locked=True)
            .limit(1)
        )
        if not candidate:
            break

        prev = candidate.status
        candidate.status = RegistrationStatus.CONFIRMED
        candidate.waitlist_position = None
        _add_log(db, candidate, prev, RegistrationStatus.CONFIRMED, actor_id, reason)
        create_notification(
            db,
            user_id=candidate.user_id,
            type=NotificationType.REGISTRATION,
            title="Promoted from waitlist",
            body="A seat is available. Your registration is now confirmed.",
            ref_id=candidate.id,
            ref_type="registration",
        )
        promoted.append(candidate)

    # Re-number waitlist to keep position contiguous.
    waiting = db.scalars(
        select(Registration)
        .where(
            Registration.course_id == course.id,
            Registration.status == RegistrationStatus.WAITLIST,
        )
        .order_by(asc(Registration.created_at), asc(Registration.id))
    ).all()
    for idx, reg in enumerate(waiting, start=1):
        reg.waitlist_position = idx

    return promoted


def create_registration(db: Session, current_user: User, course_id: int) -> Registration:
    if current_user.status == UserStatus.BANNED:
        raise HTTPException(status_code=403, detail="Banned account cannot register")

    course = db.scalar(select(Course).where(Course.id == course_id).with_for_update())
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")
    if not _is_course_open(course):
        raise HTTPException(status_code=400, detail="Course is not open for registration")

    active_existing = db.scalar(
        select(Registration).where(
            Registration.course_id == course_id,
            Registration.user_id == current_user.id,
            Registration.status.in_(
                [
                    RegistrationStatus.PENDING,
                    RegistrationStatus.CONFIRMED,
                    RegistrationStatus.WAITLIST,
                ]
            ),
        )
    )
    if active_existing:
        raise HTTPException(status_code=409, detail="You already have an active registration")

    registration = Registration(
        user_id=current_user.id,
        course_id=course_id,
        status=RegistrationStatus.PENDING,
    )
    db.add(registration)
    db.flush()

    _add_log(
        db,
        registration,
        from_status=None,
        to_status=RegistrationStatus.PENDING,
        actor_id=current_user.id,
        reason="Student submitted registration",
    )
    create_notification(
        db,
        user_id=current_user.id,
        type=NotificationType.REGISTRATION,
        title="Registration received",
        body="Your registration request is now pending review.",
        ref_id=registration.id,
        ref_type="registration",
    )

    db.commit()
    db.refresh(registration)
    return registration


def process_registration(
    db: Session,
    registration_id: int,
    actor: User,
    approve: bool,
    reason: str | None,
) -> Registration:
    if actor.role not in [UserRole.ADMIN, UserRole.INSTRUCTOR]:
        raise HTTPException(status_code=403, detail="Only admin or instructor can process")

    registration = db.scalar(
        select(Registration)
        .where(Registration.id == registration_id)
        .with_for_update(of=Registration)
    )
    if not registration:
        raise HTTPException(status_code=404, detail="Registration not found")
    if registration.status != RegistrationStatus.PENDING:
        raise HTTPException(status_code=400, detail="Only pending registrations can be processed")

    course = db.scalar(select(Course).where(Course.id == registration.course_id).with_for_update())
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    prev = registration.status
    if not approve:
        registration.status = RegistrationStatus.REJECTED
        _add_log(db, registration, prev, RegistrationStatus.REJECTED, actor.id, reason or "Rejected by admin")
        create_notification(
            db,
            user_id=registration.user_id,
            type=NotificationType.REGISTRATION,
            title="Registration rejected",
            body=reason or "Your registration has been rejected.",
            ref_id=registration.id,
            ref_type="registration",
        )
    else:
        if _active_confirmed_count(db, course.id) < course.max_capacity:
            registration.status = RegistrationStatus.CONFIRMED
            _add_log(db, registration, prev, RegistrationStatus.CONFIRMED, actor.id, reason or "Approved")
            create_notification(
                db,
                user_id=registration.user_id,
                type=NotificationType.REGISTRATION,
                title="Registration confirmed",
                body="Your registration has been confirmed. You can start learning now.",
                ref_id=registration.id,
                ref_type="registration",
            )
        else:
            raise HTTPException(status_code=400, detail="Lớp đã đủ, không thể duyệt thêm")

    db.commit()
    db.refresh(registration)
    return registration


def bulk_approve_pending(
    db: Session,
    actor: User,
    course_id: int | None,
    reason: str | None,
) -> dict:
    if actor.role not in [UserRole.ADMIN, UserRole.INSTRUCTOR]:
        raise HTTPException(status_code=403, detail="Only admin or instructor can process")

    course_filters = []
    if course_id is not None:
        course_filters.append(Registration.course_id == course_id)

    query = (
        select(Registration)
        .where(Registration.status == RegistrationStatus.PENDING, *course_filters)
        .order_by(asc(Registration.created_at), asc(Registration.id))
        .with_for_update(of=Registration)
    )
    pending_items = db.scalars(query).all()

    approved_count = 0
    rejected_count = 0
    rejected_items: list[dict] = []

    by_course: dict[int, list[Registration]] = {}
    for reg in pending_items:
        by_course.setdefault(reg.course_id, []).append(reg)

    for cid, items in by_course.items():
        course = db.scalar(select(Course).where(Course.id == cid).with_for_update())
        if not course:
            continue

        remaining = max(course.max_capacity - _active_confirmed_count(db, cid), 0)

        for index, reg in enumerate(items):
            prev = reg.status
            if index < remaining:
                reg.status = RegistrationStatus.CONFIRMED
                _add_log(db, reg, prev, RegistrationStatus.CONFIRMED, actor.id, reason or "Bulk approved")
                create_notification(
                    db,
                    user_id=reg.user_id,
                    type=NotificationType.REGISTRATION,
                    title="Registration confirmed",
                    body="Your registration has been confirmed. You can start learning now.",
                    ref_id=reg.id,
                    ref_type="registration",
                )
                approved_count += 1
            else:
                reg.status = RegistrationStatus.REJECTED
                reg.cancelled_by = actor.id
                reg.cancel_reason = "Lớp đã đủ"
                _add_log(db, reg, prev, RegistrationStatus.REJECTED, actor.id, "Lớp đã đủ")
                create_notification(
                    db,
                    user_id=reg.user_id,
                    type=NotificationType.REGISTRATION,
                    title="Registration rejected",
                    body="Lớp đã đủ, đăng ký của bạn đã bị từ chối.",
                    ref_id=reg.id,
                    ref_type="registration",
                )
                user = db.get(User, reg.user_id)
                rejected_items.append(
                    {
                        "registration_id": reg.id,
                        "user_id": reg.user_id,
                        "user_name": user.name if user else None,
                        "user_email": user.email if user else None,
                        "course_id": cid,
                        "course_title": course.title,
                        "reason": "Lớp đã đủ",
                    }
                )
                rejected_count += 1

    db.commit()
    return {
        "approved_count": approved_count,
        "rejected_count": rejected_count,
        "message": f"Đã duyệt {approved_count} đơn, từ chối {rejected_count} đơn do lớp đã đủ.",
        "rejected_items": rejected_items,
    }


def cancel_registration(
    db: Session,
    registration_id: int,
    actor: User,
    reason: str | None,
) -> Registration:
    registration = db.scalar(
        select(Registration).where(Registration.id == registration_id).with_for_update(of=Registration)
    )
    if not registration:
        raise HTTPException(status_code=404, detail="Registration not found")

    if actor.role == UserRole.STUDENT and registration.user_id != actor.id:
        raise HTTPException(status_code=403, detail="Cannot cancel someone else's registration")

    if registration.status not in [
        RegistrationStatus.PENDING,
        RegistrationStatus.CONFIRMED,
        RegistrationStatus.WAITLIST,
    ]:
        raise HTTPException(status_code=400, detail="Only active registrations can be cancelled")

    course = db.scalar(select(Course).where(Course.id == registration.course_id).with_for_update())
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    previous = registration.status
    registration.status = RegistrationStatus.CANCELLED
    registration.cancelled_by = actor.id
    registration.cancel_reason = reason
    registration.waitlist_position = None

    _add_log(db, registration, previous, RegistrationStatus.CANCELLED, actor.id, reason or "Cancelled")
    create_notification(
        db,
        user_id=registration.user_id,
        type=NotificationType.REGISTRATION,
        title="Registration cancelled",
        body=reason or "Your registration has been cancelled.",
        ref_id=registration.id,
        ref_type="registration",
    )

    if previous == RegistrationStatus.CONFIRMED:
        _pull_waitlist(
            db,
            course,
            actor_id=actor.id,
            reason="Auto-promote from waitlist after cancellation",
        )
    elif previous == RegistrationStatus.WAITLIST:
        _pull_waitlist(
            db,
            course,
            actor_id=actor.id,
            reason="Rebalance waitlist after waitlist cancellation",
        )

    db.commit()
    db.refresh(registration)
    return registration


def auto_expire_pending(db: Session, hours: int = 72) -> int:
    threshold = datetime.now(timezone.utc) - timedelta(hours=hours)
    stale_items = db.scalars(
        select(Registration).where(
            Registration.status == RegistrationStatus.PENDING,
            Registration.created_at <= threshold,
        )
    ).all()

    for reg in stale_items:
        prev = reg.status
        reg.status = RegistrationStatus.EXPIRED
        _add_log(db, reg, prev, RegistrationStatus.EXPIRED, None, "Auto-expired pending registration")

    db.commit()
    return len(stale_items)


def cancel_registrations_for_banned_user(db: Session, user_id: int, actor_id: int | None) -> int:
    items = db.scalars(
        select(Registration).where(
            Registration.user_id == user_id,
            Registration.status == RegistrationStatus.CONFIRMED,
        )
    ).all()

    total = 0
    affected_course_ids: set[int] = set()
    for reg in items:
        prev = reg.status
        reg.status = RegistrationStatus.CANCELLED
        reg.cancelled_by = actor_id
        reg.cancel_reason = "User account was banned"
        _add_log(db, reg, prev, RegistrationStatus.CANCELLED, actor_id, reg.cancel_reason)
        affected_course_ids.add(reg.course_id)
        total += 1

    for course_id in affected_course_ids:
        course = db.scalar(select(Course).where(Course.id == course_id).with_for_update())
        if course:
            _pull_waitlist(
                db,
                course,
                actor_id=actor_id,
                reason="Auto-promote from waitlist after banning user",
            )

    db.commit()
    return total


def list_registrations(
    db: Session,
    course_id: int | None,
    status: RegistrationStatus | None,
) -> list[Registration]:
    conditions = []
    if course_id is not None:
        conditions.append(Registration.course_id == course_id)
    if status is not None:
        conditions.append(Registration.status == status)

    query = select(Registration)
    if conditions:
        query = query.where(and_(*conditions))

    if status == RegistrationStatus.PENDING:
        return db.scalars(query.order_by(Registration.created_at.asc())).all()
    return db.scalars(query.order_by(Registration.created_at.desc())).all()
