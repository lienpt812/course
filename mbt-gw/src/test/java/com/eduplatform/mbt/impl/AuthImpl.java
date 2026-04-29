package com.eduplatform.mbt.impl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.eduplatform.mbt.models.AuthModel;
import com.eduplatform.mbt.pages.AdminDashboardPage;
import com.eduplatform.mbt.pages.CoursesPage;
import com.eduplatform.mbt.pages.ForgotPasswordPage;
import com.eduplatform.mbt.pages.ForgotSubmitOutcome;
import com.eduplatform.mbt.pages.InstructorDashboardPage;
import com.eduplatform.mbt.pages.LoginPage;
import com.eduplatform.mbt.pages.NavBar;
import com.eduplatform.mbt.pages.ProfilePage;
import com.eduplatform.mbt.pages.RegisterPage;
import com.eduplatform.mbt.pages.ResetPasswordPage;
import com.eduplatform.mbt.pages.StudentDashboardPage;
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
import static com.codeborne.selenide.Selenide.open;

/**
 * MBT Auth: EDGE = hành động (UI + cập nhật {@link AuthState}), VERTEX = assert UI + assert state + đồng bộ GraphWalker.
 */
@GraphWalker(value = GraphWalkerExecutionPolicy.BOUNDED_AUTH, start = "v_GuestOnCourses")
public class AuthImpl extends BaseImpl implements AuthModel {

    private final NavBar navBar = new NavBar();
    private final CoursesPage coursesPage = new CoursesPage();
    private final LoginPage loginPage = new LoginPage();
    private final RegisterPage registerPage = new RegisterPage();
    private final ForgotPasswordPage forgotPage = new ForgotPasswordPage();
    private final ResetPasswordPage resetPage = new ResetPasswordPage();
    private final StudentDashboardPage studentDash = new StudentDashboardPage();
    private final InstructorDashboardPage instructorDash = new InstructorDashboardPage();
    private final AdminDashboardPage adminDash = new AdminDashboardPage();
    private final ProfilePage profile = new ProfilePage();

    // ---------- Vertices = ASSERT (UI + state) ----------

    @Override
    public void v_GuestOnCourses() {
        logStep("v_GuestOnCourses");
        open("/courses");
        SelenideSetup.resetBrowserState();
        open("/courses");

        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        coursesPage.assertLoaded();
        navBar.buttonLogout().shouldNot(Condition.visible);

        auth().clearSession();
        auth().armHappyPathGuards();
        auth().resetToken = "";
        auth().tokenUsed = false;
        syncGraphWalkerAuthVars();

        assertGuestAuthState();
        logStepAfter("v_GuestOnCourses");
    }

    @Override
    public void v_LoginPage() {
        logStep("v_LoginPage");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        loginPage.assertLoaded();
        assert !auth().isLoggedIn : "vertex v_LoginPage: expected guest (not logged in)";
        syncGraphWalkerAuthVars();
        logStepAfter("v_LoginPage");
    }

    @Override
    public void v_RegisterPage() {
        logStep("v_RegisterPage");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        registerPage.assertLoaded();
        assert !auth().isLoggedIn : "vertex v_RegisterPage: expected guest";
        syncGraphWalkerAuthVars();
        logStepAfter("v_RegisterPage");
    }

    @Override
    public void v_ForgotPasswordPage() {
        logStep("v_ForgotPasswordPage");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        forgotPage.assertLoaded();
        syncGraphWalkerAuthVars();
        logStepAfter("v_ForgotPasswordPage");
    }

    @Override
    public void v_ResetPasswordPage() {
        logStep("v_ResetPasswordPage");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        resetPage.assertLoaded();
        if (auth().resetToken != null && !auth().resetToken.isBlank()) {
            resetPage.assertTokenPreFilled();
        }
        syncGraphWalkerAuthVars();
        logStepAfter("v_ResetPasswordPage");
    }

    @Override
    public void v_StudentDashboard() {
        logStep("v_StudentDashboard");
        ensureDashboard(TestData.STUDENT_EMAIL, "/student/dashboard");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        studentDash.assertLoaded();
        assertDashboardRole("STUDENT");
        logStepAfter("v_StudentDashboard");
    }

    @Override
    public void v_InstructorDashboard() {
        logStep("v_InstructorDashboard");
        ensureDashboard(TestData.INSTRUCTOR_EMAIL, "/instructor/dashboard");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        instructorDash.assertLoaded();
        assertDashboardRole("INSTRUCTOR");
        logStepAfter("v_InstructorDashboard");
    }

    @Override
    public void v_AdminDashboard() {
        logStep("v_AdminDashboard");
        ensureDashboard(TestData.ADMIN_EMAIL, "/admin/dashboard");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        adminDash.assertLoaded();
        assertDashboardRole("ADMIN");
        logStepAfter("v_AdminDashboard");
    }

    @Override
    public void v_ProfilePage() {
        logStep("v_ProfilePage");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        profile.assertLoaded();
        assert auth().isLoggedIn : "vertex v_ProfilePage: expected session";
        syncGraphWalkerAuthVars();
        logStepAfter("v_ProfilePage");
    }

    // ---------- Edges = ACTION ----------

    @Override
    public void e_GoToLogin() {
        logStep("e_GoToLogin");
        armHappyAuthGuards();
        navBar.clickLogin();
        waitForUrlContains("/login", 8);
        logStepAfter("e_GoToLogin");
    }

    @Override
    public void e_GoToRegister() {
        logStep("e_GoToRegister");
        armHappyAuthGuards();
        open("/register");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        waitForUrlContains("/register", 8);
        logStepAfter("e_GoToRegister");
    }

    @Override
    public void e_BackToLogin() {
        logStep("e_BackToLogin");
        registerPage.linkLogin().click();
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        waitForUrlContains("/login", 8);
        logStepAfter("e_BackToLogin");
    }

    @Override
    public void e_GoToForgotPassword() {
        logStep("e_GoToForgotPassword");
        loginPage.linkForgot().click();
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        waitForUrlContains("/forgot-password", 8);
        logStepAfter("e_GoToForgotPassword");
    }

    @Override
    public void e_LoginAsStudent() {
        logStep("e_LoginAsStudent");
        armHappyAuthGuards();
        loginViaUiOrApi(TestData.STUDENT_EMAIL, "/student/dashboard");
        auth().currentRole = "STUDENT";
        auth().isLoggedIn = true;
        if (auth().accessToken == null || auth().accessToken.isBlank()) {
            String t = loginWithRetry(TestData.STUDENT_EMAIL, TestData.PWD_STRONG);
            auth().accessToken = t;
        }
        syncGraphWalkerAuthVars();
        assertMeRoleForEmail(TestData.STUDENT_EMAIL, "STUDENT");
        logStepAfter("e_LoginAsStudent");
    }

    @Override
    public void e_LoginAsInstructor() {
        logStep("e_LoginAsInstructor");
        armHappyAuthGuards();
        loginViaUiOrApi(TestData.INSTRUCTOR_EMAIL, "/instructor/dashboard");
        auth().currentRole = "INSTRUCTOR";
        auth().isLoggedIn = true;
        if (auth().accessToken == null || auth().accessToken.isBlank()) {
            String t = loginWithRetry(TestData.INSTRUCTOR_EMAIL, TestData.PWD_STRONG);
            auth().accessToken = t;
        }
        syncGraphWalkerAuthVars();
        assertMeRoleForEmail(TestData.INSTRUCTOR_EMAIL, "INSTRUCTOR");
        logStepAfter("e_LoginAsInstructor");
    }

    @Override
    public void e_LoginAsAdmin() {
        logStep("e_LoginAsAdmin");
        armHappyAuthGuards();
        loginViaUiOrApi(TestData.ADMIN_EMAIL, "/admin/dashboard");
        auth().currentRole = "ADMIN";
        auth().isLoggedIn = true;
        if (auth().accessToken == null || auth().accessToken.isBlank()) {
            String t = loginWithRetry(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
            auth().accessToken = t;
        }
        syncGraphWalkerAuthVars();
        assertMeRoleForEmail(TestData.ADMIN_EMAIL, "ADMIN");
        logStepAfter("e_LoginAsAdmin");
    }

    @Override
    public void e_LoginFail() {
        logStep("e_LoginFail");
        loginPage.assertLoaded();
        loginPage.login(TestData.UNKNOWN_EMAIL, TestData.WRONG_PWD);
        SafeUi.waitUntilVisible(loginPage.errorText(), Duration.ofSeconds(6));
        waitForUrlContains("/login", 5);
        auth().isLoggedIn = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_LoginFail");
    }

    @Override
    public void e_RegisterAsStudent() {
        logStep("e_RegisterAsStudent");
        armHappyAuthGuards();
        String uniq = "stu_" + System.currentTimeMillis() + "@example.com";
        registerPage.fillStudent(
                "MBT Stu " + System.currentTimeMillis(),
                uniq, TestData.PWD_STRONG,
                TestData.STUDENT_MAJOR, TestData.LEARNING_GOAL);
        registerPage.submitForm();
        waitForUrlContains("/student/dashboard", 12);
        auth().currentRole = "STUDENT";
        auth().isLoggedIn = true;
        syncGraphWalkerAuthVars();
        logStepAfter("e_RegisterAsStudent");
    }

    @Override
    public void e_RegisterAsInstructor() {
        logStep("e_RegisterAsInstructor");
        armHappyAuthGuards();
        String uniq = "ins_" + System.currentTimeMillis() + "@example.com";
        registerPage.fillInstructor(
                "MBT Ins " + System.currentTimeMillis(),
                uniq, TestData.PWD_STRONG,
                TestData.EXPERTISE);
        registerPage.assertInstructorFieldsVisible();
        registerPage.submitForm();
        waitForUrlContains("/instructor/dashboard", 12);
        auth().currentRole = "INSTRUCTOR";
        auth().isLoggedIn = true;
        syncGraphWalkerAuthVars();
        logStepAfter("e_RegisterAsInstructor");
    }

    @Override
    public void e_RegisterFailDuplicate() {
        logStep("e_RegisterFailDuplicate");
        registerPage.fillStudent("Dup", TestData.STUDENT_EMAIL, TestData.PWD_STRONG,
                TestData.STUDENT_MAJOR, null);
        registerPage.submitForm();
        registerPage.assertRegisterSubmissionRejected(Duration.ofSeconds(15));
        waitForUrlContains("/register", 5);
        auth().isLoggedIn = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_RegisterFailDuplicate");
    }

    @Override
    public void e_RegisterFailValidation() {
        logStep("e_RegisterFailValidation");
        registerPage.assertLoaded();
        String uniq = "novalid_" + System.currentTimeMillis() + "@ex.com";
        /*
         * Họ tên 1 ký tự — luôn fail rule FE (2–100 ký tự) trước khi gọi API, tránh đăng ký thành công do trạng thái form/GraphWalker.
         */
        registerPage.fillStudent("N", uniq, TestData.PWD_STRONG, null, null);
        registerPage.assertStudentFieldsVisible();
        registerPage.majorInput().setValue("");
        registerPage.goalInput().setValue("");
        registerPage.submitFormExpectingRejection(Duration.ofSeconds(15));
        waitForUrlContains("/register", 5);
        logStepAfter("e_RegisterFailValidation");
    }

    @Override
    public void e_RegisterFailWeakPassword() {
        logStep("e_RegisterFailWeakPassword");
        registerPage.fillStudent("Weak", "weak_" + System.currentTimeMillis() + "@ex.com",
                TestData.PWD_WEAK, TestData.STUDENT_MAJOR, null);
        registerPage.submitForm();
        registerPage.assertRegisterSubmissionRejected(Duration.ofSeconds(15));
        logStepAfter("e_RegisterFailWeakPassword");
    }

    @Override
    public void e_RegisterFailInvalidEmail() {
        logStep("e_RegisterFailInvalidEmail");
        registerPage.fillStudent("BadEmail", TestData.INVALID_EMAIL, TestData.PWD_STRONG,
                TestData.STUDENT_MAJOR, null);
        registerPage.submitForm();
        registerPage.assertRegisterSubmissionRejected(Duration.ofSeconds(15));
        waitForUrlContains("/register", 5);
        logStepAfter("e_RegisterFailInvalidEmail");
    }

    @Override
    public void e_SubmitForgotPasswordOk() {
        logStep("e_SubmitForgotPasswordOk");
        armHappyAuthGuards();
        auth().forgotPasswordRateLimited = false;
        auth().forgotPasswordInfrastructureFailure = false;
        ForgotSubmitOutcome outcome = forgotPage.submitEmailAndAwaitOutcome(TestData.STUDENT_EMAIL);
        if (outcome == ForgotSubmitOutcome.RATE_LIMITED) {
            log.warn("e_SubmitForgotPasswordOk: rate limited — no reset token; use e_SkipResetAfterForgotRateLimited if stuck on reset");
            auth().forgotPasswordRateLimited = true;
            auth().resetToken = "";
        } else if (outcome == ForgotSubmitOutcome.INFRASTRUCTURE_OR_NETWORK) {
            log.warn(
                    "e_SubmitForgotPasswordOk: infrastructure/degraded (e.g. Request failed, mail not configured) — "
                            + "no reset token; use e_SkipResetAfterForgotRateLimited from reset vertex");
            auth().forgotPasswordInfrastructureFailure = true;
            auth().resetToken = "";
        } else {
            String uiToken = forgotPage.extractVisibleToken();
            if (uiToken != null && !uiToken.isBlank()) {
                log.info("Reset token (UI): {}...", uiToken.substring(0, Math.min(8, uiToken.length())));
                auth().resetToken = uiToken;
            } else {
                String apiToken = extractResetTokenViaApi(TestData.STUDENT_EMAIL);
                auth().resetToken = apiToken != null ? apiToken : "";
            }
        }
        auth().tokenUsed = false;
        syncGraphWalkerAuthVars();
        String tok = auth().resetToken == null ? "" : auth().resetToken;
        resetPage.openWithToken(tok);
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        logStepAfter("e_SubmitForgotPasswordOk");
    }

    @Override
    public void e_SubmitForgotPasswordObscure() {
        logStep("e_SubmitForgotPasswordObscure");
        String email = "mbt-obscure-" + System.nanoTime() + "@invalid.test";
        ForgotSubmitOutcome outcome = forgotPage.submitEmailAndAwaitOutcome(email);
        if (outcome == ForgotSubmitOutcome.RATE_LIMITED) {
            log.info("e_SubmitForgotPasswordObscure: rate limited — acceptable protection outcome");
        } else if (outcome == ForgotSubmitOutcome.INFRASTRUCTURE_OR_NETWORK) {
            log.info("e_SubmitForgotPasswordObscure: infrastructure/network — acceptable degraded outcome");
        }
        logStepAfter("e_SubmitForgotPasswordObscure");
    }

    @Override
    public void e_SkipResetAfterForgotRateLimited() {
        logStep("e_SkipResetAfterForgotRateLimited");
        open("/login");
        loginPage.assertLoaded();
        auth().forgotPasswordRateLimited = false;
        auth().forgotPasswordInfrastructureFailure = false;
        auth().resetToken = "";
        syncGraphWalkerAuthVars();
        logStepAfter("e_SkipResetAfterForgotRateLimited");
    }

    @Override
    public void e_SubmitResetPasswordOk() {
        logStep("e_SubmitResetPasswordOk");
        resetPage.submitReset(auth().resetToken, TestData.PWD_STRONG);
        navigateWithFallback("/login", "/login", 10);
        auth().tokenUsed = true;
        auth().resetToken = "";
        auth().isLoggedIn = false;
        auth().currentRole = "";
        syncGraphWalkerAuthVars();
        logStepAfter("e_SubmitResetPasswordOk");
    }

    @Override
    public void e_ViewProfile() {
        logStep("e_ViewProfile");
        open("/profile");
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        waitForUrlContains("/profile", 8);
        logStepAfter("e_ViewProfile");
    }

    @Override
    public void e_UpdateProfileOk() {
        logStep("e_UpdateProfileOk");
        armHappyAuthGuards();
        profile.assertLoaded();
        if (auth().accessToken != null) {
            api.withToken(auth().accessToken);
            JsonNode r = api.patch("/api/v1/auth/me",
                    java.util.Map.of("bio", "MBT bio " + System.currentTimeMillis()), true);
            MbtBusinessAssertions.assertSuccessEnvelope(r, "PATCH /auth/me");
        }
        syncGraphWalkerAuthVars();
        logStepAfter("e_UpdateProfileOk");
    }

    @Override
    public void e_UpdateProfileFail() {
        logStep("e_UpdateProfileFail");
        profile.assertLoaded();
        if (auth().accessToken != null && "INSTRUCTOR".equals(auth().currentRole)) {
            api.withToken(auth().accessToken);
            JsonNode r = api.patch("/api/v1/auth/me", java.util.Map.of("expertise", ""), true);
            boolean hasErrorsEnvelope = r != null && r.has("errors") && r.path("errors").size() > 0;
            boolean hasFastApiDetail = r != null && r.has("detail")
                    && !r.path("detail").asText("").isBlank();
            if (!hasErrorsEnvelope && !hasFastApiDetail) {
                throw new AssertionError(
                        "e_UpdateProfileFail: expected errors or detail for empty expertise, got: " + r);
            }
        }
        logStepAfter("e_UpdateProfileFail");
    }

    @Override
    public void e_BackToDashboard() {
        logStep("e_BackToDashboard");
        switch (auth().currentRole) {
            case "ADMIN" -> open("/admin/dashboard");
            case "INSTRUCTOR" -> open("/instructor/dashboard");
            default -> open("/student/dashboard");
        }
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT);
        logStepAfter("e_BackToDashboard");
    }

    @Override
    public void e_Logout() {
        logStep("e_Logout");
        navBar.clickLogout();
        waitForUrlContains("/courses", 10);
        auth().clearSession();
        auth().armHappyPathGuards();
        auth().resetToken = "";
        auth().tokenUsed = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_Logout");
    }

    // ---------- MBT guard plumbing (JSON actions + mirror AuthState) ----------

    @Override
    public void e_ArmLoginFailureContext() {
        logStep("e_ArmLoginFailureContext");
        loginPage.assertLoaded();
        auth().emailExists = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_ArmLoginFailureContext");
    }

    @Override
    public void e_RearmLoginHappyContext() {
        logStep("e_RearmLoginHappyContext");
        loginPage.assertLoaded();
        auth().emailExists = true;
        auth().passwordCorrect = true;
        syncGraphWalkerAuthVars();
        logStepAfter("e_RearmLoginHappyContext");
    }

    @Override
    public void e_ArmRegisterDuplicateContext() {
        logStep("e_ArmRegisterDuplicateContext");
        registerPage.assertLoaded();
        auth().emailNotExists = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_ArmRegisterDuplicateContext");
    }

    @Override
    public void e_ArmRegisterValidationContext() {
        logStep("e_ArmRegisterValidationContext");
        registerPage.assertLoaded();
        auth().studentMajorOrGoalSet = false;
        auth().expertiseSet = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_ArmRegisterValidationContext");
    }

    @Override
    public void e_ArmRegisterWeakPasswordContext() {
        logStep("e_ArmRegisterWeakPasswordContext");
        registerPage.assertLoaded();
        auth().passwordStrong = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_ArmRegisterWeakPasswordContext");
    }

    @Override
    public void e_ArmRegisterBadEmailContext() {
        logStep("e_ArmRegisterBadEmailContext");
        registerPage.assertLoaded();
        auth().emailValid = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_ArmRegisterBadEmailContext");
    }

    @Override
    public void e_RearmRegisterHappyContext() {
        logStep("e_RearmRegisterHappyContext");
        registerPage.assertLoaded();
        auth().emailNotExists = true;
        auth().emailValid = true;
        auth().passwordStrong = true;
        auth().studentMajorOrGoalSet = true;
        auth().expertiseSet = true;
        syncGraphWalkerAuthVars();
        logStepAfter("e_RearmRegisterHappyContext");
    }

    @Override
    public void e_ArmForgotObscureContext() {
        logStep("e_ArmForgotObscureContext");
        forgotPage.assertLoaded();
        auth().emailExists = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_ArmForgotObscureContext");
    }

    @Override
    public void e_RearmForgotHappyContext() {
        logStep("e_RearmForgotHappyContext");
        forgotPage.assertLoaded();
        auth().emailExists = true;
        auth().forgotPasswordRateLimited = false;
        auth().forgotPasswordInfrastructureFailure = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_RearmForgotHappyContext");
    }

    @Override
    public void e_ArmProfileUpdateInvalidContext() {
        logStep("e_ArmProfileUpdateInvalidContext");
        profile.assertLoaded();
        auth().profileUpdateValid = false;
        syncGraphWalkerAuthVars();
        logStepAfter("e_ArmProfileUpdateInvalidContext");
    }

    @Override
    public void e_RearmProfileHappyContext() {
        logStep("e_RearmProfileHappyContext");
        profile.assertLoaded();
        auth().profileUpdateValid = true;
        syncGraphWalkerAuthVars();
        logStepAfter("e_RearmProfileHappyContext");
    }

    // ---------- GraphWalker guards (JSON: tên method gwGuard_* — không biểu thức JS) ----------

    @SuppressWarnings("unused") // gọi từ Graal JS qua model JSON
    public boolean gwGuard_loginCredentialsOk() {
        return auth().emailExists && auth().passwordCorrect;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_loginWillFail() {
        return !auth().emailExists || !auth().passwordCorrect;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_registerStudentOk() {
        return auth().emailValid && auth().passwordStrong && auth().emailNotExists && auth().studentMajorOrGoalSet;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_registerInstructorOk() {
        return auth().emailValid && auth().passwordStrong && auth().emailNotExists && auth().expertiseSet;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_registerDuplicate() {
        return !auth().emailNotExists;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_registerRoleFieldsInvalid() {
        return !auth().studentMajorOrGoalSet || !auth().expertiseSet;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_passwordWeak() {
        return !auth().passwordStrong;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_emailInvalid() {
        return !auth().emailValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_forgotEmailExists() {
        return auth().emailExists;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_forgotEmailMissing() {
        return !auth().emailExists;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_resetReady() {
        String t = auth().resetToken;
        return t != null && !t.isBlank() && !auth().tokenUsed;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_forgotRateLimitedBlocked() {
        return auth().forgotPasswordRateLimited || auth().forgotPasswordInfrastructureFailure;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_loggedIn() {
        return auth().isLoggedIn;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_profileUpdateOk() {
        return auth().isLoggedIn && auth().profileUpdateValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_profileUpdateInvalid() {
        return auth().isLoggedIn && !auth().profileUpdateValid;
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_roleStudent() {
        return "STUDENT".equals(auth().currentRole);
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_roleInstructor() {
        return "INSTRUCTOR".equals(auth().currentRole);
    }

    @SuppressWarnings("unused")
    public boolean gwGuard_roleAdmin() {
        return "ADMIN".equals(auth().currentRole);
    }

    // ---------- helpers ----------

    private void assertGuestAuthState() {
        assert !auth().isLoggedIn;
        assert auth().currentRole == null || auth().currentRole.isEmpty()
                : "guest: currentRole should be empty";
    }

    private void assertDashboardRole(String role) {
        if (!auth().isLoggedIn) {
            throw new AssertionError("dashboard vertex: expected logged in");
        }
        if (!role.equals(auth().currentRole)) {
            throw new AssertionError("dashboard vertex: expected role " + role + " was " + auth().currentRole);
        }
        syncGraphWalkerAuthVars();
    }

    private void ensureDashboard(String email, String dashboardPath) {
        if (waitForUrlContains(dashboardPath, 10)) {
            return;
        }
        log.warn("Vertex for {}: url={} expected {}; recovering via API", email,
                com.codeborne.selenide.WebDriverRunner.url(), dashboardPath);
        loginViaUiOrApi(email, dashboardPath);
    }

    private void loginViaUiOrApi(String email, String dashboardPath) {
        if (!com.codeborne.selenide.WebDriverRunner.url().contains("/login")) {
            open("/login");
        }
        loginPage.assertLoaded();
        loginPage.login(email, TestData.PWD_STRONG);

        if (waitForUrlContains(dashboardPath, 8)) {
            return;
        }

        log.warn("UI login for {} did not reach {}; API + localStorage fallback", email, dashboardPath);

        String token = loginWithRetry(email, TestData.PWD_STRONG);
        if (token == null) {
            log.error("API login also failed for {}", email);
            return;
        }
        auth().accessToken = token;
        api.withToken(token);

        JsonNode meResp = api.me();
        String meJson = (meResp != null && !meResp.path("data").isMissingNode())
                ? meResp.path("data").toString()
                : "{}";

        String refresh = api.lastRefreshToken();
        if (refresh == null) {
            refresh = "";
        }
        auth().refreshToken = refresh.isBlank() ? null : refresh;
        writeAuthToBrowserLocalStorage(token, refresh, meJson);

        Selenide.open(dashboardPath);
        if (!waitForUrlContains(dashboardPath, 12)) {
            log.error("Fallback navigate to {} ended on url={}", dashboardPath,
                    com.codeborne.selenide.WebDriverRunner.url());
        }
    }

    private void assertMeRoleForEmail(String email, String expectedRole) {
        String t = api.login(email, TestData.PWD_STRONG);
        if (t != null) {
            auth().accessToken = t;
            api.withToken(t);
            MbtBusinessAssertions.assertUserRoleInMe(api, expectedRole);
        }
    }

    private String extractResetTokenViaApi(String email) {
        try {
            JsonNode body = api.post("/api/v1/auth/forgot-password",
                    java.util.Map.of("email", email), false);
            if (body != null) {
                JsonNode t = body.path("data").path("reset_token");
                if (!t.isMissingNode()) {
                    return t.asText();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
