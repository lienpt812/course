package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.MbtTestIds;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * /login — LoginPage.tsx
 *
 * UI layout (LoginPage.tsx):
 *   h1 "Đăng nhập"
 *   label "Email" → input[type=email]  (pre-filled with 'student@example.com')
 *   label "Mật khẩu" → input[type=password]  (pre-filled with 'password123')
 *   p.text-red-600  — conditional error message
 *   button[type=submit]  "Đăng nhập" / "Đang đăng nhập..." (disabled while loading)
 *   link "Đăng ký ngay" → /register
 *   link "Quên mật khẩu?" → /forgot-password
 *
 * Backend: POST /auth/login → { access_token, refresh_token, token_type }
 * Then GET /auth/me → redirect by role:
 *   ADMIN → /admin/dashboard
 *   INSTRUCTOR → /instructor/dashboard
 *   STUDENT (default) → /student/dashboard
 */
public class LoginPage {

    public LoginPage open() {
        Selenide.open("/login");
        return this;
    }

    public LoginPage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT).shouldHave(Condition.text(UiText.LOGIN_H1));
        SafeUi.waitUntilVisible(emailInput(), SafeUi.DEFAULT_TIMEOUT);
        SafeUi.waitUntilVisible(passwordInput(), SafeUi.DEFAULT_TIMEOUT);
        SafeUi.waitUntilVisible(submit(), SafeUi.DEFAULT_TIMEOUT);
        return this;
    }

    public SelenideElement emailInput()    { return $("input[type='email']"); }
    public SelenideElement passwordInput() { return $("input[type='password']"); }

    /**
     * Submit button. Text changes to "Đang đăng nhập..." and button becomes disabled while request in flight.
     * Use attribute selector (not byText) to remain stable across states.
     */
    public SelenideElement submit()        { return $("button[type='submit']"); }

    /** Error line when login fails — prefer {@code data-testid}; comma selector for older FE. */
    public SelenideElement errorText() {
        return $("[data-testid='" + MbtTestIds.LOGIN_FORM_ERROR + "'], form p.text-sm.text-red-600, p.text-red-600");
    }

    /** "Đăng ký ngay" link navigates to /register */
    public SelenideElement linkRegister()  { return $(byText("Đăng ký ngay")); }

    /** "Quên mật khẩu?" link navigates to /forgot-password */
    public SelenideElement linkForgot()    { return $(byText("Quên mật khẩu?")); }

    public void fill(String email, String pwd) {
        emailInput().setValue(email);
        passwordInput().setValue(pwd);
    }

    /**
     * Fill and submit the form. Does NOT wait for navigation — caller must poll URL.
     * On success, FE stores access_token, refresh_token, auth_user in localStorage
     * then navigates to dashboard by role.
     */
    public void login(String email, String pwd) {
        fill(email, pwd);
        SelenideElement s = submit();
        s.shouldBe(Condition.enabled);
        SafeUi.clickWhenReady(s, Duration.ofSeconds(8), 2);
    }

    /** Assert that submit is in loading state (disabled, text changed). */
    public void assertSubmitLoading() {
        submit().shouldHave(Condition.text("Đang đăng nhập..."));
        submit().shouldBe(Condition.disabled);
    }
}