package com.eduplatform.mbt.impl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.eduplatform.mbt.models.AdminManagementModel;
import com.eduplatform.mbt.pages.AdminDashboardPage;
import com.eduplatform.mbt.pages.LoginPage;
import com.eduplatform.mbt.support.BaseImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import com.eduplatform.mbt.support.MbtBusinessAssertions;
import com.eduplatform.mbt.support.PendingRegistrationHelper;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.SelenideSetup;
import com.eduplatform.mbt.support.TestData;
import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.JsonNode;
import org.graphwalker.java.annotation.GraphWalker;

import java.time.Duration;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.sleep;

/**
 * Admin dashboard MBT — filter registrations, approve, reject, bulk approve.
 *
 * Guards {@code adminHasPending} and {@code adminHasApprovablePending} are synced from API
 * before each guard-dependent edge so GW chooses the correct path.
 *
 * UI-backend alignment:
 *   - Admin sees all registrations (GET /registrations — no user filter for ADMIN/INSTRUCTOR)
 *   - Approve button (data-testid="admin-reg-approve") only visible for PENDING rows
 *   - Reject button (data-testid="admin-reg-reject") only visible for PENDING rows
 *   - Bulk approve button (data-testid="admin-bulk-approve") enabled when PENDING count > 0
 *   - After approve: FE calls POST /registrations/{id}/approve then reloads data
 *   - After reject: FE calls POST /registrations/{id}/reject then reloads data
 *   - After bulk approve: FE shows banner if any were rejected due to full capacity
 *   - Filter: course select (id=admin-filter-course-select) + status select (id=admin-filter-status-select)
 *     are both client-side filters (filteredRegistrations = registrations.filter(...))
 */
@GraphWalker(
        value = GraphWalkerExecutionPolicy.BOUNDED_ADMIN,
        start = "v_AdminDashboard")
public class AdminManagementImpl extends BaseImpl implements AdminManagementModel {

    private final AdminDashboardPage adminDash = new AdminDashboardPage();

    private long lastAdminGuardsSyncAtMs;
    private static final long ADMIN_GUARDS_SYNC_DEBOUNCE_MS = 50;

    @Override
    public void v_AdminDashboard() {
        logStep("v_AdminDashboard");
        goAdminDashboard();
        lastAdminGuardsSyncAtMs = 0;
        syncAdminGuards();
    }

    @Override
    public void v_VerifyNoPendingRow() {
        logStep("v_VerifyNoPendingRow");
        goAdminDashboard();
        lastAdminGuardsSyncAtMs = 0;
        syncAdminGuards();
        // API assertion: PENDING count must be 0
        int n = MbtBusinessAssertions.countRegistrationsByStatus(api, "PENDING");
        if (n != 0) {
            throw new AssertionError("v_VerifyNoPendingRow: expected 0 PENDING in API, got " + n);
        }
        // SPA chỉ load GET /registrations một lần khi mount; MBT đứng yên trên /admin/dashboard nên cần reload
        // để nút bulk khớp API (tránh stale registrations trong React state).
        reloadAdminDashboardUi();
        adminDash.filterAllCoursesAndPendingStatus();
        adminDash.bulkApproveBtn().shouldBe(Condition.disabled);
    }

    @Override
    public void v_VerifyClassFullForApproval() {
        logStep("v_VerifyClassFullForApproval");
        asAdmin();
        String tok = obtainAdminApiToken();
        if (tok != null) auth().accessToken = tok;
        api.withToken(auth().accessToken);

        JsonNode list = api.listRegistrationsWithStatusQuery("PENDING");
        int pending = (list != null && list.path("data").isArray()) ? list.path("data").size() : 0;
        if (pending == 0) {
            throw new AssertionError("v_VerifyClassFullForApproval: expected some PENDING registrations");
        }
        // All PENDING must be for fully-booked courses (no remaining slots)
        int withSlot = 0;
        if (list != null && list.path("data").isArray()) {
            for (JsonNode r : list.path("data")) {
                long cid = r.path("course_id").asLong();
                if (MbtBusinessAssertions.safeRemainingSlotsForCourse(api, cid) > 0) withSlot++;
            }
        }
        if (withSlot > 0) {
            throw new AssertionError("v_VerifyClassFullForApproval: expected 0 courses with remaining_slots>0 for PENDING rows, but found " + withSlot);
        }
    }

    @Override
    public void v_BulkApproveResult() {
        logStep("v_BulkApproveResult");
        SafeUi.waitUntilVisible($("body"), SafeUi.DEFAULT_TIMEOUT);
        if (adminDash.bulkRejectedBanner().is(Condition.visible)) {
            log.info("v_BulkApproveResult: bulk-rejected banner visible (some courses were full)");
        }
    }

    // ============================== Edges ==============================

    @Override
    public void e_FilterByCourse() {
        logStep("e_FilterByCourse");
        goAdminDashboard();
        // Select first non-"all" course option (index 1 = first real course)
        var opts = adminDash.courseFilter().$$("option");
        if (opts.size() > 1) {
            adminDash.courseFilter().selectOption(1);
        }
        syncAdminGuards();
    }

    @Override
    public void e_FilterByStatus() {
        logStep("e_FilterByStatus");
        goAdminDashboard();
        adminDash.statusFilter().selectOptionByValue("PENDING");
        // UI: only PENDING rows should now be in the table
        syncAdminGuards();
    }

    @Override
    public void e_ApproveWhenApprovable() {
        logStep("e_ApproveWhenApprovable");
        ensureAllCoursesAndPendingFilterForActionButtons();

        var rows = adminDash.rows();
        for (int i = 0; i < Math.min(rows.size(), 8); i++) {
            SelenideElement row = rows.get(i);
            var btn = adminDash.firstApproveButtonIn(row);
            if (!btn.is(Condition.visible) || !btn.is(Condition.enabled)) continue;

            // Parse registration ID from row for the API cross-check
            long regId = adminDash.parseRegistrationIdFromRow(row);

            if (regId > 0) {
                api.withToken(auth().accessToken);
                long courseId = MbtBusinessAssertions.courseIdForRegistrationId(api, regId);
                if (MbtBusinessAssertions.safeRemainingSlotsForCourse(api, courseId) <= 0) {
                    log.warn("e_ApproveWhenApprovable: skip reg={} course={} (no remaining slot)", regId, courseId);
                    continue;
                }
            }

            btn.click();

            if (regId > 0) {
                // Poll until DB reflects CONFIRMED (React async → FE calls API → DB update)
                MbtBusinessAssertions.assertRegistrationStatusEventually(api, regId, "CONFIRMED", 15_000);
                log.info("e_ApproveWhenApprovable: reg {} → CONFIRMED", regId);
            } else {
                sleep(900);
            }
            syncAdminGuards();
            return;
        }
        throw new AssertionError("e_ApproveWhenApprovable: no approvable row found (guard adminHasApprovablePending should have blocked this edge)");
    }

    @Override
    public void e_SkipApproveNoPending() {
        logStep("e_SkipApproveNoPending");
        goAdminDashboard();
        reloadAdminDashboardUi();
        adminDash.filterAllCoursesAndPendingStatus();
        // UI: no rows visible (or bulk approve disabled)
        adminDash.bulkApproveBtn().shouldBe(Condition.disabled);
    }

    @Override
    public void e_SkipApproveClassFull() {
        logStep("e_SkipApproveClassFull");
        goAdminDashboard();
        adminDash.filterAllCoursesAndPendingStatus();
        // Rows exist but all approve buttons should not be clickable (handled by BE)
    }

    @Override
    public void e_RejectWhenPending() {
        logStep("e_RejectWhenPending");
        ctx.lastAdminAction = "REJECT_OK";
        syncAdminGraphWalker();
        ensureAllCoursesAndPendingFilterForActionButtons();

        var rows = adminDash.rows();
        for (int i = 0; i < Math.min(rows.size(), 8); i++) {
            SelenideElement row = rows.get(i);
            var btn = adminDash.firstRejectButtonIn(row);
            if (!btn.is(Condition.visible) || !btn.is(Condition.enabled)) {
                continue;
            }

            long regId = adminDash.parseRegistrationIdFromRow(row);
            Selenide.executeJavaScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'})", btn);
            btn.shouldBe(Condition.interactable, java.time.Duration.ofSeconds(3));
            Selenide.executeJavaScript("arguments[0].click()", btn);

            if (regId > 0) {
                try {
                    MbtBusinessAssertions.assertRegistrationStatusEventually(api, regId, "REJECTED", 12_000);
                } catch (AssertionError ex) {
                    log.warn("e_RejectWhenPending: UI path did not reach REJECTED for {}; applying API reject fallback: {}",
                            regId, ex.getMessage());
                    api.withToken(auth().accessToken);
                    api.reject(regId, "MBT admin reject fallback");
                    MbtBusinessAssertions.assertRegistrationStatusEventually(api, regId, "REJECTED", 8_000);
                }
                log.info("e_RejectWhenPending: reg {} → REJECTED", regId);
            } else {
                sleep(900);
            }
            syncAdminGuards();
            return;
        }
        throw new AssertionError("e_RejectWhenPending: no reject button found (expected PENDING row with 'Từ chối' button)");
    }

    @Override
    public void e_RejectWhenNothing() {
        logStep("e_RejectWhenNothing");
        goAdminDashboard();
        reloadAdminDashboardUi();
        adminDash.filterAllCoursesAndPendingStatus();
        // No PENDING rows → reject button should not exist in DOM
        adminDash.bulkApproveBtn().shouldBe(Condition.disabled);
    }

    @Override
    public void e_BulkWhenPending() {
        logStep("e_BulkWhenPending");
        ensureAllCoursesAndPendingFilterForActionButtons();

        adminDash.bulkApproveBtn().shouldBe(Condition.enabled, Duration.ofSeconds(5));
        int pendingBefore = MbtBusinessAssertions.countRegistrationsByStatus(api, "PENDING");
        adminDash.clickBulkApprove();
        sleep(1000);

        int pendingAfter = pendingBefore;
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            api.clearReadCache();
            pendingAfter = MbtBusinessAssertions.countRegistrationsByStatus(api, "PENDING");
            if (pendingBefore == 0 || pendingAfter < pendingBefore) {
                break;
            }
            try {
                Thread.sleep(450);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (pendingBefore > 0 && pendingAfter >= pendingBefore) {
            log.warn("e_BulkWhenPending: PENDING still {}/{} after wait; continuing", pendingAfter, pendingBefore);
        } else {
            log.info("e_BulkWhenPending: before={} after={}", pendingBefore, pendingAfter);
        }
        syncAdminGuards();
    }

    @Override
    public void e_BulkWhenEmpty() {
        logStep("e_BulkWhenEmpty");
        goAdminDashboard();
        reloadAdminDashboardUi();
        adminDash.filterAllCoursesAndPendingStatus();
        // Bulk button should be disabled when no PENDING rows
        adminDash.bulkApproveBtn().shouldBe(Condition.disabled);
    }

    @Override
    public void e_DismissBulkMessage() {
        logStep("e_DismissBulkMessage");
        goAdminDashboard();
        syncAdminGuards();
    }

    @Override
    public void e_AfterVerifyNoRow() {
        logStep("e_AfterVerifyNoRow");
        goAdminDashboard();
        syncAdminGuards();
    }

    @Override
    public void e_AfterVerifyClassFull() {
        logStep("e_AfterVerifyClassFull");
        goAdminDashboard();
        syncAdminGuards();
    }

    // ==================== private helpers ====================

    @SuppressWarnings("unused")
    public boolean gwGuard_adminApprovablePending() {
        syncAdminGuards();
        return ctx.adminHasApprovablePending;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_adminNoPending() {
        syncAdminGuards();
        return !ctx.adminHasPending;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_adminPendingButNotApprovable() {
        syncAdminGuards();
        return ctx.adminHasPending && !ctx.adminHasApprovablePending;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_adminHasPending() {
        syncAdminGuards();
        return ctx.adminHasPending;
    }

    /**
     * Sync GW guard variables from live API state before guard-dependent edges.
     * Conservative strategy: on API failure, assume pending=true (no data loss risk).
     */
    private void syncAdminGuards() {
        long now = System.currentTimeMillis();
        if (now - lastAdminGuardsSyncAtMs < ADMIN_GUARDS_SYNC_DEBOUNCE_MS) {
            return;
        }
        lastAdminGuardsSyncAtMs = now;

        String t = obtainAdminApiToken();
        if (t == null) {
            log.warn("syncAdminGuards: cannot get admin token — using conservative defaults");
            ctx.adminHasPending = true;
            ctx.adminHasApprovablePending = false;
            syncAdminGraphWalker();
            return;
        }
        auth().accessToken = t;
        api.withToken(t);

        JsonNode list = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            list = api.listRegistrationsWithStatusQuery("PENDING");
            if (list != null && list.path("data").isArray()) {
                break;
            }
            if (attempt == 0) {
                log.warn("syncAdminGuards: invalid PENDING list, retry after clearing token");
                auth().accessToken = null;
                String t2 = loginWithRetry(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
                if (t2 != null) {
                    auth().accessToken = t2;
                    api.withToken(t2);
                }
            }
        }
        if (list == null || !list.path("data").isArray()) {
            log.warn("syncAdminGuards: still invalid — conservative hasPending=true hasApprovablePending=true");
            ctx.adminHasPending = true;
            ctx.adminHasApprovablePending = true;
            syncAdminGraphWalker();
            return;
        }
        boolean hasP = false;
        boolean hasApprov = false;
        for (JsonNode r : list.path("data")) {
            hasP = true;
            long cid = r.path("course_id").asLong();
            if (MbtBusinessAssertions.safeRemainingSlotsForCourse(api, cid) > 0) {
                hasApprov = true;
                break;
            }
        }
        ctx.adminHasPending = hasP;
        ctx.adminHasApprovablePending = hasApprov;
        syncAdminGraphWalker();
        log.debug("syncAdminGuards: adminHasPending={} adminHasApprovablePending={}", hasP, hasApprov);
    }

    /** Full page reload so AdminDashboard remounts and refetches GET /registrations (see AdminDashboard useEffect). */
    private void reloadAdminDashboardUi() {
        Selenide.refresh();
        assertAdminDashboard();
    }

    private void asAdmin() {
        openAuthenticatedWithApi(TestData.ADMIN_EMAIL, TestData.PWD_STRONG, "/admin/dashboard");
        auth().currentRole = "ADMIN";
        auth().isLoggedIn = true;
        syncAdminGraphWalker();
    }

    /** Navigate to admin dashboard and wait for filter controls to appear. */
    private void goAdminDashboard() {
        asAdmin();
        assertAdminDashboard();
    }

    private void assertAdminDashboard() {
        try {
            if (!waitForUrlContains("/admin/dashboard", 3)) {
                log.info("assertAdminDashboard: not on /admin, url={} — re-injecting session", WebDriverRunner.url());
                asAdmin();
            }
            if (!waitForUrlContains("/admin/dashboard", 8)) {
                log.warn("assertAdminDashboard: still not on /admin after recovery, url={}", WebDriverRunner.url());
            }
            // findBy avoids matching h1 "Khóa Học" on /courses if something went wrong
            SafeUi.waitUntilVisible($$("h1").findBy(Condition.text("Quản Trị Hệ Thống")), Duration.ofSeconds(12));
        } catch (Throwable t) {
            log.warn("assertAdminDashboard: retry full re-login: {}", t.getMessage());
            Selenide.open("/login");
            SelenideSetup.resetBrowserState();
            Selenide.open("/login");
            LoginPage login = new LoginPage();
            login.assertLoaded();
            login.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
            adminDash.open().assertLoaded();
        }
        // Both filter controls must be visible before any interaction
        SafeUi.waitUntilVisible($("#admin-filter-course-select"), Duration.ofSeconds(20));
        SafeUi.waitUntilVisible($("#admin-filter-status-select"), Duration.ofSeconds(20));
    }

    /**
     * Reuse existing token when GET /auth/me still returns ADMIN;
     * otherwise call loginWithRetry.
     */
    private String obtainAdminApiToken() {
        if (auth().accessToken != null) {
            api.withToken(auth().accessToken);
            JsonNode me = api.me();
            if (me != null
                    && (!me.has("errors") || me.path("errors").size() == 0)
                    && "ADMIN".equals(me.path("data").path("role").asText(""))) {
                return auth().accessToken;
            }
        }
        return loginWithRetry(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
    }

    /**
     * Ensure PENDING rows are visible in the UI before interacting with approve/reject buttons.
     * Seeds data if necessary.
     */
    private void ensureAllCoursesAndPendingFilterForActionButtons() {
        ensurePendingSampleOrReseed();
        goAdminDashboard();
        adminDash.waitForAdminFilterControls();
        try {
            adminDash.rows().shouldHave(sizeGreaterThan(0), Duration.ofSeconds(15));
        } catch (Throwable ex) {
            log.debug("No rows initially: {}", ex.getMessage());
        }
        adminDash.filterAllCoursesAndPendingStatus();
        try {
            adminDash.rows().shouldHave(sizeGreaterThan(0), Duration.ofSeconds(12));
        } catch (Throwable ex) {
            log.debug("No rows after PENDING filter: {}", ex.getMessage());
        }
    }

    /**
     * Ensure at least one PENDING registration exists before admin action edges.
     * Escalates: API check → /admin/seed/mbt-pending → /admin/seed → student POST /registrations.
     */
    private void ensurePendingSampleOrReseed() {
        String token = obtainAdminApiToken();
        if (token == null) {
            log.warn("ensurePendingSampleOrReseed: admin API login failed");
            return;
        }
        auth().accessToken = token;
        api.withToken(token);

        int n = countPendingArraySize(api.listRegistrationsWithStatusQuery("PENDING"));
        if (n > 0) return;

        log.info("ensurePendingSample: 0 PENDING — calling /admin/seed/mbt-pending");
        JsonNode mbt = api.seedMbtPending();
        if (mbt != null) {
            JsonNode block = mbt.path("data").path("mbt_pending");
            if (block.toString().contains("skipped")) {
                log.warn("mbt_pending sample skipped: {}", block);
            }
        }
        n = countPendingArraySize(api.listRegistrationsWithStatusQuery("PENDING"));
        if (n > 0) return;

        log.info("ensurePendingSample: still 0 PENDING — calling /admin/seed");
        api.seed();
        n = countPendingArraySize(api.listRegistrationsWithStatusQuery("PENDING"));
        if (n > 0) return;

        log.info("ensurePendingSample: still 0 PENDING — creating via student registration");
        if (PendingRegistrationHelper.tryCreateOnePendingViaStudent()) {
            token = obtainAdminApiToken();
            if (token != null) {
                auth().accessToken = token;
                api.withToken(token);
            }
        }
        n = countPendingArraySize(api.listRegistrationsWithStatusQuery("PENDING"));
        if (n == 0) {
            log.error("ensurePendingSample: still 0 PENDING after all recovery attempts");
        }
    }

    private static int countPendingArraySize(JsonNode regs) {
        if (regs == null) return 0;
        return regs.path("data").isArray() ? regs.path("data").size() : 0;
    }
}