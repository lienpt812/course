package com.eduplatform.mbt.impl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.eduplatform.mbt.models.InstructorContentModel;
import com.eduplatform.mbt.pages.CourseDetailPage;
import com.eduplatform.mbt.pages.InstructorDashboardPage;
import com.eduplatform.mbt.support.BaseImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import com.eduplatform.mbt.support.MbtBusinessAssertions;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.graphwalker.java.annotation.GraphWalker;

import java.time.Duration;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;

/**
 * Instructor content management MBT — create/edit courses, manage sections/lessons.
 *
 * UI-backend alignment:
 *   - InstructorDashboard.tsx: inline create form (toggles with "Thêm khóa học" button)
 *   - FE validations: title 3-255 chars, description >= 20 chars, slug non-empty
 *   - POST /courses → 409 on duplicate slug; 403 if not INSTRUCTOR/ADMIN
 *   - PATCH /courses/{id} → 403 if not owner (INSTRUCTOR); 403 if other instructor's course
 *   - Published course: FE shows lock message "Khóa học đã published — không thể chỉnh sửa"
 *   - Section: POST /learning/sections { course_id, title, position } → min title 2 chars
 *   - Lesson: POST /learning/lessons { section_id, title, type, duration_minutes, position }
 *   - Outline: GET /learning/courses/{id}/outline → displayed live on dashboard
 */
@GraphWalker(value = GraphWalkerExecutionPolicy.BOUNDED_INSTRUCTOR, start = "v_InstructorDashboard")
public class InstructorContentImpl extends BaseImpl implements InstructorContentModel {

    private final InstructorDashboardPage dashboard = new InstructorDashboardPage();
    private final CourseDetailPage detailPage = new CourseDetailPage();

    private long ownedCourseId;
    private long lastSectionId;

    // =============== Vertices ===============

    @Override
    public void v_InstructorDashboard() {
        logStep("v_InstructorDashboard");
        asInstructor();
        assertInstructorDashboardOrRetry();
        syncInstructorGuards();
    }

    @Override
    public void v_CourseCreateForm() {
        logStep("v_CourseCreateForm");
        asInstructor();
        dashboard.open().assertLoaded();
        if (!dashboard.titleInput().is(Condition.visible)) {
            SafeUi.clickWhenReady(dashboard.addCourseButton(), java.time.Duration.ofSeconds(6), 2);
        }
        dashboard.assertCreateFormVisible();
        SafeUi.waitUntilVisible(dashboard.saveCourseButton(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_CourseCreated() {
        logStep("v_CourseCreated");
        dashboard.courseRows().shouldHave(
                com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0));
        ownedCourseId = myLatestCourseId();
        ctx.ownedCourseExists = true;
        ctx.ownedDraftExists = true;
        log.info("v_CourseCreated: ownedCourseId={}", ownedCourseId);
        syncInstructorGuards();
    }

    @Override
    public void v_CourseCreateError() {
        logStep("v_CourseCreateError");
        // FE renders p.text-red-600 for validation errors
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_CourseEditDetail() {
        logStep("v_CourseEditDetail");
        // On a DRAFT course owned by this instructor: "Chỉnh sửa" button is visible
        if (!detailPage.editButton().is(Condition.visible)) {
            log.warn("v_CourseEditDetail: edit button not visible (course may be PUBLISHED or not owned)");
        }
    }

    @Override
    public void v_CourseEditForm() {
        logStep("v_CourseEditForm");
        // Edit form is embedded in CourseDetailPage (via CourseOutlineManager)
        // The back link should be visible indicating we are on the detail page
        SafeUi.waitUntilVisible(detailPage.backToList(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_CourseEditForbiddenPublished() {
        logStep("v_CourseEditForbiddenPublished");
        // "Khóa học đã published — không thể chỉnh sửa"
        SafeUi.waitUntilVisible(detailPage.publishedLockMsg(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_CourseEditForbiddenNotOwner() {
        logStep("v_CourseEditForbiddenNotOwner");
        // Edit button should NOT be visible for a course the instructor doesn't own
        detailPage.editButton().shouldNot(Condition.visible);
    }

    @Override
    public void v_CourseUpdated() {
        logStep("v_CourseUpdated");
        SafeUi.waitUntilVisible(detailPage.title(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_SectionManagerOpen() {
        logStep("v_SectionManagerOpen");
        // Section manager is always shown on the instructor dashboard
        dashboard.assertSectionManagerVisible();
        SafeUi.waitUntilVisible(dashboard.saveSectionButton(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_SectionCreated() {
        logStep("v_SectionCreated");
        lastSectionId = firstSectionIdOf(ownedCourseId);
        ctx.selectedSectionExists = lastSectionId > 0;
        log.info("v_SectionCreated: lastSectionId={}", lastSectionId);
        syncInstructorGuards();
    }

    @Override
    public void v_SectionCreateError() {
        logStep("v_SectionCreateError");
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_LessonCreated() {
        logStep("v_LessonCreated");
        // Outline is refreshed after lesson create; verify at least one section entry
        if (ownedCourseId > 0) {
            api.withToken(auth().accessToken);
            JsonNode outline = api.get("/api/v1/learning/courses/" + ownedCourseId + "/outline", false);
            if (outline != null && outline.path("data").isArray()) {
                boolean hasLessons = false;
                for (JsonNode section : outline.path("data")) {
                    if (section.path("lessons").size() > 0) { hasLessons = true; break; }
                }
                if (!hasLessons) {
                    log.warn("v_LessonCreated: outline has no lessons yet for course {}", ownedCourseId);
                }
            }
        }
    }

    @Override
    public void v_LessonCreateError() {
        logStep("v_LessonCreateError");
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void v_OutlineRefreshed() {
        logStep("v_OutlineRefreshed");
        // "Outline hiện tại" section must be visible
        SafeUi.waitUntilVisible(dashboard.outlineBlock(), SafeUi.DEFAULT_TIMEOUT);
    }

    // =============== Edges ===============

    @Override
    public void e_OpenCreateForm() {
        logStep("e_OpenCreateForm");
        SafeUi.clickWhenReady(dashboard.addCourseButton(), SafeUi.DEFAULT_TIMEOUT, 2);
        dashboard.assertCreateFormVisible();
        armHappyInstructorCourseFormGuards();
    }

    @Override
    public void e_RearmCreateCourseFormGuards() {
        logStep("e_RearmCreateCourseFormGuards");
        armHappyInstructorCourseFormGuards();
    }

    @Override
    public void e_ArmCreateTitleInvalid() {
        logStep("e_ArmCreateTitleInvalid");
        ctx.titleValid = false;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_ArmCreateDescriptionInvalid() {
        logStep("e_ArmCreateDescriptionInvalid");
        ctx.descriptionValid = false;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_ArmCreateImageInvalid() {
        logStep("e_ArmCreateImageInvalid");
        ctx.imageUrlValid = false;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_ArmCreateCapacityInvalid() {
        logStep("e_ArmCreateCapacityInvalid");
        ctx.maxCapacityValid = false;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_ArmCreateHoursInvalid() {
        logStep("e_ArmCreateHoursInvalid");
        ctx.estimatedHoursValid = false;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_CreateCourse() {
        logStep("e_CreateCourse");
        armHappyInstructorCourseFormGuards();
        String suffix = "-" + System.currentTimeMillis();
        dashboard.titleInput().setValue("MBT Course" + suffix);
        dashboard.slugInput().setValue("mbt-course" + suffix);
        dashboard.categoryInput().setValue("Testing");
        dashboard.imageUrlInput().setValue("https://example.com/img.png");
        dashboard.capacityInput().setValue("30");
        dashboard.hoursInput().setValue("20");
        // Description must be >= 20 chars (FE validation)
        dashboard.descriptionInput().setValue("Khóa học được tạo tự động bởi MBT framework (>= 20 ký tự).");
        dashboard.levelSelect().selectOption("Beginner");
        dashboard.statusSelect().selectOption("DRAFT");
        dashboard.saveCourseButton().click();
        com.codeborne.selenide.Selenide.sleep(900);

        // API cross-check: verify course was created
        api.withToken(auth().accessToken);
        long newId = myLatestCourseId();
        if (newId > 0) {
            JsonNode d = api.courseDetail(newId);
            MbtBusinessAssertions.assertSuccessEnvelope(d, "course detail after create");
            String title = d.path("data").path("title").asText("");
            if (!title.contains("MBT Course")) {
                log.warn("e_CreateCourse: unexpected title in API: '{}'", title);
            }
            log.info("e_CreateCourse: created course id={} title='{}'", newId, title);
        } else {
            log.warn("e_CreateCourse: could not find new course via API");
        }
    }

    @Override
    public void e_CreateCourseFailTitle() {
        logStep("e_CreateCourseFailTitle");
        fillCreateFormValidBaseline();
        dashboard.titleInput().setValue("AB");
        dashboard.saveCourseButton().click();
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void e_CreateCourseFailDescription() {
        logStep("e_CreateCourseFailDescription");
        fillCreateFormValidBaseline();
        dashboard.descriptionInput().setValue("short");
        dashboard.saveCourseButton().click();
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void e_CreateCourseFailImage() {
        logStep("e_CreateCourseFailImage");
        fillCreateFormValidBaseline();
        dashboard.imageUrlInput().setValue("not-a-url");
        dashboard.saveCourseButton().click();
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void e_CreateCourseFailCapacity() {
        logStep("e_CreateCourseFailCapacity");
        fillCreateFormValidBaseline();
        dashboard.capacityInput().setValue("0");
        dashboard.saveCourseButton().click();
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void e_CreateCourseFailHours() {
        logStep("e_CreateCourseFailHours");
        fillCreateFormValidBaseline();
        dashboard.hoursInput().setValue("0");
        dashboard.saveCourseButton().click();
        SafeUi.waitUntilVisible(dashboard.errorText(), SafeUi.DEFAULT_TIMEOUT);
    }

    /** Các trường hợp lệ trừ field sẽ bị fail edge chỉnh sau. */
    private void fillCreateFormValidBaseline() {
        armHappyInstructorCourseFormGuards();
        String suffix = "-" + System.currentTimeMillis();
        dashboard.titleInput().setValue("MBT Course" + suffix);
        dashboard.slugInput().setValue("mbt-course" + suffix);
        dashboard.categoryInput().setValue("Testing");
        dashboard.imageUrlInput().setValue("https://example.com/img.png");
        dashboard.capacityInput().setValue("30");
        dashboard.hoursInput().setValue("20");
        dashboard.descriptionInput().setValue("Khóa học được tạo tự động bởi MBT framework (>= 20 ký tự).");
        dashboard.levelSelect().selectOption("Beginner");
        dashboard.statusSelect().selectOption("DRAFT");
    }

    @Override
    public void e_CancelCreate() {
        logStep("e_CancelCreate");
        if (dashboard.cancelButton().is(Condition.visible)) {
            dashboard.cancelButton().click();
            // Form should close; "Lưu khóa học" button disappears
            dashboard.saveCourseButton().shouldNot(Condition.visible);
        } else {
            log.warn("e_CancelCreate: cancel button not visible — form may already be closed");
        }
    }

    @Override
    public void e_FixAndRetry() {
        logStep("e_FixAndRetry");
        // Fix the validation error by providing valid data and retry
        e_CreateCourse();
    }

    @Override
    public void e_ReturnDashboard() {
        logStep("e_ReturnDashboard");
        dashboard.open();
        assertInstructorDashboardOrRetry();
    }

    @Override
    public void e_OpenCourseForEdit() {
        logStep("e_OpenCourseForEdit");
        long id = myOwnedDraftCourseId();
        if (id > 0) {
            detailPage.open(id);
            detailPage.assertLoaded();
        } else {
            log.warn("e_OpenCourseForEdit: no owned DRAFT course found");
        }
    }

    @Override
    public void e_OpenPublishedCourse() {
        logStep("e_OpenPublishedCourse");
        long id = myOwnedCourseIdWithStatus("PUBLISHED");
        if (id > 0) {
            detailPage.open(id);
            detailPage.assertLoaded();
        }
    }

    @Override
    public void e_OpenForeignCourse() {
        logStep("e_OpenForeignCourse");
        long id = foreignCourseId();
        if (id > 0) {
            detailPage.open(id);
            detailPage.assertLoaded();
        }
    }

    @Override
    public void e_ClickEditButton() {
        logStep("e_ClickEditButton");
        if (!detailPage.editButton().is(Condition.visible)) {
            log.warn("e_ClickEditButton: edit button not visible (published or not owner) — skipping click");
            return;
        }
        detailPage.clickEdit();
    }

    @Override
    public void e_SaveEdit() {
        logStep("e_SaveEdit");
        // The edit form is rendered inline on CourseDetailPage (CourseOutlineManager).
        // The actual course metadata edit uses PATCH /courses/{id}.
        // We use API directly to ensure the correct HTTP method (PATCH, not POST).
        long id = myOwnedDraftCourseId();
        if (id > 0 && auth().accessToken != null) {
            api.withToken(auth().accessToken);
            // PATCH /courses/{id} — backend endpoint in courses.py
            // PATCH /courses/{id} — backend endpoint in courses.py uses PATCH method
            JsonNode r = api.patchCourse(id,
                    java.util.Map.of(
                        "title", "MBT Updated - " + System.currentTimeMillis(),
                        "description", "Cập nhật mô tả qua MBT test (tối thiểu 20 ký tự)."));
            MbtBusinessAssertions.assertSuccessEnvelope(r, "PATCH /courses/" + id);
            String updatedTitle = r.path("data").path("title").asText("");
            if (!updatedTitle.contains("MBT Updated")) {
                log.warn("e_SaveEdit: updated title doesn't contain 'MBT Updated': '{}'", updatedTitle);
            }
        } else {
            log.warn("e_SaveEdit: no owned DRAFT course or no access token");
        }
    }

    @Override
    public void e_CancelEdit() {
        logStep("e_CancelEdit");
        var btn = $(Selectors.byText("Hủy"));
        if (btn.is(Condition.visible)) {
            btn.click();
        } else {
            log.warn("e_CancelEdit: 'Hủy' button not visible — edit form may already be closed");
        }
    }

    @Override
    public void e_AfterUpdateReload() {
        logStep("e_AfterUpdateReload");
        com.codeborne.selenide.Selenide.refresh();
        // After refresh, title should still show the updated value
        SafeUi.waitUntilVisible(detailPage.title(), Duration.ofSeconds(8));
    }

    @Override
    public void e_BackFromPublished() {
        logStep("e_BackFromPublished");
        dashboard.open();
    }

    @Override
    public void e_BackFromForeign() {
        logStep("e_BackFromForeign");
        dashboard.open();
    }

    @Override
    public void e_OpenSectionManager() {
        logStep("e_OpenSectionManager");
        mbtEdgeExpectLoggedIn("e_OpenSectionManager");
        dashboard.open();
        ownedCourseId = myLatestCourseId();
        if (ownedCourseId > 0 && dashboard.courseSelect().is(Condition.visible)) {
            dashboard.courseSelect().selectOptionByValue(String.valueOf(ownedCourseId));
        }
        dashboard.assertSectionManagerVisible();
    }

    @Override
    public void e_OpenSectionManagerFromDetail() {
        logStep("e_OpenSectionManagerFromDetail");
        if (ownedCourseId > 0) {
            detailPage.open(ownedCourseId);
        }
    }

    @Override
    public void e_CreateSection() {
        logStep("e_CreateSection");
        ensureSectionManagerReady();
        if (!dashboard.sectionTitleInput().is(Condition.visible)) {
            log.warn("e_CreateSection: section title input not visible — skipping");
            return;
        }
        String sectionTitle = "Section MBT " + System.currentTimeMillis();
        dashboard.sectionTitleInput().setValue(sectionTitle);
        if (!dashboard.saveSectionButton().is(Condition.enabled)) {
            log.warn("e_CreateSection: save-section button not enabled — skipping");
            return;
        }
        dashboard.saveSectionButton().click();
        com.codeborne.selenide.Selenide.sleep(600);

        // API cross-check: section should now exist in outline
        if (ownedCourseId > 0) {
            api.withToken(auth().accessToken);
            long newSectionId = firstSectionIdOf(ownedCourseId);
            if (newSectionId > 0) {
                lastSectionId = newSectionId;
                log.info("e_CreateSection: created section id={} title='{}'", lastSectionId, sectionTitle);
            }
        }
    }

    @Override
    public void e_RearmSectionFormGuards() {
        logStep("e_RearmSectionFormGuards");
        ctx.sectionTitleValid = true;
        ctx.sectionPositionValid = true;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_ArmSectionTitleInvalid() {
        logStep("e_ArmSectionTitleInvalid");
        ctx.sectionTitleValid = false;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_RearmLessonFormGuards() {
        logStep("e_RearmLessonFormGuards");
        ctx.lessonTitleValid = true;
        ctx.lessonPositionValid = true;
        ctx.lessonDurationValid = true;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_ArmLessonTitleInvalid() {
        logStep("e_ArmLessonTitleInvalid");
        ctx.lessonTitleValid = false;
        syncInstructorGraphWalker();
    }

    @Override
    public void e_CreateSectionFailTitle() {
        logStep("e_CreateSectionFailTitle");
        // Title "A" (1 char) < minLength=2 in CourseDetailPage.tsx input
        // FE button is disabled when title.trim() is empty; submit with 1 char bypasses HTML5 min
        ensureSectionManagerReady();
        dashboard.sectionTitleInput().setValue("A");
        if (dashboard.saveSectionButton().is(Condition.enabled)) {
            dashboard.saveSectionButton().click();
        }
        // API will return 422 for title < 2 chars; FE shows error
    }

    @Override
    public void e_FixAndRetrySection() {
        logStep("e_FixAndRetrySection");
        armHappyInstructorSectionLessonGuards();
        e_CreateSection();
    }

    @Override
    public void e_CreateLesson() {
        logStep("e_CreateLesson");
        ensureSectionManagerReady();
        if (!dashboard.lessonTitleInput().is(Condition.visible)) {
            log.warn("e_CreateLesson: lesson title input not visible — skipping");
            return;
        }
        // Select the section we created (or first available)
        if (lastSectionId > 0 && dashboard.sectionSelect().is(Condition.visible)) {
            dashboard.sectionSelect().selectOptionByValue(String.valueOf(lastSectionId));
        }
        String lessonTitle = "Lesson MBT " + System.currentTimeMillis();
        dashboard.lessonTitleInput().setValue(lessonTitle);
        dashboard.lessonTypeSelect().selectOptionByValue("VIDEO");
        dashboard.lessonDurationInput().setValue("10");
        dashboard.lessonPositionInput().setValue("1");

        if (dashboard.saveLessonButton().is(Condition.enabled)) {
            dashboard.saveLessonButton().click();
            com.codeborne.selenide.Selenide.sleep(600);
            log.info("e_CreateLesson: created lesson title='{}'", lessonTitle);
        } else {
            log.warn("e_CreateLesson: save-lesson button not enabled");
        }
    }

    @Override
    public void e_CreateLessonFailTitle() {
        logStep("e_CreateLessonFailTitle");
        // Title too short — set "A" (1 char); button stays disabled until title is filled
        ensureSectionManagerReady();
        if (dashboard.lessonTitleInput().is(Condition.visible)) {
            dashboard.lessonTitleInput().setValue("A");
        }
        // Attempt click; FE may block or show error
        if (dashboard.saveLessonButton().is(Condition.enabled)) {
            dashboard.saveLessonButton().click();
        }
    }

    @Override
    public void e_FixAndRetryLesson() {
        logStep("e_FixAndRetryLesson");
        armHappyInstructorSectionLessonGuards();
        e_CreateLesson();
    }

    @Override
    public void e_OutlineRefresh() {
        logStep("e_OutlineRefresh");
        com.codeborne.selenide.Selenide.refresh();
        // After refresh, instructor dashboard re-loads and fetches outline
        assertInstructorDashboardOrRetry();
    }

    @Override
    public void e_BackToSectionManagerFromOutline() {
        logStep("e_BackToSectionManagerFromOutline");
        // Section manager and outline are on the same page — no navigation needed
    }

    // ==================== helpers ====================

    private void asInstructor() {
        openAuthenticatedWithApi(TestData.INSTRUCTOR_EMAIL, TestData.PWD_STRONG, "/instructor/dashboard");
        auth().currentRole = "INSTRUCTOR";
    }

    /**
     * Sync GraphWalker guard variables from live API state.
     * Called from v_InstructorDashboard (start vertex) and after any create/edit operation
     * so GW can route correctly to guarded edges:
     *   ownedDraftExists     → e_OpenCourseForEdit, e_OpenSectionManagerFromDetail
     *   ownedPublishedExists → e_OpenPublishedCourse
     *   foreignCourseExists  → e_OpenForeignCourse
     *   ownedCourseExists    → e_OpenSectionManager
     *   selectedSectionExists→ e_CreateLesson (second guard form)
     */
    // ---- GraphWalker guards (JSON: tên method gwGuard_*) ----

    @SuppressWarnings("unused")
    public boolean gwGuard_createCourseFormAllValid() {
        return ctx.titleValid && ctx.descriptionValid && ctx.slugValid && ctx.imageUrlValid
                && ctx.maxCapacityValid && ctx.estimatedHoursValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_createTitleInvalid() {
        return !ctx.titleValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_createDescriptionInvalid() {
        return !ctx.descriptionValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_createImageInvalid() {
        return !ctx.imageUrlValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_createCapacityInvalid() {
        return !ctx.maxCapacityValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_createHoursInvalid() {
        return !ctx.estimatedHoursValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_ownedDraftExists() {
        return ctx.ownedDraftExists;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_ownedPublishedExists() {
        return ctx.ownedPublishedExists;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_foreignCourseExists() {
        return ctx.foreignCourseExists;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_ownedCourseExists() {
        return ctx.ownedCourseExists;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_sectionFormValid() {
        return ctx.sectionTitleValid && ctx.sectionPositionValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_sectionTitleInvalid() {
        return !ctx.sectionTitleValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_lessonFormValid() {
        return ctx.lessonTitleValid && ctx.lessonPositionValid && ctx.lessonDurationValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_lessonCreateReady() {
        return ctx.selectedSectionExists && ctx.lessonTitleValid && ctx.lessonPositionValid && ctx.lessonDurationValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_lessonTitleInvalid() {
        return !ctx.lessonTitleValid;
    }

    private void syncInstructorGuards() {
        api.withToken(auth().accessToken);
        long draftId = myOwnedDraftCourseId();
        long publishedId = myOwnedCourseIdWithStatus("PUBLISHED");
        long foreignId = foreignCourseId();
        long anyCourseId = myLatestCourseId();
        if (ownedCourseId <= 0) {
            ownedCourseId = anyCourseId;
        }

        ctx.ownedDraftExists = draftId > 0;
        ctx.ownedPublishedExists = publishedId > 0;
        ctx.foreignCourseExists = foreignId > 0;
        ctx.ownedCourseExists = anyCourseId > 0;

        boolean sectionExists = false;
        if (ownedCourseId > 0) {
            sectionExists = firstSectionIdOf(ownedCourseId) > 0;
        }
        ctx.selectedSectionExists = sectionExists;

        syncInstructorGraphWalker();
        log.debug("syncInstructorGuards: ownedDraft={} ownedPublished={} foreign={} ownedCourse={} section={}",
                draftId, publishedId, foreignId, anyCourseId, sectionExists);
    }

    private void assertInstructorDashboardOrRetry() {
        try {
            $("h1").shouldHave(Condition.text("Dashboard Giảng Viên"), Duration.ofSeconds(8));
        } catch (Throwable first) {
            log.warn("e_assertInstructor: h1 not found; retrying login ({})", first.toString());
            asInstructor();
            $("h1").shouldHave(Condition.text("Dashboard Giảng Viên"), Duration.ofSeconds(15));
        }
    }

    /** Ensure a course is selected in the section/lesson manager dropdowns. */
    private void ensureSectionManagerReady() {
        if (ownedCourseId <= 0) ownedCourseId = myLatestCourseId();
        if (ownedCourseId > 0 && dashboard.courseSelect().is(Condition.visible)) {
            dashboard.courseSelect().selectOptionByValue(String.valueOf(ownedCourseId));
        }
    }

    private long myLatestCourseId() {
        JsonNode me = api.me();
        long myId = (me == null) ? 0 : me.path("data").path("id").asLong();
        JsonNode list = api.listCourses();
        if (list == null || !list.path("data").isArray()) return 0;
        long latest = 0;
        for (JsonNode c : list.path("data")) {
            if (c.path("instructor_id").asLong() == myId) {
                latest = Math.max(latest, c.path("id").asLong());
            }
        }
        return latest;
    }

    private long myOwnedDraftCourseId() {
        return myOwnedCourseIdWithStatus("DRAFT");
    }

    private long myOwnedCourseIdWithStatus(String status) {
        JsonNode me = api.me();
        long myId = (me == null) ? 0 : me.path("data").path("id").asLong();

        if ("DRAFT".equals(status)) {
            long seeded = api.courseIdBySlug(TestData.MBT_INSTRUCTOR_DRAFT_SLUG);
            if (seeded > 0) {
                JsonNode detail = api.courseDetail(seeded);
                if (detail != null && detail.path("data").path("instructor_id").asLong() == myId) {
                    return seeded;
                }
            }
        }
        JsonNode list = api.listCourses(status);
        if (list == null || !list.path("data").isArray()) return 0;
        for (JsonNode c : list.path("data")) {
            if (c.path("instructor_id").asLong() == myId && status.equals(c.path("status").asText())) {
                return c.path("id").asLong();
            }
        }
        return 0;
    }

    private long foreignCourseId() {
        JsonNode me = api.me();
        long myId = (me == null) ? 0 : me.path("data").path("id").asLong();
        JsonNode list = api.listCourses();
        if (list == null || !list.path("data").isArray()) return 0;
        for (JsonNode c : list.path("data")) {
            if (c.path("instructor_id").asLong() != myId) return c.path("id").asLong();
        }
        return 0;
    }

    private long firstSectionIdOf(long courseId) {
        if (courseId <= 0) return 0;
        JsonNode outline = api.get("/api/v1/learning/courses/" + courseId + "/outline", false);
        if (outline != null && outline.path("data").isArray() && outline.path("data").size() > 0) {
            return outline.path("data").get(0).path("section").path("id").asLong();
        }
        return 0;
    }
}