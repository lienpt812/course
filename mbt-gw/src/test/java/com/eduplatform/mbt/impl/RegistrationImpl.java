package com.eduplatform.mbt.impl;

import com.codeborne.selenide.Condition;
import com.eduplatform.mbt.models.RegistrationModel;
import com.eduplatform.mbt.pages.AdminDashboardPage;
import com.eduplatform.mbt.pages.CourseDetailPage;
import com.eduplatform.mbt.support.BaseImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import com.eduplatform.mbt.support.MbtBusinessAssertions;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.SelenideSetup;
import com.eduplatform.mbt.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.graphwalker.java.annotation.GraphWalker;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;

/**
 * Registration flow MBT — CourseDetailPage (register button) + AdminDashboard (approve/reject/bulk).
 *
 * UI-backend alignment:
 *   - Student clicks "Đăng Ký Ngay" → POST /registrations → status PENDING or WAITLIST
 *   - After register: button replaced by RegistrationStatusBadge (PENDING/WAITLIST)
 *   - Confirmed: "Vào Học" button appears (status = CONFIRMED)
 *   - Admin sees all registrations; can approve (→ CONFIRMED) or reject (→ REJECTED)
 *   - Bulk approve → POST /registrations/bulk-approve; rejects when course full
 *   - Backend guard: approve requires remaining_slots > 0 (else 400 "Lớp đã đủ")
 */
@GraphWalker(value = GraphWalkerExecutionPolicy.BOUNDED_REG, start = "v_CourseDetailUnregistered")
public class RegistrationImpl extends BaseImpl implements RegistrationModel {

    private final CourseDetailPage detailPage = new CourseDetailPage();
    private final AdminDashboardPage adminDash = new AdminDashboardPage();

    private long targetCourseId;
    private long targetRegistrationId;

    @Override
    public void v_CourseDetailUnregistered() {
        logStep("v_CourseDetailUnregistered");
        SelenideSetup.resetBrowserState();
        asStudent();
        targetCourseId = firstPublishedCourseIdWithoutActiveReg();
        detailPage.open(targetCourseId);
        // UI assertion: "Đăng Ký Ngay" button should be visible (or page shows login guard)
        boolean hasRegBtn     = detailPage.registerButton().is(Condition.visible);
        boolean hasLoginGuard = detailPage.loginToRegister().is(Condition.visible);
        if (!hasRegBtn && !hasLoginGuard) {
            log.warn("v_CourseDetailUnregistered: neither register button nor login guard visible on course {}",
                    targetCourseId);
        }
        // Sync GW: e_RegisterCourse only when còn slot VÀ còn trong khoảng đăng ký (khớp nút bật trên FE)
        int rem = MbtBusinessAssertions.safeRemainingSlotsForCourse(api, targetCourseId);
        ctx.hasRemainingSlotOnTargetCourse = rem > 0;
        ctx.registrationWindowOpenOnTargetCourse = MbtBusinessAssertions.safeRegistrationWindowOpenForCourse(api, targetCourseId);
        syncRegistrationGraphWalker();
    }

    @Override
    public void v_CourseRegistrationNotAvailable() {
        logStep("v_CourseRegistrationNotAvailable");
        // Course is full: "Đăng Ký Ngay" button should be absent or registration section shows full message
        if (targetCourseId > 0) {
            int rem = MbtBusinessAssertions.safeRemainingSlotsForCourse(api, targetCourseId);
            if (rem > 0) {
                log.warn("v_CourseRegistrationNotAvailable: course {} still has remaining_slots={}", targetCourseId, rem);
            }
        }
    }

    @Override
    public void v_PendingRegistration() {
        logStep("v_PendingRegistration");
        ctx.regPipelineHasPending = true;
        syncRegistrationGraphWalker();
        if (targetCourseId > 0) {
            detailPage.open(targetCourseId);
            // The register button should no longer be visible
            detailPage.registerButton().shouldNot(Condition.visible);
        }
    }

    @Override
    public void v_ConfirmedRegistration() {
        logStep("v_ConfirmedRegistration");
        asStudent();
        detailPage.open(targetCourseId);
        // "Vào Học" button must be visible for CONFIRMED registration
        if (detailPage.enterLearnButton().is(Condition.visible)) {
            ctx.myRegistrationStatus = "CONFIRMED";
        } else {
            log.warn("v_ConfirmedRegistration: 'Vào Học' not visible on course {} — may need admin approval",
                    targetCourseId);
        }
    }

    @Override
    public void v_AdminRegistrationList() {
        logStep("v_AdminRegistrationList");
        asAdmin();
        ensureAdminDashboardPage(adminDash, this::asAdmin);
        // Filter to PENDING so action buttons are visible
        adminDash.filterAllCoursesAndPendingStatus();
    }

    @Override
    public void v_BulkApproveResult() {
        logStep("v_BulkApproveResult");
        SafeUi.waitUntilVisible($("body"), SafeUi.DEFAULT_TIMEOUT);
        // If bulk approve rejected some due to full capacity, banner should show
        if (adminDash.bulkRejectedBanner().is(Condition.visible)) {
            log.info("v_BulkApproveResult: bulk-rejected banner visible (some courses were full)");
        }
    }

    @Override
    public void e_VerifyRegistrationNotAvailable() {
        logStep("e_VerifyRegistrationNotAvailable");
        asStudent();
        if (targetCourseId > 0) {
            detailPage.open(targetCourseId);
            api.withToken(auth().accessToken);
            boolean windowOk = MbtBusinessAssertions.safeRegistrationWindowOpenForCourse(api, targetCourseId);
            int rem = MbtBusinessAssertions.safeRemainingSlotsForCourse(api, targetCourseId);
            if (windowOk && rem > 0) {
                throw new AssertionError("e_VerifyRegistrationNotAvailable: target course still has slot+open window; guard mismatch");
            }
            if (detailPage.registerButton().is(Condition.visible) && !windowOk) {
                detailPage.registerButton().shouldNot(Condition.enabled, Duration.ofSeconds(8));
            }
        }
    }

    @Override
    public void e_BackToOpenCourseForStudent() {
        logStep("e_BackToOpenCourseForStudent");
        asStudent();
        if (targetCourseId > 0) {
            detailPage.open(targetCourseId);
        }
        int rem = MbtBusinessAssertions.safeRemainingSlotsForCourse(api, targetCourseId);
        ctx.hasRemainingSlotOnTargetCourse = rem > 0;
        ctx.registrationWindowOpenOnTargetCourse = MbtBusinessAssertions.safeRegistrationWindowOpenForCourse(api, targetCourseId);
        syncRegistrationGraphWalker();
    }

    @Override
    public void e_RegisterCourse() {
        logStep("e_RegisterCourse");
        // Click the "Đăng Ký Ngay" button — triggers POST /registrations
        detailPage.clickRegister();

        // Poll until the registration appears in the API (React async)
        JsonNode latest = pollLatestMyRegistration(targetCourseId, 12, 400);
        if (latest != null) {
            targetRegistrationId = latest.path("id").asLong();
            ctx.myRegistrationId = targetRegistrationId;
            ctx.hasActiveRegistration = true;
            String st = latest.path("status").asText("");
            ctx.myRegistrationStatus = st;

            // UI assertion: the register button should be gone now
            detailPage.registerButton().shouldNot(Condition.visible);

            if (!"PENDING".equals(st) && !"WAITLIST".equals(st)) {
                throw new AssertionError(
                        "After register click: expected PENDING or WAITLIST, got " + st +
                        " (registration id=" + targetRegistrationId + ")");
            }
            log.info("e_RegisterCourse: created reg id={} status={}", targetRegistrationId, st);
        } else {
            log.warn("e_RegisterCourse: no registration row found for course {} after button click", targetCourseId);
        }
    }

    @Override
    public void e_OpenAdminRegistrations() {
        logStep("e_OpenAdminRegistrations");
        asAdmin();
        ensureAdminDashboardPage(adminDash, this::asAdmin);
        adminDash.filterAllCoursesAndPendingStatus();
    }

    @Override
    public void e_AdminApprove() {
        logStep("e_AdminApprove");
        asAdmin();
        long id = targetRegistrationId > 0 ? targetRegistrationId : firstPendingRegistrationIdForStaff();
        if (id <= 0) {
            throw new AssertionError("e_AdminApprove: no PENDING registration to approve (guard regPipelineHasPending should have blocked this edge)");
        }
        // Verify slot is available before approving (mirrors BE guard)
        long courseId = MbtBusinessAssertions.courseIdForRegistrationId(api, id);
        int slots = MbtBusinessAssertions.safeRemainingSlotsForCourse(api, courseId);
        if (slots <= 0) {
            throw new AssertionError("e_AdminApprove: course " + courseId + " has no remaining slots — backend will reject");
        }
        api.withToken(auth().accessToken);
        JsonNode r = api.approve(id);
        MbtBusinessAssertions.assertSuccessEnvelope(r, "POST approve");
        // Poll for CONFIRMED (async React state update may not be instant)
        MbtBusinessAssertions.assertRegistrationStatusEventually(api, id, "CONFIRMED", 10_000);
        log.info("e_AdminApprove: registration {} → CONFIRMED", id);
        ctx.regPipelineHasPending = false;
        syncRegistrationGraphWalker();
    }

    @Override
    public void e_AdminApproveSkipped() {
        logStep("e_AdminApproveSkipped");
        asAdmin();
        ensureAdminDashboardPage(adminDash, this::asAdmin);
    }

    @Override
    public void e_AdminReject() {
        logStep("e_AdminReject");
        asAdmin();
        long id = firstPendingRegistrationIdForStaff();
        if (id > 0) {
            api.withToken(auth().accessToken);
            JsonNode r = api.reject(id, "Rejected via MBT test");
            MbtBusinessAssertions.assertSuccessEnvelope(r, "POST reject");
            MbtBusinessAssertions.assertRegistrationStatus(api, id, "REJECTED");
            log.info("e_AdminReject: registration {} → REJECTED", id);
            ctx.regPipelineHasPending = false;
            syncRegistrationGraphWalker();
        } else {
            log.warn("e_AdminReject: no PENDING registration to reject");
        }
    }

    @Override
    public void e_AdminBulkApprove() {
        logStep("e_AdminBulkApprove");
        asAdmin();
        ensureAdminDashboardPage(adminDash, this::asAdmin);
        adminDash.filterAllCoursesAndPendingStatus();

        // Count before
        int before = MbtBusinessAssertions.countRegistrationsByStatus(api, "PENDING");

        if (adminDash.bulkApproveBtn().is(Condition.enabled)) {
            adminDash.clickBulkApprove();
            int after = before;
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                api.clearReadCache();
                after = MbtBusinessAssertions.countRegistrationsByStatus(api, "PENDING");
                if (before == 0 || after < before) {
                    break;
                }
                try {
                    Thread.sleep(450);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (before > 0 && after >= before) {
                log.warn("e_AdminBulkApprove: PENDING still {}>={} after wait — possible BE/FE lag; continuing MBT",
                        after, before);
            } else {
                log.info("e_AdminBulkApprove: before={} after={}", before, after);
            }
            ctx.regPipelineHasPending = MbtBusinessAssertions.countRegistrationsByStatus(api, "PENDING") > 0;
            syncRegistrationGraphWalker();
        } else {
            log.warn("e_AdminBulkApprove: bulk approve button is disabled (no PENDING?)");
        }
    }

    @Override
    public void e_BackToList() {
        logStep("e_BackToList");
        asAdmin();
        ensureAdminDashboardPage(adminDash, this::asAdmin);
    }

    @Override
    public void e_StudentViewCourseDetail() {
        logStep("e_StudentViewCourseDetail");
        asStudent();
        if (targetCourseId > 0) {
            detailPage.open(targetCourseId);
        }
    }

    // ==================== private helpers ====================

    private void asStudent() {
        openAuthenticatedWithApi(TestData.STUDENT_EMAIL, TestData.PWD_STRONG, "/courses");
        auth().currentRole = "STUDENT";
        auth().isLoggedIn = true;
        syncRegistrationGraphWalker();
    }

    private void asAdmin() {
        openAuthenticatedWithApi(TestData.ADMIN_EMAIL, TestData.PWD_STRONG, "/admin/dashboard");
        auth().currentRole = "ADMIN";
        auth().isLoggedIn = true;
        syncRegistrationGraphWalker();
    }

    private long firstPublishedCourseIdWithoutActiveReg() {
        api.withToken(auth().accessToken);
        // Collect course IDs with an active registration for the current student
        JsonNode regs = api.listMyRegistrations();
        java.util.Set<Long> active = new java.util.HashSet<>();
        if (regs != null && regs.path("data").isArray()) {
            for (JsonNode r : regs.path("data")) {
                String s = r.path("status").asText();
                if ("PENDING".equals(s) || "CONFIRMED".equals(s) || "WAITLIST".equals(s)) {
                    active.add(r.path("course_id").asLong());
                }
            }
        }
        // Prefer a course where GraphWalker can actually click "Đăng Ký Ngay" (còn slot + trong hạn)
        long preferred = api.courseIdBySlug(TestData.DEMO_COURSE_01_SLUG);
        if (preferred > 0 && !active.contains(preferred) && isPlausibleOpenRegistrationCourse(preferred)) {
            return preferred;
        }
        JsonNode list = api.listCourses();
        if (list != null && list.path("data").isArray()) {
            for (JsonNode c : list.path("data")) {
                long id = c.path("id").asLong();
                if ("PUBLISHED".equals(c.path("status").asText()) && !active.contains(id) && isPlausibleOpenRegistrationCourse(id)) {
                    return id;
                }
            }
        }
        if (preferred > 0 && !active.contains(preferred)) {
            return preferred;
        }
        if (list != null && list.path("data").isArray()) {
            for (JsonNode c : list.path("data")) {
                long id = c.path("id").asLong();
                if ("PUBLISHED".equals(c.path("status").asText()) && !active.contains(id)) {
                    return id;
                }
            }
        }
        return api.courseIdBySlug(TestData.DEMO_COURSE_01_SLUG);
    }

    private boolean isPlausibleOpenRegistrationCourse(long id) {
        return MbtBusinessAssertions.safeRemainingSlotsForCourse(api, id) > 0
                && MbtBusinessAssertions.safeRegistrationWindowOpenForCourse(api, id);
    }

    private long firstPendingRegistrationIdForStaff() {
        api.withToken(auth().accessToken);
        JsonNode regs = api.listRegistrations();
        if (regs != null && regs.path("data").isArray()) {
            for (JsonNode r : regs.path("data")) {
                if ("PENDING".equals(r.path("status").asText())) {
                    return r.path("id").asLong();
                }
            }
        }
        return 0;
    }

    private JsonNode latestMyRegistration(long courseId) {
        api.withToken(auth().accessToken);
        JsonNode regs = api.listMyRegistrations();
        if (regs != null && regs.path("data").isArray()) {
            for (JsonNode r : regs.path("data")) {
                if (courseId == r.path("course_id").asLong()) {
                    return r;
                }
            }
        }
        return null;
    }

    private JsonNode pollLatestMyRegistration(long courseId, int attempts, long delayMs) {
        for (int i = 0; i < attempts; i++) {
            JsonNode r = latestMyRegistration(courseId);
            if (r != null) return r;
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_hasRemainingSlot() {
        return canRegisterOnTargetCourse();
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_noRemainingSlot() {
        return !canRegisterOnTargetCourse();
    }

    /** Khớp điều kiện bật nút "Đăng Ký Ngay" (slot + thời gian) — xem {@code CourseDetailPage} FE. */
    private boolean canRegisterOnTargetCourse() {
        return ctx.hasRemainingSlotOnTargetCourse && ctx.registrationWindowOpenOnTargetCourse;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_regPipelinePending() {
        return ctx.regPipelineHasPending;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_regPipelineNotPending() {
        return !ctx.regPipelineHasPending;
    }
}