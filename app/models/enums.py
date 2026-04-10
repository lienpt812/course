from enum import Enum


class UserRole(str, Enum):
    GUEST = "GUEST"
    STUDENT = "STUDENT"
    INSTRUCTOR = "INSTRUCTOR"
    ADMIN = "ADMIN"


class UserStatus(str, Enum):
    ACTIVE = "ACTIVE"
    BANNED = "BANNED"


class CourseStatus(str, Enum):
    DRAFT = "DRAFT"
    PUBLISHED = "PUBLISHED"
    ARCHIVED = "ARCHIVED"
    COMING_SOON = "COMING_SOON"


class RegistrationStatus(str, Enum):
    PENDING = "PENDING"
    CONFIRMED = "CONFIRMED"
    WAITLIST = "WAITLIST"
    CANCELLED = "CANCELLED"
    REJECTED = "REJECTED"
    EXPIRED = "EXPIRED"


class LessonType(str, Enum):
    VIDEO = "VIDEO"
    QUIZ = "QUIZ"
    DOC = "DOC"
    TEXT = "TEXT"


class NotificationType(str, Enum):
    REGISTRATION = "REGISTRATION"
    LEARNING = "LEARNING"
    CERTIFICATE = "CERTIFICATE"
    SYSTEM = "SYSTEM"
