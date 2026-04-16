using OpenQA.Selenium;
using OpenQA.Selenium.Chrome;

namespace CourseRegistration.Tests.Helpers;

public static class DriverFactory
{
    /// <summary>
    /// Creates a new ChromeDriver instance.
    /// Do NOT use a singleton - each call returns a fresh browser.
    /// Setup.Driver holds the current instance.
    /// </summary>
    public static IWebDriver Create(bool headless = false)
    {
        var options = new ChromeOptions();
        options.AddArgument("--no-sandbox");
        options.AddArgument("--disable-dev-shm-usage");
        options.AddArgument("--window-size=1920,1080");
        options.AddArgument("--disable-extensions");
        options.AddArgument("--disable-gpu");
        if (headless)
            options.AddArgument("--headless=new");

        return new ChromeDriver(options);
    }
}
