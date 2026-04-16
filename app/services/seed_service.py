from sqlalchemy import delete, func, select
from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.models.certificate import Certificate
from app.models.course import Course
from app.models.enums import CourseStatus, LessonType, RegistrationStatus, UserRole
from app.models.lesson import Lesson
from app.models.progress import Progress
from app.models.registration import Registration
from app.models.section import Section
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


def _get_or_create_test_user(
    db: Session,
    *,
    email: str,
    password: str,
    name: str,
    role: UserRole,
    student_major: str | None = None,
    learning_goal: str | None = None,
    expertise: str | None = None,
) -> User:
    u = db.scalar(select(User).where(User.email == email))
    if u:
        return u
    u = User(
        name=name,
        email=email,
        password_hash=hash_password(password),
        role=role,
        student_major=student_major,
        learning_goal=learning_goal,
        expertise=expertise,
    )
    db.add(u)
    db.flush()
    return u


def ensure_altwalker_e2e_fixtures(db: Session) -> dict:
    """
    MBT / AltWalker: accounts student@test.com, instructor@test.com, admin@test.com;
    one published course (slug altwalker-e2e-cert) with ≥2 lessons, cert enabled;
    student CONFIRMED; progress + certificate cleared so completion is never 100% at seed time.
    """
    student = _get_or_create_test_user(
        db,
        email="student@test.com",
        password="Password123!",
        name="Test Student",
        role=UserRole.STUDENT,
        student_major="Computer Science",
        learning_goal="E2E testing",
    )
    instructor = _get_or_create_test_user(
        db,
        email="instructor@test.com",
        password="Password123!",
        name="Test Instructor",
        role=UserRole.INSTRUCTOR,
        expertise="Software Engineering",
    )
    _get_or_create_test_user(
        db,
        email="admin@test.com",
        password="Password123!",
        name="Test Admin",
        role=UserRole.ADMIN,
    )

    slug = "altwalker-e2e-cert"
    course = db.scalar(select(Course).where(Course.slug == slug))
    if not course:
        course = Course(
            title="AltWalker E2E — Certificate flow",
            slug=slug,
            description="Seeded for MBT: student should start with incomplete lessons.",
            image_url="https://picsum.photos/id/60/1200/720",
            instructor_id=instructor.id,
            category="Testing",
            max_capacity=50,
            estimated_hours=4,
            level="Beginner",
            status=CourseStatus.PUBLISHED,
            price=0,
            certificate_enabled=True,
        )
        db.add(course)
        db.flush()
    else:
        course.instructor_id = instructor.id
        course.status = CourseStatus.PUBLISHED
        course.certificate_enabled = True

    section = db.scalar(
        select(Section).where(Section.course_id == course.id).order_by(Section.position.asc()).limit(1)
    )
    if not section:
        section = Section(course_id=course.id, title="MBT Section 1", position=1)
        db.add(section)
        db.flush()

    while (
        int(db.scalar(select(func.count(Lesson.id)).where(Lesson.section_id == section.id)) or 0) < 2
    ):
        max_pos = db.scalar(select(func.max(Lesson.position)).where(Lesson.section_id == section.id)) or 0
        next_pos = int(max_pos) + 1
        db.add(
            Lesson(
                section_id=section.id,
                title=f"MBT Lesson {next_pos}",
                type=LessonType.TEXT,
                position=next_pos,
                duration_minutes=10,
                is_preview=False,
            )
        )
        db.flush()

    reg = db.scalar(
        select(Registration).where(
            Registration.user_id == student.id,
            Registration.course_id == course.id,
        )
    )
    if not reg:
        reg = Registration(
            user_id=student.id,
            course_id=course.id,
            status=RegistrationStatus.CONFIRMED,
        )
        db.add(reg)
        db.flush()
    elif reg.status != RegistrationStatus.CONFIRMED:
        reg.status = RegistrationStatus.CONFIRMED

    lesson_ids = db.scalars(
        select(Lesson.id).join(Section, Section.id == Lesson.section_id).where(Section.course_id == course.id)
    ).all()
    if lesson_ids:
        db.execute(delete(Progress).where(Progress.user_id == student.id, Progress.lesson_id.in_(lesson_ids)))
    db.execute(delete(Certificate).where(Certificate.user_id == student.id, Certificate.course_id == course.id))

    return {
        "altwalker_course_id": course.id,
        "altwalker_course_slug": course.slug,
        "lessons_seeded": len(lesson_ids),
        "student_registration": "CONFIRMED",
        "progress_reset": True,
    }


def ensure_mbt_pending_registration_sample(db: Session) -> dict:
    """Dedicated student with PENDING registration on a demo course (admin approval MBT)."""
    u = _get_or_create_test_user(
        db,
        email="mbt-pending-student@test.com",
        password="Password123!",
        name="MBT Pending Student",
        role=UserRole.STUDENT,
        student_major="QA",
        learning_goal="Admin approval tests",
    )
    course = db.scalar(select(Course).where(Course.slug == "demo-course-03"))
    if not course:
        return {"mbt_pending": "skipped_no_demo_course_03"}

    reg = db.scalar(
        select(Registration).where(Registration.user_id == u.id, Registration.course_id == course.id)
    )
    if not reg:
        reg = Registration(user_id=u.id, course_id=course.id, status=RegistrationStatus.PENDING)
        db.add(reg)
    else:
        reg.status = RegistrationStatus.PENDING
    db.flush()
    return {"mbt_pending_registration_id": reg.id, "course_id": course.id}
