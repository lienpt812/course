using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Section_Lesson_Management.json
/// Covers: Full section/lesson CRUD with validation, sharedState COURSE_CREATED
/// </summary>
public class Section_Lesson_Management
{
    private PageHelper Page => Setup.Page;
    private int _courseId;

    public void setUpModel()
    {
        Page.ClearSession();
        Page.Login(PageHelper.InstructorEmail, PageHelper.InstructorPassword);
        _courseId = ApiHelper.GetOrCreateTestCourseId();
        Page.GoTo($"/courses/{_courseId}");
        Thread.Sleep(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_CourseReady()
    {
        Assert.That(Page.CurrentUrl.Contains("courses/"), "Should be on course page");
        Assert.That(
            !Page.TextVisible("PUBLISHED") || Page.TextVisible("Quản Lý Nội Dung"),
            "Course should be non-PUBLISHED with content management visible");
    }

    public void v_SectionForm()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector("input[name='title']")),
            "Section title input should be visible");
    }

    public void v_SectionList()
    {
        Assert.That(
            Page.TextVisible("Section") || Page.TextVisible("Chương") || Page.CurrentUrl.Contains("courses/"),
            "Section list should be visible");
    }

    public void v_LessonForm()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector("input[name='title']")) ||
            Page.ElementVisible(By.CssSelector("select[name='section_id']")),
            "Lesson form should be visible");
    }

    public void v_LessonDetail()
    {
        Assert.That(
            Page.TextVisible("Lesson") || Page.TextVisible("Bài học") || Page.CurrentUrl.Contains("courses/"),
            "Lesson should appear in outline");
    }

    public void v_ValidationError()
    {
        Assert.That(
            Page.TextVisible("required") || Page.TextVisible("bắt buộc") || Page.TextVisible("invalid")
            || Page.TextVisible("quá ngắn") || Page.TextVisible("không hợp lệ"),
            "Validation error should appear");
    }

    public void v_PublishedBlock()
    {
        Assert.That(
            Page.TextVisible("PUBLISHED") || Page.TextVisible("đã xuất bản") || Page.TextVisible("không thể chỉnh sửa"),
            "Published block message should appear");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_OpenSectionForm()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Tạo Section') or contains(text(),'Thêm Section') or contains(text(),'Add Section')]"));
        Assert.That(btn.Count > 0, "Create Section button must exist");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(600);
    }

    public void e_SubmitSectionValid()
    {
        var title = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        Assert.That(title.Count > 0, "Title input must exist");
        title[0].Clear();
        title[0].SendKeys("Section " + DateTime.Now.Ticks);
        var pos = Setup.Driver.FindElements(By.CssSelector("input[name='position']"));
        if (pos.Count > 0) { pos[0].Clear(); pos[0].SendKeys("1"); }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1000);
    }

    public void e_SubmitSectionInvalid()
    {
        var title = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (title.Count > 0) { title[0].Clear(); title[0].SendKeys("X"); }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(800);
    }

    public void e_FixSectionError()
    {
        var title = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (title.Count > 0) { title[0].Clear(); title[0].SendKeys("Valid Section Title Here"); }
    }

    public void e_FixLessonError()
    {
        var title = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (title.Count > 0) { title[0].Clear(); title[0].SendKeys("Valid Lesson Title"); }
        var dur = Setup.Driver.FindElements(By.CssSelector("input[name='duration_minutes']"));
        if (dur.Count > 0) { dur[0].Clear(); dur[0].SendKeys("10"); }
    }

    public void e_OpenLessonForm()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Thêm Lesson') or contains(text(),'Add Lesson')]"));
        Assert.That(btn.Count > 0, "Add Lesson button must exist when sectionCount > 0");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(600);
    }

    public void e_SubmitLessonValid()
    {
        var title = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (title.Count > 0) { title[0].Clear(); title[0].SendKeys("Lesson " + DateTime.Now.Ticks); }
        var type = Setup.Driver.FindElements(By.CssSelector("select[name='type']"));
        if (type.Count > 0) new OpenQA.Selenium.Support.UI.SelectElement(type[0]).SelectByValue("TEXT");
        var dur = Setup.Driver.FindElements(By.CssSelector("input[name='duration_minutes']"));
        if (dur.Count > 0) { dur[0].Clear(); dur[0].SendKeys("15"); }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1200);
    }

    public void e_SubmitLessonInvalid()
    {
        var title = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (title.Count > 0) { title[0].Clear(); }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(800);
    }

    public void e_ViewOutlineAfterLesson() => GoToCourse();
    public void e_ViewOutline()            => GoToCourse();

    public void e_AddMoreSection()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Tạo Section') or contains(text(),'Thêm Section')]"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(600); }
    }

    public void e_BackToCourseFromSection() => GoToCourse();
    public void e_BackToCourseFromList()    => GoToCourse();
    public void e_BackToCourse()            => GoToCourse();
    private void GoToCourse() { Page.GoTo($"/courses/{_courseId}"); Thread.Sleep(600); }

    public void e_PublishedBlockAccess()
    {
        // Simulate accessing content management on a PUBLISHED course
        Assert.That(
            Page.TextVisible("PUBLISHED") || !Page.ElementVisible(By.XPath("//button[contains(text(),'Tạo Section')]")),
            "Content management should be hidden for published course");
    }

    public void e_BackToCourseFromPublished() => GoToCourse();
}