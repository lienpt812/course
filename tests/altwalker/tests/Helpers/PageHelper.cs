using System.Text.RegularExpressions;
using OpenQA.Selenium;
using OpenQA.Selenium.Chrome;
using OpenQA.Selenium.Support.UI;

namespace CourseRegistration.Tests.Helpers;

/// <summary>
/// Wraps all Selenium interactions. Single instance shared via Setup.Page.
/// </summary>
public class PageHelper
{
    // ── Credentials (override via env vars or appsettings) ───────────────────
    /// <summary>URL SPA (Vite). Mặc định <c>:3000</c> — khớp <c>fe_new/vite.config.ts</c>.</summary>
    public static string BaseUrl        => Environment.GetEnvironmentVariable("APP_BASE_URL")        ?? "http://localhost:3000";
    public static string AdminEmail     => Environment.GetEnvironmentVariable("ADMIN_EMAIL")         ?? "admin@test.com";
    public static string AdminPassword  => Environment.GetEnvironmentVariable("ADMIN_PASSWORD")      ?? "Password123!";
    public static string InstructorEmail    => Environment.GetEnvironmentVariable("INSTRUCTOR_EMAIL")    ?? "instructor@test.com";
    public static string InstructorPassword => Environment.GetEnvironmentVariable("INSTRUCTOR_PASSWORD") ?? "Password123!";
    public static string StudentEmail    => Environment.GetEnvironmentVariable("STUDENT_EMAIL")      ?? "student@test.com";
    public static string StudentPassword => Environment.GetEnvironmentVariable("STUDENT_PASSWORD")   ?? "Password123!";

    // ── Driver ───────────────────────────────────────────────────────────────
    private readonly IWebDriver _driver;
    private readonly WebDriverWait _wait;

    public PageHelper(IWebDriver driver)
    {
        _driver = driver;
        _wait   = new WebDriverWait(driver, TimeSpan.FromSeconds(15));
    }

    // ── Navigation ───────────────────────────────────────────────────────────
    public string CurrentUrl => _driver.Url;

    public void GoTo(string path)
    {
        if (path.StartsWith("http", StringComparison.OrdinalIgnoreCase))
        {
            _driver.Navigate().GoToUrl(path);
            return;
        }

        // Must insert "/" between base and relative path; "instructor/x" + base without slash => invalid URL (Chrome: invalid argument)
        var baseUrl = BaseUrl.TrimEnd('/');
        var rel = path.StartsWith('/') ? path : "/" + path;
        _driver.Navigate().GoToUrl($"{baseUrl}{rel}");
    }

    public void WaitForUrl(string urlFragment, int timeoutSec = 15)
    {
        var wait = new WebDriverWait(_driver, TimeSpan.FromSeconds(timeoutSec));
        wait.Until(d => d.Url.Contains(urlFragment));
    }

    // ── Auth ─────────────────────────────────────────────────────────────────
    public void ClearSession()
    {
        _driver.Manage().Cookies.DeleteAllCookies();
        try
        {
            ((IJavaScriptExecutor)_driver).ExecuteScript("localStorage.clear(); sessionStorage.clear();");
        }
        catch { /* ignore if page not loaded */ }
    }

    public void Login(string email, string password)
    {
        GoTo("/login");
        Thread.Sleep(800);
        TypeInto(By.CssSelector("input[type='email'], input[name='email']"), email);
        TypeInto(By.CssSelector("input[type='password'], input[name='password']"), password);
        Click(By.CssSelector("button[type='submit']"));
        Thread.Sleep(1500);
    }

    public void Logout()
    {
        try
        {
            var logoutBtn = _driver.FindElements(By.XPath(
                "//button[contains(text(),'Logout') or contains(text(),'Đăng xuất') or contains(text(),'Sign out')]"));
            if (logoutBtn.Count > 0)
            {
                JsClick(logoutBtn[0]);
                Thread.Sleep(800);
            }
            else
            {
                ClearSession();
                GoTo("/");
            }
        }
        catch { ClearSession(); GoTo("/"); }
    }

    // ── Element interactions ─────────────────────────────────────────────────
    public void Click(By locator)
    {
        var el = _wait.Until(d => d.FindElement(locator));
        el.Click();
    }

    public void JsClick(IWebElement element)
    {
        ((IJavaScriptExecutor)_driver).ExecuteScript("arguments[0].click();", element);
    }

    public void TypeInto(By locator, string text)
    {
        var el = _wait.Until(d => d.FindElement(locator));
        el.Clear();
        el.SendKeys(text);
    }

    /// <summary>Gõ text vào phần tử đã có (dùng trong flow chỉnh sửa khóa học).</summary>
    public void Type(IWebElement element, string text)
    {
        element.Clear();
        element.SendKeys(text);
    }

    /// <summary>Lấy id khóa học đầu tiên từ link trên dashboard / danh sách.</summary>
    public int GetFirstCourseId()
    {
        GoTo("/instructor/dashboard");
        Thread.Sleep(600);
        var id = TryParseCourseIdFromPage();
        if (id.HasValue) return id.Value;
        GoTo("/courses");
        Thread.Sleep(500);
        id = TryParseCourseIdFromPage();
        return id ?? 1;
    }

    private int? TryParseCourseIdFromPage()
    {
        foreach (var link in _driver.FindElements(By.CssSelector("a[href*='/courses/']")))
        {
            var href = link.GetAttribute("href") ?? "";
            var m = Regex.Match(href, @"/courses/(\d+)");
            if (m.Success && int.TryParse(m.Groups[1].Value, out var id)) return id;
        }
        return null;
    }

    public void ScrollIntoView(IWebElement element)
    {
        ((IJavaScriptExecutor)_driver).ExecuteScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    public void SelectDropdown(By locator, string visibleText)
    {
        var el   = _wait.Until(d => d.FindElement(locator));
        var sel  = new SelectElement(el);
        sel.SelectByText(visibleText);
    }

    // ── Visibility helpers ───────────────────────────────────────────────────
    public bool TextVisible(string text)
    {
        try
        {
            return _driver.FindElements(By.XPath($"//*[contains(text(),'{text}')]")).Count > 0;
        }
        catch { return false; }
    }

    public bool ElementVisible(By locator)
    {
        try { return _driver.FindElement(locator).Displayed; }
        catch { return false; }
    }

    public IWebElement? FindOrNull(By locator)
    {
        try { return _driver.FindElement(locator); }
        catch { return null; }
    }

    // ── Wait helpers ─────────────────────────────────────────────────────────
    public void WaitForText(string text, int timeoutSec = 10)
    {
        var wait = new WebDriverWait(_driver, TimeSpan.FromSeconds(timeoutSec));
        wait.Until(d => d.FindElements(By.XPath($"//*[contains(text(),'{text}')]")).Count > 0);
    }

    public void WaitForElement(By locator, int timeoutSec = 10)
    {
        var wait = new WebDriverWait(_driver, TimeSpan.FromSeconds(timeoutSec));
        wait.Until(d => d.FindElement(locator).Displayed);
    }

    // ── Form helpers ─────────────────────────────────────────────────────────
    /// <summary>
    /// Điền ô theo placeholder/label. Với <c>email</c>/<c>password</c> dùng selector cố định
    /// (khớp ForgotPasswordPage: label "Email", placeholder you@example.com — XPath cũ không tìm thấy).
    /// </summary>
    public void FillField(string labelOrPlaceholder, string value)
    {
        if (string.Equals(labelOrPlaceholder, "email", StringComparison.OrdinalIgnoreCase))
        {
            TypeInto(By.CssSelector("input[type='email']"), value);
            return;
        }

        if (string.Equals(labelOrPlaceholder, "password", StringComparison.OrdinalIgnoreCase))
        {
            TypeInto(By.CssSelector("input[type='password']"), value);
            return;
        }

        var safe = labelOrPlaceholder.Replace("'", "\\'");
        var locator = By.XPath(
            $"//input[@placeholder='{safe}'] | " +
            $"//textarea[@placeholder='{safe}'] | " +
            $"//label[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'),'{safe.ToLowerInvariant()}')]/following::input[1] | " +
            $"//label[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'),'{safe.ToLowerInvariant()}')]/following::textarea[1]");
        TypeInto(locator, value);
    }
}