from datetime import datetime, timedelta, timezone
import secrets

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from jose import JWTError, jwt
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.deps import get_current_user
from app.core.security import (
    create_access_token,
    create_refresh_token,
    hash_password,
    verify_password,
)
from app.db.session import get_db
from app.models.user import User
from app.models.password_reset_token import PasswordResetToken
from app.schemas.auth import (
    ForgotPasswordRequest,
    LoginRequest,
    ProfileUpdateRequest,
    RefreshTokenRequest,
    RegisterRequest,
    ResetPasswordRequest,
    UserResponse,
)
from app.schemas.common import success_response
from app.services.notification_service import create_notification
from app.models.enums import NotificationType, UserRole

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register")
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    if payload.role == UserRole.INSTRUCTOR and not payload.expertise:
        raise HTTPException(status_code=400, detail="INSTRUCTOR registration requires expertise")

    if payload.role == UserRole.STUDENT and (not payload.student_major and not payload.learning_goal):
        raise HTTPException(status_code=400, detail="STUDENT registration requires student_major or learning_goal")

    existing = db.scalar(select(User).where(User.email == payload.email))
    if existing:
        raise HTTPException(status_code=409, detail="Email already exists")

    user = User(
        name=payload.name,
        email=payload.email,
        password_hash=hash_password(payload.password),
        phone_number=payload.phone,
        email_verified=False,
        education=payload.education,
        expertise=payload.expertise,
        learning_goal=payload.learning_goal,
        student_major=payload.student_major,
    )
    user.role = payload.role
    
    db.add(user)
    db.commit()
    db.refresh(user)

    access = create_access_token(str(user.id))
    refresh = create_refresh_token(str(user.id))
    return success_response(
        data={
            "access_token": access,
            "refresh_token": refresh,
            "token_type": "bearer",
            "user": UserResponse.model_validate(user).model_dump(),
        }
    )


@router.post("/login")
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.email == payload.email))
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    access = create_access_token(str(user.id))
    refresh = create_refresh_token(str(user.id))
    return success_response(
        data={
            "access_token": access,
            "refresh_token": refresh,
            "token_type": "bearer",
        }
    )


@router.post("/refresh")
def refresh_token(payload: RefreshTokenRequest):
    try:
        decoded = jwt.decode(payload.refresh_token, settings.secret_key, algorithms=[settings.algorithm])
        if decoded.get("type") != "refresh":
            raise HTTPException(status_code=401, detail="Invalid token type")
        user_id = decoded.get("sub")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    return success_response(
        data={
            "access_token": create_access_token(str(user_id)),
            "refresh_token": create_refresh_token(str(user_id)),
            "token_type": "bearer",
        }
    )


@router.get("/me")
def me(current_user: Annotated[User, Depends(get_current_user)]):
    return success_response(data=UserResponse.model_validate(current_user).model_dump())


@router.patch("/me")
def update_me(
    payload: ProfileUpdateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    updates = payload.model_dump(exclude_unset=True)

    if current_user.role == UserRole.INSTRUCTOR:
        if "education" in updates and not updates.get("education"):
            raise HTTPException(status_code=400, detail="education cannot be empty for INSTRUCTOR")
        if "expertise" in updates and not updates.get("expertise"):
            raise HTTPException(status_code=400, detail="expertise cannot be empty for INSTRUCTOR")

    if current_user.role == UserRole.STUDENT:
        if "student_major" in updates and not updates.get("student_major") and not current_user.learning_goal:
            raise HTTPException(status_code=400, detail="student_major or learning_goal is required for STUDENT")
        if "learning_goal" in updates and not updates.get("learning_goal") and not current_user.student_major:
            raise HTTPException(status_code=400, detail="student_major or learning_goal is required for STUDENT")

    for key, value in updates.items():
        setattr(current_user, key, value)

    db.add(current_user)
    db.commit()
    db.refresh(current_user)
    return success_response(data=UserResponse.model_validate(current_user).model_dump())


@router.get("/debug/me")
def debug_me(current_user: Annotated[User, Depends(get_current_user)]):
    """Debug endpoint to check user data"""
    return success_response(
        data={
            "id": current_user.id,
            "name": current_user.name,
            "email": current_user.email,
            "role": current_user.role,
            "role_value": current_user.role.value if hasattr(current_user.role, 'value') else str(current_user.role),
            "status": current_user.status,
        }
    )


@router.post("/forgot-password")
def forgot_password(payload: ForgotPasswordRequest, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.email == payload.email))
    if not user:
        return success_response(data={"message": "If email exists, reset instructions were sent."})

    token = secrets.token_urlsafe(32)
    item = PasswordResetToken(
        user_id=user.id,
        token=token,
        expires_at=datetime.now(timezone.utc) + timedelta(minutes=30),
    )
    db.add(item)
    create_notification(
        db,
        user_id=user.id,
        type=NotificationType.SYSTEM,
        title="Password reset requested",
        body="A password reset request was created for your account.",
    )
    db.commit()
    return success_response(
        data={
            "message": "Reset token generated. Integrate email sender in production.",
            "reset_token": token,
        }
    )


@router.post("/reset-password")
def reset_password(payload: ResetPasswordRequest, db: Session = Depends(get_db)):
    token_item = db.scalar(select(PasswordResetToken).where(PasswordResetToken.token == payload.token))
    if not token_item or token_item.used:
        raise HTTPException(status_code=400, detail="Invalid reset token")
    if token_item.expires_at < datetime.now(timezone.utc):
        raise HTTPException(status_code=400, detail="Reset token expired")

    user = db.get(User, token_item.user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    user.password_hash = hash_password(payload.new_password)
    token_item.used = True
    db.commit()
    return success_response(data={"password_reset": True})


# Test endpoint - Create test accounts with specific roles
@router.post("/test/create-account-with-role")
def create_test_account(
    name: str,
    email: str,
    password: str,
    role: UserRole,
    db: Session = Depends(get_db),
):
    """Endpoint for testing only - creates accounts with specified roles"""
    existing = db.scalar(select(User).where(User.email == email))
    if existing:
        raise HTTPException(status_code=409, detail="Email already exists")

    user = User(
        name=name,
        email=email,
        password_hash=hash_password(password),
        role=role,
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    return success_response(
        data={
            "id": user.id,
            "name": user.name,
            "email": user.email,
            "role": user.role,
        }
    )


@router.delete("/test/delete-user-by-email")
def delete_user_by_email(
    email: str,
    db: Session = Depends(get_db),
):
    """Endpoint for testing only - deletes user by email"""
    user = db.scalar(select(User).where(User.email == email))
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    user_data = {
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "role": user.role,
    }

    db.delete(user)
    db.commit()

    return success_response(data={"deleted_user": user_data})
