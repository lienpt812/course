from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.course import Course
from app.models.enums import CourseStatus, RegistrationStatus, UserRole
from app.models.registration import Registration
from app.models.registration_log import RegistrationLog
from app.models.user import User
from app.schemas.common import success_response
from app.schemas.course import CourseCreateRequest, CourseDetail, CourseListItem, CourseUpdateRequest
from app.services.registration_service import _pull_waitlist

router = APIRouter(prefix="/courses", tags=["courses"])


@router.get("")
def list_courses(
    status: CourseStatus | None = Query(default=CourseStatus.PUBLISHED),
    db: Session = Depends(get_db),
):
    query = select(Course)
    if status is not None:
        query = query.where(Course.status == status)

    items = db.scalars(query.order_by(Course.created_at.desc())).all()
    instructor_ids = {x.instructor_id for x in items}
    instructors = db.scalars(select(User).where(User.id.in_(instructor_ids))).all() if instructor_ids else []
    instructor_map = {u.id: u for u in instructors}

    rows: list[dict] = []
    for course in items:
        dto = CourseListItem.model_validate(course).model_dump()
        instructor = instructor_map.get(course.instructor_id)
        dto["instructor_name"] = instructor.name if instructor else None
        dto["instructor_email"] = instructor.email if instructor else None
        rows.append(dto)

    return success_response(data=rows)


@router.get("/{course_id}")
def course_detail(course_id: int, db: Session = Depends(get_db)):
    course = db.get(Course, course_id)
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    confirmed = (
        db.query(Registration)
        .filter(
            Registration.course_id == course_id,
            Registration.status == RegistrationStatus.CONFIRMED,
        )
        .count()
    )

    dto = CourseDetail.model_validate(course).model_dump()
    instructor = db.get(User, course.instructor_id)
    dto["instructor_name"] = instructor.name if instructor else None
    dto["instructor_email"] = instructor.email if instructor else None
    dto["confirmed_slots"] = confirmed
    dto["remaining_slots"] = max(course.max_capacity - confirmed, 0)
    return success_response(data=dto)


@router.post("")
def create_course(
    payload: CourseCreateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    if current_user.role not in [UserRole.ADMIN, UserRole.INSTRUCTOR]:
        raise HTTPException(status_code=403, detail="Only instructor/admin can create course")

    existing = db.scalar(select(Course).where(Course.slug == payload.slug))
    if existing:
        raise HTTPException(status_code=409, detail="Slug already exists")

    course = Course(
        **payload.model_dump(),
        instructor_id=current_user.id,
    )
    db.add(course)
    db.commit()
    db.refresh(course)
    dto = CourseDetail.model_validate(course).model_dump()
    dto["instructor_name"] = current_user.name
    dto["instructor_email"] = current_user.email
    return success_response(data=dto)


@router.patch("/{course_id}")
def update_course(
    course_id: int,
    payload: CourseUpdateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    course = db.get(Course, course_id)
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    if current_user.role not in [UserRole.ADMIN, UserRole.INSTRUCTOR]:
        raise HTTPException(status_code=403, detail="Only instructor/admin can update course")
    if current_user.role == UserRole.INSTRUCTOR and course.instructor_id != current_user.id:
        raise HTTPException(status_code=403, detail="Cannot update another instructor course")

    updates = payload.model_dump(exclude_unset=True)
    previous_status = course.status
    previous_capacity = course.max_capacity
    for k, v in updates.items():
        setattr(course, k, v)

    if previous_status != CourseStatus.ARCHIVED and course.status == CourseStatus.ARCHIVED:
        pendings = db.scalars(
            select(Registration).where(
                Registration.course_id == course.id,
                Registration.status == RegistrationStatus.PENDING,
            )
        ).all()
        for reg in pendings:
            reg.status = RegistrationStatus.CANCELLED
            reg.cancelled_by = current_user.id
            reg.cancel_reason = "Course archived"
            db.add(
                RegistrationLog(
                    registration_id=reg.id,
                    from_status=RegistrationStatus.PENDING,
                    to_status=RegistrationStatus.CANCELLED,
                    actor_id=current_user.id,
                    reason="Course archived: pending registration cancelled",
                )
            )

    if course.max_capacity > previous_capacity:
        _pull_waitlist(
            db,
            course,
            actor_id=current_user.id,
            reason="Auto-promote from waitlist after capacity increase",
        )

    db.commit()
    db.refresh(course)
    dto = CourseDetail.model_validate(course).model_dump()
    instructor = db.get(User, course.instructor_id)
    dto["instructor_name"] = instructor.name if instructor else None
    dto["instructor_email"] = instructor.email if instructor else None
    return success_response(data=dto)
