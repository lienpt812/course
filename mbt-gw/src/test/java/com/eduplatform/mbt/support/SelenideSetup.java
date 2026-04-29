package com.eduplatform.mbt.support;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

import java.nio.file.Path;

public final class SelenideSetup {

    private static volatile boolean initialised = false;

    private SelenideSetup() {}

    public static synchronized void init() {
        if (initialised) return;
        Configuration.baseUrl = TestConfig.webBaseUrl();
        Configuration.browser = TestConfig.browser();
        Configuration.headless = TestConfig.headless();
        Configuration.timeout = Math.max(TestConfig.timeoutMs(), 12000);
        Configuration.pageLoadTimeout = Math.max(TestConfig.timeoutMs() * 3, 30000);
        Configuration.screenshots = true;
        Configuration.savePageSource = false;
        // Đường dẫn tuyệt đối cùng ổ với user.dir — tránh Path.relativize báo 'other' has different root (Windows: D:\ vs C:\)
        Configuration.reportsFolder = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath()
                .normalize()
                .resolve("target/selenide-reports")
                .toString();
        // Keep fastSetValue=false (default). fastSetValue=true uses the native value setter
        // which bypasses React's synthetic onChange — FE state stays at its initial default
        // (e.g. LoginPage's hard-coded "student@example.com") and login would fail.
        Configuration.fastSetValue = false;
        Configuration.browserSize = "1440x900";
        initialised = true;
    }

    /**
     * Clear cookies + localStorage/sessionStorage. Safe to call before the browser
     * is up: any failure (no WebDriver bound yet) is silently ignored so callers
     * can invoke it unconditionally at the start of a vertex.
     */
    public static void resetBrowserState() {
        if (!WebDriverRunner.hasWebDriverStarted()) return;
        try { Selenide.clearBrowserCookies(); } catch (Exception ignored) {}
        try {
            Selenide.executeJavaScript(
                    "try{localStorage.clear();sessionStorage.clear();}catch(e){}");
        } catch (Exception ignored) {}
    }

    public static void tearDown() {
        try { Selenide.closeWebDriver(); } catch (Exception ignored) {}
    }
}
