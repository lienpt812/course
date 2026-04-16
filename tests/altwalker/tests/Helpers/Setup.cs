using OpenQA.Selenium;
using OpenQA.Selenium.Chrome;
using NUnit.Framework;

namespace CourseRegistration.Tests.Helpers;

/// <summary>
/// Global test setup. Exposes Driver and Page for all model classes.
/// ChromeDriver is created once per NUnit test run (assembly scope).
/// </summary>
[SetUpFixture]
public class Setup
{
    public static IWebDriver Driver { get; private set; } = null!;
    public static PageHelper  Page  { get; private set; } = null!;

    [OneTimeSetUp]
    public void setUpRun()
    {
        var options = new ChromeOptions();

        if (Environment.GetEnvironmentVariable("HEADLESS") == "true")
            options.AddArgument("--headless=new");

        options.AddArgument("--no-sandbox");
        options.AddArgument("--disable-dev-shm-usage");
        options.AddArgument("--disable-gpu");
        options.AddArgument("--window-size=1920,1080");
        options.AddArgument("--remote-debugging-port=9222");

        Driver = new ChromeDriver(options);
        Driver.Manage().Timeouts().ImplicitWait = TimeSpan.FromSeconds(5);
        Driver.Manage().Window.Maximize();

        Page = new PageHelper(Driver);

        // Seed all test data once before any tests run
        try { ApiHelper.SeedAll(); }
        catch (Exception ex)
        {
            TestContext.Progress.WriteLine($"[WARN] SeedAll failed (tests may still pass): {ex.Message}");
        }
    }

    [OneTimeTearDown]
    public void tearDownRun()
    {
        Driver?.Quit();
        Driver?.Dispose();
    }
}