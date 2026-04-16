using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;
using NUnit.Framework;

namespace CourseRegistration.Tests.Models;

/// <summary>
/// Model: Test_Explorecourse.json
/// Covers: Guest/Student/Instructor/Admin browsing course list, filter, search, course detail
/// </summary>
public class Test_Explorecourse
{
    private PageHelper Page => Setup.Page;

    public void setUpModel()
    {
        Page.ClearSession();
        Page.GoTo("/courses");
        Thread.Sleep(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────
    public void v_CourseListPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses") || Page.CurrentUrl == PageHelper.BaseUrl + "/",
            "Should be on course list page");
        Assert.That(
            Page.ElementVisible(By.CssSelector(".course-card, [data-testid='course-card'], .card")) ||
            Page.TextVisible("Khoá Học") || Page.TextVisible("Course"),
            "Course cards or heading should be visible");
    }

    public void v_CourseDetailPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("courses/") || Page.CurrentUrl.Contains("course/"),
            "Should be on course detail page");
        Assert.That(
            Page.TextVisible("Đăng ký") || Page.TextVisible("Register") || Page.TextVisible("Xem chi tiết") || Page.TextVisible("Giới thiệu"),
            "Course detail content should be visible");
    }

    public void v_LoginPage()
    {
        Assert.That(Page.CurrentUrl.Contains("login"), "Should be redirected to login page");
    }

    public void v_StudentDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard") || Page.CurrentUrl.Contains("student"),
            "Should be on student dashboard");
    }

    public void v_InstructorDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard") || Page.CurrentUrl.Contains("instructor"),
            "Should be on instructor dashboard");
    }

    public void v_AdminDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard") || Page.CurrentUrl.Contains("admin"),
            "Should be on admin dashboard");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────
    public void e_FilterByCategory()
    {
        var filterEl = Setup.Driver.FindElements(
            By.XPath("//select[contains(@name,'category')] | //button[contains(text(),'Backend')] | //*[@data-filter='category']"));
        if (filterEl.Count > 0)
        {
            Page.ScrollIntoView(filterEl[0]);
            Page.JsClick(filterEl[0]);
            Thread.Sleep(600);
        }
        else
        {
            // Try URL-based filter
            Page.GoTo("/courses?category=Backend");
            Thread.Sleep(600);
        }
    }

    public void e_FilterByLevel()
    {
        var filterEl = Setup.Driver.FindElements(
            By.XPath("//select[contains(@name,'level')] | //button[contains(text(),'Beginner')] | //*[@data-filter='level']"));
        if (filterEl.Count > 0)
        {
            Page.ScrollIntoView(filterEl[0]);
            Page.JsClick(filterEl[0]);
            Thread.Sleep(600);
        }
        else
        {
            Page.GoTo("/courses?level=Beginner");
            Thread.Sleep(600);
        }
    }

    public void e_SearchCourse()
    {
        var searchInput = Setup.Driver.FindElements(
            By.CssSelector("input[type='search'], input[placeholder*='search' i], input[placeholder*='tìm' i], input[name='q']"));
        if (searchInput.Count > 0)
        {
            searchInput[0].Clear();
            searchInput[0].SendKeys("Test");
            searchInput[0].SendKeys(OpenQA.Selenium.Keys.Return);
            Thread.Sleep(800);
        }
        else
        {
            Page.GoTo("/courses?q=Test");
            Thread.Sleep(600);
        }
    }

    public void e_ClickCourseCard()
    {
        var cards = Setup.Driver.FindElements(
            By.XPath("//a[contains(@href,'/courses/')] | //*[@data-testid='course-card']//a"));
        Assert.That(cards.Count > 0, "At least one course card must be visible to click");
        Page.ScrollIntoView(cards[0]);
        Thread.Sleep(200);
        Page.JsClick(cards[0]);
        Thread.Sleep(800);
    }

    public void e_BackToCourseList()
    {
        Page.GoTo("/courses");
        Thread.Sleep(600);
    }

    public void e_GuestClickRegister()
    {
        var btn = Setup.Driver.FindElements(
            By.XPath("//button[contains(text(),'Đăng ký') or contains(text(),'Register')] | //a[contains(text(),'Đăng ký')]"));
        if (btn.Count > 0) { Page.JsClick(btn[0]); Thread.Sleep(800); }
        else { Page.GoTo("/login"); Thread.Sleep(600); }
    }

    public void e_LoginAsStudent()
    {
        Page.GoTo("/login");
        Thread.Sleep(400);
        Page.TypeInto(By.CssSelector("input[type='email'], input[name='email']"), PageHelper.StudentEmail);
        Page.TypeInto(By.CssSelector("input[type='password'], input[name='password']"), PageHelper.StudentPassword);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_LoginAsInstructor()
    {
        Page.GoTo("/login");
        Thread.Sleep(400);
        Page.TypeInto(By.CssSelector("input[type='email'], input[name='email']"), PageHelper.InstructorEmail);
        Page.TypeInto(By.CssSelector("input[type='password'], input[name='password']"), PageHelper.InstructorPassword);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_LoginAsAdmin()
    {
        Page.GoTo("/login");
        Thread.Sleep(400);
        Page.TypeInto(By.CssSelector("input[type='email'], input[name='email']"), PageHelper.AdminEmail);
        Page.TypeInto(By.CssSelector("input[type='password'], input[name='password']"), PageHelper.AdminPassword);
        Page.Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void e_ViewCourses()
    {
        Page.GoTo("/courses");
        Thread.Sleep(600);
    }

    public void e_StudentViewCourses()  => e_ViewCourses();
    public void e_InstructorViewCourses() => e_ViewCourses();
    public void e_AdminViewCourses()    => e_ViewCourses();

    public void e_Logout()
    {
        Page.Logout();
        Thread.Sleep(600);
    }

    public void e_LogoutStudent()     => e_Logout();
    public void e_LogoutInstructor()  => e_Logout();
    public void e_LogoutAdmin()       => e_Logout();

    public void e_GoToLogin()
    {
        Page.GoTo("/login");
        Thread.Sleep(500);
    }
}