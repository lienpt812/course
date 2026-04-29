package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.MbtTestIds;
import com.eduplatform.mbt.support.SafeUi;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * /courses/:courseId — CourseDetailPage.tsx
 *
 * UI states (mutually exclusive based on course status + registration state):
 *
 *   GUEST / not logged in → "Đăng nhập để đăng ký" button
 *   PUBLISHED + open registration window + no active reg → "Đăng Ký Ngay" button
 *   PUBLISHED + PENDING reg → RegistrationStatusBadge with status "PENDING"
 *   PUBLISHED + CONFIRMED reg → "Vào Học" button (navigates to /learn/:courseId)
 *   PUBLISHED + full capacity → "Lớp đã đầy" message or waitlist indication
 *   NOT PUBLISHED (DRAFT/ARCHIVED) → "Khóa học chưa được công khai."
 *   reg window not open yet → "Đăng ký mở từ {date}" (amber text)
 *   reg window expired → "Đã hết hạn đăng ký."
 *   course not found (404) → "Không tìm thấy khóa học"
 *
 *   INSTRUCTOR (owner of this course, DRAFT status) → "Chỉnh sửa" button
 *   INSTRUCTOR (owner of this course, PUBLISHED) → "Khóa học đã published — không thể chỉnh sửa"
 *   INSTRUCTOR (not owner) → no edit button
 *
 * Backend: GET /courses/{id}
 * Returns: title, slug, description, instructor_name, max_capacity,
 *          confirmed_slots, remaining_slots, registration_open_at, registration_close_at, status
 */
public class CourseDetailPage {

    public CourseDetailPage open(long courseId) {
        Selenide.open("/courses/" + courseId);
        return this;
    }

    /** Course title h1 */
    public SelenideElement title()            { return $("h1"); }

    /**
     * Price block — "0 ₫" or formatted VND price.
     * Selector targets the price display with emerald color and large text.
     */
    public SelenideElement priceBlock()       { return $(".text-3xl.text-emerald-700"); }

    /**
     * "Đăng Ký Ngay" button — shown when:
     *   - course is PUBLISHED
     *   - registration window is open
     *   - user is logged in and has no active registration
     */
    public SelenideElement registerButton()   { return $(byText("Đăng Ký Ngay")); }

    /**
     * "Vào Học" button — shown when user has CONFIRMED registration for this course.
     * Clicking navigates to /learn/{courseId}.
     */
    public SelenideElement enterLearnButton() { return $(byText("Vào Học")); }

    /**
     * "Đăng nhập để đăng ký" button — shown when user is not logged in.
     * Clicking navigates to /login.
     */
    public SelenideElement loginToRegister()  { return $(byText("Đăng nhập để đăng ký")); }

    /**
     * "Khóa học chưa được công khai." — shown when course status is not PUBLISHED.
     */
    public SelenideElement notPublishedMsg()  { return $(byText("Khóa học chưa được công khai.")); }

    /**
     * "Đăng ký mở từ {date}" — shown when registration_open_at is in the future.
     * Text is amber colored (p.text-amber-600).
     */
    public SelenideElement openFromMsg() {
        return $$("p.text-amber-600").filter(Condition.text("Đăng ký mở từ")).first();
    }

    /**
     * "Đã hết hạn đăng ký." — shown when registration_close_at is in the past.
     */
    public SelenideElement expiredMsg()       { return $(byText("Đã hết hạn đăng ký.")); }

    /**
     * "Không tìm thấy khóa học" — shown when course_id does not exist (API 404).
     */
    public SelenideElement notFoundMsg()      { return $(byText("Không tìm thấy khóa học")); }

    /**
     * "Chỉnh sửa" button — only visible when:
     *   - logged-in user is the instructor who owns this course
     *   - course status is DRAFT (not PUBLISHED)
     * Backend: PATCH /courses/{id} — only owner can update their own course
     */
    /**
     * "Chỉnh sửa" — {@code data-testid} first, then text (older bundles).
     */
    public SelenideElement editButton() {
        return $("[data-testid='" + MbtTestIds.COURSE_EDIT_BUTTON + "']");
    }

    public SelenideElement editButtonFallback() { return $(byText("Chỉnh sửa")); }

    /**
     * "Khóa học đã published — không thể chỉnh sửa" — shown when instructor
     * owns this course but it is already PUBLISHED.
     */
    public SelenideElement publishedLockMsg() { return $(byText("Khóa học đã published — không thể chỉnh sửa")); }

    /** "Quay lại danh sách" link → /courses */
    public SelenideElement backToList()       { return $(byText("Quay lại danh sách")); }

    /**
     * Remaining slots text — shown in the capacity section.
     * e.g. "Còn X chỗ trống" — text includes the number.
     */
    public SelenideElement remainingSlotsText() {
        return $$("p, span, div").filter(Condition.text("chỗ trống")).first();
    }

    /**
     * Registration status badge — shown when user has an active registration (PENDING/WAITLIST).
     * RegistrationStatusBadge component renders a badge with the status text.
     */
    public SelenideElement registrationStatusBadge() {
        return $(".inline-flex.items-center");
    }

    // ========== action helpers ==========

    /**
     * Click register only when visible and enabled.
     * Guards against flaky clicks when button is temporarily disabled during loading.
     */
    public void clickRegister() {
        SelenideElement b = registerButton();
        SafeUi.waitUntilVisible(b, Duration.ofSeconds(8));
        b.shouldBe(Condition.enabled);
        SafeUi.clickWhenReady(b, Duration.ofSeconds(5), 2);
    }

    /** Click "Vào Học" to navigate to learning page */
    public void clickEnterLearn() {
        SafeUi.clickWhenReady(enterLearnButton(), SafeUi.DEFAULT_TIMEOUT, 2);
    }

    /**
     * Click edit — only when instructor owns course and it is not PUBLISHED.
     * Nav may intercept; delegated to {@link SafeUi#clickWhenReady(SelenideElement, java.time.Duration, int)}.
     */
    public void clickEdit() {
        SelenideElement el = editButton();
        if (!SafeUi.tryWaitUntilVisible(el, Duration.ofSeconds(3))) {
            el = editButtonFallback();
        }
        el.shouldBe(Condition.enabled);
        SafeUi.clickWhenReady(el, Duration.ofSeconds(6), 2);
    }

    /**
     * Assert course title from UI matches expected (case-insensitive contains).
     */
    public void assertTitleContains(String expected) {
        title().shouldHave(Condition.text(expected));
    }

    /**
     * Assert the course detail page fully loaded (title visible, back link visible).
     */
    public void assertLoaded() {
        SafeUi.waitUntilVisible(title(), Duration.ofSeconds(10));
        SafeUi.waitUntilVisible(backToList(), SafeUi.DEFAULT_TIMEOUT);
    }
}