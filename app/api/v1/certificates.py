import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.certificate import Certificate
from app.models.course import Course
from app.models.enums import RegistrationStatus
from app.models.lesson import Lesson
from app.models.progress import Progress
from app.models.registration import Registration
from app.models.section import Section
from app.models.user import User
from app.schemas.certificate import CertificateItem
from app.schemas.common import success_response
from app.services.notification_service import create_notification
from app.models.enums import NotificationType

router = APIRouter(prefix="/certificates", tags=["certificates"])


@router.post("/issue/{course_id}")
def issue_certificate(
    course_id: int,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    course = db.get(Course, course_id)
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")
    if not course.certificate_enabled:
        raise HTTPException(status_code=400, detail="Certificate is disabled for this course")

    reg = db.scalar(
        select(Registration).where(
            Registration.user_id == current_user.id,
            Registration.course_id == course_id,
            Registration.status == RegistrationStatus.CONFIRMED,
        )
    )
    if not reg:
        raise HTTPException(status_code=403, detail="Only confirmed students can issue certificate")

    lesson_ids = db.scalars(
        select(Lesson.id).join(Section, Section.id == Lesson.section_id).where(Section.course_id == course_id)
    ).all()
    if not lesson_ids:
        raise HTTPException(status_code=400, detail="Course has no lessons")

    completed = db.scalars(
        select(Progress).where(
            Progress.user_id == current_user.id,
            Progress.lesson_id.in_(lesson_ids),
            Progress.completed.is_(True),
        )
    ).all()

    if len(completed) < len(lesson_ids):
        raise HTTPException(status_code=400, detail="Course completion is not 100%")

    existing = db.scalar(select(Certificate).where(Certificate.user_id == current_user.id, Certificate.course_id == course_id))
    if existing:
        return success_response(data=CertificateItem.model_validate(existing).model_dump())

    cert = Certificate(
        user_id=current_user.id,
        course_id=course_id,
        verification_code=uuid.uuid4().hex,
        pdf_url=f"/api/v1/certificates/{course_id}/pdf/{current_user.id}",
    )
    db.add(cert)
    create_notification(
        db,
        user_id=current_user.id,
        type=NotificationType.CERTIFICATE,
        title="Certificate issued",
        body=f"Certificate for course {course.title} has been issued.",
        ref_id=course_id,
        ref_type="course",
    )
    db.commit()
    db.refresh(cert)
    return success_response(data=CertificateItem.model_validate(cert).model_dump())


@router.get("/verify/{code}")
def verify_certificate(code: str, db: Session = Depends(get_db)):
    cert = db.scalar(select(Certificate).where(Certificate.verification_code == code))
    if not cert:
        return success_response(data={"valid": False})
    return success_response(data={"valid": True, "certificate": CertificateItem.model_validate(cert).model_dump()})


@router.get("/me")
def my_certificates(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    items = db.scalars(select(Certificate).where(Certificate.user_id == current_user.id)).all()
    return success_response(data=[CertificateItem.model_validate(x).model_dump() for x in items])
