using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Student_Dashboard.json
/// Covers: Student views courses, certs, registration history, insight modal, learning page
/// </summary>
public class Student_Dashboard
{
    private PageHelper Page => Setup.Page;

    public void setUpModel()
    {
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/dashboard");
        Page.WaitForUrl("dashboard");
        Thread.Sleep(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_StudentDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on student dashboard");
        Assert.That(
            Page.TextVisible("Dashboard") || Page.TextVisible("Khoá Học") || Page.TextVisible("Đang học"),
            "Dashboard content should be visible");
    }

    public void v_MyCourseCards()
    {
        Assert.That(
            Page.TextVisible("Khoá Học Của Tôi") || Page.TextVisible("Tiếp tục học") || Page.TextVisible("Continue"),
            "My Course cards section should be visible");
    }

    public void v_LearningPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("learn") || Page.CurrentUrl.Contains("learning"),
            "Should be on learning page");
    }

    public void v_CertificateSection()
    {
        Assert.That(
            Page.TextVisible("Chứng Chỉ") || Page.TextVisible("Certificate") || Page.TextVisible("Verification"),
            "Certificate section should be visible");
    }

    public void v_RegistrationHistory()
    {
        Assert.That(
            Page.TextVisible("Lịch Sử Đăng Ký") || Page.TextVisible("Registration History") || Page.TextVisible("PENDING"),
            "Registration history should be visible");
    }

    public void v_CourseDetailFromHistory()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses/"),
            "Should be on course detail page from history link");
    }

    public void v_NoCertPlaceholder()
    {
        Assert.That(
            Page.TextVisible("Hoàn thành khoá học") || Page.TextVisible("Chưa có chứng chỉ") || Page.TextVisible("Complete"),
            "No-cert placeholder text should be visible");
    }

    public void v_InsightModal()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector(".modal, [role='dialog'], [data-testid='insight-modal']")) ||
            Page.TextVisible("Chi tiết") || Page.TextVisible("Insight"),
            "Insight modal should be visible");
    }

    public void v_CourseListPage()
    {
        Assert.That(Page.CurrentUrl.Contains("courses"), "Should be on course list page");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_ViewMyCourseCards()
    {
        var section = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'Khoá Học Của Tôi') or contains(text(),'My Courses')]"));
        if (section.Count > 0) Page.ScrollIntoView(section[0]);
        Thread.Sleep(400);
    }

    public void e_ClickContinueLearning()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Tiếp tục') or contains(text(),'Continue')] | //a[contains(@href,'/learn/')]"));
        Assert.That(btn.Count > 0, "Continue Learning button must exist (currentCourses > 0)");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(1000);
    }

    public void e_BackToDashboardFromLearning() => GoToDashboard();
    public void e_BackToDashboard()             => GoToDashboard();
    private void GoToDashboard() { Page.GoTo("/dashboard"); Thread.Sleep(600); }

    public void e_ViewCertSection()
    {
        var section = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'Chứng Chỉ Của Tôi') or contains(text(),'My Certificates')]"));
        if (section.Count > 0) Page.ScrollIntoView(section[0]);
        Thread.Sleep(400);
    }

    public void e_ViewNoCertPlaceholder()
    {
        var section = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'Chứng Chỉ') or contains(text(),'Certificate')]"));
        if (section.Count > 0) Page.ScrollIntoView(section[0]);
        Thread.Sleep(400);
    }

    public void e_BackToDashboardFromCert()     => GoToDashboard();
    public void e_BackToDashboardFromNoCert()    => GoToDashboard();

    public void e_ViewRegistrationHistory()
    {
        var section = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'Lịch Sử Đăng Ký') or contains(text(),'Registration History')]"));
        if (section.Count > 0) Page.ScrollIntoView(section[0]);
        else
        {
            var link = Setup.Driver.FindElements(By.XPath("//a[contains(@href,'history') or contains(@href,'registrations')]"));
            if (link.Count > 0) { Page.JsClick(link[0]); Thread.Sleep(600); }
        }
        Thread.Sleep(400);
    }

    public void e_ClickViewDetail()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Xem Chi Tiết') or contains(text(),'View')] | //a[contains(@href,'/courses/')]"));
        if (btn.Count > 0) { Page.ScrollIntoView(btn[0]); Page.JsClick(btn[0]); Thread.Sleep(800); }
    }

    public void e_BackToDashboardFromHistory()     => GoToDashboard();
    public void e_BackToDashboardFromCourseDetail() => GoToDashboard();

    public void e_ClickStatCard()
    {
        var cards = Setup.Driver.FindElements(
            By.CssSelector(".stat-card, [data-testid='stat-card'], .dashboard-stat"));
        if (cards.Count > 0) { Page.ScrollIntoView(cards[0]); Page.JsClick(cards[0]); Thread.Sleep(700); }
    }

    public void e_CloseInsightModal()
    {
        var closeBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Đóng') or contains(text(),'Close') or contains(@aria-label,'close')]"));
        if (closeBtn.Count > 0) { Page.JsClick(closeBtn[0]); Thread.Sleep(400); }
        else
        {
            // Click outside modal
            try { Setup.Driver.FindElement(By.CssSelector(".modal-backdrop, .overlay")).Click(); } catch { }
            Thread.Sleep(400);
        }
    }

    public void e_GoToCourseList()
    {
        Page.GoTo("/courses");
        Thread.Sleep(600);
    }

    public void e_BackToDashboardFromCourseList() => GoToDashboard();
    public void e_BackToDashboardFromMyCourses()  => GoToDashboard();
}