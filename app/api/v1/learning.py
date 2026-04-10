from typing import Annotated
import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.course import Course
from app.models.enums import RegistrationStatus, UserRole
from app.models.enums import NotificationType
from app.models.certificate import Certificate
from app.models.lesson import Lesson
from app.models.progress import Progress
from app.models.registration import Registration
from app.models.section import Section
from app.models.user import User
from app.schemas.common import success_response
from app.schemas.learning import (
    LessonCreateRequest,
    LessonItem,
    ProgressUpsertRequest,
    SectionCreateRequest,
    SectionItem,
)
from app.services.notification_service import create_notification

router = APIRouter(prefix="/learning", tags=["learning"])


def _must_manage_content(user: User):
    if user.role not in [UserRole.ADMIN, UserRole.INSTRUCTOR]:
        raise HTTPException(status_code=403, detail="Only instructor/admin")


@router.post("/sections")
def create_section(
    payload: SectionCreateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    _must_manage_content(current_user)
    course = db.get(Course, payload.course_id)
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    section = Section(course_id=payload.course_id, title=payload.title, position=payload.position)
    db.add(section)
    db.commit()
    db.refresh(section)
    return success_response(data=SectionItem.model_validate(section).model_dump())


@router.post("/lessons")
def create_lesson(
    payload: LessonCreateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    _must_manage_content(current_user)
    section = db.get(Section, payload.section_id)
    if not section:
        raise HTTPException(status_code=404, detail="Section not found")

    lesson = Lesson(**payload.model_dump())
    db.add(lesson)
    db.commit()
    db.refresh(lesson)
    return success_response(data=LessonItem.model_validate(lesson).model_dump())


@router.get("/courses/{course_id}/outline")
def course_outline(course_id: int, db: Session = Depends(get_db)):
    sections = db.scalars(select(Section).where(Section.course_id == course_id).order_by(Section.position.asc())).all()
    payload = []
    for section in sections:
        lessons = db.scalars(
            select(Lesson).where(Lesson.section_id == section.id).order_by(Lesson.position.asc())
        ).all()
        payload.append(
            {
                "section": SectionItem.model_validate(section).model_dump(),
                "lessons": [LessonItem.model_validate(x).model_dump() for x in lessons],
            }
        )
    return success_response(data=payload)


@router.post("/progress")
def upsert_progress(
    payload: ProgressUpsertRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    lesson = db.get(Lesson, payload.lesson_id)
    if not lesson:
        raise HTTPException(status_code=404, detail="Lesson not found")

    section = db.get(Section, lesson.section_id)
    if not section:
        raise HTTPException(status_code=404, detail="Section not found")

    if not lesson.is_preview:
        registration = db.scalar(
            select(Registration).where(
                Registration.user_id == current_user.id,
                Registration.course_id == section.course_id,
                Registration.status == RegistrationStatus.CONFIRMED,
            )
        )
        if not registration:
            raise HTTPException(status_code=403, detail="Only confirmed students can update progress")

    item = db.scalar(
        select(Progress).where(Progress.user_id == current_user.id, Progress.lesson_id == payload.lesson_id)
    )
    if not item:
        item = Progress(user_id=current_user.id, lesson_id=payload.lesson_id)
        db.add(item)

    item.completion_pct = payload.completion_pct
    item.completed = payload.completion_pct >= 80 if lesson.type.value == "VIDEO" else payload.completion_pct >= 100

    # Auto issue certificate from DB state when user completes all lessons.
    cert_issued = False
    cert_id = None
    if section.course_id:
        lesson_ids = db.scalars(
            select(Lesson.id).join(Section, Section.id == Lesson.section_id).where(Section.course_id == section.course_id)
        ).all()
        if lesson_ids:
            completed_count = int(
                db.scalar(
                    select(func.count(Progress.id)).where(
                        Progress.user_id == current_user.id,
                        Progress.lesson_id.in_(lesson_ids),
                        Progress.completed.is_(True),
                    )
                )
                or 0
            )
            if completed_count >= len(lesson_ids):
                course = db.get(Course, section.course_id)
                if course and course.certificate_enabled:
                    existing_cert = db.scalar(
                        select(Certificate).where(
                            Certificate.user_id == current_user.id,
                            Certificate.course_id == section.course_id,
                        )
                    )
                    if existing_cert:
                        cert_issued = True
                        cert_id = existing_cert.id
                    else:
                        cert = Certificate(
                            user_id=current_user.id,
                            course_id=section.course_id,
                            verification_code=uuid.uuid4().hex,
                            pdf_url=f"/api/v1/certificates/{section.course_id}/pdf/{current_user.id}",
                        )
                        db.add(cert)
                        db.flush()
                        create_notification(
                            db,
                            user_id=current_user.id,
                            type=NotificationType.CERTIFICATE,
                            title="Certificate issued",
                            body=f"Certificate for course {course.title} has been issued.",
                            ref_id=section.course_id,
                            ref_type="course",
                        )
                        cert_issued = True
                        cert_id = cert.id

    db.commit()
    db.refresh(item)

    return success_response(
        data={
            "id": item.id,
            "user_id": item.user_id,
            "lesson_id": item.lesson_id,
            "completion_pct": item.completion_pct,
            "completed": item.completed,
            "certificate_issued": cert_issued,
            "certificate_id": cert_id,
        }
    )


@router.get("/courses/{course_id}/progress")
def course_progress(
    course_id: int,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    lesson_ids = db.scalars(
        select(Lesson.id).join(Section, Section.id == Lesson.section_id).where(Section.course_id == course_id)
    ).all()

    if not lesson_ids:
        return success_response(data={"total_lessons": 0, "completed_lessons": 0, "completion_pct": 0})

    completed_count = int(
        db.scalar(
            select(func.count(Progress.id)).where(
                Progress.user_id == current_user.id,
                Progress.lesson_id.in_(lesson_ids),
                Progress.completed.is_(True),
            )
        )
        or 0
    )
    total = len(lesson_ids)
    pct = int((completed_count / total) * 100)
    return success_response(data={"total_lessons": total, "completed_lessons": completed_count, "completion_pct": pct})


@router.get("/courses/{course_id}/progress-detail")
def course_progress_detail(
    course_id: int,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    lesson_ids = db.scalars(
        select(Lesson.id).join(Section, Section.id == Lesson.section_id).where(Section.course_id == course_id)
    ).all()

    if not lesson_ids:
        return success_response(data={"completed_lesson_ids": [], "completion_by_lesson": {}})

    items = db.scalars(
        select(Progress).where(
            Progress.user_id == current_user.id,
            Progress.lesson_id.in_(lesson_ids),
        )
    ).all()

    completion_map = {str(x.lesson_id): x.completion_pct for x in items}
    completed_ids = [x.lesson_id for x in items if x.completed]
    return success_response(data={"completed_lesson_ids": completed_ids, "completion_by_lesson": completion_map})
