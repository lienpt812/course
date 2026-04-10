from pydantic import BaseModel, Field

from app.models.enums import LessonType


class SectionCreateRequest(BaseModel):
    course_id: int
    title: str = Field(min_length=2, max_length=255)
    position: int = Field(default=1, ge=1)


class LessonCreateRequest(BaseModel):
    section_id: int
    title: str = Field(min_length=2, max_length=255)
    type: LessonType = LessonType.VIDEO
    content_url: str | None = None
    is_preview: bool = False
    position: int = Field(default=1, ge=1)
    duration_minutes: int = Field(default=0, ge=0)


class ProgressUpsertRequest(BaseModel):
    lesson_id: int
    completion_pct: int = Field(ge=0, le=100)


class SectionItem(BaseModel):
    id: int
    course_id: int
    title: str
    position: int

    class Config:
        from_attributes = True


class LessonItem(BaseModel):
    id: int
    section_id: int
    title: str
    type: LessonType
    content_url: str | None = None
    is_preview: bool
    position: int
    duration_minutes: int

    class Config:
        from_attributes = True
