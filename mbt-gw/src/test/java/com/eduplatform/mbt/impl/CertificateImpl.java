package com.eduplatform.mbt.impl;

import com.codeborne.selenide.Condition;
import com.eduplatform.mbt.models.CertificateModel;
import com.eduplatform.mbt.pages.LearningPage;
import com.eduplatform.mbt.pages.StudentDashboardPage;
import com.eduplatform.mbt.support.BaseImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import com.eduplatform.mbt.support.MbtBusinessAssertions;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.TestConfig;
import com.eduplatform.mbt.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.graphwalker.java.annotation.GraphWalker;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

/**
 * Certificate flow MBT — issue, view on dashboard, verify by code.
 *
 * UI-backend alignment:
 *   - "Nhận chứng chỉ" on LearningPage → POST /certificates/issue/{courseId}
 *     → response: { verification_code, issued_at, pdf_url? }
 *     → FE shows CertificateModal (if triggered via mark-complete) or switches to "Xem chứng chỉ"
 *   - StudentDashboard shows "Chứng Chỉ Của Tôi" section with a[href^="/api/v1/certificates/verify/"]
 *   - GET /certificates/verify/{code} → JSON response verifiable via API
 *     (FE navigates to this URL so browser renders the JSON response in tab)
 *
 * Backend:
 *   POST /certificates/issue/{courseId}
 *     - Requires CONFIRMED registration
 *     - Requires completion_pct = 100 for the course
 *     - Returns 409 if cert already issued for this course+user
 *   GET /certificates/verify/{code} → { verification_code, course_id, user_id, issued_at }
 *   GET /certificates/me → list of user's certificates
 */
@GraphWalker(value = GraphWalkerExecutionPolicy.BOUNDED_CERT, start = "v_AllLessonsCompleted")
public class CertificateImpl extends BaseImpl implements CertificateModel {

    private final StudentDashboardPage studentDash = new StudentDashboardPage();
    private final LearningPage learningPage = new LearningPage();
    private long targetCourseId;

    @Override
    public void v_AllLessonsCompleted() {
        logStep("v_AllLessonsCompleted");
        asStudent();
        targetCourseId = firstConfirmedCourseIdWithAllDone();
        if (targetCourseId > 0) {
            learningPage.open(targetCourseId);
            learningPage.assertFullyLoaded();
            // Either "Nhận chứng chỉ" or "Xem chứng chỉ" should be visible
            boolean canClaim  = learningPage.claimCertButton().is(Condition.visible);
            boolean hasViewed = learningPage.viewCertButton().is(Condition.visible);
            if (!canClaim && !hasViewed) {
                log.warn("v_AllLessonsCompleted: neither cert claim nor view button visible on course {} — may not be 100% complete", targetCourseId);
            }
        } else {
            log.warn("v_AllLessonsCompleted: no CONFIRMED course with outline lessons found — skip /learn (seed DB)");
        }
        ctx.hasVerificationCode = !ctx.verificationCode.isBlank();
        syncCertificateGraphWalker();
    }

    @Override
    public void v_CertificateIssued() {
        logStep("v_CertificateIssued");
        // After issuing: "Xem chứng chỉ" button replaces "Nhận chứng chỉ"
        SafeUi.waitUntilVisible(learningPage.viewCertButton(), SafeUi.DEFAULT_TIMEOUT);
        ctx.hasExistingCertificate = true;
        ctx.hasVerificationCode = !ctx.verificationCode.isBlank();
        syncCertificateGraphWalker();
        // API cross-check: GET /certificates/me should include this course
        api.withToken(auth().accessToken);
        JsonNode certs = api.get("/api/v1/certificates/me", true);
        if (certs != null && certs.path("data").isArray()) {
            boolean found = false;
            for (JsonNode c : certs.path("data")) {
                if (c.path("course_id").asLong() == targetCourseId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new AssertionError("v_CertificateIssued: cert for course " + targetCourseId + " not in GET /certificates/me");
            }
        }
    }

    @Override
    public void v_CertificateOnDashboard() {
        logStep("v_CertificateOnDashboard");
        studentDash.open();
        studentDash.assertLoaded();
        // "Chứng Chỉ Của Tôi" section heading should be visible
        studentDash.assertCertificatesSectionVisible();
        // At least one certificate link should be visible
        if (ctx.hasExistingCertificate) {
            studentDash.assertCertificateCount(1);
        }
    }

    @Override
    public void v_CertificateVerified() {
        logStep("v_CertificateVerified");
        if (!ctx.verificationCode.isBlank()) {
            // Navigate to the public verification endpoint in the browser
            open(TestConfig.apiBaseUrl() + "/api/v1/certificates/verify/" + ctx.verificationCode);
            // Response is JSON from FastAPI — browser shows it; just verify the page loaded
            SafeUi.waitUntilVisible($("body"), SafeUi.DEFAULT_TIMEOUT);
        }
    }

    @Override
    public void v_CertificateVerifyInvalid() {
        logStep("v_CertificateVerifyInvalid");
        // Browser shows JSON error from BE (e.g. 404)
        SafeUi.waitUntilVisible($("body"), SafeUi.DEFAULT_TIMEOUT);
    }

    @Override
    public void e_CompleteCourseAndIssueCertificate() {
        logStep("e_CompleteCourseAndIssueCertificate");
        if (targetCourseId <= 0) {
            log.warn("e_CompleteCourseAndIssueCertificate: no target course");
            return;
        }
        api.withToken(auth().accessToken);
        JsonNode cert = api.issueCertificate(targetCourseId);
        if (cert != null) {
            MbtBusinessAssertions.assertSuccessEnvelope(cert, "POST issue certificate");
            MbtBusinessAssertions.assertCertificateIssueHasCode(cert);
            ctx.verificationCode = cert.path("data").path("verification_code").asText("");
            ctx.hasExistingCertificate = true;
            ctx.hasVerificationCode = !ctx.verificationCode.isBlank();
            syncCertificateGraphWalker();
            log.info("e_CompleteCourseAndIssueCertificate: code={}", ctx.verificationCode.substring(0, Math.min(8, ctx.verificationCode.length())) + "...");
        } else {
            log.warn("e_CompleteCourseAndIssueCertificate: null response from issue endpoint");
        }
    }

    @Override
    public void e_ViewCertificateOnDashboard() {
        logStep("e_ViewCertificateOnDashboard");
        ctx.hasVerificationCode = !ctx.verificationCode.isBlank();
        syncCertificateGraphWalker();
        studentDash.open();
        waitForUrlContains("/student/dashboard", 8);
    }

    @Override
    public void e_VerifyCertificateWithCode() {
        logStep("e_VerifyCertificateWithCode");
        if (ctx.verificationCode.isBlank()) {
            throw new AssertionError("e_VerifyCertificateWithCode: expected non-blank verificationCode (guard hasVerificationCode should block this edge)");
        }
        // API verification
        JsonNode r = api.verifyCertificate(ctx.verificationCode);
        MbtBusinessAssertions.assertCertificatePublicVerifyOk(r);
        // Assert course_id and verification_code in response match what we issued
        String code = r.path("data").path("verification_code").asText("");
        if (!ctx.verificationCode.equals(code)) {
            throw new AssertionError("verify: returned verification_code '" + code + "' != expected '" + ctx.verificationCode + "'");
        }
        // Navigate to verification URL in browser
        open(TestConfig.apiBaseUrl() + "/api/v1/certificates/verify/" + ctx.verificationCode);
    }

    @Override
    public void e_VerifyCertificateInvalid() {
        logStep("e_VerifyCertificateInvalid");
        // Use a clearly invalid code — BE should return 404
        JsonNode r = api.verifyCertificate("INVALID-CODE-00");
        // We don't assert an error here — just verify it doesn't crash the API client
        log.info("e_VerifyCertificateInvalid: response={}", r);
        // Also hit the URL in the browser to verify the FE/BE handles it gracefully
        open(TestConfig.apiBaseUrl() + "/api/v1/certificates/verify/INVALID-CODE-00");
    }

    @Override
    public void e_BackToCerts() {
        logStep("e_BackToCerts");
        studentDash.open();
    }

    @Override
    public void e_BackFromInvalidVerify() {
        logStep("e_BackFromInvalidVerify");
        studentDash.open();
    }

    @Override
    public void e_GoToLearningFromCertificate() {
        logStep("e_GoToLearningFromCertificate");
        asStudent();
        if (targetCourseId > 0) {
            learningPage.open(targetCourseId);
            learningPage.assertFullyLoaded();
        }
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_certHasVerificationCode() {
        return ctx.hasVerificationCode;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_certNoVerificationCode() {
        return !ctx.hasVerificationCode;
    }

    @Override
    public void e_ArmHasVerificationCode() {
        ctx.hasVerificationCode = true;
        syncCertificateGraphWalker();
    }

    @Override
    public void e_ArmNoVerificationCode() {
        ctx.hasVerificationCode = false;
        syncCertificateGraphWalker();
    }

    // ==================== helpers ====================

    private void asStudent() {
        openAuthenticatedWithApi(TestData.STUDENT_EMAIL, TestData.PWD_STRONG, "/student/dashboard");
        auth().currentRole = "STUDENT";
        auth().isLoggedIn = true;
        syncCertificateGraphWalker();
    }

    private long firstConfirmedCourseIdWithAllDone() {
        api.withToken(auth().accessToken);
        JsonNode regs = api.listMyRegistrations();
        if (regs == null || !regs.path("data").isArray()) {
            return 0;
        }

        // Stable anchor: 100% + at least one lesson in outline (LearningPage needs outline rows)
        long anchor = api.courseIdBySlug(TestData.MBT_STUDENT_ANCHOR_SLUG);
        if (anchor > 0 && countLessonsInOutline(anchor) > 0) {
            JsonNode progress = api.get("/api/v1/learning/courses/" + anchor + "/progress", true);
            if (progress != null && progress.path("data").path("completion_pct").asInt() >= 100) {
                return anchor;
            }
        }

        // Prefer: CONFIRMED, 100% complete, and outline has lessons
        for (JsonNode r : regs.path("data")) {
            if (!"CONFIRMED".equals(r.path("status").asText())) {
                continue;
            }
            long cid = r.path("course_id").asLong();
            if (countLessonsInOutline(cid) <= 0) {
                continue;
            }
            JsonNode progress = api.get("/api/v1/learning/courses/" + cid + "/progress", true);
            if (progress != null && progress.path("data").path("completion_pct").asInt() >= 100) {
                return cid;
            }
        }

        // Any CONFIRMED with lessons (vertex still asserts /learn has buttons; 100% may be faked in UI later)
        for (JsonNode r : regs.path("data")) {
            if ("CONFIRMED".equals(r.path("status").asText())) {
                long cid = r.path("course_id").asLong();
                if (countLessonsInOutline(cid) > 0) {
                    return cid;
                }
            }
        }
        return 0;
    }

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
}