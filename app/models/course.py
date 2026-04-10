from datetime import datetime
from decimal import Decimal

from sqlalchemy import DateTime, Enum, ForeignKey, Integer, Numeric, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base
from app.models.enums import CourseStatus


class Course(Base):
    __tablename__ = "courses"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    slug: Mapped[str] = mapped_column(String(255), unique=True, nullable=False, index=True)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    image_url: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    instructor_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    category: Mapped[str | None] = mapped_column(String(100), nullable=True)
    max_capacity: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    estimated_hours: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    level: Mapped[str] = mapped_column(String(50), nullable=False, default="Beginner")
    status: Mapped[CourseStatus] = mapped_column(Enum(CourseStatus), nullable=False, default=CourseStatus.DRAFT)
    price: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False, default=0)
    prerequisites: Mapped[str | None] = mapped_column(Text, nullable=True)
    start_date: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    end_date: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    registration_open_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    registration_close_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    certificate_enabled: Mapped[bool] = mapped_column(default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    registrations = relationship("Registration", back_populates="course")
    sections = relationship("Section", cascade="all, delete-orphan")
