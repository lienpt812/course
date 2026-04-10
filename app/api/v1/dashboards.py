from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.course import Course
from app.models.enums import RegistrationStatus, UserRole
from app.models.registration import Registration
from app.models.user import User
from app.schemas.common import success_response

router = APIRouter(prefix="/dashboards", tags=["dashboards"])


@router.get("/student")
def student_dashboard(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    current_courses = int(
        db.scalar(
            select(func.count(Registration.id)).where(
                Registration.user_id == current_user.id,
                Registration.status == RegistrationStatus.CONFIRMED,
            )
        )
        or 0
    )
    history_count = int(db.scalar(select(func.count(Registration.id)).where(Registration.user_id == current_user.id)) or 0)
    return success_response(data={"current_courses": current_courses, "registration_history": history_count})


@router.get("/instructor")
def instructor_dashboard(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    if current_user.role not in [UserRole.INSTRUCTOR, UserRole.ADMIN]:
        raise HTTPException(status_code=403, detail="Instructor/Admin only")

    # Get all courses for this instructor
    courses = db.scalars(select(Course).where(Course.instructor_id == current_user.id)).all()
    
    if not courses:
        return success_response(
            data={
                "total_courses": 0,
                "total_students": 0,
                "total_registrations": 0,
                "courses": []
            }
        )

    course_ids = [c.id for c in courses]
    
    total_students = int(
        db.scalar(
            select(func.count(Registration.id)).where(
                Registration.course_id.in_(course_ids),
                Registration.status == RegistrationStatus.CONFIRMED,
            )
        )
        or 0
    )
    
    total_registrations = int(
        db.scalar(
            select(func.count(Registration.id)).where(
                Registration.course_id.in_(course_ids)
            )
        )
        or 0
    )

    # Format course data for frontend
    courses_data = []
    for course in courses:
        confirmed_count = int(
            db.scalar(
                select(func.count(Registration.id)).where(
                    Registration.course_id == course.id,
                    Registration.status == RegistrationStatus.CONFIRMED,
                )
            )
            or 0
        )
        courses_data.append({
            "id": course.id,
            "title": course.title,
            "status": course.status.value if hasattr(course.status, 'value') else str(course.status),
            "current_participants": confirmed_count,
            "max_participants": course.max_capacity or 30,
            "estimated_hours": course.estimated_hours or 0,
            "instructor_name": current_user.name,
        })

    return success_response(
        data={
            "total_courses": len(courses),
            "total_students": total_students,
            "total_registrations": total_registrations,
            "courses": courses_data,
        }
    )


@router.get("/admin")
def admin_dashboard(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    if current_user.role != UserRole.ADMIN:
        raise HTTPException(status_code=403, detail="Admin only")

    courses = int(db.scalar(select(func.count(Course.id))) or 0)
    users = int(db.scalar(select(func.count(User.id))) or 0)
    pending = int(
        db.scalar(select(func.count(Registration.id)).where(Registration.status == RegistrationStatus.PENDING)) or 0
    )

    return success_response(data={"total_courses": courses, "total_users": users, "pending_registrations": pending})
