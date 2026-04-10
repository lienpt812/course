from pydantic import BaseModel, EmailStr, Field

from app.models.enums import UserRole, UserStatus


class RegisterRequest(BaseModel):
    name: str = Field(min_length=2, max_length=255)
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    role: UserRole = UserRole.STUDENT
    phone: str | None = None
    education: str | None = None
    expertise: str | None = None
    learning_goal: str | None = None
    student_major: str | None = None


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenPair(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class ResetPasswordRequest(BaseModel):
    token: str
    new_password: str = Field(min_length=8, max_length=128)


class UserResponse(BaseModel):
    id: int
    name: str
    email: EmailStr
    phone_number: str | None = None
    role: UserRole
    status: UserStatus
    email_verified: bool = False
    avatar_url: str | None = None
    bio: str | None = None
    interests: str | None = None
    education: str | None = None
    expertise: str | None = None
    learning_goal: str | None = None
    student_major: str | None = None

    class Config:
        from_attributes = True


class ProfileUpdateRequest(BaseModel):
    name: str | None = Field(default=None, min_length=2, max_length=255)
    phone_number: str | None = None
    avatar_url: str | None = None
    bio: str | None = None
    interests: str | None = None
    education: str | None = None
    expertise: str | None = None
    learning_goal: str | None = None
    student_major: str | None = None
