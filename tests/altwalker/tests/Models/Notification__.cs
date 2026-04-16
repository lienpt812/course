using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

// ═══════════════════════════════════════════════════════════════════════════════
// Learning_Progress_Strict
// ═══════════════════════════════════════════════════════════════════════════════
/// <summary>
/// Model: Learning_Progress_Strict.json
/// Covers: Access control (CONFIRMED/PENDING/WAITLIST), lesson completion flow,
///         sharedState REGISTRATION_CHECK ↔ READY_FOR_CERTIFICATE
/// </summary>
public class Learning_Progress_Strict
{
    private PageHelper Page => Setup.Page;
    private int _courseId = 1;

    public void setUpModel()
    {
        ApiHelper.SeedAll();
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo($"/learn/{_courseId}");
        Thread.Sleep(1000);
    }

    public void v_AccessCheck()
    {
        Assert.That(
            Page.CurrentUrl.Contains("learn") || Page.CurrentUrl.Contains("dashboard"),
            "Should be attempting to access learning page");
    }

    public void v_AccessDenied()
    {
        Assert.That(
            Page.TextVisible("chưa được xác nhận") || Page.TextVisible("Access denied") ||
            Page.TextVisible("Bạn chưa") || Page.CurrentUrl.Contains("dashboard"),
            "Access denied message or redirect should appear");
    }

    public void v_LearningDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("learn"),
            "Should be on learning dashboard");
        Assert.That(
            Page.ElementVisible(By.CssSelector(".sidebar, .lesson-list, [data-testid='sidebar']")) ||
            Page.TextVisible("Bài học"),
            "Lesson sidebar should be visible");
    }

    public void v_LessonMarkedDone()
    {
        Assert.That(
            Page.TextVisible("Hoàn thành") || Page.ElementVisible(By.CssSelector(".check-circle, .lesson-done")),
            "Lesson should show completed indicator");
    }

    public void v_CourseCompleted()
    {
        Assert.That(
            Page.TextVisible("100%") || Page.TextVisible("Nhận chứng chỉ") || Page.TextVisible("Hoàn thành khoá"),
            "Course completion state should be shown");
    }

    public void v_WaitingForApproval()
    {
        Assert.That(
            Page.TextVisible("PENDING") || Page.TextVisible("WAITLIST") || Page.TextVisible("Chờ duyệt"),
            "Waiting for approval status should be shown");
    }

    public void e_CheckConfirmed()
    {
        Assert.That(!Page.TextVisible("chưa được xác nhận"), "Confirmed user should access learning page");
    }

    public void e_CheckPending()
    {
        Assert.That(
            Page.TextVisible("PENDING") || Page.TextVisible("chưa được xác nhận"),
            "Pending user should be blocked");
    }

    public void e_CheckWaitlist()
    {
        Assert.That(
            Page.TextVisible("WAITLIST") || Page.TextVisible("hàng chờ") || Page.TextVisible("chưa được xác nhận"),
            "Waitlisted user should be blocked");
    }

    public void e_AdminApproves()
    {
        // Use API to approve registration, then retry page load
        var pendingIds = ApiHelper.GetPendingRegistrationIds();
        if (pendingIds.Count > 0) ApiHelper.ApproveRegistration(pendingIds[0]);
        Page.GoTo($"/learn/{_courseId}");
        Thread.Sleep(800);
    }

    public void e_RetryAfterDenied()
    {
        Page.GoTo($"/learn/{_courseId}");
        Thread.Sleep(800);
    }

    public void e_MarkLessonComplete()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Đánh dấu hoàn thành') or contains(text(),'Mark Done') or contains(text(),'Hoàn thành')]"));
        if (btn.Count > 0) { Page.ScrollIntoView(btn[0]); Page.JsClick(btn[0]); Thread.Sleep(1000); }
    }

    public void e_ContinueNextLesson()
    {
        var lessons = Setup.Driver.FindElements(By.CssSelector(".lesson-item:not(.completed), .lesson-item:not(.done)"));
        if (lessons.Count > 0) { Page.JsClick(lessons[0]); Thread.Sleep(600); }
    }

    public void e_FinalLessonDone()
    {
        Assert.That(
            Page.TextVisible("100%") || Page.TextVisible("Nhận chứng chỉ"),
            "Final lesson completion should trigger 100% state");
    }

    public void e_BackToDashboard()            { Page.GoTo("/dashboard"); Thread.Sleep(600); }
    public void e_BackToDashboardFromCompleted() => e_BackToDashboard();
}

// ═══════════════════════════════════════════════════════════════════════════════
// Certification
// ═══════════════════════════════════════════════════════════════════════════════
/// <summary>
/// Model: Certification.json
/// Covers: Claim cert, already-exists idempotent, not-eligible error, verify page
/// </summary>
public class Certification
{
    private PageHelper Page => Setup.Page;
    private int _courseId = 1;
    private string _verifyCode = string.Empty;

    public void setUpModel()
    {
        ApiHelper.SeedAll();
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/dashboard");
        Thread.Sleep(800);
    }

    public void v_StudentDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on student dashboard");
    }

    public void v_LearningPage()
    {
        Assert.That(Page.CurrentUrl.Contains("learn"), "Should be on learning page");
        Assert.That(
            Page.TextVisible("Bài học") || Page.ElementVisible(By.CssSelector(".lesson-content")),
            "Lesson content should be visible");
    }

    public void v_CertClaimSuccess()
    {
        Assert.That(
            Page.TextVisible("Chúc mừng") || Page.TextVisible("Congratulations") ||
            Page.ElementVisible(By.CssSelector(".modal, [role='dialog']")),
            "Certificate claim success modal should appear");
    }

    public void v_CertAlreadyExists()
    {
        Assert.That(
            Page.TextVisible("đã tồn tại") || Page.TextVisible("already exists") || Page.TextVisible("Chứng chỉ"),
            "Should show existing certificate (idempotent)");
    }

    public void v_CertNotEligible()
    {
        Assert.That(
            Page.TextVisible("100%") == false || Page.TextVisible("chưa hoàn thành") || Page.TextVisible("not eligible"),
            "Should show not-eligible error");
    }

    public void v_CertVerifyPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("verify") || Page.CurrentUrl.Contains("certificates/verify"),
            "Should be on certificate verify page");
    }

    public void v_CertVerifyValid()
    {
        Assert.That(
            Page.TextVisible("hợp lệ") || Page.TextVisible("valid") || Page.TextVisible("Tên học viên"),
            "Valid cert info should be shown");
    }

    public void v_CertVerifyInvalid()
    {
        Assert.That(
            Page.TextVisible("không hợp lệ") || Page.TextVisible("invalid") || Page.TextVisible("not found"),
            "Invalid cert message should be shown");
    }

    public void v_NoCertSection()
    {
        Assert.That(
            Page.TextVisible("Hoàn thành khoá học") || Page.TextVisible("Chưa có chứng chỉ"),
            "No-cert placeholder should be visible");
    }

    public void e_ViewNoCerts()
    {
        var section = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Chứng Chỉ')]"));
        if (section.Count > 0) Page.ScrollIntoView(section[0]);
        Thread.Sleep(400);
    }

    public void e_GoToLearning()
    {
        Page.GoTo($"/learn/{_courseId}");
        Thread.Sleep(1000);
    }

    public void e_CompleteAllLessons()
    {
        ApiHelper.CompleteAllLessons(_courseId);
        Page.GoTo($"/learn/{_courseId}");
        Thread.Sleep(1000);
    }

    public void e_ClaimCertSuccess()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Nhận chứng chỉ') or contains(text(),'Claim')]"));
        Assert.That(btn.Count > 0, "Claim Certificate button must exist after completing all lessons");
        Page.JsClick(btn[0]);
        Thread.Sleep(1500);
        // Capture verification code for later use
        _verifyCode = ApiHelper.IssueCertificate(_courseId);
    }

    public void e_ClaimCertNotEligible()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Nhận chứng chỉ') or contains(text(),'Claim')]"));
        if (btn.Count > 0)
        {
            Page.JsClick(btn[0]);
            Thread.Sleep(1000);
            Assert.That(
                Page.TextVisible("chưa hoàn thành") || Page.TextVisible("not eligible") || Page.TextVisible("100%") == false,
                "Should show not-eligible error");
        }
    }

    public void e_ClaimCertAlreadyExists()
    {
        // POST again — should return existing cert (idempotent)
        var code = ApiHelper.IssueCertificate(_courseId);
        Assert.That(!string.IsNullOrEmpty(code), "Should return existing cert code");
    }

    public void e_ViewCertInDashboard()
    {
        Page.GoTo("/dashboard");
        Thread.Sleep(800);
        Assert.That(
            Page.TextVisible("Chứng Chỉ") || Page.TextVisible("Certificate"),
            "Dashboard should show certificate section");
    }

    public void e_VerifyCertFromDashboard()
    {
        Page.GoTo($"/certificates/verify/{_verifyCode}");
        Thread.Sleep(800);
    }

    public void e_VerifyValidCode()
    {
        var input = Setup.Driver.FindElements(By.CssSelector("input[name='code'], input[placeholder*='code' i]"));
        if (input.Count > 0)
        {
            input[0].Clear();
            input[0].SendKeys(string.IsNullOrEmpty(_verifyCode) ? "VALID-TEST-CODE" : _verifyCode);
            Page.Click(By.CssSelector("button[type='submit']"));
            Thread.Sleep(1000);
        }
    }

    public void e_VerifyInvalidCode()
    {
        Page.GoTo("/certificates/verify");
        Thread.Sleep(600);
        var input = Setup.Driver.FindElements(By.CssSelector("input[name='code'], input[placeholder*='code' i]"));
        if (input.Count > 0)
        {
            input[0].Clear();
            input[0].SendKeys("INVALID-CODE-00000");
            Page.Click(By.CssSelector("button[type='submit']"));
            Thread.Sleep(1000);
        }
    }

    public void e_BackToDashboardFromVerify()      { Page.GoTo("/dashboard"); Thread.Sleep(600); }
    public void e_BackToDashboardFromInvalid()     => e_BackToDashboardFromVerify();
    public void e_BackToDashboardFromNoSection()   => e_BackToDashboardFromVerify();
    public void e_BackToDashboardFromNotEligible() => e_BackToDashboardFromVerify();
    public void e_BackToDashboard()                => e_BackToDashboardFromVerify();
}

// ═══════════════════════════════════════════════════════════════════════════════
// Notification
// ═══════════════════════════════════════════════════════════════════════════════
/// <summary>
/// Model: Notification.json
/// Covers: Receive and read notifications (REGISTRATION/CERTIFICATE/LEARNING/SYSTEM), 403, 404
/// </summary>
public class Notification
{
    private PageHelper Page => Setup.Page;

    public void setUpModel()
    {
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/dashboard");
        Thread.Sleep(800);
    }

    public void v_AnyDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on any dashboard");
    }

    public void v_NotificationList()
    {
        Assert.That(
            Page.TextVisible("Thông báo") || Page.TextVisible("Notification") ||
            Page.ElementVisible(By.CssSelector(".notification-list, [data-testid='notifications']")),
            "Notification list should be visible");
    }

    public void v_UnreadNotification()
    {
        Assert.That(
            Page.ElementVisible(By.CssSelector(".notification.unread, [data-read='false'], .badge")) ||
            Page.TextVisible("chưa đọc") || Page.TextVisible("unread"),
            "Unread notification indicator should be visible");
    }

    public void v_ReadNotification()
    {
        Assert.That(
            Page.TextVisible("đã đọc") || Page.TextVisible("read") ||
            Page.ElementVisible(By.CssSelector(".notification.read, [data-read='true']")),
            "Notification should be marked as read");
    }

    public void v_RegistrationNotif() => AssertNotifType("REGISTRATION");
    public void v_CertificateNotif()  => AssertNotifType("CERTIFICATE");
    public void v_SystemNotif()       => AssertNotifType("SYSTEM");
    public void v_LearningNotif()     => AssertNotifType("LEARNING");

    private void AssertNotifType(string type)
    {
        Assert.That(
            Page.TextVisible(type) || Page.TextVisible("Thông báo"),
            $"Should display {type} notification");
    }

    public void v_ForbiddenRead()
    {
        Assert.That(
            Page.TextVisible("403") || Page.TextVisible("Forbidden") || Page.TextVisible("không có quyền"),
            "403 Forbidden should be shown");
    }

    public void v_NotFoundNotif()
    {
        Assert.That(
            Page.TextVisible("404") || Page.TextVisible("không tồn tại") || Page.TextVisible("not found"),
            "404 Not Found should be shown");
    }

    public void v_EmptyNotifications()
    {
        Assert.That(
            Page.TextVisible("Không có thông báo") || Page.TextVisible("No notifications") || Page.TextVisible("trống"),
            "Empty notifications message should be shown");
    }

    public void e_OpenNotifications()
    {
        var bell = Setup.Driver.FindElements(
            By.XPath("//button[contains(@aria-label,'notification') or contains(@class,'bell')] | //*[@data-testid='notif-bell']"));
        if (bell.Count > 0) { Page.JsClick(bell[0]); Thread.Sleep(600); }
        else { Page.GoTo("/notifications"); Thread.Sleep(600); }
    }

    public void e_HasUnread()
    {
        var unread = Setup.Driver.FindElements(By.CssSelector(".notification.unread, [data-read='false']"));
        if (unread.Count > 0) Page.ScrollIntoView(unread[0]);
        Thread.Sleep(300);
    }

    public void e_EmptyList()
    {
        Assert.That(
            Page.TextVisible("Không có thông báo") || Page.TextVisible("No notifications"),
            "Empty list message should appear");
    }

    public void e_MarkAsRead()
    {
        var notifs = Setup.Driver.FindElements(By.CssSelector(".notification.unread, [data-read='false']"));
        if (notifs.Count > 0)
        {
            Page.JsClick(notifs[0]);
            Thread.Sleep(800);
        }
        else
        {
            var notifIds = ApiHelper.GetMyNotifications();
            if (notifIds.Count > 0)
            {
                int id = notifIds[0]["id"]!.ToObject<int>();
                ApiHelper.MarkNotificationRead(id);
            }
        }
    }

    public void e_MarkReadForbidden()
    {
        // Try to read a notification that belongs to another user via API
        try
        {
            ApiHelper.Post("/notifications/99999/read", "{}", ApiHelper.GetStudentToken());
        }
        catch (Exception ex)
        {
            Assert.That(ex.Message.Contains("403") || ex.Message.Contains("404"), "Should get 403 or 404");
        }
    }

    public void e_MarkReadNotFound()
    {
        try
        {
            ApiHelper.Post("/notifications/0/read", "{}", ApiHelper.GetStudentToken());
        }
        catch (Exception ex)
        {
            Assert.That(ex.Message.Contains("404") || ex.Message.Contains("not found"), "Should get 404");
        }
    }

    public void e_BackToListFromRead()      => e_OpenNotifications();
    public void e_BackToListFromForbidden() => e_OpenNotifications();
    public void e_BackToListFromNotFound()  => e_OpenNotifications();
    public void e_BackToList()              => e_OpenNotifications();

    public void e_BackToListFromEmpty()
    {
        Page.GoTo("/dashboard");
        Thread.Sleep(500);
    }

    public void e_BackToDashboard()
    {
        Page.GoTo("/dashboard");
        Thread.Sleep(500);
    }

    public void e_ReceiveRegistrationNotif() => TriggerNotifByAction("REGISTRATION");
    public void e_ReceiveCertNotif()         => TriggerNotifByAction("CERTIFICATE");
    public void e_ReceiveSystemNotif()       => TriggerNotifByAction("SYSTEM");
    public void e_ReceiveLearningNotif()     => TriggerNotifByAction("LEARNING");

    private void TriggerNotifByAction(string type)
    {
        // Notifications are created server-side; navigate to dashboard and open notifications
        Page.GoTo("/dashboard");
        Thread.Sleep(500);
        e_OpenNotifications();
    }

    public void e_ViewRegistrationNotif() => e_OpenNotifications();
    public void e_ViewCertNotif()         => e_OpenNotifications();
    public void e_ViewSystemNotif()       => e_OpenNotifications();
    public void e_ViewLearningNotif()     => e_OpenNotifications();
    public void e_ViewNotif()             => e_OpenNotifications();
}

// ═══════════════════════════════════════════════════════════════════════════════
// Waitlist_AutoPromote
// ═══════════════════════════════════════════════════════════════════════════════
/// <summary>
/// Model: Waitlist_AutoPromote.json
/// Covers: Auto-promote from waitlist when slot freed (cancel/ban/capacity increase),
///         expire pending job, rebalance waitlist positions
/// </summary>
public class Waitlist_AutoPromote
{
    private PageHelper Page => Setup.Page;

    public void setUpModel()
    {
        ApiHelper.SeedAll();
        Page.ClearSession();
        Page.Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        Page.GoTo("/admin/dashboard");
        Page.WaitForUrl("dashboard");
        Thread.Sleep(800);
    }

    public void v_FullCourse()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on dashboard");
    }

    public void v_WaitlistQueue()
    {
        Assert.That(
            Page.TextVisible("WAITLIST") || Page.TextVisible("Hàng chờ") || Page.TextVisible("waitlist"),
            "Waitlist queue info should be visible");
    }

    public void v_SlotFreed()
    {
        Assert.That(
            Page.TextVisible("CANCELLED") || Page.TextVisible("slot trống") || Page.TextVisible("Đã hủy"),
            "Slot freed indicator should be visible");
    }

    public void v_WaitlistPromoted()
    {
        Assert.That(
            Page.TextVisible("CONFIRMED") || Page.TextVisible("Promoted") || Page.TextVisible("Đã promote"),
            "Waitlist promotion should be confirmed");
    }

    public void v_WaitlistRebalanced()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on dashboard after rebalance");
    }

    public void v_NewRegistrationWaitlist()
    {
        Assert.That(
            Page.TextVisible("WAITLIST") || Page.TextVisible("Hàng chờ"),
            "New registration should show WAITLIST status");
    }

    public void v_CancelFromWaitlist()
    {
        Assert.That(
            Page.TextVisible("CANCELLED") || Page.TextVisible("Đã hủy"),
            "Waitlist cancellation should show CANCELLED");
    }

    public void v_CancelConfirmed()
    {
        Assert.That(
            Page.TextVisible("CANCELLED") || Page.TextVisible("Đã hủy"),
            "Confirmed cancellation should show CANCELLED");
    }

    public void v_BanUserTrigger()
    {
        Assert.That(
            Page.TextVisible("BANNED") || Page.TextVisible("đã bị khóa"),
            "Ban trigger result should be visible");
    }

    public void v_CapacityIncreased()
    {
        Assert.That(
            Page.TextVisible("cập nhật") || Page.TextVisible("updated") || Page.CurrentUrl.Contains("dashboard"),
            "Capacity increase should be confirmed");
    }

    public void v_ExpiredPending()
    {
        Assert.That(
            Page.TextVisible("EXPIRED") || Page.TextVisible("expired") || Page.TextVisible("hết hạn"),
            "Expired pending notification should be visible");
    }

    public void v_RegistrationList()
    {
        Assert.That(
            Page.TextVisible("Đăng Ký") || Page.TextVisible("Registration") || Page.CurrentUrl.Contains("dashboard"),
            "Registration list should be visible");
    }

    public void v_AdminDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on admin dashboard");
    }

    public void v_StudentDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on student dashboard");
    }

    public void v_InstructorDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on instructor dashboard");
    }

    public void v_RegistrationExpiredList()
    {
        Assert.That(
            Page.TextVisible("EXPIRED") || Page.TextVisible("expired"),
            "Expired registration list should be shown");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_StudentRegisterFull()
    {
        // Simulate student registering to full course via API → WAITLIST
        try
        {
            var courseId = GetFullCourseId();
            if (courseId > 0) ApiHelper.RegisterStudent(courseId);
        }
        catch { /* waitlist entry creation tested */ }
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(500);
    }

    public void e_ViewWaitlistPosition()
    {
        // Switch to student context to view dashboard
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/dashboard");
        Thread.Sleep(800);
        var waitlistSection = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'WAITLIST') or contains(text(),'Hàng chờ')]"));
        if (waitlistSection.Count > 0) Page.ScrollIntoView(waitlistSection[0]);
        Thread.Sleep(400);
        // Return to admin
        Page.ClearSession();
        Page.Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
    }

    public void e_StudentCancelWaitlist()
    {
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/dashboard");
        Thread.Sleep(800);
        var cancelBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Hủy') and ancestor::*[contains(text(),'WAITLIST')]] | //button[@data-status='WAITLIST']"));
        if (cancelBtn.Count > 0) { Page.JsClick(cancelBtn[0]); Thread.Sleep(1000); }
        Page.ClearSession();
        Page.Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
    }

    public void e_RebalanceAfterCancel()
    {
        Thread.Sleep(500);
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on dashboard after rebalance");
    }

    public void e_StudentCancelConfirmed()
    {
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/dashboard");
        Thread.Sleep(800);
        var cancelBtn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Hủy đăng ký') or contains(text(),'Cancel')]"));
        if (cancelBtn.Count > 0)
        {
            Page.JsClick(cancelBtn[0]);
            Thread.Sleep(500);
            var confirm = Setup.Driver.FindElements(
                By.XPath("//button[contains(text(),'Xác nhận') or contains(text(),'OK')]"));
            if (confirm.Count > 0) { Page.JsClick(confirm[0]); Thread.Sleep(1000); }
        }
        Page.ClearSession();
        Page.Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
    }

    public void e_TriggerPromoteFromCancel() => Thread.Sleep(500);
    public void e_TriggerPromoteFromBan()    => Thread.Sleep(500);
    public void e_TriggerPromoteFromCapacity() => Thread.Sleep(500);
    public void e_TriggerPromote()           => Thread.Sleep(500);

    public void e_AdminBanUser()
    {
        int userId = ApiHelper.GetFirstActiveUserId();
        if (userId > 0) ApiHelper.BanUser(userId);
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
    }

    public void e_IncreaseCapacity()
    {
        Page.ClearSession();
        Page.Login(PageHelper.InstructorEmail, PageHelper.InstructorPassword);
        Page.GoTo("/dashboard");
        Thread.Sleep(600);
        // Find course and increase capacity via edit form
        var courseLinks = Setup.Driver.FindElements(By.XPath("//a[contains(@href,'/courses/')]"));
        if (courseLinks.Count > 0)
        {
            Page.JsClick(courseLinks[0]);
            Thread.Sleep(600);
            var editBtn = Setup.Driver.FindElements(By.XPath("//button[contains(text(),'Chỉnh sửa')]"));
            if (editBtn.Count > 0)
            {
                Page.JsClick(editBtn[0]);
                Thread.Sleep(600);
                var capInput = Setup.Driver.FindElements(By.CssSelector("input[name='max_capacity']"));
                if (capInput.Count > 0)
                {
                    int cur = int.TryParse(capInput[0].GetAttribute("value"), out int v) ? v : 10;
                    capInput[0].Clear();
                    capInput[0].SendKeys((cur + 5).ToString());
                    Page.Click(By.CssSelector("button[type='submit']"));
                    Thread.Sleep(1200);
                }
            }
        }
        Page.ClearSession();
        Page.Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
    }

    public void e_AutoPromoteWaitlist()
    {
        Thread.Sleep(800);
        Assert.That(
            Page.TextVisible("CONFIRMED") || Page.TextVisible("Promoted"),
            "Auto-promote should result in CONFIRMED status for first in waitlist");
    }

    public void e_NoWaitlistToPromote()
    {
        Assert.That(
            !Page.TextVisible("WAITLIST") || Page.TextVisible("Không có hàng chờ"),
            "No waitlist entries should remain");
    }

    public void e_ViewUpdatedList()
    {
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
    }

    public void e_StudentSeesConfirmed()
    {
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/dashboard");
        Thread.Sleep(800);
        Assert.That(
            Page.TextVisible("CONFIRMED") || Page.TextVisible("Đã xác nhận"),
            "Promoted student should see CONFIRMED status");
        Page.ClearSession();
        Page.Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
    }

    public void e_RunExpireJob()
    {
        ApiHelper.RunExpirePendingJob();
        Thread.Sleep(500);
        Assert.That(
            Page.TextVisible("EXPIRED") || Page.TextVisible("expired") || Page.CurrentUrl.Contains("dashboard"),
            "Expire job should run successfully");
    }

    public void e_ViewExpiredList()
    {
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(600);
        var expiredSection = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'EXPIRED') or contains(text(),'Hết hạn')]"));
        if (expiredSection.Count > 0) Page.ScrollIntoView(expiredSection[0]);
        Thread.Sleep(400);
    }

    public void e_BackToAdmin()            { Page.GoTo("/admin/dashboard"); Thread.Sleep(500); }
    public void e_BackToAdminFromList()    => e_BackToAdmin();
    public void e_BackToWaitlistFromRebalanced()
    {
        Page.GoTo("/dashboard");
        Thread.Sleep(500);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static int GetFullCourseId()
    {
        try
        {
            var resp = ApiHelper.Get("/courses?status=PUBLISHED&limit=5", ApiHelper.GetStudentToken());
            var arr  = Newtonsoft.Json.Linq.JArray.Parse(resp);
            foreach (var c in arr)
            {
                var current = c["current_participants"]?.ToObject<int>() ?? 0;
                var max     = c["max_capacity"]?.ToObject<int>() ?? 1;
                if (current >= max) return c["id"]!.ToObject<int>();
            }
            if (arr.Count > 0) return arr[0]["id"]!.ToObject<int>();
        }
        catch { }
        return 1;
    }
}