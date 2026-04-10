from sqlalchemy import Boolean, Enum, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base
from app.models.enums import LessonType


class Lesson(Base):
    __tablename__ = "lessons"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    section_id: Mapped[int] = mapped_column(ForeignKey("sections.id"), nullable=False, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    type: Mapped[LessonType] = mapped_column(Enum(LessonType), nullable=False, default=LessonType.VIDEO)
    content_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    is_preview: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    position: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    duration_minutes: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    section = relationship("Section", back_populates="lessons")
