package com.eduplatform.mbt.support;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

import static com.codeborne.selenide.Selenide.$;

/**
 * Mandatory UI layer for MBT: centralizes waits, visibility checks, and clicks so tests do not
 * call raw {@code $(...).shouldBe(visible)} across page objects. Uses Selenide conditions
 * <strong>only here</strong> with timeouts, retries, and debug logging.
 */
public final class SafeUi {

    private static final Logger LOG = LoggerFactory.getLogger(SafeUi.class);
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(TestConfig.timeoutMs());
    public static final Duration LONG_TIMEOUT = Duration.ofSeconds(15);

    private SafeUi() {}

    // -------------------------------------------------------------------------
    // Existence / visibility
    // -------------------------------------------------------------------------

    public static boolean exists(SelenideElement el) {
        try {
            return el != null && el.exists();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isVisible(SelenideElement el) {
        try {
            return el != null && el.is(Condition.visible);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Use instead of {@code $(sel).shouldBe(visible)} — waits until visible or times out
     * (throws {@link com.codeborne.selenide.ex.ElementShould}).
     */
    public static SelenideElement waitUntilVisible(String cssSelector, Duration timeout) {
        logSelector("waitUntilVisible(css)", cssSelector);
        return $(cssSelector).shouldBe(Condition.visible, timeout);
    }

    public static SelenideElement waitUntilVisible(SelenideElement el, Duration timeout) {
        logElement("waitUntilVisible", el);
        Objects.requireNonNull(el, "element");
        return el.shouldBe(Condition.visible, timeout);
    }

    /**
     * True if the element becomes visible within the timeout; does not throw.
     */
    public static boolean tryWaitUntilVisible(SelenideElement el, Duration timeout) {
        if (el == null) {
            return false;
        }
        try {
            el.shouldBe(Condition.visible, timeout);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Text
    // -------------------------------------------------------------------------

    public static String getTextSafe(SelenideElement el) {
        return textOrEmpty(el);
    }

    public static String textOrEmpty(SelenideElement el) {
        try {
            if (el == null || !el.exists()) {
                return "";
            }
            String t = el.getText();
            return t == null ? "" : t;
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isNonEmptyVisible(ElementsCollection coll) {
        try {
            if (coll == null || coll.isEmpty()) {
                return false;
            }
            SelenideElement first = coll.first();
            return first.is(Condition.visible) && !textOrEmpty(first).isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Clicks (scroll + interactable + JS fallback, retry on intercept)
    // -------------------------------------------------------------------------

    public static void clickWhenReady(SelenideElement el) {
        clickWhenReady(el, DEFAULT_TIMEOUT, 2);
    }

    public static void clickWhenReady(SelenideElement el, Duration interactableTimeout, int maxAttempts) {
        Objects.requireNonNull(el, "element");
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logElement("clickWhenReady (attempt " + attempt + ")", el);
                Selenide.executeJavaScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'})", el);
                el.shouldBe(Condition.interactable, interactableTimeout);
                el.click();
                return;
            } catch (ElementClickInterceptedException ex) {
                LOG.warn("Click intercepted; retry with JS. url={} msg={}", currentUrl(), ex.getMessage());
                try {
                    Selenide.executeJavaScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'})", el);
                    el.shouldBe(Condition.interactable, Duration.ofSeconds(2));
                } catch (Exception ignored) {
                    // continue to JS click
                }
                try {
                    Selenide.executeJavaScript("arguments[0].click()", el);
                    return;
                } catch (Exception e2) {
                    if (attempt == maxAttempts) {
                        throw e2;
                    }
                }
            } catch (StaleElementReferenceException ex) {
                LOG.warn("Stale element on click; url={} — re-resolve caller selector", currentUrl());
                throw ex;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Valid UI outcomes (success OR error OR empty — never assume data)
    // -------------------------------------------------------------------------

    /**
     * At least one of the elements is visible after waiting each up to {@code perElementWait}.
     */
    public static void waitUntilAnyVisible(SelenideElement[] candidates, Duration perElementWait) {
        if (candidates == null || candidates.length == 0) {
            throw new IllegalArgumentException("candidates");
        }
        long deadline = System.currentTimeMillis() + perElementWait.toMillis() * (long) candidates.length + 2000L;
        while (System.currentTimeMillis() < deadline) {
            for (SelenideElement c : candidates) {
                if (c != null && tryWaitUntilVisible(c, perElementWait)) {
                    logElement("waitUntilAnyVisible (matched)", c);
                    return;
                }
            }
            sleepMs(150);
        }
        throw new AssertionError("None of the expected UI regions became visible. url=" + currentUrl());
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    public static void sleepMs(long ms) {
        if (ms > 0) {
            Selenide.sleep(ms);
        }
    }

    public static String currentUrl() {
        try {
            if (WebDriverRunner.hasWebDriverStarted()) {
                return WebDriverRunner.url();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "";
    }

    public static void logContext(String action, String extra) {
        if (LOG.isInfoEnabled()) {
            LOG.info("MBT UI {} | url={} | {}", action, currentUrl(), extra == null ? "" : extra);
        }
    }

    private static void logSelector(String op, String selector) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} selector={} url={}", op, selector, currentUrl());
        }
    }

    private static void logElement(String op, SelenideElement el) {
        if (LOG.isDebugEnabled()) {
            String desc;
            try {
                desc = el.toString();
            } catch (Exception e) {
                desc = el.getClass().getSimpleName();
            }
            LOG.debug("{} element={} url={}", op, desc, currentUrl());
        }
    }
}
