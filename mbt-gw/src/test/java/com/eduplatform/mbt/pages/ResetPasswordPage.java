package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * /reset-password — ResetPasswordPage.tsx
 *
 * UI layout:
 *   h1 "Đặt lại mật khẩu"
 *   input[type=text]       → Token field (pre-filled from ?token= URL param)
 *   input[type=password]   → Mật khẩu mới (minLength=8)
 *   input[type=password]   → Xác nhận mật khẩu
 *   p.text-red-600         → error (mismatch / short / invalid token)
 *   button[type=submit]    "Đặt lại mật khẩu" / "Đang xử lý..." (disabled while loading)
 *   link "← Quay lại đăng nhập" → /login
 *
 * FE validations (before API):
 *   - newPassword !== confirmPassword → "Mật khẩu xác nhận không khớp"
 *   - newPassword.length < 8 → "Mật khẩu phải có ít nhất 8 ký tự"
 *
 * Backend: POST /auth/reset-password { token, new_password }
 *   - 200 { password_reset: true } → navigate('/login')
 *   - 400 "Invalid reset token" / "Reset token expired"
 *
 * Note: useSearchParams pre-fills the token input from URL ?token=<value>.
 * Calling openWithToken() passes the token via URL so FE auto-populates it.
 */
public class ResetPasswordPage {

    public ResetPasswordPage open() {
        Selenide.open("/reset-password");
        return this;
    }

    /** Open with token pre-filled via URL query param (uses useSearchParams in FE). */
    public ResetPasswordPage openWithToken(String token) {
        Selenide.open("/reset-password?token=" + token);
        return this;
    }

    public ResetPasswordPage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT).shouldHave(Condition.text(UiText.RESET_PASSWORD_H1));
        SafeUi.waitUntilVisible(tokenInput(), SafeUi.DEFAULT_TIMEOUT);
        return this;
    }

    /** Token input[type=text] — auto-populated from ?token= URL param */
    public SelenideElement tokenInput()   { return $("input[type='text']"); }

    /** Mật khẩu mới field */
    public SelenideElement pwdInput()     { return $$("input[type='password']").get(0); }

    /** Xác nhận mật khẩu field */
    public SelenideElement confirmInput() { return $$("input[type='password']").get(1); }

    public SelenideElement submit()       { return $("button[type='submit']"); }

    /**
     * Error message. Covers both FE validation errors and BE errors:
     *   - "Mật khẩu xác nhận không khớp"
     *   - "Mật khẩu phải có ít nhất 8 ký tự"
     *   - "Token không hợp lệ hoặc đã hết hạn" (from catch block)
     */
    public SelenideElement errorText()    { return $("p.text-red-600"); }

    /** "← Quay lại đăng nhập" link */
    public SelenideElement backToLogin()  { return $(byText("← Quay lại đăng nhập")); }

    /**
     * Fill all fields and submit.
     * If the page was opened with openWithToken(), the token field is already populated;
     * setValue() overwrites it here (safe).
     */
    public void submitReset(String token, String newPwd) {
        tokenInput().setValue(token);
        pwdInput().setValue(newPwd);
        confirmInput().setValue(newPwd);
        submit().shouldBe(Condition.enabled).click();
    }

    /**
     * Fill with mismatched confirm password to trigger FE validation error.
     * "Mật khẩu xác nhận không khớp" should appear without an API call.
     */
    public void submitMismatch(String token, String pwd, String mismatch) {
        tokenInput().setValue(token);
        pwdInput().setValue(pwd);
        confirmInput().setValue(mismatch);
        submit().click();
    }

    /** Assert token input is pre-filled (non-empty) after openWithToken(). */
    public void assertTokenPreFilled() {
        tokenInput().shouldNotBe(Condition.empty);
    }
}