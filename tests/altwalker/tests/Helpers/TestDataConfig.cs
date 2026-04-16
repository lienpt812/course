namespace CourseRegistration.Tests.Helpers;

/// <summary>
/// Centralized test data configuration.
/// All test accounts, courses, and expected states are defined here.
/// ApiHelper reads from this config to seed data before tests run.
/// </summary>
public static class TestDataConfig
{
    // ── Base URL ─────────────────────────────────────────────────────────────
    public const string ApiBase  = "http://localhost:8000/api/v1";
    public const string AppBase  = "http://localhost:3000";

    // ── Test Accounts ─────────────────────────────────────────────────────────
    public static class Accounts
    {
        public static readonly TestUser Student = new(
            Name:     "Test Student",
            Email:    "student@test.com",
            Password: "Password123!",
            Role:     "STUDENT",
            Extra:    new { student_major = "Computer Science", learning_goal = "Learn programming" }
        );

        public static readonly TestUser Instructor = new(
            Name:     "Test Instructor",
            Email:    "instructor@test.com",
            Password: "Password123!",
            Role:     "INSTRUCTOR",
            Extra:    new { expertise = "Software Engineering", education = "Master" }
        );

        public static readonly TestUser Admin = new(
            Name:     "Test Admin",
            Email:    "admin@test.com",
            Password: "Password123!",
            Role:     "ADMIN",
            Extra:    null
        );

        public static IEnumerable<TestUser> All => [Student, Instructor, Admin];
    }

    // ── Test Courses ──────────────────────────────────────────────────────────
    public static class Courses
    {
        /// Draft course - instructor can edit, students cannot register
        public static readonly TestCourse Draft = new(
            Title:       "Test Draft Course",
            Slug:        "test-draft-course",
            Description: "A draft course for testing",
            Level:       "Beginner",
            Category:    "Backend",
            MaxCapacity: 10,
            Price:       0,
            Status:      "DRAFT",
            CertEnabled: true
        );

        /// Published course - students can register
        public static readonly TestCourse Published = new(
            Title:       "Test Published Course",
            Slug:        "test-published-course",
            Description: "A published course for testing registration",
            Level:       "Beginner",
            Category:    "Backend",
            MaxCapacity: 30,
            Price:       0,
            Status:      "PUBLISHED",
            CertEnabled: true
        );

        /// Full course - max 2 slots, used for waitlist testing
        public static readonly TestCourse Full = new(
            Title:       "Test Full Course",
            Slug:        "test-full-course",
            Description: "A course with limited capacity for waitlist testing",
            Level:       "Intermediate",
            Category:    "Frontend",
            MaxCapacity: 2,
            Price:       0,
            Status:      "PUBLISHED",
            CertEnabled: false
        );

        public static IEnumerable<TestCourse> All => [Draft, Published, Full];
    }

    // ── Test Sections & Lessons ───────────────────────────────────────────────
    public static class Content
    {
        public static readonly TestSection Section1 = new(
            Title:    "Section 1: Introduction",
            Position: 1
        );

        public static readonly TestSection Section2 = new(
            Title:    "Section 2: Advanced Topics",
            Position: 2
        );

        public static readonly TestLesson Lesson1 = new(
            Title:           "Lesson 1: Getting Started",
            Type:            "VIDEO",
            Position:        1,
            DurationMinutes: 10,
            IsPreview:       true
        );

        public static readonly TestLesson Lesson2 = new(
            Title:           "Lesson 2: Core Concepts",
            Type:            "TEXT",
            Position:        2,
            DurationMinutes: 15,
            IsPreview:       false
        );
    }

    // ── Expected State Descriptions ───────────────────────────────────────────
    public static class States
    {
        /// State needed for Admin_Approval_Management tests
        public const string AdminApproval = "admin_with_pending_registrations";

        /// State needed for Learning/Certificate tests
        public const string StudentLearning = "student_confirmed_in_course";

        /// State needed for Waitlist tests
        public const string WaitlistFull = "course_full_with_waitlist";

        /// State needed for Instructor tests
        public const string InstructorWithCourse = "instructor_has_draft_course";
    }
}

// ── Value types ───────────────────────────────────────────────────────────────

public record TestUser(
    string Name,
    string Email,
    string Password,
    string Role,
    object? Extra
);

public record TestCourse(
    string Title,
    string Slug,
    string Description,
    string Level,
    string Category,
    int    MaxCapacity,
    decimal Price,
    string Status,
    bool   CertEnabled
);

public record TestSection(
    string Title,
    int    Position
);

public record TestLesson(
    string Title,
    string Type,
    int    Position,
    int    DurationMinutes,
    bool   IsPreview
);
