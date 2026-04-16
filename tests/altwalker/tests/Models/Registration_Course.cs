using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Registration_Course.json
/// Covers: Student registers for a course (success/waitlist/closed/duplicate), cancel
/// </summary>
public class Registration_Course
{
    private PageHelper Page => Setup.Page;

    public void setUpModel()
    {
        ApiHelper.SeedAll();
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/courses");
        Thread.Sleep(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_CourseListPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses"),
            "Should be on course list page");
    }

    public void v_CourseDetailPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses/") || Page.CurrentUrl.Contains("course/"),
            "Should be on course detail page");
        Assert.That(
            Page.TextVisible("Đăng ký") || Page.TextVisible("Register") || Page.TextVisible("Slot") || Page.TextVisible("Chỗ"),
            "Course detail registration info should be visible");
    }

    public void v_PendingState()
    {
        Assert.That(
            Page.TextVisible("PENDING") || Page.TextVisible("Chờ duyệt") || Page.TextVisible("Đang chờ"),
            "Should show PENDING registration status");
    }

    public void v_WaitlistState()
    {
        Assert.That(
            Page.TextVisible("WAITLIST") || Page.TextVisible("Hàng chờ") || Page.TextVisible("waitlist"),
            "Should show WAITLIST status");
    }

    public void v_RegistrationClosedError()
    {
        Assert.That(
            Page.TextVisible("closed") || Page.TextVisible("đóng") || Page.TextVisible("chưa mở") || Page.TextVisible("hết hạn"),
            "Should show registration closed error");
    }

    public void v_AlreadyRegisteredError()
    {
        Assert.That(
            Page.TextVisible("already") || Page.TextVisible("đã đăng ký") || Page.TextVisible("active registration"),
            "Should show already-registered error");
    }

    public void v_StudentDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard"),
            "Should be on student dashboard");
        Assert.That(
            Page.TextVisible("Đăng Ký") || Page.TextVisible("PENDING") || Page.TextVisible("WAITLIST") || Page.TextVisible("Lịch sử"),
            "Dashboard registration section should be visible");
    }

    public void v_CancelledState()
    {
        Assert.That(
            Page.TextVisible("CANCELLED") || Page.TextVisible("Đã hủy") || Page.TextVisible("hủy thành công"),
            "Should show CANCELLED status");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_ClickCourseCard()
    {
        var cards = Setup.Driver.FindElements(
            By.XPath("//a[contains(@href,'/courses/')] | //*[@data-testid='course-card']//a"));
        Assert.That(cards.Count > 0, "Course cards must exist on course list page");
        Page.ScrollIntoView(cards[0]);
        Thread.Sleep(200);
        Page.JsClick(cards[0]);
        Thread.Sleep(800);
    }

    public void e_RegisterSuccess()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Đăng ký ngay') or contains(text(),'Register') or contains(text(),'Đăng Ký')]"));
        Assert.That(btn.Count > 0, "Register button should be visible (courseIsOpen == true)");
        Page.ScrollIntoView(btn[0]);
        Thread.Sleep(200);
        Page.JsClick(btn[0]);
        Thread.Sleep(1200);
    }

    public void e_RegisterWaitlist()
    {
        // Click register on a full course → expect WAITLIST
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Đăng ký ngay') or contains(text(),'Vào hàng chờ') or contains(text(),'Waitlist')]"));
        if (btn.Count > 0)
        {
            Page.ScrollIntoView(btn[0]);
            Thread.Sleep(200);
            Page.JsClick(btn[0]);
            Thread.Sleep(1200);
        }
    }

    public void e_RegisterClosedError()
    {
        // courseIsOpen == false: try to register and expect error
        var btn = Setup.Driver.FindElements(By.XPath("//button[contains(text(),'Đăng ký')]"));
        if (btn.Count > 0)
        {
            Page.JsClick(btn[0]);
            Thread.Sleep(800);
        }
        Assert.That(
            Page.TextVisible("closed") || Page.TextVisible("đóng") || Page.TextVisible("chưa mở"),
            "Should show closed error");
    }

    public void e_RegisterDuplicateError()
    {
        var btn = Setup.Driver.FindElements(By.XPath("//button[contains(text(),'Đăng ký')]"));
        if (btn.Count > 0)
        {
            Page.JsClick(btn[0]);
            Thread.Sleep(800);
        }
        Assert.That(
            Page.TextVisible("already") || Page.TextVisible("đã đăng ký") || Page.TextVisible("active"),
            "Should show duplicate registration error");
    }

    public void e_BackToCourseListFromDetail() => GoToCourseList();
    public void e_BackToCourseListFromError()  => GoToCourseList();
    public void e_BackToCourseList()           => GoToCourseList();
    private void GoToCourseList() { Page.GoTo("/courses"); Thread.Sleep(600); }

    public void e_ViewDashboardFromPending()  => GoToDashboard();
    public void e_ViewDashboardFromWaitlist() => GoToDashboard();
    public void e_ViewDashboard()             => GoToDashboard();
    private void GoToDashboard() { Page.GoTo("/dashboard"); Thread.Sleep(800); }

    public void e_CancelRegistration()
    {
        var cancelBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Hủy đăng ký') or contains(text(),'Cancel') or contains(text(),'Hủy')]"));
        Assert.That(cancelBtn.Count > 0, "Cancel button must be visible on dashboard");
        Page.ScrollIntoView(cancelBtn[0]);
        Thread.Sleep(200);
        Page.JsClick(cancelBtn[0]);
        // Confirm dialog
        Thread.Sleep(500);
        var confirm = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Xác nhận') or contains(text(),'OK') or contains(text(),'Có')]"));
        if (confirm.Count > 0) { Page.JsClick(confirm[0]); Thread.Sleep(1000); }
    }

    public void e_BackToCourseListFromDashboard() => GoToCourseList();
    public void e_BackToCourseListFromCancelled()  => GoToCourseList();
}