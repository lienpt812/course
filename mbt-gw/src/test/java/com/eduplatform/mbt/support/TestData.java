package com.eduplatform.mbt.support;

/**
 * Canonical test accounts + credentials used by every module.
 * Created on demand by {@link TestDataSeeder}.
 */
public final class TestData {

    private TestData() {}

    // ---- Passwords respecting RegisterPage regex ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$
    public static final String PWD_STRONG = "Passw0rd!";
    public static final String PWD_WEAK   = "weakpass";

    // ---- Default seeded accounts. Create-if-missing in TestDataSeeder.
    public static final String STUDENT_EMAIL    = "mbt.student@example.com";
    public static final String STUDENT_NAME     = "MBT Student";
    public static final String INSTRUCTOR_EMAIL = "mbt.instructor@example.com";
    public static final String INSTRUCTOR_NAME  = "MBT Instructor";
    public static final String ADMIN_EMAIL      = "mbt.admin@example.com";
    public static final String ADMIN_NAME       = "MBT Admin";

    // ---- Invalid inputs for negative edges
    public static final String UNKNOWN_EMAIL    = "mbt.unknown@example.com";
    /** Fails {@code <input type="email">} constraint validation in browsers. */
    public static final String INVALID_EMAIL    = "not-an-email";
    /** Same intent as {@link #INVALID_EMAIL}; use when a distinct invalid value is needed. */
    public static final String INVALID_EMAIL_2  = "bad@@domain";
    public static final String WRONG_PWD        = "NotTheRealPwd1!";
    /**
     * Fails FE password regex (no uppercase) while meeting HTML5 min length — rejection is via React
     * {@code setError}, not HTML5.
     */
    public static final String PWD_FAILS_STRENGTH_REACT_ONLY = "alllower1";

    // ---- Registration sample fields
    public static final String STUDENT_MAJOR    = "Công nghệ thông tin";
    public static final String LEARNING_GOAL    = "Học backend Python";
    public static final String EXPERTISE        = "Backend Python & Testing";

    /**
     * Stable course slugs — must match {@code app/services/seed_service.py} (MBT GraphWalker fixtures).
     */
    public static final String MBT_STUDENT_ANCHOR_SLUG   = "mbt-gw-student-anchor";
    public static final String MBT_INSTRUCTOR_DRAFT_SLUG = "mbt-gw-instructor-draft";
    /** Demo catalog (ensure_demo_courses): predictable published course for exploration/registration. */
    public static final String DEMO_COURSE_01_SLUG = "demo-course-01";
    /** Used by {@code ensure_mbt_pending_registration_sample} (admin pending approval). */
    public static final String DEMO_COURSE_03_SLUG = "demo-course-03";
    /** Lớp đầy + 1 PENDING không duyệt thêm được — seed {@code ensure_mbt_class_full_pending_scenario}. */
    public static final String MBT_CLASS_FULL_PENDING_SLUG = "mbt-gw-class-full-pending";

    /** Seeded pending student (not the MBT primary student). */
    public static final String MBT_PENDING_STUDENT_EMAIL = "mbt-pending-student@test.com";
    /**
     * Học viên mẫu thứ hai (khớp {@code ensure_mbt_pending_registration_sample} trên BE) khi
     * tài khoản đầu đã đăng ký/duyệt hết mọi khóa còn dùng được.
     */
    public static final String MBT_PENDING_STUDENT_2_EMAIL = "mbt-pending-student-2@test.com";
}
