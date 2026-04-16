using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Update_Learning_Progress.json
/// Covers: Student marks lessons done, progress bar, completes course, claims certificate
/// </summary>
public class Update_Learning_Progress
{
    private PageHelper Page => Setup.Page;
    private int _courseId;

    public void setUpModel()
    {
        // Ensure a CONFIRMED registration and course with lessons exist
        ApiHelper.SeedAll();
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        _courseId = GetConfirmedCourseId();
        Page.GoTo($"/learn/{_courseId}");
        Thread.Sleep(1000);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_LearningPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("learn") || Page.CurrentUrl.Contains("learning"),
            "Should be on learning page");
    }

    public void v_AccessDenied()
    {
        Assert.That(
            Page.TextVisible("chưa được xác nhận") || Page.TextVisible("Access denied") || Page.TextVisible("Bạn chưa"),
            "Access denied message should be visible");
    }

    public void v_LessonContent()
    {
        Assert.That(
            Page.TextVisible("Bài học") || Page.TextVisible("Lesson") ||
            Page.ElementVisible(By.CssSelector(".lesson-content, .sidebar, [data-testid='lesson-sidebar']")),
            "Lesson content/sidebar should be visible");
    }

    public void v_ProgressUpdated()
    {
        Assert.That(
            Page.TextVisible("Hoàn thành") || Page.TextVisible("Done") || Page.TextVisible("%") ||
            Page.ElementVisible(By.CssSelector(".progress-bar, [role='progressbar']")),
            "Progress update should be reflected");
    }

    public void v_CourseCompleted()
    {
        Assert.That(
            Page.TextVisible("100%") || Page.TextVisible("Hoàn thành khoá học") || Page.TextVisible("Nhận chứng chỉ"),
            "Course completion state should be shown");
    }

    public void v_CertificateModal()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector(".modal, [role='dialog']")) ||
            Page.TextVisible("Chúc mừng") || Page.TextVisible("Congratulations") || Page.TextVisible("chứng chỉ"),
            "Certificate modal should appear");
    }

    public void v_StudentDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on student dashboard");
    }

    public void v_RevisitWithCert()
    {
        Assert.That(
            Page.TextVisible("Đã có chứng chỉ") || Page.TextVisible("Xem") ||
            Page.ElementVisible(By.CssSelector(".cert-banner, [data-testid='cert-banner']")),
            "Certificate banner should appear when revisiting completed course");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_AccessConfirmed()
    {
        // Guard: registrationStatus == CONFIRMED — page should load normally
        Assert.That(
            !Page.TextVisible("chưa được xác nhận"),
            "Access should be granted for confirmed registration");
    }

    public void e_AccessDenied()
    {
        Assert.That(
            Page.TextVisible("chưa được xác nhận") || Page.TextVisible("Access denied"),
            "Access should be denied for non-confirmed registration");
    }

    public void e_BackToDashboardFromDenied() => GoToDashboard();

    public void e_SelectLesson()
    {
        var lessons = Setup.Driver.FindElements(
            By.CssSelector(".lesson-item, [data-testid='lesson-item'], .sidebar-lesson"));
        if (lessons.Count > 1)
        {
            Page.ScrollIntoView(lessons[1]);
            Page.JsClick(lessons[1]);
            Thread.Sleep(600);
        }
    }

    public void e_MarkLessonDone()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Đánh dấu hoàn thành') or contains(text(),'Mark Done') or contains(text(),'Hoàn thành')]"));
        Assert.That(btn.Count > 0, "Mark Done button must exist (completedCount < totalLessons)");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(1200);
    }

    public void e_ContinueLearning()
    {
        // Move to next lesson
        var lessons = Setup.Driver.FindElements(
            By.XPath("//li[not(contains(@class,'done'))]//a | //*[contains(@class,'lesson-item') and not(contains(@class,'completed'))]"));
        if (lessons.Count > 0) { Page.JsClick(lessons[0]); Thread.Sleep(600); }
    }

    public void e_AllLessonsDone()
    {
        Assert.That(
            Page.TextVisible("100%") || Page.TextVisible("Hoàn thành") || Page.TextVisible("Nhận chứng chỉ"),
            "All lessons done — should show completion state");
    }

    public void e_ClaimCertificate()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Nhận chứng chỉ') or contains(text(),'Claim Certificate') or contains(text(),'Get Certificate')]"));
        Assert.That(btn.Count > 0, "Claim certificate button must exist (certEnabled == true)");
        Page.ScrollIntoView(btn[0]);
        Page.JsClick(btn[0]);
        Thread.Sleep(1500);
    }

    public void e_NoCertificate()
    {
        Assert.That(!Page.TextVisible("Nhận chứng chỉ"), "Certificate button should NOT appear when certEnabled == false");
    }

    public void e_ViewCertGoToDashboard()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Xem chứng chỉ') or contains(text(),'View Certificate')] | //a[contains(@href,'dashboard')]"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(800); }
        else GoToDashboard();
    }

    public void e_ContinueLearningFromModal()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Tiếp tục học') or contains(text(),'Continue Learning')]"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(800); }
    }

    public void e_BackToDashboardFromLesson() => GoToDashboard();
    public void e_BackToDashboard()           => GoToDashboard();
    private void GoToDashboard() { Page.GoTo("/dashboard"); Thread.Sleep(600); }

    public void e_RevisitCompletedCourse()
    {
        Page.GoTo($"/learn/{_courseId}");
        Thread.Sleep(1000);
    }

    public void e_ViewCertBannerModal()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Xem') or contains(text(),'View')] | //*[contains(@class,'cert-banner')]//button"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(800); }
    }

    public void e_BackToDashboardFromRevisit() => GoToDashboard();

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static int GetConfirmedCourseId()
    {
        try
        {
            var resp = ApiHelper.Get("/dashboards/student", ApiHelper.GetStudentToken());
            var obj  = Newtonsoft.Json.Linq.JObject.Parse(resp);
            var courses = obj["confirmed_courses"] ?? obj["current_courses"] ?? obj["data"]?["courses"];
            if (courses is Newtonsoft.Json.Linq.JArray arr && arr.Count > 0)
                return arr[0]["course_id"]?.ToObject<int>() ?? arr[0]["id"]?.ToObject<int>() ?? 1;
        }
        catch { }
        return 1; // fallback
    }
}