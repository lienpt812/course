package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
/**
 * /admin/dashboard — AdminDashboard.tsx
 *
 * UI layout:
 *   h1 "Quản Trị Hệ Thống"
 *   stat cards (6): Chờ Duyệt, Đã Xác Nhận, Hàng Chờ, Tổng Đăng Ký, Tổng Khóa Học, Tổng Users
 *   InsightModal — opens when a stat card is clicked
 *   Filter bar:
 *     select#admin-filter-course-select — "all" | course id options
 *     select#admin-filter-status-select — "all"|"PENDING"|"CONFIRMED"|"WAITLIST"|"CANCELLED"|"REJECTED"|"EXPIRED"
 *   Registrations table:
 *     thead: ID | Khóa Học | Học Viên | Ngày Đăng Ký | Trạng Thái | Hành Động
 *     tbody tr — one row per filtered registration
 *       td[0]: registration id
 *       td[1]: course title
 *       td[2]: user
 *       td[3]: created_at
 *       td[4]: RegistrationStatusBadge
 *       td[5]: buttons — [data-testid=admin-reg-approve] / [data-testid=admin-reg-reject]
 *                        (only visible for PENDING rows)
 *   Bulk approve button: data-testid="admin-bulk-approve"
 *     - enabled when filtered view has at least one PENDING row
 *     - disabled when no PENDING in current filter
 *   "Danh sách học viên bị từ chối do lớp đã đủ" banner — shown after bulk approve with rejections
 *
 * Backend used:
 *   GET /dashboards/admin → { total_courses, total_users, pending_registrations }
 *   GET /registrations    → all registrations (admin sees all)
 *   GET /courses          → course list for filter dropdown
 *   POST /registrations/{id}/approve
 *   POST /registrations/{id}/reject
 *   POST /registrations/bulk-approve
 */
public class AdminDashboardPage {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardPage.class);

    /**
     * Open /admin/dashboard and wait for filter controls.
     * If the browser is already on /admin/dashboard (e.g. after {@code asAdmin()}), skips a second
     * full navigation to avoid an extra load race with {@code RootLayout}'s {@code authApi.me()}.
     */
    public AdminDashboardPage open() {
        String url;
        try {
            url = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.url() : null;
        } catch (Exception e) {
            url = null;
        }
        if (url == null || !url.contains("admin/dashboard")) {
            Selenide.open("/admin/dashboard");
        }
        waitForAdminPathThenFilters();
        return this;
    }

    public AdminDashboardPage assertLoaded() {
        SafeUi.waitUntilVisible($$("h1").findBy(Condition.text(UiText.ADMIN_DASHBOARD_H1)), Duration.ofSeconds(15));
        return this;
    }

    /**
     * After session switch via API, call when the URL is already /admin/dashboard to wait for
     * filters without forcing another {@link Selenide#open(String)}.
     */
    public void waitForAdminPathThenFilters() {
        waitForUrlFragment("admin/dashboard", 16);
        waitForAdminFilterControls();
    }

    private static void waitForUrlFragment(String fragment, int maxSeconds) {
        long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (WebDriverRunner.hasWebDriverStarted()) {
                    String u = WebDriverRunner.url();
                    if (u != null && u.contains(fragment)) {
                        return;
                    }
                }
            } catch (Exception ignored) {
                // continue polling
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        try {
            log.warn("URL still does not contain '{}' after {}s (url={})",
                    fragment, maxSeconds, WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.url() : "n/a");
        } catch (Exception ignored) {
            // ignore
        }
    }

    /**
     * Wait for SPA render: h1 + both filter selects must be visible.
     * Use {@code $$..findBy(text)} for h1 (not first {@code $("h1")}) so we never match
     * {@code CoursesPage}'s h1 "Khóa Học" if a redirect left the app on /courses.
     */
    public void waitForAdminFilterControls() {
        SafeUi.waitUntilVisible($$("h1").findBy(Condition.text(UiText.ADMIN_DASHBOARD_H1)), Duration.ofSeconds(20));
        SafeUi.waitUntilVisible($("#admin-filter-course-select"), Duration.ofSeconds(20));
        SafeUi.waitUntilVisible($("#admin-filter-status-select"), Duration.ofSeconds(20));
    }

    // ========== filter controls ==========

    /** Course filter — id="admin-filter-course-select"; value "all" or course.id as string */
    public SelenideElement courseFilter()   { return $("#admin-filter-course-select"); }

    /**
     * Status filter — id="admin-filter-status-select".
     * Valid values: "all", "PENDING", "CONFIRMED", "WAITLIST", "CANCELLED", "REJECTED", "EXPIRED"
     */
    public SelenideElement statusFilter()   { return $("#admin-filter-status-select"); }

    /**
     * Bulk approve button — data-testid="admin-bulk-approve".
     * Enabled only when filtered view contains at least one PENDING registration.
     */
    public SelenideElement bulkApproveBtn() { return $(byAttribute("data-testid", "admin-bulk-approve")); }

    /**
     * Scrolls the bulk-approve control to the middle of the viewport, then clicks.
     * {@code scrollIntoView(true)} (align to top) leaves the target under the sticky header so the
     * click can hit the nav bar (e.g. profile icon) instead of the button.
     */
    public void clickBulkApprove() {
        SafeUi.clickWhenReady(bulkApproveBtn(), Duration.ofSeconds(8), 2);
    }

    // ========== table ==========

    /** tbody rows from the registrations table */
    public ElementsCollection rows()        { return $$("table tbody tr"); }

    /**
     * Approve button inside a specific row — data-testid="admin-reg-approve".
     * Only rendered for PENDING rows.
     */
    public SelenideElement firstApproveButtonIn(SelenideElement row) {
        return row.$(byAttribute("data-testid", "admin-reg-approve"));
    }

    /**
     * Reject button inside a specific row — data-testid="admin-reg-reject".
     * Only rendered for PENDING rows.
     */
    public SelenideElement firstRejectButtonIn(SelenideElement row) {
        return row.$(byAttribute("data-testid", "admin-reg-reject"));
    }

    /** Any approve button currently in the DOM (useful for existence check) */
    public SelenideElement anyApproveButton() { return $(byAttribute("data-testid", "admin-reg-approve")); }

    // ========== notifications / banners ==========

    /**
     * Banner shown after bulk approve when some registrations were rejected due to full capacity.
     * Text: "Danh sách học viên bị từ chối do lớp đã đủ"
     */
    public SelenideElement bulkRejectedBanner() {
        return $(byText("Danh sách học viên bị từ chối do lớp đã đủ"));
    }

    /** Info/success message paragraph (emerald color) */
    public SelenideElement infoMessage() { return $("p.text-emerald-700"); }

    /** Error paragraph */
    public SelenideElement errorText()   { return $("p.text-red-600"); }

    // ========== stat cards ==========

    /**
     * "Chờ Duyệt" stat card (first card). The number inside is pendingRegs.length.
     * Clicking opens the InsightModal with pending detail.
     */
    public SelenideElement statCardPending()    { return $$("button.bg-white.border.border-emerald-100").get(0); }

    /** "Đã Xác Nhận" stat card */
    public SelenideElement statCardConfirmed()  { return $$("button.bg-white.border.border-emerald-100").get(1); }

    /** "Hàng Chờ" stat card */
    public SelenideElement statCardWaitlist()   { return $$("button.bg-white.border.border-emerald-100").get(2); }

    /** InsightModal dialog — shown after clicking a stat card */
    public SelenideElement insightModal()       { return $("div[role='dialog']"); }

    // ========== filter presets ==========

    /**
     * Set filter to "all courses" + "PENDING" status — standard preset for
     * approve/reject/bulk operations. Call before interacting with action buttons.
     */
    public void filterAllCoursesAndPendingStatus() {
        courseFilter().selectOptionByValue("all");
        statusFilter().selectOptionByValue("PENDING");
    }

    /**
     * Reset both filters to "all" — shows all registrations across all statuses and courses.
     */
    public void resetFilters() {
        courseFilter().selectOptionByValue("all");
        statusFilter().selectOptionByValue("all");
    }

    // ========== table cell helpers ==========

    /**
     * Parse registration ID from the first td of a row.
     * Cột bảng (AdminDashboard.tsx): ID | Khóa học | Học viên | Ngày | Trạng thái | Thao tác
     * Returns 0 if parsing fails (defensive — avoids NPE in callers).
     */
    public long parseRegistrationIdFromRow(SelenideElement row) {
        try {
            String text = row.$$("td").first().getText().trim();
            return Long.parseLong(text);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get the status text from a row's RegistrationStatusBadge (index 4 — khớp AdminDashboard.tsx).
     */
    public String parseStatusFromRow(SelenideElement row) {
        try {
            return row.$$("td").get(4).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }
}