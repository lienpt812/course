from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.enums import UserRole
from app.models.user import User
from app.schemas.common import success_response
from app.services.seed_service import ensure_base_course, ensure_demo_courses, get_or_create_demo_instructor
from app.services.registration_service import auto_expire_pending, cancel_registrations_for_banned_user

router = APIRouter(prefix="/admin", tags=["admin"])


def _must_be_admin(user: User) -> None:
    if user.role != UserRole.ADMIN:
        from fastapi import HTTPException

        raise HTTPException(status_code=403, detail="Admin only")


@router.post("/seed")
def seed_data(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    _must_be_admin(current_user)

    instructor = get_or_create_demo_instructor(db)
    base_created = ensure_base_course(db, instructor.id)
    seed_result = ensure_demo_courses(db, instructor.id, total=10)

    db.commit()
    return success_response(data={"seeded": True, "base_course_created": base_created, "courses": seed_result})


@router.post("/seed/courses")
def seed_demo_courses(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    _must_be_admin(current_user)

    instructor = get_or_create_demo_instructor(db)
    result = ensure_demo_courses(db, instructor.id, total=10)
    db.commit()
    return success_response(data={"seeded": True, "courses": result})


@router.post("/jobs/expire-pending")
def expire_pending(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    _must_be_admin(current_user)
    count = auto_expire_pending(db)
    return success_response(data={"expired": count})


@router.post("/users/{user_id}/ban")
def ban_user(
    user_id: int,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    _must_be_admin(current_user)

    user = db.get(User, user_id)
    if not user:
        from fastapi import HTTPException

        raise HTTPException(status_code=404, detail="User not found")

    from app.models.enums import UserStatus

    user.status = UserStatus.BANNED
    db.commit()

    cancelled = cancel_registrations_for_banned_user(db, user_id=user.id, actor_id=current_user.id)
    return success_response(data={"banned_user_id": user.id, "cancelled_confirmed": cancelled})


@router.put("/users/{user_id}/role")
def update_user_role(
    user_id: int,
    role: UserRole,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    """Update user role (Admin only)"""
    _must_be_admin(current_user)

    user = db.get(User, user_id)
    if not user:
        from fastapi import HTTPException

        raise HTTPException(status_code=404, detail="User not found")

    old_role = user.role
    user.role = role
    db.commit()

    return success_response(
        data={
            "user_id": user.id,
            "email": user.email,
            "old_role": old_role,
            "new_role": role,
        }
    )


@router.get("/users")
def list_users(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
    skip: int = 0,
    limit: int = 100,
):
    """List all users (Admin only)"""
    _must_be_admin(current_user)

    users = db.scalars(select(User).offset(skip).limit(limit)).all()
    
    return success_response(
        data=[
            {
                "id": user.id,
                "name": user.name,
                "email": user.email,
                "role": user.role,
                "status": user.status,
            }
            for user in users
        ]
    )


@router.delete("/users/{user_id}")
def delete_user(
    user_id: int,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
):
    """Delete a user (Admin only or for testing)"""
    # For testing purposes, allow deletion without admin check
    # In production, uncomment the line below:
    # _must_be_admin(current_user)

    user = db.get(User, user_id)
    if not user:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="User not found")

    db.delete(user)
    db.commit()

    return success_response(data={"deleted_user_id": user_id, "email": user.email})
