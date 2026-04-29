package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.MbtTestIds;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * /instructor/dashboard — InstructorDashboard.tsx
 *
 * UI layout:
 *   h1 "Dashboard Giảng Viên"
 *   Stat cards: Tổng Khóa Học, Học Viên Xác Nhận, Tổng Đăng Ký, Tỷ Lệ Lấp Đầy
 *   "Thêm khóa học" button → shows inline create course form
 *
 *   Create Course form (shown when showCreateForm=true):
 *     input[placeholder="Tên khóa học"]      — title (3-255 chars)
 *     input[placeholder^="Để trống"]         — slug (auto-generated from title, editable)
 *     input[placeholder="Danh mục"]          — category
 *     input[placeholder="https://..."]       — image_url
 *     input[placeholder="Sĩ số"]            — max_capacity (number)
 *     input[placeholder="Số giờ học"]       — estimated_hours (number)
 *     textarea[placeholder="Mô tả khóa học"] — description (min 20 chars)
 *     select[0] — level: Beginner | Intermediate | Advanced
 *     select[1] — status: DRAFT | PUBLISHED | COMING_SOON
 *     button "Lưu khóa học"
 *     button "Hủy"
 *     p.text-red-600 — validation errors
 *
 *   Courses table: thead + tbody tr (one per instructor course)
 *     Link "Chỉnh Sửa" → /courses/{id} (navigates to CourseDetailPage)
 *
 *   Section/Lesson manager (always visible below table):
 *     select "Chọn khóa học" — course selector
 *     Section form:
 *       input[placeholder="Tên section"]   — section title (min 2 chars)
 *       input[type=number] position        — section position
 *       button "Lưu section"
 *     Lesson form:
 *       select "Chọn section"             — section selector
 *       select (type)                     — VIDEO | TEXT | DOC | QUIZ
 *       input[placeholder="Tên lesson"]   — lesson title
 *       input[placeholder="Thời lượng"]   — duration_minutes (number)
 *       input[placeholder="Thứ tự"]       — position (number)
 *       input[placeholder="Content URL (optional)"]
 *       button "Lưu lesson"
 *     "Outline hiện tại" section heading + outline tree
 *
 * Backend endpoints used:
 *   GET /dashboards/instructor
 *   POST /courses
 *   GET /learning/courses/{id}/outline
 *   POST /learning/sections
 *   POST /learning/lessons
 *
 * FE validations for course create:
 *   - title: 3-255 chars
 *   - description: >= 20 chars
 *   - slug: non-empty after normalizeSlug()
 *   - slug: <= 255 chars
 */
public class InstructorDashboardPage {

    public InstructorDashboardPage open() {
        Selenide.open("/instructor/dashboard");
        return this;
    }

    public InstructorDashboardPage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), Duration.ofSeconds(12))
                .shouldHave(Condition.text(UiText.INSTRUCTOR_DASHBOARD_H1), Duration.ofSeconds(12));
        return this;
    }

    // ========== top-level actions ==========

    /** "Thêm khóa học" button — toggles the create course form */
    public SelenideElement addCourseButton()  { return $(byText("Thêm khóa học")); }

    // ========== create course form ==========

    public SelenideElement saveCourseButton() { return $(byText("Lưu khóa học")); }
    public SelenideElement cancelButton()     { return $(byText("Hủy")); }

    /** Error shown when FE validation fails for course create */
    public SelenideElement errorText()        { return $("p.text-red-600"); }

    public SelenideElement titleInput()       { return $("input[placeholder='Tên khóa học']"); }

    /**
     * Slug input — placeholder starts with "Để trống" (auto-generated from title if blank).
     * Instructor can override but must be url-safe after normalizeSlug().
     */
    public SelenideElement slugInput()        { return $("input[placeholder^='Để trống']"); }

    public SelenideElement categoryInput()    { return $("input[placeholder='Danh mục']"); }
    public SelenideElement imageUrlInput()    { return $("input[placeholder='https://...']"); }
    public SelenideElement capacityInput()    { return $("input[placeholder='Sĩ số']"); }
    public SelenideElement hoursInput()       { return $("input[placeholder='Số giờ học']"); }
    public SelenideElement descriptionInput() { return $("textarea[placeholder='Mô tả khóa học']"); }

    /**
     * Level select — first select within the create form.
     * Options: "Beginner", "Intermediate", "Advanced"
     * Note: uses index 0 within the form context. Using $$(select).get(0) is fragile
     * when section/lesson selects are also visible — scope to form container when possible.
     */
    public SelenideElement levelSelect()      { return $$("select").get(0); }

    /**
     * Status select — second select within the create form.
     * Options: "DRAFT", "PUBLISHED", "COMING_SOON"
     */
    public SelenideElement statusSelect()     { return $$("select").get(1); }

    // ========== courses table ==========

    public ElementsCollection courseRows()    { return $$("table tbody tr"); }

    /** "Chỉnh Sửa" links in the courses table — navigate to /courses/{id} */
    public ElementsCollection editLinks()     { return $$("a").filter(Condition.text("Chỉnh Sửa")); }

    // ========== section / lesson manager ==========

    /**
     * Course selector in the section/lesson manager — {@code data-testid} first (stable),
     * then the {@code <select>} whose options include the placeholder "Chọn khóa học".
     */
    public SelenideElement courseSelect() {
        return $("[data-testid='" + MbtTestIds.INSTRUCTOR_MANAGE_COURSE_SELECT + "']");
    }

    /** Fallback for older FE bundles without {@link MbtTestIds#INSTRUCTOR_MANAGE_COURSE_SELECT}. */
    public SelenideElement courseSelectByPlaceholderOption() {
        return $$("select").findBy(Condition.text("Chọn khóa học"));
    }

    public SelenideElement sectionTitleInput()   { return $("input[placeholder='Tên section']"); }

    /** "Lưu section" button */
    public SelenideElement saveSectionButton()   { return $(byText("Lưu section")); }

    /**
     * Section selector in the lesson form.
     * Identified by the placeholder option text "Chọn section".
     */
    public SelenideElement sectionSelect() {
        return $$("select").findBy(Condition.text("Chọn section"));
    }

    public SelenideElement lessonTitleInput()    { return $("input[placeholder='Tên lesson']"); }

    /**
     * Lesson type select — identified by having "VIDEO" as one of its options.
     * Options: VIDEO, TEXT, DOC, QUIZ
     */
    public SelenideElement lessonTypeSelect() {
        return $$("select").findBy(Condition.text("VIDEO"));
    }

    public SelenideElement lessonDurationInput() { return $("input[placeholder='Thời lượng']"); }
    public SelenideElement lessonPositionInput()  { return $("input[placeholder='Thứ tự']"); }
    public SelenideElement lessonContentUrlInput(){ return $("input[placeholder='Content URL (optional)']"); }

    /** "Lưu lesson" button */
    public SelenideElement saveLessonButton()    { return $(byText("Lưu lesson")); }

    /**
     * "Outline hiện tại" section — the block containing the course outline tree.
     * Using parent() to get the container div.
     */
    public SelenideElement outlineBlock()        { return $(byText("Outline hiện tại")).parent(); }

    // ========== helpers ==========

    /**
     * Assert create form is visible (triggered by clicking "Thêm khóa học").
     */
    public void assertCreateFormVisible() {
        SafeUi.waitUntilVisible(titleInput(), Duration.ofSeconds(5));
        SafeUi.waitUntilVisible(saveCourseButton(), SafeUi.DEFAULT_TIMEOUT);
    }

    /**
     * Assert the section manager is ready (course selector visible).
     * Scroll: block is below the fold; the nav may delay paint until the section is in view.
     */
    public void assertSectionManagerVisible() {
        SelenideElement sectionHeading = $(byText("Quản Lý Nội Dung Khóa Học"));
        SafeUi.waitUntilVisible(sectionHeading, Duration.ofSeconds(8));
        Selenide.executeJavaScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'})", sectionHeading);
        SelenideElement sel = courseSelect();
        if (!SafeUi.tryWaitUntilVisible(sel, Duration.ofSeconds(4))) {
            sel = courseSelectByPlaceholderOption();
        }
        SafeUi.waitUntilVisible(sel, Duration.ofSeconds(12));
        SafeUi.waitUntilVisible(sectionTitleInput(), Duration.ofSeconds(8));
    }
}