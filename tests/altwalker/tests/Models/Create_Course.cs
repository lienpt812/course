using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Create_Course.json
/// Covers: Instructor creates, edits, publishes course; validation errors
/// </summary>
public class Create_Course
{
    private PageHelper Page => Setup.Page;
    private readonly string _slug = "test-course-" + DateTime.Now.Ticks;

    public void setUpModel()
    {
        Page.ClearSession();
        Page.Login(PageHelper.InstructorEmail, PageHelper.InstructorPassword);
        Page.GoTo("instructor/dashboard");
        Page.WaitForUrl("instructor/dashboard");
        Thread.Sleep(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_InstructorDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on instructor dashboard");
        Assert.That(
            Page.TextVisible("Tạo khoá học") || Page.TextVisible("Create Course") || Page.TextVisible("Khoá Học"),
            "Dashboard should show course management");
    }

    public void v_CreateCourseForm()
    {
        Assert.That(
            Page.CurrentUrl.Contains("create") || Page.CurrentUrl.Contains("new"),
            "Should be on create-course form");
        Assert.That(
            Page.ElementVisible(By.CssSelector("input[name='title'], input[placeholder*='title' i]")),
            "Title input should be visible");
    }

    public void v_CourseDetailPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses/") || Page.CurrentUrl.Contains("course/"),
            "Should be on course detail page");
        Assert.That(
            Page.TextVisible("DRAFT") || Page.TextVisible("Chỉnh sửa") || Page.TextVisible("Edit"),
            "Should show DRAFT status or edit button");
    }

    public void v_EditCourseForm()
    {
        Assert.That(
            Page.CurrentUrl.Contains("edit") || Page.TextVisible("Chỉnh sửa") || Page.TextVisible("Edit Course"),
            "Should be on edit course form");
    }

    public void v_ValidationError()
    {
        Assert.That(
            Page.TextVisible("required") || Page.TextVisible("bắt buộc") || Page.TextVisible("already exists")
            || Page.TextVisible("tồn tại") || Page.TextVisible("invalid") || Page.TextVisible("không hợp lệ"),
            "Should show validation error message");
    }

    public void v_PublishedCourse()
    {
        Assert.That(
            Page.TextVisible("PUBLISHED") || Page.TextVisible("Đã xuất bản") || Page.TextVisible("đã published"),
            "Should show PUBLISHED status");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_ClickCreateCourse()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Tạo khoá học') or contains(text(),'Create Course') or contains(text(),'Tạo Khoá')]" +
                     " | //a[contains(@href,'create') or contains(@href,'new')]"));
        if (btn.Count > 0) { Page.ScrollIntoView(btn[0]); Page.JsClick(btn[0]); Thread.Sleep(800); }
        else { Page.GoTo("/courses/create"); Thread.Sleep(600); }
    }

    public void e_SubmitValidForm()
    {
        FillCourseForm(title: "Test Course " + DateTime.Now.Ticks, slug: _slug);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_SubmitDuplicateSlug()
    {
        FillCourseForm(title: "Duplicate Slug Course", slug: "existing-slug");
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1000);
    }

    public void e_SubmitMissingFields()
    {
        // Submit form without filling required fields
        var titleInput = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (titleInput.Count > 0) { titleInput[0].Clear(); }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(800);
    }

    public void e_FixAndResubmit()
    {
        FillCourseForm(title: "Fixed Course " + DateTime.Now.Ticks, slug: "fixed-" + DateTime.Now.Ticks);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_CancelCreate()
    {
        var cancelBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Hủy') or contains(text(),'Cancel')]"));
        if (cancelBtn.Count > 0) { Page.JsClick(cancelBtn[0]); Thread.Sleep(600); }
        else { Page.GoTo("/dashboard"); Thread.Sleep(600); }
    }

    public void e_ClickEditDraft()
    {
        var editBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Chỉnh sửa') or contains(text(),'Edit')] | //a[contains(@href,'edit')]"));
        Assert.That(editBtn.Count > 0, "Edit button should be visible for DRAFT course");
        Page.ScrollIntoView(editBtn[0]);
        Page.JsClick(editBtn[0]);
        Thread.Sleep(800);
    }

    public void e_CannotEditPublished()
    {
        Assert.That(
            Page.TextVisible("PUBLISHED") || Page.TextVisible("không thể chỉnh sửa") || Page.TextVisible("cannot edit"),
            "Should indicate course is published and cannot be edited");
    }

    public void e_SaveEditSuccess()
    {
        var titleInput = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (titleInput.Count > 0)
        {
            titleInput[0].Clear();
            titleInput[0].SendKeys("Updated Course Title " + DateTime.Now.Ticks);
        }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1200);
    }

    public void e_PublishCourse()
    {
        var publishBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Xuất bản') or contains(text(),'Publish')]"));
        if (publishBtn.Count > 0)
        {
            Page.ScrollIntoView(publishBtn[0]);
            Page.JsClick(publishBtn[0]);
            Thread.Sleep(1000);
            var confirm = Setup.Driver.FindElements(
                By.XPath("//button[contains(text(),'Xác nhận') or contains(text(),'OK')]"));
            if (confirm.Count > 0) { Page.JsClick(confirm[0]); Thread.Sleep(1000); }
        }
    }

    public void e_CancelEdit()
    {
        var cancelBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Hủy') or contains(text(),'Cancel')]"));
        if (cancelBtn.Count > 0) { Page.JsClick(cancelBtn[0]); Thread.Sleep(600); }
        else { Page.GoTo("/dashboard"); Thread.Sleep(600); }
    }

    public void e_BackToDashboard()               { Page.GoTo("/dashboard"); Thread.Sleep(600); }
    public void e_BackToDashboardFromPublished()   => e_BackToDashboard();

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void FillCourseForm(string title, string slug)
    {
        void Fill(string name, string val)
        {
            var els = Setup.Driver.FindElements(By.CssSelector($"input[name='{name}'], textarea[name='{name}']"));
            if (els.Count > 0) { els[0].Clear(); els[0].SendKeys(val); }
        }
        Fill("title", title);
        Fill("slug", slug);
        Fill("description", "Auto-generated test course description.");
        Fill("max_capacity", "10");
        Fill("price", "0");
    }
}