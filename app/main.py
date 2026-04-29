from pathlib import Path
import time
from collections import defaultdict, deque

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from sqlalchemy import text

from app.api.v1.router import api_router
from app.core.config import settings
from app.db.session import Base, SessionLocal, engine
from app.models import (
    certificate,
    course,
    lesson,
    notification,
    password_reset_token,
    progress,
    registration,
    registration_log,
    section,
    user,
)  # noqa: F401
from app.models.course import Course
from app.models.enums import CourseStatus, RegistrationStatus
from app.models.registration import Registration
from app.schemas.common import error_response
from app.services.seed_service import (
    ensure_altwalker_e2e_fixtures,
    ensure_base_course,
    ensure_demo_courses,
    get_or_create_demo_instructor,
)

BASE_DIR = Path(__file__).resolve().parent

app = FastAPI(title=settings.app_name, version="1.0.0")

_rate_windows: dict[tuple[str, str], deque[float]] = defaultdict(deque)


@app.middleware("http")
async def tier_rate_limit(request: Request, call_next):
    if not settings.http_rate_limit_enabled:
        return await call_next(request)
    path = request.url.path
    if path in {"/health", "/docs", "/openapi.json", "/redoc"} or path.startswith("/static"):
        return await call_next(request)

    auth_header = request.headers.get("authorization", "")
    is_authenticated = auth_header.lower().startswith("bearer ")
    # MBT + browser + ApiClient can exceed 500/min on loopback during long GraphWalker runs.
    limit = 5000 if is_authenticated else 100

    client = request.client.host if request.client else "unknown"
    key = (client, "auth" if is_authenticated else "public")
    now = time.time()
    window = 60.0
    q = _rate_windows[key]

    while q and now - q[0] > window:
        q.popleft()

    if len(q) >= limit:
        return JSONResponse(
            status_code=429,
            content=error_response("Rate limit exceeded", code="RATE_LIMITED", status=429),
        )

    q.append(now)
    return await call_next(request)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin.strip() for origin in settings.cors_origins.split(",")],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory=str(BASE_DIR / "static")), name="static")
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))


@app.on_event("startup")
def on_startup() -> None:
    Base.metadata.create_all(bind=engine)
    with engine.begin() as conn:
        # Keep existing environments compatible when new optional fields are introduced.
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30)"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(1000)"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS interests VARCHAR(1000)"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS education VARCHAR(255)"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS expertise VARCHAR(500)"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS learning_goal VARCHAR(1000)"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS student_major VARCHAR(255)"))
        conn.execute(text("ALTER TABLE courses ADD COLUMN IF NOT EXISTS estimated_hours INTEGER NOT NULL DEFAULT 0"))
        conn.execute(text("ALTER TABLE courses ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000)"))
        conn.execute(text("ALTER TABLE courses ADD COLUMN IF NOT EXISTS prerequisites TEXT"))

    with SessionLocal() as db:
        instructor = get_or_create_demo_instructor(db)
        ensure_base_course(db, instructor.id)
        ensure_demo_courses(db, instructor.id, total=10)
        # MBT / GraphWalker: admin@test.com, student@test.com, instructor@test.com (Password123!)
        ensure_altwalker_e2e_fixtures(db)
        db.commit()


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception):
    return JSONResponse(status_code=500, content=error_response(str(exc), code="INTERNAL_ERROR", status=500))


@app.get("/", response_class=HTMLResponse)
async def landing(request: Request):
    with SessionLocal() as db:
        courses = (
            db.query(Course)
            .filter(Course.status == CourseStatus.PUBLISHED)
            .order_by(Course.created_at.desc())
            .limit(12)
            .all()
        )
    return templates.TemplateResponse(
        "index.html",
        {
            "request": request,
            "courses": courses,
        },
    )


@app.get("/pages/courses", response_class=HTMLResponse)
async def courses_page(request: Request):
    with SessionLocal() as db:
        courses = (
            db.query(Course)
            .filter(Course.status == CourseStatus.PUBLISHED)
            .order_by(Course.created_at.desc())
            .all()
        )
    return templates.TemplateResponse("courses.html", {"request": request, "courses": courses})


@app.get("/pages/courses/{course_id}", response_class=HTMLResponse)
async def course_detail_page(request: Request, course_id: int):
    with SessionLocal() as db:
        course = db.query(Course).filter(Course.id == course_id).first()
        if not course:
            return templates.TemplateResponse(
                "course_detail.html",
                {"request": request, "course": None, "confirmed_count": 0, "remaining_slots": 0},
                status_code=404,
            )

        confirmed_count = (
            db.query(Registration)
            .filter(
                Registration.course_id == course.id,
                Registration.status == RegistrationStatus.CONFIRMED,
            )
            .count()
        )
        remaining_slots = max(course.max_capacity - confirmed_count, 0)

    return templates.TemplateResponse(
        "course_detail.html",
        {
            "request": request,
            "course": course,
            "confirmed_count": confirmed_count,
            "remaining_slots": remaining_slots,
        },
    )


@app.get("/pages/student-dashboard", response_class=HTMLResponse)
async def student_dashboard_page(request: Request):
    return templates.TemplateResponse("student_dashboard.html", {"request": request})


@app.get("/pages/admin-registrations", response_class=HTMLResponse)
async def admin_registrations_page(request: Request):
    return templates.TemplateResponse("admin_registrations.html", {"request": request})


@app.get("/pages/certificate-verify", response_class=HTMLResponse)
async def certificate_verify_page(request: Request):
    return templates.TemplateResponse("certificate_verify.html", {"request": request})


@app.get("/pages/login", response_class=HTMLResponse)
async def login_page(request: Request):
    return templates.TemplateResponse("login.html", {"request": request})


@app.get("/health")
def health_check():
    return {"status": "ok"}


app.include_router(api_router, prefix=settings.api_v1_prefix)
