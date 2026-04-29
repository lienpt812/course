package com.eduplatform.mbt.impl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.models.LearningModel;
import com.eduplatform.mbt.pages.LearningPage;
import com.eduplatform.mbt.pages.StudentDashboardPage;
import com.eduplatform.mbt.support.BaseImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import com.eduplatform.mbt.support.MbtBusinessAssertions;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.graphwalker.java.annotation.GraphWalker;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;

/**
 * Learning page MBT — lesson selection, mark-complete, progress tracking, certificate trigger.
 *
 * UI-backend alignment:
 *   - /learn/{courseId} requires CONFIRMED registration; otherwise shows access-denied h1
 *   - LearningPage.tsx loads: course, registrations, outline, progressDetail, certificates in parallel
 *   - "Đánh dấu hoàn thành" → POST /learning/progress { lesson_id, completion_pct: 100 }
 *     → response may contain { certificate_issued: true } if all lessons done
 *   - Progress bar width = (completed / total) * 100%
 *   - "Nhận chứng chỉ" shown when all completed + no cert yet
 *   - CertificateModal auto-shown when 100% + cert exists (sessionStorage gate per course)
 */
@GraphWalker(value = GraphWalkerExecutionPolicy.BOUNDED_LEARN, start = "v_StudentDashboardEntry")
public class LearningImpl extends BaseImpl implements LearningModel {

    private final StudentDashboardPage studentDash = new StudentDashboardPage();
    private final LearningPage learningPage = new LearningPage();

    private long targetCourseId;
    /** Index trong {@link LearningPage#lessonButtons()} sau lần click gần nhất. */
    private int lastLessonButtonIndex;

    // =============== Vertices ===============

    @Override
    public void v_StudentDashboardEntry() {
        logStep("v_StudentDashboardEntry");
        asStudent();
        // asStudent() already lands on /student/dashboard (API login or inject). A second Selenide.open here
        // can race RootLayout's authApi.me() and sporadically leave the SPA on /courses.
        if (!waitForUrlContains("/student/dashboard", 12)) {
            log.warn("v_StudentDashboardEntry: not on /student/dashboard after asStudent; url={} — opening again",
                    com.codeborne.selenide.WebDriverRunner.url());
            studentDash.open();
            waitForUrlContains("/student/dashboard", 8);
        }
        studentDash.assertLoaded();

        // Find a confirmed course to use as target
        targetCourseId = firstConfirmedCourseId();

        // Sync GW guard based on registration status
        api.withToken(auth().accessToken);
        String regStatus = "NONE";
        JsonNode myRegs = api.listMyRegistrations();
        if (myRegs != null && myRegs.path("data").isArray()) {
            for (JsonNode r : myRegs.path("data")) {
                String s = r.path("status").asText();
                if ("CONFIRMED".equals(s)) { regStatus = "CONFIRMED"; break; }
                if ("NONE".equals(regStatus) &&
                        ("PENDING".equals(s) || "WAITLIST".equals(s))) {
                    regStatus = s;
                }
            }
        }
        ctx.registrationStatus = regStatus;
        syncLearningGraphWalker();
    }

    @Override
    public void v_LearningAccessDenied() {
        logStep("v_LearningAccessDenied");
        // LearningPage.tsx renders "Bạn chưa được xác nhận vào khóa học này"
        // when registration.status !== 'CONFIRMED'
        if (!learningPage.accessDeniedMsg().is(Condition.visible)) {
            log.warn("v_LearningAccessDenied: access-denied message not visible (user may have been confirmed)");
        }
    }

    @Override
    public void v_LearningPageLoaded() {
        logStep("v_LearningPageLoaded");
        // Full learning page for a confirmed student: sidebar + lesson buttons
        learningPage.assertFullyLoaded();
        refreshCourseProgressFromApi();
        int uiLessons = learningPage.lessonButtons().size();
        if (uiLessons > 0) {
            ctx.totalLessons = Math.max(ctx.totalLessons, uiLessons);
        }
        log.info("v_LearningPageLoaded: totalLessons={} completed={} pct={}",
                ctx.totalLessons, ctx.completedLessons, ctx.completionPct);
        syncLearningGraphWalker();
    }

    @Override
    public void v_LessonSelected() {
        logStep("v_LessonSelected");
        refreshSelectedLessonFromApi();
        boolean incomplete = learningPage.markCompleteBtn().is(Condition.visible);
        boolean done = learningPage.completedLabel().is(Condition.visible);
        if (!incomplete && !done) {
            throw new AssertionError("v_LessonSelected: expected mark-complete or completed label");
        }
        syncLearningGraphWalker();
    }

    @Override
    public void v_LessonCompleted() {
        logStep("v_LessonCompleted");
        // "Đã hoàn thành" label replaces the mark-complete button
        SafeUi.waitUntilVisible(learningPage.completedLabel(), SafeUi.DEFAULT_TIMEOUT);
        ctx.completedLessons++;
    }

    @Override
    public void v_AllLessonsCompleted() {
        logStep("v_AllLessonsCompleted");
        ctx.allLessonsCompleted = true;
        ctx.completionPct = 100;
        refreshCourseProgressFromApi();
        syncLearningGraphWalker();
        // Progress bar should show ~100%
        int pct = learningPage.readProgressPct();
        if (pct >= 0 && pct < 90) {
            log.warn("v_AllLessonsCompleted: progress bar shows {}% (expected ~100%)", pct);
        }
    }

    // =============== Edges ===============

    @Override
    public void e_StartLearning() {
        logStep("e_StartLearning");
        if (targetCourseId > 0) {
            learningPage.open(targetCourseId);
        } else {
            log.warn("e_StartLearning: no confirmed course; opening /learn/1 as fallback");
            open("/learn/1");
            targetCourseId = 1;
        }
        learningPage.assertFullyLoaded();
        refreshCourseProgressFromApi();
        syncLearningGraphWalker();
    }

    @Override
    public void e_StartLearningDenied() {
        logStep("e_StartLearningDenied");
        // Open a course where user is not CONFIRMED → access denied page
        long unc = firstUnconfirmedCourseId();
        open("/learn/" + (unc > 0 ? unc : 1));
    }

    @Override
    public void e_SelectLesson() {
        logStep("e_SelectLesson");
        var buttons = learningPage.lessonButtons();
        if (!buttons.isEmpty()) {
            int idx = Math.min(ctx.completedLessons, buttons.size() - 1);
            lastLessonButtonIndex = idx;
            buttons.get(idx).click();
            sleep(400);
            refreshSelectedLessonFromApi();
        } else {
            log.warn("e_SelectLesson: no lesson buttons visible");
        }
        syncLearningGraphWalker();
    }

    @Override
    public void e_MarkLessonCompleted() {
        logStep("e_MarkLessonCompleted");
        // Read progress before click
        int beforePct = learningPage.readProgressPct();

        SelenideElement btn = learningPage.markCompleteBtn();
        if (btn.exists() && btn.isEnabled()) {
            btn.click();
            sleep(700); // wait for React state update

            // Progress bar must not decrease after marking a lesson complete
            int afterPct = learningPage.readProgressPct();
            if (beforePct >= 0 && afterPct >= 0 && afterPct < beforePct) {
                throw new AssertionError(
                        "Progress dropped after marking lesson complete: " + beforePct + "% → " + afterPct + "%");
            }

            // API cross-check: GET /learning/courses/{id}/progress
            if (targetCourseId > 0) {
                api.withToken(auth().accessToken);
                JsonNode p = api.get("/api/v1/learning/courses/" + targetCourseId + "/progress", true);
                if (p != null) {
                    MbtBusinessAssertions.assertSuccessEnvelope(p, "GET learning progress after mark-complete");
                }
            }
            refreshCourseProgressFromApi();
            ctx.currentLessonCompletionPct = 100;
            refreshSelectedLessonFromApi();
        } else {
            log.warn("e_MarkLessonCompleted: mark-complete button not present/enabled");
        }
        syncLearningGraphWalker();
    }

    @Override
    public void e_MarkLessonPartial() {
        logStep("e_MarkLessonPartial");
        // POST a partial progress (50%) via API to simulate a mid-lesson save
        // This does NOT mark the lesson as completed (completed = completion_pct >= 100)
        long lessonId = firstPendingLessonId();
        if (lessonId > 0) {
            api.withToken(auth().accessToken);
            JsonNode r = api.post("/api/v1/learning/progress",
                    java.util.Map.of("lesson_id", lessonId, "completion_pct", 50), true);
            MbtBusinessAssertions.assertSuccessEnvelope(r, "POST learning/progress partial (50%)");
            // completed field in response should be false for pct < 100
            boolean completed = r.path("data").path("completed").asBoolean(false);
            if (completed) {
                throw new AssertionError("partial progress (50%) returned completed=true — BE threshold may have changed");
            }
            ctx.currentLessonCompletionPct = 50;
            ctx.currentLessonIsPreview = false;
            refreshSelectedLessonFromApi();
        } else {
            log.warn("e_MarkLessonPartial: no uncompleted lesson found");
        }
        syncLearningGraphWalker();
    }

    @Override
    public void e_MarkPreviewLesson() {
        logStep("e_MarkPreviewLesson");
        // Preview lessons can be marked even without confirmed registration
        long lessonId = firstPreviewLessonId();
        if (lessonId > 0) {
            api.withToken(auth().accessToken);
            JsonNode r = api.post("/api/v1/learning/progress",
                    java.util.Map.of("lesson_id", lessonId, "completion_pct", 100), true);
            MbtBusinessAssertions.assertSuccessEnvelope(r, "POST learning/progress preview 100%");
            refreshCourseProgressFromApi();
            ctx.currentLessonCompletionPct = 100;
            refreshSelectedLessonFromApi();
        } else {
            log.warn("e_MarkPreviewLesson: no preview lesson found in course {}", targetCourseId);
        }
        syncLearningGraphWalker();
    }

    @Override
    public void e_ContinueToNextLesson() {
        logStep("e_ContinueToNextLesson");
        var buttons = learningPage.lessonButtons();
        if (ctx.completedLessons < buttons.size()) {
            lastLessonButtonIndex = ctx.completedLessons;
            buttons.get(ctx.completedLessons).click();
            sleep(400);
            refreshSelectedLessonFromApi();
        } else {
            log.warn("e_ContinueToNextLesson: completedLessons={} >= total={}", ctx.completedLessons, buttons.size());
        }
        syncLearningGraphWalker();
    }

    @Override
    public void e_ReachAllCompleted() {
        logStep("e_ReachAllCompleted");
        ctx.allLessonsCompleted = true;
        ctx.completionPct = 100;
        refreshCourseProgressFromApi();
        syncLearningGraphWalker();
        // When all lessons are completed, FE shows "Nhận chứng chỉ" or "Xem chứng chỉ" button
        if (learningPage.claimCertButton().is(Condition.visible)) {
            log.info("e_ReachAllCompleted: 'Nhận chứng chỉ' button visible — cert not yet issued");
        } else if (learningPage.viewCertButton().is(Condition.visible)) {
            log.info("e_ReachAllCompleted: 'Xem chứng chỉ' visible — cert already issued");
        }
    }

    @Override
    public void e_RefreshProgress() {
        logStep("e_RefreshProgress");
        com.codeborne.selenide.Selenide.refresh();
        // After refresh: LearningPage reloads outline + progressDetail from API
        // completedLessons (UI) should remain the same (persisted in DB)
        learningPage.assertFullyLoaded();
        refreshCourseProgressFromApi();
        syncLearningGraphWalker();
    }

    @Override
    public void e_BackToDashboard() {
        logStep("e_BackToDashboard");
        if (learningPage.backToDashboard().is(Condition.visible)) {
            learningPage.backToDashboard().click();
            waitForUrlContains("/student/dashboard", 5);
        } else {
            open("/student/dashboard");
        }
    }

    // ==================== helpers ====================

    private void asStudent() {
        if (auth().isLoggedIn && "STUDENT".equals(auth().currentRole)
                && auth().accessToken != null && !auth().accessToken.isBlank()) {
            if (injectBrowserSessionAndOpen("/student/dashboard")) {
                auth().currentRole = "STUDENT";
                auth().isLoggedIn = true;
                syncLearningGraphWalker();
                return;
            }
            log.warn("asStudent: session reuse failed — full API login (may hit rate limits if repeated often)");
        }
        openAuthenticatedWithApi(TestData.STUDENT_EMAIL, TestData.PWD_STRONG, "/student/dashboard");
        auth().currentRole = "STUDENT";
        auth().isLoggedIn = true;
        syncLearningGraphWalker();
    }

    @Override
    public void e_ArmLessonProgressPartial() {
        ctx.currentLessonCompletionPct = 50;
        ctx.currentLessonIsPreview = false;
        syncLearningGraphWalker();
    }

    @Override
    public void e_ArmPreviewLessonComplete() {
        ctx.currentLessonIsPreview = true;
        ctx.currentLessonCompletionPct = 100;
        syncLearningGraphWalker();
    }

    @Override
    public void e_RearmLessonProgressHappy() {
        ctx.currentLessonIsPreview = false;
        ctx.currentLessonCompletionPct = 100;
        syncLearningGraphWalker();
    }

    private void refreshCourseProgressFromApi() {
        if (targetCourseId <= 0) return;
        api.withToken(auth().accessToken);
        JsonNode p = api.get("/api/v1/learning/courses/" + targetCourseId + "/progress", true);
        if (p != null && p.path("data").isObject()) {
            JsonNode d = p.path("data");
            ctx.totalLessons = d.path("total_lessons").asInt(ctx.totalLessons);
            ctx.completedLessons = d.path("completed_lessons").asInt(ctx.completedLessons);
            ctx.completionPct = d.path("completion_pct").asInt(ctx.completionPct);
        }
    }

    private void refreshSelectedLessonFromApi() {
        if (targetCourseId <= 0) return;
        api.withToken(auth().accessToken);
        JsonNode detail = api.get("/api/v1/learning/courses/" + targetCourseId + "/progress-detail", true);
        JsonNode outline = api.get("/api/v1/learning/courses/" + targetCourseId + "/outline", false);
        if (outline == null || !outline.path("data").isArray()) return;

        List<Long> lessonIds = new ArrayList<>();
        List<Boolean> previews = new ArrayList<>();
        for (JsonNode section : outline.path("data")) {
            for (JsonNode lesson : section.path("lessons")) {
                lessonIds.add(lesson.path("id").asLong());
                previews.add(lesson.path("is_preview").asBoolean(false));
            }
        }
        if (lessonIds.isEmpty()) return;

        int idx = Math.min(Math.max(lastLessonButtonIndex, 0), lessonIds.size() - 1);
        long lid = lessonIds.get(idx);
        ctx.currentLessonIsPreview = previews.get(idx);

        JsonNode map = detail != null ? detail.path("data").path("completion_by_lesson") : null;
        String key = String.valueOf(lid);
        int pct = 0;
        if (map != null && map.has(key)) {
            pct = map.get(key).asInt(0);
        }
        ctx.currentLessonCompletionPct = pct;
    }

    private long firstConfirmedCourseId() {
        api.withToken(auth().accessToken);
        long anchor = api.courseIdBySlug(TestData.MBT_STUDENT_ANCHOR_SLUG);
        if (anchor > 0 && countLessonsInOutline(anchor) > 0) {
            return anchor;
        }
        long demo = api.courseIdBySlug(TestData.DEMO_COURSE_01_SLUG);
        if (demo > 0 && countLessonsInOutline(demo) > 0) {
            return demo;
        }
        JsonNode regs = api.listMyRegistrations();
        if (regs != null && regs.path("data").isArray()) {
            for (JsonNode r : regs.path("data")) {
                if ("CONFIRMED".equals(r.path("status").asText())) {
                    long cid = r.path("course_id").asLong();
                    if (cid > 0 && countLessonsInOutline(cid) > 0) {
                        return cid;
                    }
                }
            }
            for (JsonNode r : regs.path("data")) {
                if ("CONFIRMED".equals(r.path("status").asText())) {
                    return r.path("course_id").asLong();
                }
            }
        }
        if (anchor > 0) {
            return anchor;
        }
        return demo;
    }

    /**
     * Learning MBT needs at least one lesson in outline; some CONFIRMED courses can be empty in DB.
     */
    private int countLessonsInOutline(long courseId) {
        if (courseId <= 0) {
            return 0;
        }
        api.withToken(auth().accessToken);
        JsonNode outline = api.get("/api/v1/learning/courses/" + courseId + "/outline", false);
        if (outline == null) {
            return 0;
        }
        if (outline.has("errors") && outline.path("errors").isArray() && outline.path("errors").size() > 0) {
            return 0;
        }
        if (!outline.path("data").isArray()) {
            return 0;
        }
        int n = 0;
        for (JsonNode section : outline.path("data")) {
            if (!section.path("lessons").isArray()) {
                continue;
            }
            n += section.path("lessons").size();
        }
        return n;
    }

    private long firstUnconfirmedCourseId() {
        api.withToken(auth().accessToken);
        JsonNode regs = api.listMyRegistrations();
        if (regs != null && regs.path("data").isArray()) {
            for (JsonNode r : regs.path("data")) {
                if (!"CONFIRMED".equals(r.path("status").asText())) {
                    return r.path("course_id").asLong();
                }
            }
        }
        // No registration at all — pick any published course
        JsonNode list = api.listCourses();
        if (list != null && list.path("data").size() > 0) {
            return list.path("data").get(0).path("id").asLong();
        }
        return 0;
    }

    private long firstPendingLessonId() {
        return firstLessonId(false);
    }

    private long firstPreviewLessonId() {
        return firstLessonId(true);
    }

    private long firstLessonId(boolean preview) {
        if (targetCourseId <= 0) return 0;
        JsonNode outline = api.get("/api/v1/learning/courses/" + targetCourseId + "/outline", false);
        if (outline == null || !outline.path("data").isArray()) return 0;
        for (JsonNode section : outline.path("data")) {
            for (JsonNode lesson : section.path("lessons")) {
                if (lesson.path("is_preview").asBoolean() == preview) {
                    return lesson.path("id").asLong();
                }
            }
        }
        return 0;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_registrationConfirmed() {
        return "CONFIRMED".equals(ctx.registrationStatus);
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_registrationNotConfirmed() {
        return !"CONFIRMED".equals(ctx.registrationStatus);
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_markLessonCompleteAllowed() {
        return "CONFIRMED".equals(ctx.registrationStatus) && ctx.currentLessonCompletionPct >= 80;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_markLessonPartialAllowed() {
        return "CONFIRMED".equals(ctx.registrationStatus) && ctx.currentLessonCompletionPct < 80;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_previewLessonMarkAllowed() {
        return ctx.currentLessonIsPreview && ctx.currentLessonCompletionPct >= 100;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_hasMoreLessonsToComplete() {
        return ctx.completedLessons < ctx.totalLessons;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_allLessonsComplete() {
        return ctx.totalLessons > 0 && ctx.completedLessons >= ctx.totalLessons;
    }
}