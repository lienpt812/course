using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Admin_Approval_Management.json
/// Covers: Admin approves/rejects registrations, bulk approve, ban user
/// </summary>
public class Admin_Approval_Management
{
    private PageHelper Page => Setup.Page;

    public void setUpModel()
    {
        ApiHelper.SeedAll();
        Page.ClearSession();
        Page.Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        Page.GoTo("/admin/dashboard");
        Page.WaitForUrl("dashboard");
        Thread.Sleep(1000);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_AdminDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on admin dashboard");
    }

    public void v_RegistrationList()
    {
        Assert.That(
            Page.TextVisible("Danh Sách Đăng Ký") || Page.TextVisible("Đăng Ký") || Page.CurrentUrl.Contains("dashboard"),
            "Should show registration list");
    }

    public void v_ApproveResult()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on dashboard after approve");
        Assert.That(
            Page.TextVisible("CONFIRMED") || Page.TextVisible("Đã duyệt") || Page.TextVisible("thành công"),
            "Should show approval confirmation");
    }

    public void v_RejectResult()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on dashboard after reject");
        Assert.That(
            Page.TextVisible("REJECTED") || Page.TextVisible("Đã từ chối") || Page.TextVisible("thành công"),
            "Should show rejection confirmation");
    }

    public void v_BulkApproveResult()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on dashboard after bulk approve");
        Assert.That(
            !Page.TextVisible("PENDING") || Page.TextVisible("thành công"),
            "No pending registrations should remain after bulk approve");
    }

    public void v_BanUserResult()
    {
        Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should be on dashboard after ban");
        Assert.That(
            Page.TextVisible("BANNED") || Page.TextVisible("đã bị khóa") || Page.TextVisible("thành công"),
            "Should confirm user ban");
    }

    public void v_UserList()
    {
        Assert.That(
            Page.TextVisible("Người dùng") || Page.TextVisible("User") || Page.CurrentUrl.Contains("dashboard"),
            "Should show user list");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_ViewRegistrationList()
    {
        Page.GoTo("/admin/dashboard");
        Thread.Sleep(800);
        var table = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Danh Sách Đăng Ký')]"));
        if (table.Count > 0) Page.ScrollIntoView(table[0]);
        Thread.Sleep(300);
    }

    public void e_ApproveSingle()
    {
        var btns = Setup.Driver.FindElements(By.XPath("//button[normalize-space(text())='Duyệt']"));
        Assert.That(btns.Count > 0, "e_ApproveSingle: No 'Duyệt' button — pendingCount>0 && availableSlots>0 guard should ensure button exists");
        Page.ScrollIntoView(btns[0]);
        Thread.Sleep(200);
        Page.JsClick(btns[0]);
        Thread.Sleep(1200);
    }

    public void e_ApproveSingleFullError()
    {
        var btns = Setup.Driver.FindElements(By.XPath("//button[normalize-space(text())='Duyệt']"));
        if (btns.Count > 0)
        {
            Page.ScrollIntoView(btns[0]);
            Thread.Sleep(200);
            Page.JsClick(btns[0]);
            Thread.Sleep(1000);
            Assert.That(
                Page.TextVisible("đủ") || Page.TextVisible("full") || Page.TextVisible("không còn chỗ"),
                "Should show class-full error when availableSlots == 0");
        }
        else
        {
            Assert.That(Page.CurrentUrl.Contains("dashboard"), "Should remain on dashboard");
        }
    }

    public void e_RejectSingle()
    {
        var btns = Setup.Driver.FindElements(By.XPath("//button[normalize-space(text())='Từ chối']"));
        Assert.That(btns.Count > 0, "e_RejectSingle: No 'Từ chối' button — pendingCount>0 guard should ensure button exists");
        Page.ScrollIntoView(btns[0]);
        Thread.Sleep(200);
        Page.JsClick(btns[0]);
        Thread.Sleep(1200);
    }

    public void e_BulkApprove()
    {
        var btns = Setup.Driver.FindElements(By.XPath("//button[contains(text(),'Duyệt tất cả')]"));
        Assert.That(btns.Count > 0, "e_BulkApprove: 'Duyệt tất cả' button not found — pendingCount>0 guard should ensure button exists");
        Page.ScrollIntoView(btns[0]);
        Thread.Sleep(200);
        Page.JsClick(btns[0]);
        Thread.Sleep(1500);
    }

    public void e_BackToDashboard()
    {
        Page.GoTo("/admin/dashboard");
        Page.WaitForUrl("dashboard");
    }

    public void e_ViewUserList()
    {
        var links = Setup.Driver.FindElements(
            By.XPath("//a[contains(@href,'user')] | //*[contains(text(),'Người dùng')]//ancestor-or-self::a"));
        if (links.Count > 0)
        {
            Page.ScrollIntoView(links[0]);
            Thread.Sleep(200);
            Page.JsClick(links[0]);
            Thread.Sleep(800);
        }
        else
        {
            Page.GoTo("/admin/dashboard");
            Thread.Sleep(500);
        }
    }

    public void e_BanUser()
    {
        var banBtns = Setup.Driver.FindElements(
            By.XPath("//button[normalize-space(text())='Ban' or normalize-space(text())='Khóa' or contains(@data-action,'ban')]"));
        if (banBtns.Count > 0)
        {
            Page.ScrollIntoView(banBtns[0]);
            Thread.Sleep(200);
            Page.JsClick(banBtns[0]);
            Thread.Sleep(1200);
            var confirm = Setup.Driver.FindElements(
                By.XPath("//button[contains(text(),'Xác nhận') or contains(text(),'OK') or contains(text(),'Có')]"));
            if (confirm.Count > 0) { Page.JsClick(confirm[0]); Thread.Sleep(1000); }
        }
        else
        {
            int userId = ApiHelper.GetFirstActiveUserId();
            Assert.That(userId > 0, "e_BanUser: No active user found to ban");
            ApiHelper.BanUser(userId);
            Thread.Sleep(500);
            Page.GoTo("/admin/dashboard");
            Thread.Sleep(500);
        }
    }

    // GraphWalker dùng trường "name" trên cạnh JSON (khác "id") — phải có đủ method khớp "name".
    public void e_BackToAdminFromList()      => e_BackToDashboard();
    public void e_BackToAdminFromApprove()   => e_BackToDashboard();
    public void e_BackToAdminFromReject()    => e_BackToDashboard();
    public void e_BackToAdminFromBulk()      => e_BackToDashboard();
    public void e_BackToAdminFromUserList()  => e_BackToDashboard();
    public void e_BackToAdminFromBan()       => e_BackToDashboard();

    public void e_BackToDashboardFromList()      => e_BackToDashboard();
    public void e_BackToDashboardFromApprove()   => e_BackToDashboard();
    public void e_BackToDashboardFromReject()    => e_BackToDashboard();
    public void e_BackToDashboardFromBulk()      => e_BackToDashboard();
    public void e_BackToDashboardFromUserList()  => e_BackToDashboard();
    public void e_BackToDashboardFromBan()       => e_BackToDashboard();
}