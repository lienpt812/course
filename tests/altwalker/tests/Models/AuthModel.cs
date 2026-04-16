using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Test_Auth.json
/// Covers: Login (student/instructor/admin), register, forgot/reset password, logout
/// </summary>
public class Test_Auth
{
    private PageHelper Page => Setup.Page;

    // Unique suffix per run so register tests don't collide
    private readonly string _suffix = DateTime.Now.Ticks.ToString()[^6..];

    public void setUpModel()
    {
        Page.ClearSession();
        Page.GoTo("/courses");
        Thread.Sleep(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_GuestOnCourses()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses") || Page.CurrentUrl.Contains("login") || Page.CurrentUrl == PageHelper.BaseUrl + "/",
            "Should be on courses page or home as guest");
    }

    public void v_LoginPage()
    {
        Assert.That(Page.CurrentUrl.Contains("login"), "Should be on login page");
        Assert.That(
            Page.ElementVisible(By.CssSelector("input[type='email'], input[name='email']")),
            "Email input should be visible");
    }

    public void v_RegisterPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("register") || Page.CurrentUrl.Contains("signup"),
            "Should be on register page");
    }

    public void v_ForgotPasswordPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("forgot") || Page.CurrentUrl.Contains("reset"),
            "Should be on forgot-password page");
    }

    public void v_ResetPasswordPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("reset") || Page.CurrentUrl.Contains("new-password"),
            "Should be on reset-password page");
    }

    public void v_StudentDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard") || Page.CurrentUrl.Contains("student"),
            "Should be on student dashboard");
        Assert.That(
            Page.TextVisible("Dashboard") || Page.TextVisible("Khoá Học Của Tôi") || Page.TextVisible("Đăng Ký"),
            "Student dashboard content should be visible");
    }

    public void v_InstructorDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard") || Page.CurrentUrl.Contains("instructor"),
            "Should be on instructor dashboard");
        Assert.That(
            Page.TextVisible("Dashboard") || Page.TextVisible("Khoá Học") || Page.TextVisible("Tạo"),
            "Instructor dashboard content should be visible");
    }

    public void v_AdminDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard") || Page.CurrentUrl.Contains("admin"),
            "Should be on admin dashboard");
        Assert.That(
            Page.TextVisible("Dashboard") || Page.TextVisible("Đăng Ký") || Page.TextVisible("Quản lý"),
            "Admin dashboard content should be visible");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_GoToLogin()
    {
        var link = Setup.Driver.FindElements(
            By.XPath("//a[contains(@href,'login')] | //button[contains(text(),'Đăng nhập') or contains(text(),'Login')]"));
        if (link.Count > 0) { Page.JsClick(link[0]); Thread.Sleep(600); }
        else { Page.GoTo("/login"); Thread.Sleep(600); }
    }

    public void e_LoginAsStudent()
    {
        Page.GoTo("/login");
        Thread.Sleep(500);
        Page.TypeInto(By.CssSelector("input[type='email'], input[name='email']"), PageHelper.StudentEmail);
        Page.TypeInto(By.CssSelector("input[type='password'], input[name='password']"), PageHelper.StudentPassword);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_LoginAsInstructor()
    {
        Page.GoTo("/login");
        Thread.Sleep(500);
        Page.TypeInto(By.CssSelector("input[type='email'], input[name='email']"), PageHelper.InstructorEmail);
        Page.TypeInto(By.CssSelector("input[type='password'], input[name='password']"), PageHelper.InstructorPassword);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_LoginAsAdmin()
    {
        Page.GoTo("/login");
        Thread.Sleep(500);
        Page.TypeInto(By.CssSelector("input[type='email'], input[name='email']"), PageHelper.AdminEmail);
        Page.TypeInto(By.CssSelector("input[type='password'], input[name='password']"), PageHelper.AdminPassword);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_LoginFail()
    {
        Page.GoTo("/login");
        Thread.Sleep(400);
        Page.TypeInto(By.CssSelector("input[type='email'], input[name='email']"), "wrong@example.com");
        Page.TypeInto(By.CssSelector("input[type='password'], input[name='password']"), "WrongPass999!");
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1000);
        Assert.That(
            Page.TextVisible("Invalid") || Page.TextVisible("incorrect") || Page.TextVisible("Sai") || Page.TextVisible("không đúng"),
            "Should show login error message");
    }

    public void e_GoToRegister()
    {
        var link = Setup.Driver.FindElements(
            By.XPath("//a[contains(@href,'register') or contains(@href,'signup')] | //button[contains(text(),'Register') or contains(text(),'Đăng ký')]"));
        if (link.Count > 0) { Page.JsClick(link[0]); Thread.Sleep(600); }
        else { Page.GoTo("/register"); Thread.Sleep(600); }
    }

    public void e_RegisterAsStudent()
    {
        Page.GoTo("/register");
        Thread.Sleep(500);
        Page.FillField("email", $"student_{_suffix}@test.com");
        Page.FillField("password", "Test@12345");
        // role selector if present
        var roleSelect = Setup.Driver.FindElements(By.CssSelector("select[name='role']"));
        if (roleSelect.Count > 0) Page.SelectDropdown(By.CssSelector("select[name='role']"), "STUDENT");
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_RegisterAsInstructor()
    {
        Page.GoTo("/register");
        Thread.Sleep(500);
        Page.FillField("email", $"instructor_{_suffix}@test.com");
        Page.FillField("password", "Test@12345");
        var roleSelect = Setup.Driver.FindElements(By.CssSelector("select[name='role']"));
        if (roleSelect.Count > 0) Page.SelectDropdown(By.CssSelector("select[name='role']"), "INSTRUCTOR");
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_RegisterAsAdmin()
    {
        // Admin accounts typically cannot be self-registered; skip via API or assert not possible
        Page.GoTo("/register");
        Thread.Sleep(500);
        Page.FillField("email", $"admin_{_suffix}@test.com");
        Page.FillField("password", "Test@12345");
        var roleSelect = Setup.Driver.FindElements(By.CssSelector("select[name='role']"));
        if (roleSelect.Count > 0) Page.SelectDropdown(By.CssSelector("select[name='role']"), "ADMIN");
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_RegisterFailDuplicate()
    {
        Page.GoTo("/register");
        Thread.Sleep(500);
        // Use already-existing student email to force duplicate
        Page.FillField("email", PageHelper.StudentEmail);
        Page.FillField("password", "Test@12345");
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1000);
        Assert.That(
            Page.TextVisible("already") || Page.TextVisible("tồn tại") || Page.TextVisible("duplicate") || Page.TextVisible("đã được đăng ký"),
            "Should show duplicate-email error");
    }

    public void e_GoToForgotPassword()
    {
        var link = Setup.Driver.FindElements(
            By.XPath("//a[contains(@href,'forgot') or contains(text(),'Quên mật khẩu') or contains(text(),'Forgot')]"));
        if (link.Count > 0) { Page.JsClick(link[0]); Thread.Sleep(600); }
        else { Page.GoTo("/forgot-password"); Thread.Sleep(600); }
    }

    public void e_SubmitForgotPassword()
    {
        Page.FillField("email", PageHelper.StudentEmail);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1000);
        Assert.That(
            Page.TextVisible("gửi") || Page.TextVisible("sent") || Page.TextVisible("Reset token") || Page.TextVisible("Yêu cầu"),
            "Should confirm forgot-password response (message or dev token)");
    }

    public void e_SubmitResetPassword()
    {
        // In tests, use a seeded reset token or simulate via API
        var token = "test-reset-token-" + _suffix;
        Page.GoTo($"/reset-password?token={token}");
        Thread.Sleep(500);
        var passInputs = Setup.Driver.FindElements(By.CssSelector("input[type='password']"));
        if (passInputs.Count >= 2)
        {
            passInputs[0].Clear(); passInputs[0].SendKeys("NewPass@123");
            passInputs[1].Clear(); passInputs[1].SendKeys("NewPass@123");
            Page.Click(By.CssSelector("button[type='submit']"));
            Thread.Sleep(1000);
        }
    }

    public void e_ResetTokenExpired()
    {
        Page.GoTo("/reset-password?token=expired-invalid-token");
        Thread.Sleep(800);
        Assert.That(
            Page.TextVisible("expired") || Page.TextVisible("hết hạn") || Page.TextVisible("invalid") || Page.TextVisible("không hợp lệ"),
            "Should show token expired error");
    }

    public void e_Logout()
    {
        Page.Logout();
        Thread.Sleep(800);
    }

    // These edges exist in the model for graph coverage; they navigate back without logout
    public void e_BackToLoginFromStudent()
    {
        Page.GoTo("/login");
        Thread.Sleep(500);
    }

    public void e_BackToRegisterFromStudent()
    {
        Page.GoTo("/register");
        Thread.Sleep(500);
    }

    // Named variants used in JSON (same source vertex → same action)
    public void e_LogoutStudent()     => e_Logout();
    public void e_LogoutInstructor()  => e_Logout();
    public void e_LogoutAdmin()       => e_Logout();
    public void e_BackToLoginFromReset()    { Page.GoTo("/login"); Thread.Sleep(500); }
    public void e_BackToLoginFromRegister() { Page.GoTo("/login"); Thread.Sleep(500); }

    /// <summary>GraphWalker edge name <c>e_BackToLogin</c> (from Register → Login).</summary>
    public void e_BackToLogin() => e_BackToLoginFromRegister();
}