using System.Text.RegularExpressions;
using OpenQA.Selenium;
using OpenQA.Selenium.Support.UI;

namespace Lms.MbtWeb.Helpers;

/// <summary>Facade Selenium — mọi tương tác UI đều đi qua đây.</summary>
public sealed class PageHelper
{
    private readonly IWebDriver _driver;
    private readonly WebDriverWait _wait;

    public PageHelper(IWebDriver driver)
    {
        _driver = driver;
        _wait = new WebDriverWait(driver, TimeSpan.FromSeconds(TestEnv.DefaultWaitSeconds));
    }

    public string CurrentUrl => _driver.Url;
    public IWebDriver Driver => _driver;

    // ── Navigation ────────────────────────────────────────────────────────────

    public void GoTo(string path)
    {
        if (path.StartsWith("http", StringComparison.OrdinalIgnoreCase))
        {
            _driver.Navigate().GoToUrl(path);
            return;
        }

        var url = TestEnv.AppBaseUrl.TrimEnd('/') + "/" + path.TrimStart('/');
        _driver.Navigate().GoToUrl(url);
    }

    public void WaitForUrl(string fragment, int timeoutSec = 15) =>
        new WebDriverWait(_driver, TimeSpan.FromSeconds(timeoutSec)).Until(d => d.Url.Contains(fragment));

    public void Refresh() => _driver.Navigate().Refresh();

    // ── Session ───────────────────────────────────────────────────────────────

    public void ClearSession()
    {
        try
        {
            _driver.Manage().Cookies.DeleteAllCookies();
            ((IJavaScriptExecutor)_driver).ExecuteScript(
                "try { localStorage.clear(); sessionStorage.clear(); } catch(e) {}");
        }
        catch { /* ignore */ }
    }

    public void Login(string email, string password)
    {
        GoTo("/login");
        Wait(800);
        Fill(By.CssSelector("input[type='email'], input[name='email']"), email);
        Fill(By.CssSelector("input[type='password'], input[name='password']"), password);
        Click(By.CssSelector("button[type='submit']"));
        Wait(1500);
    }

    public void LoginAdmin()        => Login(TestEnv.AdminEmail,      TestEnv.AdminPassword);
    public void LoginInstructor()   => Login(TestEnv.InstructorEmail,  TestEnv.InstructorPassword);
    public void LoginStudent()      => Login(TestEnv.StudentEmail,      TestEnv.StudentPassword);

    public void Logout()
    {
        try
        {
            var btns = FindAll(By.XPath(
                "//button[contains(text(),'Logout') or contains(text(),'Đăng xuất') or contains(text(),'Sign out')]"));
            if (btns.Count > 0) { JsClick(btns[0]); Wait(800); }
            else { ClearSession(); GoTo("/"); }
        }
        catch { ClearSession(); GoTo("/"); }
    }

    // ── Interactions ──────────────────────────────────────────────────────────

    public void Click(By locator)
    {
        var el = _wait.Until(d => d.FindElement(locator));
        ScrollIntoView(el);
        el.Click();
    }

    public void JsClick(IWebElement element) =>
        ((IJavaScriptExecutor)_driver).ExecuteScript("arguments[0].click();", element);

    public void Fill(By locator, string text)
    {
        var el = _wait.Until(d => d.FindElement(locator));
        el.Clear();
        el.SendKeys(text);
    }

    public void FillByLabelOrPlaceholder(string hint, string value)
    {
        if (string.Equals(hint, "email", StringComparison.OrdinalIgnoreCase))
        { Fill(By.CssSelector("input[type='email']"), value); return; }
        if (string.Equals(hint, "password", StringComparison.OrdinalIgnoreCase))
        { Fill(By.CssSelector("input[type='password']"), value); return; }

        var safe = hint.Replace("'", "\\'");
        Fill(By.XPath(
            $"//input[@placeholder='{safe}'] | //textarea[@placeholder='{safe}'] | " +
            $"//label[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'{safe.ToLowerInvariant()}')]/following::input[1]"),
            value);
    }

    public void ScrollIntoView(IWebElement element) =>
        ((IJavaScriptExecutor)_driver).ExecuteScript(
            "arguments[0].scrollIntoView({block:'center'});", element);

    public void SelectDropdown(By locator, string visibleText)
    {
        var el  = _wait.Until(d => d.FindElement(locator));
        var sel = new SelectElement(el);
        sel.SelectByText(visibleText);
    }

    // ── Finding ───────────────────────────────────────────────────────────────

    public IWebElement? FindOrNull(By locator)
    {
        try { return _driver.FindElement(locator); } catch { return null; }
    }

    public List<IWebElement> FindAll(By locator)
    {
        try { return _driver.FindElements(locator).ToList(); }
        catch { return new List<IWebElement>(); }
    }

    // ── Assertions / checks ───────────────────────────────────────────────────

    public bool TextVisible(string text)
    {
        try
        {
            var safe = text.Replace("'", "\\'");
            return _driver.FindElements(By.XPath($"//*[contains(text(),'{safe}')]")).Count > 0;
        }
        catch { return false; }
    }

    public bool ElementExists(By locator)
    {
        try { return _driver.FindElement(locator).Displayed; }
        catch { return false; }
    }

    public void WaitForText(string text, int timeoutSec = 10)
    {
        var safe = text.Replace("'", "\\'");
        new WebDriverWait(_driver, TimeSpan.FromSeconds(timeoutSec))
            .Until(d => d.FindElements(By.XPath($"//*[contains(text(),'{safe}')]")).Count > 0);
    }

    public void WaitForElement(By locator, int timeoutSec = 10) =>
        new WebDriverWait(_driver, TimeSpan.FromSeconds(timeoutSec))
            .Until(d => d.FindElement(locator).Displayed);

    // ── Helpers ───────────────────────────────────────────────────────────────

    public int GetFirstCourseIdFromLinks()
    {
        foreach (var link in (IEnumerable<IWebElement>)FindAll(By.CssSelector("a[href*='/courses/']")))
        {
            var href = link.GetAttribute("href") ?? "";
            var m    = Regex.Match(href, @"/courses/(\d+)");
            if (m.Success && int.TryParse(m.Groups[1].Value, out var id)) return id;
        }

        return 0;
    }

    public int GetFirstLearnCourseIdFromLinks()
    {
        foreach (var link in (IEnumerable<IWebElement>)FindAll(By.CssSelector("a[href*='/learn/']")))
        {
            var href = link.GetAttribute("href") ?? "";
            var m    = Regex.Match(href, @"/learn/(\d+)");
            if (m.Success && int.TryParse(m.Groups[1].Value, out var id)) return id;
        }

        return 0;
    }

    public static void Wait(int ms) => Thread.Sleep(ms);
}
