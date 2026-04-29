package com.eduplatform.mbt.pages;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.SafeUi;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.sleep;
import com.codeborne.selenide.WebDriverRunner;

/**
 * /learn/:courseId — LearningPage.tsx
 *
 * UI states:
 *   Loading: fetches course, registrations, outline, progressDetail, certificates in parallel
 *   Not found: h1 "Không tìm thấy khóa học" + link "Quay lại dashboard"
 *   Not confirmed: h1 "Bạn chưa được xác nhận vào khóa học này" + link "Quay lại dashboard"
 *   Loaded (CONFIRMED): two-panel layout
 *     LEFT PANEL (main content):
 *       Progress bar: .h-2.bg-neutral-100 > div  (width = completion%)
 *       Lesson count text: "X/Y bài học hoàn thành"
 *       Lesson content area: video/text/quiz based on lesson type
 *       "Đánh dấu hoàn thành" button — only when current lesson is not yet completed
 *       "Đã hoàn thành" label — shown when current lesson is completed (CheckCircle icon)
 *       "Nhận chứng chỉ" / "Xem chứng chỉ" button — shown when all lessons completed
 *     RIGHT PANEL (sidebar, w-96 class):
 *       h2 with course title (sidebar title)
 *       lesson buttons — one per lesson in outline (sorted by position)
 *         completed lessons show CheckCircle icon
 *         current lesson is highlighted
 *   CertificateModal: shown when 100% complete and cert just issued (sessionStorage key)
 *     h2 "Chúc mừng!"
 *     "Xem chứng chỉ" link → /student/dashboard
 *     "Tiếp tục học" button → closes modal
 *
 * Backend endpoints:
 *   GET /courses/{id}
 *   GET /registrations  (filtered client-side for this course+user)
 *   GET /learning/courses/{id}/outline
 *   GET /learning/courses/{id}/progress-detail → { completed_lesson_ids, completion_by_lesson }
 *   GET /certificates/me
 *   POST /learning/progress { lesson_id, completion_pct: 100 } → may return { certificate_issued, certificate_id }
 *   POST /certificates/issue/{courseId}
 */
public class LearningPage {

    public LearningPage open(long courseId) {
        Selenide.open("/learn/" + courseId);
        return this;
    }

    // ========== error states ==========

    /** Shown when course not found (API 404 or course data null after load) */
    public SelenideElement notFoundMsg()     { return $(byText("Không tìm thấy khóa học")); }

    /**
     * Shown when user is not CONFIRMED for this course.
     * Text: "Bạn chưa được xác nhận vào khóa học này"
     */
    public SelenideElement accessDeniedMsg() { return $(byText("Bạn chưa được xác nhận vào khóa học này")); }

    // ========== sidebar (right panel, w-96) ==========

    /**
     * Sidebar course title — {@code data-testid="learning-course-title"} on FE (stable vs Tailwind class output).
     */
    public SelenideElement sidebarTitle() {
        return $(byAttribute("data-testid", "learning-course-title"));
    }

    /**
     * All lesson buttons in the sidebar (may include not-yet-visible during paint).
     */
    public ElementsCollection lessonButtons() {
        return $$(byAttribute("data-testid", "learning-lesson-button"));
    }

    public ElementsCollection visibleLessonButtons() {
        return lessonButtons().filter(Condition.visible);
    }

    // ========== main content panel ==========

    /**
     * Progress bar fill div.
     * Width style = completion percentage, e.g. style="width: 33%"
     * Parent: .h-2.bg-neutral-100
     */
    public SelenideElement progressBar()       { return $(".h-2.bg-neutral-100 > div"); }

    /**
     * "X/Y bài học hoàn thành" text shown below the progress bar.
     */
    public SelenideElement completedCountText() { return $(".text-sm.text-neutral-600.mb-3"); }

    /**
     * "Đánh dấu hoàn thành" button — rendered when current lesson has not been completed yet.
     * POST /learning/progress { lesson_id, completion_pct: 100 }
     * After click: button replaced by "Đã hoàn thành" label; progress bar updates.
     */
    public SelenideElement markCompleteBtn()   { return $(byText("Đánh dấu hoàn thành")); }

    /**
     * "Đã hoàn thành" label — shown when the currently selected lesson is completed.
     * Rendered with CheckCircle icon + text.
     */
    public SelenideElement completedLabel()    { return $(byText("Đã hoàn thành")); }

    /**
     * "Nhận chứng chỉ" button — shown when all lessons are complete and no cert yet.
     * POST /certificates/issue/{courseId}
     */
    public SelenideElement claimCertButton()   { return $(byText("Nhận chứng chỉ")); }

    /**
     * "Xem chứng chỉ" button — shown in main panel when cert already issued.
     * Links to /student/dashboard certificate section.
     */
    public SelenideElement viewCertButton()    { return $(byText("Xem chứng chỉ")); }

    // ========== certificate modal ==========

    /**
     * CertificateModal h2 "Chúc mừng!" — appears auto-shown when:
     *   - all lessons are completed
     *   - cert exists
     *   - sessionStorage key cert_modal_shown_{courseId} not set
     */
    public SelenideElement certModal()         { return $(byText("Chúc mừng!")); }

    /**
     * "Tiếp tục học" button inside the cert modal — closes the modal.
     */
    public SelenideElement certModalContinue() { return $(byText("Tiếp tục học")); }

    // ========== navigation ==========

    /**
     * "Quay lại Dashboard" link — navigates to /student/dashboard.
     * Present in both the access-denied state and the full learning view.
     */
    public SelenideElement backToDashboard()   { return $(byText("Quay lại Dashboard")); }

    // ========== helpers ==========

    /**
     * Assert the full learning page is loaded for a confirmed user.
     * Waits through the loading shell and fails fast on not-found / access-denied.
     */
    public void assertFullyLoaded() {
        Duration budget = Duration.ofSeconds(45);
        long deadline = System.currentTimeMillis() + budget.toMillis();
        SelenideElement loadingShell = $(byAttribute("data-testid", "learning-page-loading"));
        while (System.currentTimeMillis() < deadline) {
            if (notFoundMsg().is(Condition.visible)) {
                throw new AssertionError(
                        "Learning page: course not found; url=" + WebDriverRunner.url());
            }
            if (accessDeniedMsg().is(Condition.visible)) {
                throw new AssertionError(
                        "Learning page: access denied (not CONFIRMED); url=" + WebDriverRunner.url());
            }
            if (sidebarTitle().is(Condition.visible)) {
                try {
                    lessonButtons().shouldHave(CollectionCondition.sizeGreaterThan(0), Duration.ofSeconds(35));
                } catch (AssertionError e) {
                    throw new AssertionError(
                            "Learning page: no lesson buttons (empty outline?); url=" + WebDriverRunner.url()
                                    + " — pick a course with sections/lessons in seed/DB");
                }
                SafeUi.waitUntilVisible(lessonButtons().first(), Duration.ofSeconds(15));
                return;
            }
            sleep(200);
        }
        throw new AssertionError(String.format(
                "Learning page: sidebar not visible within %ds (loadingShell=%s); url=%s",
                budget.toSeconds(),
                loadingShell.is(Condition.visible),
                WebDriverRunner.url()));
    }

    /**
     * Extract the current progress percentage from the progress bar style attribute.
     * Returns -1 if not parseable.
     */
    public int readProgressPct() {
        try {
            String style = progressBar().getAttribute("style");
            if (style == null) return -1;
            // style="width: 33%"
            int start = style.indexOf("width:") + 6;
            int end   = style.indexOf("%", start);
            if (start < 6 || end < 0) return -1;
            return Integer.parseInt(style.substring(start, end).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Assert progress bar shows at least the given percentage.
     */
    public void assertProgressAtLeast(int minPct) {
        int actual = readProgressPct();
        if (actual < minPct) {
            throw new AssertionError("Expected progress >= " + minPct + "% but was " + actual + "%");
        }
    }
}