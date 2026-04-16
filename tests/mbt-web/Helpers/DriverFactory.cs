using OpenQA.Selenium;
using OpenQA.Selenium.Chrome;

namespace Lms.MbtWeb.Helpers;

public static class DriverFactory
{
    public static IWebDriver Create()
    {
        var opts = new ChromeOptions();
        opts.AddArgument("--no-sandbox");
        opts.AddArgument("--disable-dev-shm-usage");
        opts.AddArgument("--window-size=1920,1080");
        opts.AddArgument("--disable-extensions");
        opts.AddArgument("--disable-gpu");
        opts.AddArgument("--lang=vi");
        if (TestEnv.Headless)
            opts.AddArgument("--headless=new");
        return new ChromeDriver(opts);
    }
}
