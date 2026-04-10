from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.models.course import Course
from app.models.enums import CourseStatus, UserRole
from app.models.user import User

_DEMO_IMAGE_URLS = [
    "https://picsum.photos/id/180/1200/720",
    "https://picsum.photos/id/26/1200/720",
    "https://picsum.photos/id/0/1200/720",
    "https://picsum.photos/id/48/1200/720",
    "https://picsum.photos/id/366/1200/720",
    "https://picsum.photos/id/52/1200/720",
    "https://picsum.photos/id/119/1200/720",
    "https://picsum.photos/id/60/1200/720",
    "https://picsum.photos/id/3/1200/720",
    "https://picsum.photos/id/29/1200/720",
]

_DEMO_PRICES = [
    299000,
    349000,
    399000,
    459000,
    319000,
    499000,
    429000,
    389000,
    469000,
    359000,
]


def get_or_create_demo_instructor(db: Session) -> User:
    instructor = db.scalar(select(User).where(User.email == "instructor@example.com"))
    if instructor:
        return instructor

    instructor = User(
        name="Demo Instructor",
        email="instructor@example.com",
        password_hash=hash_password("Password123!"),
        role=UserRole.INSTRUCTOR,
    )
    db.add(instructor)
    db.flush()
    return instructor


def ensure_demo_courses(db: Session, instructor_id: int, total: int = 10) -> dict:
    categories = [
        "Backend",
        "Frontend",
        "DevOps",
        "Data",
        "Mobile",
        "AI",
        "Security",
        "Testing",
        "Cloud",
        "Product",
    ]
    levels = ["Beginner", "Intermediate", "Advanced"]

    created_count = 0
    existing_count = 0

    for i in range(1, total + 1):
        slug = f"demo-course-{i:02d}"
        image_url = _DEMO_IMAGE_URLS[(i - 1) % len(_DEMO_IMAGE_URLS)]
        price = _DEMO_PRICES[(i - 1) % len(_DEMO_PRICES)]
        existing = db.scalar(select(Course).where(Course.slug == slug))
        category = categories[(i - 1) % len(categories)]
        level = levels[(i - 1) % len(levels)]
        if existing:
            # Keep demo data consistent across runs (price/image can be refreshed).
            existing.title = f"{category} Practical Track {i}"
            existing.description = f"Khoa hoc demo so {i} ve {category} voi bai tap thuc hanh va huong dan tung buoc."
            existing.image_url = image_url
            existing.category = category
            existing.max_capacity = 20 + i
            existing.estimated_hours = 8 + i
            existing.level = level
            existing.status = CourseStatus.PUBLISHED
            existing.price = price
            existing.certificate_enabled = i % 2 == 0
            existing_count += 1
            continue

        db.add(
            Course(
                title=f"{category} Practical Track {i}",
                slug=slug,
                description=f"Khoa hoc demo so {i} ve {category} voi bai tap thuc hanh va huong dan tung buoc.",
                image_url=image_url,
                instructor_id=instructor_id,
                category=category,
                max_capacity=20 + i,
                estimated_hours=8 + i,
                level=level,
                status=CourseStatus.PUBLISHED,
                price=price,
                certificate_enabled=(i % 2 == 0),
            )
        )
        created_count += 1

    return {"created": created_count, "existing": existing_count, "target": total}


def ensure_base_course(db: Session, instructor_id: int) -> bool:
    course = db.scalar(select(Course).where(Course.slug == "python-backend-foundation"))
    if course:
        # Keep this course updated too (fix broken image and pricing).
        course.image_url = "https://picsum.photos/id/180/1200/720"
        course.price = 259000
        course.estimated_hours = max(course.estimated_hours or 0, 12)
        return False

    db.add(
        Course(
            title="Python Backend Foundation",
            slug="python-backend-foundation",
            description="FastAPI, PostgreSQL, Docker cho nguoi moi bat dau.",
            image_url="https://picsum.photos/id/180/1200/720",
            instructor_id=instructor_id,
            category="Backend",
            max_capacity=2,
            estimated_hours=12,
            level="Beginner",
            status=CourseStatus.PUBLISHED,
            price=259000,
            certificate_enabled=True,
        )
    )
    return True
