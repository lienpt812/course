from datetime import datetime

from pydantic import BaseModel, Field

from app.models.enums import CourseStatus


class CourseCreateRequest(BaseModel):
    title: str = Field(min_length=2, max_length=255)
    slug: str = Field(min_length=2, max_length=255)
    description: str = Field(min_length=10)
    image_url: str | None = None
    category: str | None = None
    max_capacity: int = Field(default=1, ge=1)
    estimated_hours: int = Field(default=0, ge=0)
    level: str = "Beginner"
    status: CourseStatus = CourseStatus.DRAFT
    price: float = Field(default=0, ge=0)
    prerequisites: str | None = None
    start_date: datetime | None = None
    end_date: datetime | None = None
    registration_open_at: datetime | None = None
    registration_close_at: datetime | None = None
    certificate_enabled: bool = False


class CourseUpdateRequest(BaseModel):
    title: str | None = Field(default=None, min_length=2, max_length=255)
    description: str | None = Field(default=None, min_length=10)
    image_url: str | None = None
    category: str | None = None
    max_capacity: int | None = Field(default=None, ge=1)
    estimated_hours: int | None = Field(default=None, ge=0)
    level: str | None = None
    status: CourseStatus | None = None
    price: float | None = Field(default=None, ge=0)
    prerequisites: str | None = None
    start_date: datetime | None = None
    end_date: datetime | None = None
    registration_open_at: datetime | None = None
    registration_close_at: datetime | None = None
    certificate_enabled: bool | None = None


class CourseListItem(BaseModel):
    id: int
    title: str
    slug: str
    description: str
    image_url: str | None = None
    instructor_id: int
    instructor_name: str | None = None
    instructor_email: str | None = None
    category: str | None = None
    max_capacity: int
    estimated_hours: int = 0
    status: CourseStatus
    level: str
    price: float = 0
    prerequisites: str | None = None
    start_date: datetime | None = None
    end_date: datetime | None = None
    certificate_enabled: bool = False

    class Config:
        from_attributes = True


class CourseDetail(CourseListItem):
    registration_open_at: datetime | None = None
    registration_close_at: datetime | None = None
