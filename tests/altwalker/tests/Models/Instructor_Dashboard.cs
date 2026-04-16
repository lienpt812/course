using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Instructor_Dashboard.json
/// Covers: Instructor views dashboard, courses, registrations, content management, public browse
/// </summary>
public class Instructor_Dashboard
{
    private PageHelper Page => Setup.Page;

    public void setUpModel()
    {
        Page.ClearSession();
        Page.Login(PageHelper.InstructorEmail, PageHelper.InstructorPassword);
        Page.GoTo("/dashboard");
        Page.WaitForUrl("dashboard");
        Thread.Sleep(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_InstructorDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on instructor dashboard");
        Assert.That(
            Page.TextVisible("Khoá Học") || Page.TextVisible("Dashboard") || Page.TextVisible("Tổng"),
            "Dashboard stats should be visible");
    }

    public void v_MyCourseList()
    {
        Assert.That(
            Page.TextVisible("Khoá Học Của Tôi") || Page.TextVisible("My Courses") || Page.CurrentUrl.Contains("courses"),
            "My course list should be visible");
    }

    public void v_CourseDetailPage()
    {
        Assert.That(Page.CurrentUrl.Contains("courses/"), "Should be on course detail page");
    }

    public void v_EditCourseForm()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector("input[name='title']")) ||
            Page.CurrentUrl.Contains("edit"),
            "Edit form should be visible");
    }

    public void v_RegistrationManagement()
    {
        Assert.That(
            Page.TextVisible("Đăng Ký") || Page.TextVisible("Registration") || Page.TextVisible("PENDING"),
            "Registration management should be visible");
    }

    public void v_ApproveSuccess()
    {
        Assert.That(
            Page.TextVisible("CONFIRMED") || Page.TextVisible("Đã duyệt") || Page.TextVisible("thành công"),
            "Approve success should be shown");
    }

    public void v_RejectSuccess()
    {
        Assert.That(
            Page.TextVisible("REJECTED") || Page.TextVisible("Đã từ chối") || Page.TextVisible("thành công"),
            "Reject success should be shown");
    }

    public void v_BulkApproveResult()
    {
        Assert.That(
            !Page.TextVisible("PENDING") || Page.TextVisible("thành công"),
            "Bulk approve should clear all PENDING items");
    }

    public void v_ContentManagement()
    {
        Assert.That(
            Page.TextVisible("Section") || Page.TextVisible("Lesson") || Page.TextVisible("Nội Dung"),
            "Content management section should be visible");
    }

    public void v_PublishedBlock()
    {
        Assert.That(
            Page.TextVisible("PUBLISHED") || Page.TextVisible("không thể chỉnh sửa"),
            "Published block should be visible");
    }

    public void v_OtherInstructorCourse()
    {
        Assert.That(
            !Page.ElementVisible(By.XPath("//button[contains(text(),'Chỉnh sửa')]")),
            "Edit button should NOT be visible for another instructor's course");
    }

    public void v_CourseListPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses"),
            "Should be on public course list page");
        Assert.That(
            !Page.ElementVisible(By.XPath("//button[contains(text(),'Đăng ký ngay')]")),
            "Instructor should NOT see Register button on course list");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_ViewMyCourses()
    {
        var link = Setup.Driver.FindElements(
            By.XPath("//a[contains(text(),'Khoá Học Của Tôi') or contains(@href,'my-courses')] | //button[contains(text(),'Khoá Học')]"));
        if (link.Count > 0) { Page.JsClick(link[0]); Thread.Sleep(600); }
        else { Page.GoTo("/dashboard/courses"); Thread.Sleep(600); }
    }

    public void e_ClickCourseOwned()
    {
        var links = Setup.Driver.FindElements(By.XPath("//a[contains(@href,'/courses/')]"));
        Assert.That(links.Count > 0, "At least one owned course must be visible");
        Page.ScrollIntoView(links[0]);
        Page.JsClick(links[0]);
        Thread.Sleep(800);
    }

    public void e_ClickEditDraft()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Chỉnh sửa') or contains(text(),'Edit')]"));
        Assert.That(btn.Count > 0, "Edit button must be visible for DRAFT course");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(700);
    }

    public void e_CannotEditPublished()
    {
        Assert.That(
            Page.TextVisible("PUBLISHED") || !Page.ElementVisible(By.XPath("//button[contains(text(),'Chỉnh sửa')]")),
            "Edit should be blocked for published course");
    }

    public void e_SaveEditSuccess()
    {
        var titleInput = Setup.Driver.FindElements(By.CssSelector("input[name='title']"));
        if (titleInput.Count > 0) { titleInput[0].Clear(); titleInput[0].SendKeys("Updated " + DateTime.Now.Ticks); }
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1200);
    }

    public void e_CancelEdit()
    {
        var cancel = Setup.Driver.FindElements(By.XPath("//button[contains(text(),'Hủy') or contains(text(),'Cancel')]"));
        if (cancel.Count > 0) { Page.JsClick(cancel[0]); Thread.Sleep(600); }
        else { Page.GoTo("/dashboard"); Thread.Sleep(600); }
    }

    public void e_ViewRegistrations()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Quản lý đăng ký') or contains(text(),'Registrations')] | //a[contains(@href,'registrations')]"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(700); }
    }

    public void e_ApproveSingle()
    {
        var btns = Setup.Driver.FindElements(By.XPath("//button[normalize-space(text())='Duyệt']"));
        if (btns.Count > 0) { Page.ScrollIntoView(btns[0]); Page.JsClick(btns[0]); Thread.Sleep(1000); }
        else Assert.Warn("No Duyệt button visible");
    }

    public void e_RejectSingle()
    {
        var btns = Setup.Driver.FindElements(By.XPath("//button[normalize-space(text())='Từ chối']"));
        if (btns.Count > 0) { Page.ScrollIntoView(btns[0]); Page.JsClick(btns[0]); Thread.Sleep(1000); }
        else Assert.Warn("No Từ chối button visible");
    }

    public void e_BulkApprove()
    {
        var btns = Setup.Driver.FindElements(By.XPath("//button[contains(text(),'Duyệt tất cả')]"));
        if (btns.Count > 0) { Page.ScrollIntoView(btns[0]); Page.JsClick(btns[0]); Thread.Sleep(1200); }
        else Assert.Warn("Bulk approve button not found");
    }

    public void e_BackToRegistrations()           { Page.GoTo("/dashboard"); Thread.Sleep(500); }
    public void e_BackToRegistrationsFromReject() => e_BackToRegistrations();
    public void e_BackToRegistrationsFromBulk()   => e_BackToRegistrations();

    public void e_ManageContent()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Quản lý nội dung') or contains(text(),'Content')] | //a[contains(@href,'content')]"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(600); }
    }

    public void e_BackToCourseDetail()
    {
        var btn = Setup.Driver.FindElements(By.XPath("//button[contains(text(),'Quay lại')] | //a[contains(text(),'Quay lại')]"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(500); }
        else { Page.GoTo("/dashboard"); Thread.Sleep(500); }
    }

    public void e_BackToMyCourses()   { Page.GoTo("/dashboard/courses"); Thread.Sleep(500); }
    public void e_BackToDashboard()   { Page.GoTo("/dashboard"); Thread.Sleep(500); }
    public void e_BackToDashboardFromPublished() => e_BackToDashboard();

    public void e_ViewPublicCourses()
    {
        Page.GoTo("/courses");
        Thread.Sleep(600);
    }

    public void e_ClickOtherCourse()
    {
        var links = Setup.Driver.FindElements(By.XPath("//a[contains(@href,'/courses/')]"));
        if (links.Count > 0) { Page.ScrollIntoView(links[0]); Page.JsClick(links[0]); Thread.Sleep(700); }
    }

    public void e_BackToCourseList()
    {
        Page.GoTo("/courses");
        Thread.Sleep(600);
    }
}