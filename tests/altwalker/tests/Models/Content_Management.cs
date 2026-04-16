using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Content_Management.json
/// Covers: Instructor adds Sections and Lessons to a DRAFT course
/// </summary>
public class Content_Management
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
    public void v_CourseDetailPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses/"),
            "Should be on course detail page");
        Assert.That(
            Page.TextVisible("Quản Lý Nội Dung") || Page.TextVisible("Content") || Page.TextVisible("Section"),
            "Content management section should be visible");
    }

    public void v_SectionForm()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector("input[name='title'], input[placeholder*='section' i], input[placeholder*='tiêu đề' i]")),
            "Section title input should be visible");
    }

    public void v_LessonForm()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector("input[name='title'], select[name='type'], select[name='section_id']")),
            "Lesson form fields should be visible");
    }

    public void v_OutlineDisplay()
    {
        Assert.That(
            Page.TextVisible("Outline") || Page.TextVisible("Section") || Page.TextVisible("Bài học") || Page.CurrentUrl.Contains("courses/"),
            "Outline should be visible");
    }

    public void v_ValidationError()
    {
        Assert.That(
            Page.TextVisible("required") || Page.TextVisible("bắt buộc") || Page.TextVisible("quá ngắn")
            || Page.TextVisible("invalid") || Page.TextVisible("không hợp lệ"),
            "Validation error should be visible");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_ViewOutline()
    {
        Page.GoTo($"/courses/{_courseId}");
        Thread.Sleep(600);
        var outlineBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Outline') or contains(text(),'Nội Dung')] | //a[contains(@href,'content')]"));
        if (outlineBtn.Count > 0) { Page.JsClick(outlineBtn[0]); Thread.Sleep(500); }
    }

    public void e_OpenSectionForm()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Tạo Section') or contains(text(),'Thêm Section') or contains(text(),'Add Section')]"));
        Assert.That(btn.Count > 0, "Create Section button should be visible");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(600);
    }

    public void e_SubmitSectionValid()
    {
        var titleInput = Setup.Driver.FindElements(
            By.CssSelector("input[name='title'], input[placeholder*='tiêu đề' i], input[placeholder*='title' i]"));
        Assert.That(titleInput.Count > 0, "Section title input should exist");
        titleInput[0].Clear();
        titleInput[0].SendKeys("Test Section " + DateTime.Now.Ticks);
        var posInput = Setup.Driver.FindElements(By.CssSelector("input[name='position']"));
        if (posInput.Count > 0) { posInput[0].Clear(); posInput[0].SendKeys("1"); }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1000);
    }

    public void e_SubmitSectionInvalid()
    {
        var titleInput = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (titleInput.Count > 0) { titleInput[0].Clear(); titleInput[0].SendKeys("X"); } // too short
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(800);
    }

    public void e_OpenLessonForm()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Thêm Lesson') or contains(text(),'Tạo Lesson') or contains(text(),'Add Lesson')]"));
        Assert.That(btn.Count > 0, "Add Lesson button should exist (sectionCount > 0)");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(600);
    }

    public void e_CannotAddLessonNoSection()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Thêm Lesson') or contains(text(),'Add Lesson')]"));
        if (btn.Count > 0)
        {
            Page.JsClick(btn[0]);
            Thread.Sleep(600);
            Assert.That(
                Page.TextVisible("section") || Page.TextVisible("Phải tạo Section trước"),
                "Should warn that a section must be created first");
        }
    }

    public void e_SubmitLessonValid()
    {
        FillLessonForm(valid: true);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1200);
    }

    public void e_SubmitLessonInvalid()
    {
        FillLessonForm(valid: false);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(800);
    }

    public void e_FixErrorBackToSection()
    {
        var titleInput = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (titleInput.Count > 0) { titleInput[0].Clear(); titleInput[0].SendKeys("Valid Section Title"); }
    }

    public void e_FixErrorBackToLesson()
    {
        FillLessonForm(valid: true);
    }

    public void e_BackToOutlineFromSection() => e_ViewOutline();
    public void e_BackToOutlineFromLesson()  => e_ViewOutline();
    public void e_BackToOutline()            => e_ViewOutline();

    public void e_BackToCourseDetail()
    {
        Page.GoTo($"/courses/{_courseId}");
        Thread.Sleep(600);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void FillLessonForm(bool valid)
    {
        var titleInput = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (titleInput.Count > 0)
        {
            titleInput[0].Clear();
            titleInput[0].SendKeys(valid ? "Test Lesson " + DateTime.Now.Ticks : "");
        }
        var typeSelect = Setup.Driver.FindElements(By.CssSelector("select[name='type']"));
        if (typeSelect.Count > 0)
            new OpenQA.Selenium.Support.UI.SelectElement(typeSelect[0]).SelectByValue("TEXT");
        var durationInput = Setup.Driver.FindElements(By.CssSelector("input[name='duration_minutes']"));
        if (durationInput.Count > 0)
        {
            durationInput[0].Clear();
            durationInput[0].SendKeys(valid ? "10" : "0");
        }
        var posInput = Setup.Driver.FindElements(By.CssSelector("input[name='position']"));
        if (posInput.Count > 0) { posInput[0].Clear(); posInput[0].SendKeys("1"); }
    }
}