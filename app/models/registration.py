from datetime import datetime

from sqlalchemy import (
    DateTime,
    Enum,
    ForeignKey,
    Index,
    Integer,
    String,
    UniqueConstraint,
    func,
    text,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base
from app.models.enums import RegistrationStatus


class Registration(Base):
    __tablename__ = "registrations"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    course_id: Mapped[int] = mapped_column(ForeignKey("courses.id"), nullable=False)
    status: Mapped[RegistrationStatus] = mapped_column(
        Enum(RegistrationStatus), default=RegistrationStatus.PENDING, nullable=False
    )
    waitlist_position: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )
    cancelled_by: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True)
    cancel_reason: Mapped[str | None] = mapped_column(String(500), nullable=True)

    user = relationship("User", back_populates="registrations", foreign_keys=[user_id])
    course = relationship("Course", back_populates="registrations")
    logs = relationship("RegistrationLog", back_populates="registration", cascade="all, delete-orphan")

    __table_args__ = (
        UniqueConstraint("id", "course_id", name="uq_registration_id_course_id"),
        Index(
            "ix_registration_user_course_active",
            "user_id",
            "course_id",
            unique=True,
            postgresql_where=text("status IN ('PENDING','CONFIRMED','WAITLIST')"),
        ),
        Index("ix_registration_course_status_waitlist", "course_id", "status", "waitlist_position"),
    )
