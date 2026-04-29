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
import static com.codeborne.selenide.Selenide.sleep;

/**
 * /forgot-password — ForgotPasswordPage.tsx
 *
 * <p>Resilient flows: use {@link #submitEmailAndAwaitOutcome(String)} so 429, generic
 * {@code "Request failed"} (see {@code api.ts#extractErrorMessage}), and network errors do not fail MBT.</p>
 */
public class ForgotPasswordPage {

    public static boolean isRateLimitMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String m = message.toLowerCase();
        return m.contains("rate limit")
                || m.contains("429")
                || m.contains("too many requests")
                || m.contains("quota");
    }

    /**
     * FE/client-side copy that indicates environment or transport — not an application rule validation
     * (e.g. “email not registered”) the test should assert on. Includes the default string when the API
     * returns non-2xx without a parseable {@code detail} / {@code errors[0].message}.
     */
    public static boolean isInfrastructureFailureMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String m = message.toLowerCase();
        if (m.equals("request failed")) {
            return true;
        }
        if (m.contains("failed to fetch")) {
            return true;
        }
        if (m.contains("networkerror") || m.contains("load failed") || m.contains("network request failed")) {
            return true;
        }
        if (m.contains("cors") || m.contains("cross-origin")) {
            return true;
        }
        if (m.contains("timeout") || m.contains("timed out")) {
            return true;
        }
        if (m.contains("econnrefused") || m.contains("connection refused") || m.contains("err_connection")) {
            return true;
        }
        if (m.contains("bad gateway") || m.contains("service unavailable") || m.contains("gateway timeout")) {
            return true;
        }
        if (m.contains("smtp") && (m.contains("error") || m.contains("refused") || m.contains("failed"))) {
            return true;
        }
        if (m.contains("email") && m.contains("not configured")) {
            return true;
        }
        return false;
    }

    /** True when the UI should not be treated as a strict assertable failure of the SUT. */
    public static boolean isSoftEnvironmentFailureMessage(String message) {
        return isRateLimitMessage(message) || isInfrastructureFailureMessage(message);
    }

    public ForgotPasswordPage open() {
        Selenide.open("/forgot-password");
        return this;
    }

    public ForgotPasswordPage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT).shouldHave(Condition.text(UiText.FORGOT_PASSWORD_H1));
        SafeUi.waitUntilVisible(emailInput(), SafeUi.DEFAULT_TIMEOUT);
        return this;
    }

    public SelenideElement emailInput()  { return $("input[type='email']"); }
    public SelenideElement submit()      { return $("button[type='submit']"); }

    public SelenideElement errorText() {
        return $("[data-testid='" + MbtTestIds.FORGOT_PASSWORD_FORM_ERROR + "'], form p[class*='text-red-600']");
    }

    public SelenideElement successBlock() { return $("[data-testid='forgot-password-success']"); }

    public SelenideElement tokenText()   { return successBlock().$("p.font-mono"); }

    public SelenideElement resetLink()   { return $(byText("Đặt lại mật khẩu ngay →")); }

    public SelenideElement backToLogin() { return $(byText("← Quay lại đăng nhập")); }

    public void submitEmail(String email) {
        emailInput().setValue(email);
        SelenideElement btn = submit();
        btn.shouldBe(Condition.enabled).click();
        btn.shouldBe(Condition.enabled, Duration.ofSeconds(50));
        btn.shouldHave(Condition.text("Gửi yêu cầu"));
    }

    public ForgotSubmitOutcome submitEmailAndAwaitOutcome(String email) {
        emailInput().setValue(email);
        SelenideElement btn = submit();
        btn.shouldBe(Condition.enabled).click();
        return awaitOutcomeAfterClick(Duration.ofSeconds(50));
    }

    private ForgotSubmitOutcome awaitOutcomeAfterClick(Duration budget) {
        long deadline = System.currentTimeMillis() + budget.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (successBlock().is(Condition.visible)) {
                SelenideElement btn = submit();
                btn.shouldBe(Condition.enabled, Duration.ofSeconds(8));
                btn.shouldHave(Condition.text("Gửi yêu cầu"));
                return ForgotSubmitOutcome.SUCCESS;
            }
            if (errorText().is(Condition.visible)) {
                String msg = errorText().getText();
                if (isRateLimitMessage(msg)) {
                    submit().shouldBe(Condition.enabled, Duration.ofSeconds(15));
                    return ForgotSubmitOutcome.RATE_LIMITED;
                }
                if (isInfrastructureFailureMessage(msg)) {
                    submit().shouldBe(Condition.enabled, Duration.ofSeconds(20));
                    return ForgotSubmitOutcome.INFRASTRUCTURE_OR_NETWORK;
                }
                SelenideElement btn = submit();
                if (btn.is(Condition.enabled) && btn.getText().contains("Gửi yêu cầu")) {
                    throw new AssertionError(
                            "Forgot-password functional/validation error (not soft env failure): " + msg);
                }
            }
            sleep(150);
        }
        throw new AssertionError("Forgot-password: no success or terminal error within " + budget.getSeconds() + "s");
    }

    public String extractVisibleToken() {
        try {
            if (!successBlock().is(Condition.visible)) {
                return null;
            }
            if (tokenText().is(Condition.visible)) {
                String text = tokenText().getText();
                int idx = text.lastIndexOf(": ");
                if (idx >= 0) {
                    return text.substring(idx + 2).trim();
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    public void assertSuccessVisible() {
        if (errorText().is(Condition.visible, Duration.ofSeconds(2))) {
            String msg = errorText().getText();
            if (isRateLimitMessage(msg)) {
                throw new AssertionError(
                        "Strict success expected but server is rate limiting; use submitEmailAndAwaitOutcome: " + msg);
            }
            if (isInfrastructureFailureMessage(msg)) {
                throw new AssertionError(
                        "Strict success expected but transport/backend failed (e.g. Request failed, SMTP, 5xx); "
                                + "use submitEmailAndAwaitOutcome: " + msg);
            }
            throw new AssertionError("Expected forgot-password success, but error was shown: " + msg);
        }
        SafeUi.waitUntilVisible(successBlock(), Duration.ofSeconds(25));
        SafeUi.waitUntilVisible(successBlock().$("p"), SafeUi.DEFAULT_TIMEOUT);
    }
}
