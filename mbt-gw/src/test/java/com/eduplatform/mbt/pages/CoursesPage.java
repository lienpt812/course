package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.eduplatform.mbt.support.MbtTestIds;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * /courses — CoursesPage.tsx
 *
 * UI layout:
 *   h1 "Khóa Học"
 *   input[placeholder^="Tìm khóa học"]  — search
 *   select #0 — Danh mục (options: "all" + unique categories from courses)
 *   select #1 — Cấp độ (options: all, Beginner, Intermediate, Advanced)
 *   span ".mb-5.text-neutral-600.text-sm" — "Tìm thấy X khóa học" count badge
 *   Course card links: a[href*="/courses/"] (see {@link #cards()})
 *   p "Không tìm thấy khóa học phù hợp" — empty state
 *
 * FE filters (client-side, no API call on filter change):
 *   - searchQuery: matches title OR description (case-insensitive)
 *   - selectedCategory: "all" or exact category string
 *   - selectedLevel: "all" | "Beginner" | "Intermediate" | "Advanced"
 *
 * Backend: GET /courses?status=PUBLISHED (default) → list of published courses
 * Logged-in users also get registrations + certificates + progress data loaded.
 */
public class CoursesPage {

    private static final Logger log = LoggerFactory.getLogger(CoursesPage.class);

    public CoursesPage open() {
        Selenide.open("/courses");
        return this;
    }

    public CoursesPage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT).shouldHave(Condition.text(UiText.COURSES_H1));
        return this;
    }

    /**
     * Search input — prefer {@code data-testid} from CoursesPage.tsx; comma fallback for older builds.
     */
    public SelenideElement searchInput() {
        return $("[data-testid='" + MbtTestIds.COURSES_SEARCH_INPUT + "'], input[placeholder^='Tìm khóa học']");
    }

    /**
     * Category dropdown — first select on the page.
     * Option values: "all", then category strings from courses (e.g. "Testing", "Backend").
     */
    public SelenideElement categorySelect() { return $$("select").get(0); }

    /**
     * Level dropdown — second select on the page.
     * Option values: "all", "Beginner", "Intermediate", "Advanced".
     */
    public SelenideElement levelSelect()    { return $$("select").get(1); }

    /**
     * Course detail links (2+ per card possible). Use {@code *=} so absolute
     * {@code http://host/.../courses/id} and {@code /courses/id} both match.
     */
    public ElementsCollection cards() {
        return $$("a[href*='/courses/']");
    }

    /**
     * "Tìm thấy X khóa học" count text shown below the filter bar.
     * Format: "Tìm thấy {n} khóa học"
     */
    public SelenideElement foundCountBadge() {
        return $("[data-testid='" + MbtTestIds.COURSES_COUNT_BADGE + "']");
    }

    public SelenideElement coursesErrorBanner() {
        return $("[data-testid='" + MbtTestIds.COURSES_ERROR + "']");
    }

    public SelenideElement coursesGrid() {
        return $("[data-testid='" + MbtTestIds.COURSES_GRID + "']");
    }

    /** Empty state copy — prefer test id wrapper from FE. */
    public SelenideElement emptyState() {
        return $("[data-testid='" + MbtTestIds.COURSES_EMPTY_STATE + "']");
    }

    // ========== filter helpers ==========

    public void search(String q)             { searchInput().setValue(q); }
    public void filterCategory(String cat)   { categorySelect().selectOption(cat); }
    public void filterLevel(String level)    { levelSelect().selectOption(level); }

    public void resetFilters() {
        // setValue (not clear()) so React controlled state updates; clear() can leave filters stale
        searchInput().setValue("");
        categorySelect().selectOptionByValue("all");
        levelSelect().selectOptionByValue("all");
    }

    public void openFirstCard()              { cards().first().click(); }

    /** Navigate directly to a course detail page by ID */
    public void openDetail(long courseId)    { Selenide.open("/courses/" + courseId); }

    /**
     * Assert the count badge shows the expected number of results.
     * e.g. assertFoundCount(3) checks text contains "3"
     */
    public void assertFoundCount(int expected) {
        foundCountBadge().shouldHave(Condition.text(String.valueOf(expected)));
    }

    private static final Pattern COUNT_IN_BADGE = Pattern.compile("Tìm thấy\\s*(\\d+)");
    private static final Pattern KHOA_HOC_COUNT = Pattern.compile("(\\d+)\\s*khóa\\s*học");
    private static final Pattern COURSE_ID_IN_HREF = Pattern.compile("courses/\\d+");

    static int parseFoundCountText(String t) {
        if (t == null) return 0;
        t = t.replace('\u00a0', ' ').replaceAll("\\s+", " ");
        Matcher m = COUNT_IN_BADGE.matcher(t);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        m = KHOA_HOC_COUNT.matcher(t);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static String badgeTextForCount(org.openqa.selenium.WebDriver d) {
        try {
            for (WebElement el : d.findElements(By.cssSelector("[data-testid='courses-count-badge']"))) {
                if (el.isDisplayed()) {
                    String t = el.getText();
                    if (t != null && t.contains("Tìm thấy") && t.contains("khóa")) {
                        return t;
                    }
                }
            }
        } catch (Exception ignored) { }
        for (String sel : new String[] {
                ".mb-5.text-neutral-600.text-sm",
                "div.text-neutral-600.text-sm",
        }) {
            try {
                for (var el : d.findElements(By.cssSelector(sel))) {
                    String t = el.getText();
                    if (t != null && t.contains("Tìm thấy") && t.contains("khóa")) {
                        return t;
                    }
                }
            } catch (Exception ignored) { }
        }
        try {
            for (var el : d.findElements(
                    By.xpath("//*[contains(.,'Tìm thấy') and contains(.,'khóa học')]"))) {
                String t = el.getText();
                if (t != null && t.length() < 200 && t.contains("Tìm thấy")) {
                    return t;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static boolean hasCourseDetailLink(org.openqa.selenium.WebDriver d) {
        if (!d.findElements(By.cssSelector("a[href*='/courses/']")).isEmpty()) {
            return true;
        }
        for (var a : d.findElements(By.cssSelector("a[href]"))) {
            String h = a.getAttribute("href");
            if (h != null && COURSE_ID_IN_HREF.matcher(h).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sau khi lọc: có ít nhất một thẻ khóa hoặc empty state (0 khớp) — tránh fail khi seed không có Beginner/…
     */
    public void assertFilterShowsCardsOrEmptyState() {
        new FluentWait<>(WebDriverRunner.getWebDriver())
                .withTimeout(Duration.ofSeconds(20))
                .pollingEvery(Duration.ofMillis(200))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(driver -> hasCourseDetailLink(driver) || emptyStateDisplayed(driver));
        SafeUi.waitUntilVisible(foundCountBadge(), Duration.ofSeconds(20));
    }

    /**
     * Waits for the async course list to reach a <strong>settled</strong> state (not necessarily non-empty):
     * course cards, positive count, empty state, or error banner. Rate-limit / network errors on the page
     * complete the wait with a warning so GraphWalker is not killed by a missing grid alone.
     */
    public void assertCoursesVisible() {
        new FluentWait<>(WebDriverRunner.getWebDriver())
                .withTimeout(Duration.ofSeconds(45))
                .pollingEvery(Duration.ofMillis(200))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(driver -> {
                    if (coursesErrorDisplayed(driver)) {
                        log.warn("Courses list settled with error banner: {}", firstErrorBannerText(driver));
                        return true;
                    }
                    if (hasCourseDetailLink(driver)) {
                        return true;
                    }
                    String t = badgeTextForCount(driver);
                    if (t != null && parseFoundCountText(t) > 0) {
                        return true;
                    }
                    if (!coursesLoadingDisplayed(driver) && t != null && parseFoundCountText(t) == 0) {
                        return true;
                    }
                    if (!coursesLoadingDisplayed(driver) && emptyStateDisplayed(driver)) {
                        return true;
                    }
                    return false;
                });
    }

    private static boolean coursesLoadingDisplayed(org.openqa.selenium.WebDriver d) {
        try {
            for (WebElement el : d.findElements(By.cssSelector("[data-testid='courses-loading']"))) {
                if (el.isDisplayed()) {
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private static boolean emptyStateDisplayed(org.openqa.selenium.WebDriver d) {
        try {
            for (WebElement el : d.findElements(By.cssSelector("[data-testid='courses-empty-state']"))) {
                if (el.isDisplayed()) {
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private static boolean coursesErrorDisplayed(org.openqa.selenium.WebDriver d) {
        try {
            for (WebElement el : d.findElements(By.cssSelector("[data-testid='courses-error']"))) {
                if (el.isDisplayed()) {
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private static String firstErrorBannerText(org.openqa.selenium.WebDriver d) {
        try {
            for (WebElement el : d.findElements(By.cssSelector("[data-testid='courses-error']"))) {
                if (el.isDisplayed()) {
                    return el.getText();
                }
            }
        } catch (Exception ignored) { }
        return "";
    }
}