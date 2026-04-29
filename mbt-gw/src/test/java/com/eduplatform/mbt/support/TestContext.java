package com.eduplatform.mbt.support;

/**
 * Context MBT: trạng thái theo module (không gồm auth — xem {@link #getAuthState()}).
 * Auth được tách {@link AuthState} để impl chỉ đọc/ghi một nơi và log rõ ràng.
 */
public class TestContext {

    private static final ThreadLocal<AuthState> AUTH = ThreadLocal.withInitial(AuthState::new);

    public static AuthState getAuthState() {
        return AUTH.get();
    }

    /** Gọi đầu mỗi lần chạy GraphWalker (hoặc test) nếu cần tránh “rác” từ lần trước cùng thread. */
    public static void resetAuthStateForThread() {
        AUTH.set(new AuthState());
    }

    /** Last GraphWalker element name executed ({@code v_*} or {@code e_*}), for impl diagnostics. */
    public String mbtCurrentModelElement = "";
    /** Last traversed vertex name ({@code v_*}), approximates SPA “page”. */
    public String mbtCurrentPage = "";
    /** Last traversed edge name ({@code e_*}). */
    public String mbtLastEdge = "";

    // Course exploration (GraphWalker CourseExplorationModel)
    public boolean coursesLoaded;
    public int totalCourses;
    public int filteredCount;
    public String explorationSearchQuery = "";
    public String explorationCategory = "all";
    public String explorationLevel = "all";
    public boolean registrationOpenFuture;
    public boolean registrationClosePast;

    public long selectedCourseId;
    public String selectedCourseStatus = "PUBLISHED";
    public boolean selectedCourseOpen = true;

    // Registration (GraphWalker RegistrationModel)
    public boolean hasRemainingSlotOnTargetCourse = true;
    /**
     * Khớp {@code CourseDetailPage}: nút "Đăng Ký Ngay" tắt khi ngoài khoảng
     * {@code registration_open_at} / {@code registration_close_at} dù còn slot.
     */
    public boolean registrationWindowOpenOnTargetCourse = true;
    public boolean regPipelineHasPending = false;

    // Registration
    public long myRegistrationId;
    public String myRegistrationStatus = "NONE";
    public boolean hasActiveRegistration;
    public int maxCapacity = 2;
    public int confirmedCount;
    public int pendingCount;
    public String userStatus = "ACTIVE";
    public boolean targetIsOwnRegistration = true;

    // Learning
    public String registrationStatus = "NONE";
    public int totalLessons;
    public int completedLessons;
    public int completionPct;
    public String currentLessonType = "VIDEO";
    public boolean currentLessonIsPreview;
    public int currentLessonCompletionPct = 100;

    // Certificate
    public boolean certificateEnabled = true;
    public boolean allLessonsCompleted;
    public boolean hasExistingCertificate;
    /** Đồng bộ guard GraphWalker (tách khỏi verificationCode rỗng sau issue). */
    public boolean hasVerificationCode;
    public String verificationCode = "";
    public boolean courseHasLessons = true;

    // Instructor content
    public boolean titleValid = true;
    public boolean descriptionValid = true;
    public boolean slugValid = true;
    public boolean categoryValid = true;
    public boolean imageUrlValid = true;
    public boolean maxCapacityValid = true;
    public boolean estimatedHoursValid = true;
    public boolean sectionTitleValid = true;
    public boolean sectionPositionValid = true;
    public boolean lessonTitleValid = true;
    public boolean lessonPositionValid = true;
    public boolean lessonDurationValid = true;
    public boolean lessonContentUrlValid = true;
    public long selectedSectionId;
    public String courseStatus = "DRAFT";
    public boolean isOwnerOfCourse = true;
    public boolean ownedCourseExists;
    public boolean ownedDraftExists;
    public boolean ownedPublishedExists;
    public boolean foreignCourseExists;
    public boolean selectedSectionExists;

    // Admin dashboard MBT model (GraphWalker AdminManagementModel)
    public boolean adminHasPending;
    public boolean adminHasApprovablePending;
    public String lastAdminAction = "INIT";

    // Admin
    public boolean targetUserExists = true;
    public long targetUserId;
    public String targetUserStatus = "ACTIVE";

    public void reset() {
        resetAuthStateForThread();
    }
}
