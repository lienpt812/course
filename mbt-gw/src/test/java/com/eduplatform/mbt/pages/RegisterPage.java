package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.sleep;

/**
 * /register — RegisterPage.tsx
 *
 * UI layout:
 *   h1 "Đăng ký"
 *   input[type=text]    → Họ tên (minLength=2, maxLength=100)
 *   input[type=email]   → Email (maxLength=255)
 *   input[type=password]→ Mật khẩu (minLength=8, maxLength=64)
 *   select              → Vai trò: STUDENT | INSTRUCTOR | ADMIN
 *   input[type=text] #2 → Số điện thoại (optional, maxLength=15)
 *
 *   When role=INSTRUCTOR:
 *     input sau label "Chuyên môn giảng dạy" (required, min=3) — locator XPath theo label (ổn định hơn placeholder).
 *
 *   When role=STUDENT:
 *     input sau label "Chuyên ngành" / "Mục tiêu học tập" (1 trong 2 bắt buộc).
 *
 *   [data-testid=register-form-error] — FE validation / API error (setError)
 *   button[type=submit] "Tạo tài khoản" / "Đang đăng ký..." (disabled while loading)
 *
 * FE validations (client-side, before API):
 *   - name: 2–100 chars
 *   - email: regex /^[^\s@]+@[^\s@]+\.[^\s@]+$/
 *   - password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/ (uppercase + lowercase + digit)
 *   - phone (if given): /^\+?[0-9]{9,15}$/
 *   - INSTRUCTOR: expertise length >= 3
 *   - STUDENT: studentMajor OR learningGoal must be non-empty
 *
 * Backend: POST /auth/register → 409 if email exists; 400 if role-specific fields missing
 */
public class RegisterPage {

    public RegisterPage open() {
        Selenide.open("/register");
        return this;
    }

    public RegisterPage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT).shouldHave(Condition.text(UiText.REGISTER_H1));
        SafeUi.waitUntilVisible(nameInput(), SafeUi.DEFAULT_TIMEOUT);
        SafeUi.waitUntilVisible(emailInput(), SafeUi.DEFAULT_TIMEOUT);
        SafeUi.waitUntilVisible(passwordInput(), SafeUi.DEFAULT_TIMEOUT);
        SafeUi.waitUntilVisible(roleSelect(), SafeUi.DEFAULT_TIMEOUT);
        return this;
    }

    /** Họ tên field — first text input */
    public SelenideElement nameInput()     { return $("input[type='text']"); }

    public SelenideElement emailInput()    { return $("input[type='email']"); }
    public SelenideElement passwordInput() { return $("input[type='password']"); }

    /** Role dropdown: options STUDENT, INSTRUCTOR, ADMIN */
    public SelenideElement roleSelect()    { return $("select"); }

    /**
     * Phone input — second text input (first is name).
     * Only present when the form has phone field visible.
     */
    public SelenideElement phoneInput()    { return $$("input[type='text']").get(1); }

    /**
     * Expertise — trong {@code <div><label/> <input/></div>} (React RegisterPage.tsx).
     */
    public SelenideElement expertiseInput() {
        return $x("//div[label[contains(normalize-space(.),'Chuyên môn giảng dạy')]]/input[@type='text']");
    }

    /** Chuyên ngành — role STUDENT. */
    public SelenideElement majorInput() {
        return $x("//div[label[contains(normalize-space(.),'Chuyên ngành')]]/input[@type='text']");
    }

    /** Mục tiêu học tập — role STUDENT. */
    public SelenideElement goalInput() {
        return $x("//div[label[contains(normalize-space(.),'Mục tiêu học tập')]]/input[@type='text']");
    }

    public SelenideElement submit()    { return $("button[type='submit']"); }

    /**
     * Form-level error (FE {@code setError}). Only in DOM when non-empty; stable vs label {@code span.text-red-600} asterisks.
     * See RegisterPage.tsx {@code data-testid="register-form-error"}.
     * <p>For negative tests, prefer {@link #assertRegisterSubmissionRejected(Duration)} — it does not assume a single
     * locator and handles HTML5 vs React.
     */
    public SelenideElement errorText() {
        return $("[data-testid='register-form-error']");
    }

    /**
     * Visible error {@code p} nodes under the form (excludes label asterisk {@code span}s).
     */
    public ElementsCollection visibleFormErrorParagraphs() {
        return $$("form p[class*='text-red-600']").filter(Condition.visible);
    }

    /**
     * After {@link #submitForm()}: registration should not complete.
     * Passes if any of the following holds (behavior-driven, not CSS-only):
     * <ul>
     *   <li>App-level message: {@code [data-testid=register-form-error]} visible with non-blank text (React {@code setError} or API error)
     *   <li>Any non-empty visible error paragraph in the form (same line of UI)
     *   <li>HTML5 constraint validation: some control in the form fails {@code checkValidity()} (browser blocked submit before React)
     * </ul>
     * Fails fast if the URL shows a post-login dashboard (unexpected success).
     */
    public void assertRegisterSubmissionRejected(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String url = WebDriverRunner.url();
            if (isUnexpectedRegistrationSuccessUrl(url)) {
                throw new AssertionError(
                        "Expected registration to be rejected, but navigation looks like success. url=" + url);
            }
            if (isVisibleNonEmptyRegisterErrorBanner()) {
                return;
            }
            if (visibleFormErrorWithText()) {
                return;
            }
            if (hasHtml5FormConstraintViolation()) {
                return;
            }
            sleep(150);
        }
        throw new AssertionError(registerRejectionTimeoutMessage());
    }

    /** {@link #submit()} then {@link #assertRegisterSubmissionRejected(Duration)}. */
    public void submitFormExpectingRejection(Duration timeout) {
        submit().shouldBe(Condition.enabled).click();
        assertRegisterSubmissionRejected(timeout);
    }

    /** @deprecated Prefer {@link #assertRegisterSubmissionRejected(Duration)} (success-URL + HTML5 + React). */
    @Deprecated
    public void assertAnyRegisterValidationVisible(Duration timeout) {
        assertRegisterSubmissionRejected(timeout);
    }

    private static boolean isUnexpectedRegistrationSuccessUrl(String url) {
        if (url == null) {
            return false;
        }
        return url.contains("/student/dashboard")
                || url.contains("/instructor/dashboard")
                || url.contains("/admin/dashboard");
    }

    private boolean isVisibleNonEmptyRegisterErrorBanner() {
        ElementsCollection byId = $$("[data-testid='register-form-error']").filter(Condition.visible);
        if (byId.isEmpty()) {
            return false;
        }
        String t = byId.first().getText();
        return t != null && !t.trim().isEmpty();
    }

    private boolean visibleFormErrorWithText() {
        ElementsCollection reds = visibleFormErrorParagraphs();
        for (int i = 0; i < reds.size(); i++) {
            String t = reds.get(i).getText();
            if (t != null && !t.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHtml5FormConstraintViolation() {
        try {
            Object o = Selenide.executeJavaScript(
                    "const f=document.querySelector('form'); if(!f) return false;"
                            + "for (const el of f.querySelectorAll('input,select,textarea')) {"
                            + " if (!el.checkValidity()) return true; } return false;");
            return Boolean.TRUE.equals(o);
        } catch (Exception e) {
            return false;
        }
    }

    private String registerRejectionTimeoutMessage() {
        String url = WebDriverRunner.url();
        String errMsg = "";
        try {
            ElementsCollection c = $$("[data-testid='register-form-error']").filter(Condition.visible);
            if (!c.isEmpty()) {
                errMsg = " data-testid text=" + c.first().getText();
            }
        } catch (Exception ignored) {
            // no-op
        }
        return "Expected register rejection (React error, form error line, or HTML5 invalid) but none detected within "
                + "timeout. url=" + url + errMsg + " html5Invalid=" + hasHtml5FormConstraintViolation();
    }

    /** "Đăng nhập" link at bottom of form → /login */
    public SelenideElement linkLogin() { return $(byText("Đăng nhập")); }

    // ========== helpers ==========

    /**
     * Đổi vai trò trên {@code <select>} controlled React: chọn theo text option + bắn {@code input/change}
     * để {@code setRole} trong FE chạy (tránh chỉ đổi DOM mà không mount ô instructor/student).
     */
    public RegisterPage role(String roleValue) {
        SelenideElement sel = SafeUi.waitUntilVisible(roleSelect(), SafeUi.DEFAULT_TIMEOUT);
        String optionText = switch (roleValue) {
            case "STUDENT" -> "Student";
            case "INSTRUCTOR" -> "Instructor";
            case "ADMIN" -> "Admin";
            default -> null;
        };
        if (optionText != null) {
            sel.selectOption(optionText);
        } else {
            sel.selectOptionByValue(roleValue);
        }
        Selenide.executeJavaScript(
                "const s = arguments[0], v = arguments[1];"
                        + "s.value = v;"
                        + "s.dispatchEvent(new Event('input', { bubbles: true }));"
                        + "s.dispatchEvent(new Event('change', { bubbles: true }));",
                sel,
                roleValue);
        return this;
    }

    /**
     * Fill Student registration form.
     * @param major     Chuyên ngành (may be null if learningGoal is provided)
     * @param goal      Mục tiêu học tập (may be null if major is provided)
     */
    public RegisterPage fillStudent(String name, String email, String pwd, String major, String goal) {
        nameInput().setValue(name);
        emailInput().setValue(email);
        passwordInput().setValue(pwd);
        role("STUDENT");
        phoneInput().setValue("");
        assertStudentFieldsVisible();
        if (major != null && !major.isEmpty()) {
            majorInput().setValue(major);
        } else {
            majorInput().setValue("");
        }
        if (goal != null && !goal.isEmpty()) {
            goalInput().setValue(goal);
        } else {
            goalInput().setValue("");
        }
        return this;
    }

    /**
     * Fill Instructor registration form.
     * expertise is required (minLength 3) for role=INSTRUCTOR.
     */
    public RegisterPage fillInstructor(String name, String email, String pwd, String expertise) {
        nameInput().setValue(name);
        emailInput().setValue(email);
        passwordInput().setValue(pwd);
        role("INSTRUCTOR");
        SafeUi.waitUntilVisible(expertiseInput(), Duration.ofSeconds(12));
        if (expertise != null && !expertise.isEmpty()) {
            expertiseInput().setValue(expertise);
        } else {
            expertiseInput().setValue("");
        }
        return this;
    }

    public void submitForm() { submit().shouldBe(Condition.enabled).click(); }

    /** Assert submit disabled + loading text while API call is in flight. */
    public void assertSubmitLoading() {
        submit().shouldHave(Condition.text("Đang đăng ký..."));
        submit().shouldBe(Condition.disabled);
    }

    /**
     * Assert expertise field is visible (role=INSTRUCTOR was selected).
     * Useful to ensure re-render happened before filling expertise.
     */
    public void assertInstructorFieldsVisible() {
        SafeUi.waitUntilVisible(expertiseInput(), SafeUi.DEFAULT_TIMEOUT);
    }

    /**
     * Assert student-specific fields are visible (role=STUDENT was selected).
     */
    public void assertStudentFieldsVisible() {
        SafeUi.waitUntilVisible(majorInput(), SafeUi.DEFAULT_TIMEOUT);
        SafeUi.waitUntilVisible(goalInput(), SafeUi.DEFAULT_TIMEOUT);
    }
}