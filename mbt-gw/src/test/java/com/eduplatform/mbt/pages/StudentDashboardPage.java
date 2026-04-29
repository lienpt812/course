package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * /student/dashboard — StudentDashboard.tsx
 *
 * UI layout:
 *   h1 "Dashboard Học Viên"
 *   Stat cards (4, each a button.bg-white.border.border-emerald-100):
 *     [0] "Khóa Học Đang Học"  → stats.current_courses
 *     [1] "Chờ Duyệt"         → pending registrations count
 *     [2] "Hàng Chờ"          → waitlist count
 *     [3] "Lịch Sử Đăng Ký"  → stats.registration_history
 *   InsightModal — shown when a stat card is clicked
 *
 *   Confirmed Courses section ("Khóa Học Đang Học"):
 *     CourseCard-like rows with progress bar
 *     "Tiếp tục học" / "Bắt đầu học" link button per confirmed course → /learn/{courseId}
 *
 *   All Registrations section:
 *     table with columns: Khóa học | Trạng thái | Ngày đăng ký | Hành động
 *     tbody tr — one per registration (all statuses)
 *     RegistrationStatusBadge per row
 *
 *   Chứng Chỉ Của Tôi section:
 *     h2 "Chứng Chỉ Của Tôi"
 *     certificate cards — one per issued certificate
 *       a[href^='/api/v1/certificates/verify/'] — verification link
 *
 * Backend:
 *   GET /dashboards/student → { current_courses, registration_history }
 *   GET /registrations      → student's own registrations
 *   GET /courses            → published courses
 *   GET /certificates/me    → student's certificates
 *   GET /learning/courses/{id}/progress — for each confirmed course
 */
public class StudentDashboardPage {

    public StudentDashboardPage open() {
        Selenide.open("/student/dashboard");
        return this;
    }

    public StudentDashboardPage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), Duration.ofSeconds(12))
                .shouldHave(Condition.text(UiText.STUDENT_DASHBOARD_H1), Duration.ofSeconds(12));
        return this;
    }

    // ========== stat cards ==========

    /**
     * "Khóa Học Đang Học" stat card (index 0).
     * Clicking opens InsightModal with learning detail.
     */
    public SelenideElement statLearning() { return $$("button.bg-white.border.border-emerald-100").get(0); }

    /** "Chờ Duyệt" stat card (index 1) */
    public SelenideElement statPending()  { return $$("button.bg-white.border.border-emerald-100").get(1); }

    /** "Hàng Chờ" stat card (index 2) */
    public SelenideElement statWaitlist() { return $$("button.bg-white.border.border-emerald-100").get(2); }

    /** "Lịch Sử Đăng Ký" stat card (index 3) */
    public SelenideElement statHistory()  { return $$("button.bg-white.border.border-emerald-100").get(3); }

    // ========== courses in progress ==========

    /**
     * "Tiếp tục học" or "Bắt đầu học" link buttons for confirmed courses.
     * Navigate to /learn/{courseId}.
     */
    public ElementsCollection continueButtons() {
        return $$("a").filter(
            Condition.or("continue or start",
                Condition.text("Tiếp tục học"),
                Condition.text("Bắt đầu học"))
        );
    }

    /** First "Tiếp tục học" / "Bắt đầu học" button */
    public SelenideElement continueButton() { return $(byText("Tiếp tục học")); }

    // ========== certificates section ==========

    /**
     * "Chứng Chỉ Của Tôi" section heading.
     */
    public SelenideElement sectionCertificates() { return $(byText("Chứng Chỉ Của Tôi")); }

    /**
     * Certificate verification links.
     * Each certificate card contains an <a href="/api/v1/certificates/verify/{code}">
     */
    public ElementsCollection certificates() {
        return $$("a[href^='/api/v1/certificates/verify/']");
    }

    // ========== registrations table ==========

    /** tbody rows in the full registration history table */
    public ElementsCollection registrationRows() { return $$("table tbody tr"); }

    // ========== helpers ==========

    /**
     * Assert certificates section is visible (the heading).
     */
    public void assertCertificatesSectionVisible() {
        SafeUi.waitUntilVisible(sectionCertificates(), Duration.ofSeconds(5));
    }

    /**
     * Assert at least N certificates are displayed.
     */
    public void assertCertificateCount(int atLeast) {
        if (certificates().size() < atLeast) {
            throw new AssertionError("Expected at least " + atLeast + " certificate(s) but found " + certificates().size());
        }
    }

    /**
     * Assert the "Khóa Học Đang Học" stat card shows the expected count.
     * The count is the text number inside the card.
     */
    public void assertCurrentCoursesCount(int expected) {
        statLearning().$(".text-3xl").shouldHave(Condition.text(String.valueOf(expected)));
    }
}