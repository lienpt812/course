from fastapi import APIRouter

from app.api.v1 import (
	admin,
	auth,
	certificates,
	courses,
	dashboards,
	learning,
	notifications,
	registrations,
)

api_router = APIRouter()
api_router.include_router(auth.router)
api_router.include_router(courses.router)
api_router.include_router(registrations.router)
api_router.include_router(admin.router)
api_router.include_router(learning.router)
api_router.include_router(certificates.router)
api_router.include_router(notifications.router)
api_router.include_router(dashboards.router)
