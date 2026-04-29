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


# --- Java GraphWalker MBT (test-web) — must match TestData.java emails -----------------
MBT_STUDENT_EMAIL = "mbt.student@example.com"
MBT_INSTRUCTOR_EMAIL = "mbt.instructor@example.com"
MBT_GW_STUDENT_SLUG = "mbt-gw-student-anchor"
MBT_GW_INSTRUCTOR_DRAFT_SLUG = "mbt-gw-instructor-draft"


def ensure_mbt_graphwalker_fixtures(db: Session) -> dict:
    """
    Stable anchor data for Selenide MBT tests: mbt.student has one published course with
    lessons, CONFIRMED registration, 100% completion, certificate_enabled, and no Certificate
    row (so issue/view flows work). mbt.instructor owns a DRAFT course with outline data.

    Call after ensure_demo_courses so demo-course-03 exists for mbt_pending sample.
    Requires users created first (Java TestDataSeeder calls create-account before POST /seed).
    """
    student = db.scalar(select(User).where(User.email == MBT_STUDENT_EMAIL))
    instructor = db.scalar(select(User).where(User.email == MBT_INSTRUCTOR_EMAIL))
    if not student or not instructor:
        return {
            "skipped": True,
            "reason": "mbt_accounts_missing",
            "detail": {
                "student_found": bool(student),
                "instructor_found": bool(instructor),
            },
        }

    student_course = _ensure_mbt_student_anchor_course(db, student.id, instructor.id)
    draft_course = _ensure_mbt_instructor_draft_course(db, instructor.id)
    return {
        "skipped": False,
        "mbt_student_anchor": student_course,
        "mbt_instructor_draft": draft_course,
    }


def _ensure_mbt_student_anchor_course(db: Session, student_id: int, instructor_id: int) -> dict:
    course = db.scalar(select(Course).where(Course.slug == MBT_GW_STUDENT_SLUG))
    if not course:
        course = Course(
            title="MBT — Student anchor (GraphWalker)",
            slug=MBT_GW_STUDENT_SLUG,
            description="Seeded for Java MBT: confirmed reg, lessons, 100% progress, cert enabled.",
            image_url="https://picsum.photos/id/119/1200/720",
            instructor_id=instructor_id,
            category="Testing",
            max_capacity=80,
            estimated_hours=6,
            level="Beginner",
            status=CourseStatus.PUBLISHED,
            price=0,
            certificate_enabled=True,
        )
        db.add(course)
        db.flush()
    else:
        course.instructor_id = instructor_id
        course.status = CourseStatus.PUBLISHED
        course.certificate_enabled = True

    section = db.scalar(
        select(Section).where(Section.course_id == course.id).order_by(Section.position.asc()).limit(1)
    )
    if not section:
        section = Section(course_id=course.id, title="MBT GW Section", position=1)
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
                title=f"MBT GW Lesson {next_pos}",
                type=LessonType.TEXT,
                position=next_pos,
                duration_minutes=10,
                is_preview=(next_pos == 1),
            )
        )
        db.flush()

    lesson_ids = db.scalars(
        select(Lesson.id).join(Section, Section.id == Lesson.section_id).where(Section.course_id == course.id)
    ).all()

    reg = db.scalar(
        select(Registration).where(
            Registration.user_id == student_id,
            Registration.course_id == course.id,
        )
    )
    if not reg:
        reg = Registration(
            user_id=student_id,
            course_id=course.id,
            status=RegistrationStatus.CONFIRMED,
        )
        db.add(reg)
        db.flush()
    elif reg.status != RegistrationStatus.CONFIRMED:
        reg.status = RegistrationStatus.CONFIRMED

    if lesson_ids:
        db.execute(delete(Progress).where(Progress.user_id == student_id, Progress.lesson_id.in_(lesson_ids)))
        for lid in lesson_ids:
            db.add(
                Progress(
                    user_id=student_id,
                    lesson_id=lid,
                    completed=True,
                    completion_pct=100,
                )
            )
        db.flush()

    db.execute(delete(Certificate).where(Certificate.user_id == student_id, Certificate.course_id == course.id))

    return {
        "course_id": course.id,
        "slug": course.slug,
        "lessons": len(lesson_ids),
        "registration": "CONFIRMED",
        "completion": "100%",
        "certificate_cleared": True,
    }


def _ensure_mbt_instructor_draft_course(db: Session, instructor_id: int) -> dict:
    course = db.scalar(select(Course).where(Course.slug == MBT_GW_INSTRUCTOR_DRAFT_SLUG))
    if not course:
        course = Course(
            title="MBT — Instructor draft (GraphWalker)",
            slug=MBT_GW_INSTRUCTOR_DRAFT_SLUG,
            description="Owned by mbt.instructor for dashboard / outline MBT paths.",
            image_url="https://picsum.photos/id/52/1200/720",
            instructor_id=instructor_id,
            category="Testing",
            max_capacity=40,
            estimated_hours=10,
            level="Beginner",
            status=CourseStatus.DRAFT,
            price=0,
            certificate_enabled=False,
        )
        db.add(course)
        db.flush()
    else:
        course.instructor_id = instructor_id
        course.status = CourseStatus.DRAFT

    section = db.scalar(
        select(Section).where(Section.course_id == course.id).order_by(Section.position.asc()).limit(1)
    )
    if not section:
        section = Section(course_id=course.id, title="MBT Draft Section", position=1)
        db.add(section)
        db.flush()

    if (
        int(db.scalar(select(func.count(Lesson.id)).where(Lesson.section_id == section.id)) or 0) < 1
    ):
        db.add(
            Lesson(
                section_id=section.id,
                title="MBT Draft Lesson 1",
                type=LessonType.TEXT,
                position=1,
                duration_minutes=5,
                is_preview=False,
            )
        )
        db.flush()

    return {"course_id": course.id, "slug": course.slug, "status": course.status.value}


def _ensure_user_pending_on_course(db: Session, user: User, course: Course) -> Registration:
    """Một (user, course) — đưa về PENDING để admin MBT luôn thấy nút Duyệt/Từ chối."""
    reg = db.scalar(
        select(Registration).where(
            Registration.user_id == user.id,
            Registration.course_id == course.id,
        )
    )
    if not reg:
        reg = Registration(
            user_id=user.id,
            course_id=course.id,
            status=RegistrationStatus.PENDING,
        )
        db.add(reg)
    else:
        reg.status = RegistrationStatus.PENDING
    db.flush()
    return reg


def ensure_mbt_pending_registration_sample(db: Session) -> dict:
    """
    Hai đơn PENDING (hai học viên + hai khóa khi có đủ demo) để GraphWalker approve/bulk
    không làm cạn mẫu sau một bước. Nếu thiếu demo-course-03, dùng bất kỳ khóa PUBLISHED nào.
    """
    u1 = _get_or_create_test_user(
        db,
        email="mbt-pending-student@test.com",
        password="Passw0rd!",
        name="MBT Pending Student",
        role=UserRole.STUDENT,
        student_major="QA",
        learning_goal="Admin approval tests",
    )
    course = db.scalar(select(Course).where(Course.slug == "demo-course-03"))
    if not course:
        course = db.scalar(
            select(Course)
            .where(Course.status == CourseStatus.PUBLISHED)
            .order_by(Course.id.asc())
            .limit(1)
        )
    if not course:
        return {"mbt_pending": "skipped_no_published_course"}

    reg1 = _ensure_user_pending_on_course(db, u1, course)

    u2 = _get_or_create_test_user(
        db,
        email="mbt-pending-student-2@test.com",
        password="Passw0rd!",
        name="MBT Pending Student 2",
        role=UserRole.STUDENT,
        student_major="QA",
        learning_goal="Admin approval tests",
    )
    course2 = db.scalar(select(Course).where(Course.slug == "demo-course-04"))
    if not course2 or course2.id == course.id:
        course2 = db.scalar(
            select(Course)
            .where(
                Course.status == CourseStatus.PUBLISHED,
                Course.id != course.id,
            )
            .order_by(Course.id.asc())
            .limit(1)
        )

    out: dict = {
        "mbt_pending_registration_id": reg1.id,
        "course_id": course.id,
        "course_slug": course.slug,
    }
    if course2:
        reg2 = _ensure_user_pending_on_course(db, u2, course2)
        out["mbt_pending_registration_2_id"] = reg2.id
        out["course_2_id"] = course2.id
        out["course_2_slug"] = course2.slug
    return out


MBT_CLASS_FULL_PENDING_SLUG = "mbt-gw-class-full-pending"


def ensure_mbt_class_full_pending_scenario(db: Session) -> dict:
    """
    Seed a 'class full + PENDING' scenario cho AdminManagement MBT:
      - Một khóa capacity=1 đã bị lấp đầy bởi mbt.student (CONFIRMED).
      - mbt-pending-student có một đơn PENDING trên khóa đó.
    GraphWalker v_VerifyClassFullForApproval cần: PENDING > 0 && remaining_slots == 0.
    Idempotent — gọi nhiều lần không tạo bản ghi thừa.
    """
    student = db.scalar(select(User).where(User.email == MBT_STUDENT_EMAIL))
    instructor = db.scalar(select(User).where(User.email == MBT_INSTRUCTOR_EMAIL))
    if not student or not instructor:
        return {
            "skipped": True,
            "reason": "mbt_accounts_missing",
            "detail": {
                "student_found": bool(student),
                "instructor_found": bool(instructor),
            },
        }

    pending_student = _get_or_create_test_user(
        db,
        email="mbt-pending-student@test.com",
        password="Passw0rd!",
        name="MBT Pending Student",
        role=UserRole.STUDENT,
        student_major="QA",
        learning_goal="Admin approval tests",
    )

    # Create / update the class-full course (max_capacity=1)
    course = db.scalar(select(Course).where(Course.slug == MBT_CLASS_FULL_PENDING_SLUG))
    if not course:
        course = Course(
            title="MBT — Class Full (GraphWalker)",
            slug=MBT_CLASS_FULL_PENDING_SLUG,
            description="Seeded for admin MBT: capacity=1, fully booked with 1 PENDING.",
            image_url="https://picsum.photos/id/3/1200/720",
            instructor_id=instructor.id,
            category="Testing",
            max_capacity=1,
            estimated_hours=2,
            level="Beginner",
            status=CourseStatus.PUBLISHED,
            price=0,
            certificate_enabled=False,
        )
        db.add(course)
        db.flush()
    else:
        course.max_capacity = 1
        course.status = CourseStatus.PUBLISHED

    # Ensure mbt.student has a CONFIRMED registration (fills the 1 slot)
    confirmed_reg = db.scalar(
        select(Registration).where(
            Registration.user_id == student.id,
            Registration.course_id == course.id,
        )
    )
    if not confirmed_reg:
        confirmed_reg = Registration(
            user_id=student.id,
            course_id=course.id,
            status=RegistrationStatus.CONFIRMED,
        )
        db.add(confirmed_reg)
    else:
        confirmed_reg.status = RegistrationStatus.CONFIRMED
    db.flush()

    # Ensure pending_student has a PENDING registration (cannot be approved — slot full)
    pending_reg = db.scalar(
        select(Registration).where(
            Registration.user_id == pending_student.id,
            Registration.course_id == course.id,
        )
    )
    if not pending_reg:
        pending_reg = Registration(
            user_id=pending_student.id,
            course_id=course.id,
            status=RegistrationStatus.PENDING,
        )
        db.add(pending_reg)
    else:
        pending_reg.status = RegistrationStatus.PENDING
    db.flush()

    return {
        "course_id": course.id,
        "course_slug": course.slug,
        "max_capacity": course.max_capacity,
        "confirmed_registration_id": confirmed_reg.id,
        "pending_registration_id": pending_reg.id,
    }