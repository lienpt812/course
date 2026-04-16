using Altom.AltWalker;
using Lms.MbtWeb.Helpers;
using OpenQA.Selenium;

namespace Lms.MbtWeb;

/// <summary>Global setup / teardown — chạy 1 lần cho toàn bộ test run.</summary>
public class Setup
{
    internal static IWebDriver Driver = null!;
    internal static PageHelper Page   = null!;

    public void setUpRun()
    {
        if (!ApiHelper.IsBackendReachable())
            throw new Exception(
                $"[Setup] Backend không chạy tại {TestEnv.HealthUrl}. " +
                "Khởi động API + Postgres trước, hoặc set API_BASE_URL / API_HEALTH_URL.");

        // Seed data — in log rõ ràng nhưng không throw (throw → AltWalker báo GW 500 khó đọc)
        try { ApiHelper.SeedAll(); }
        catch (Exception ex)
        {
            Console.Error.WriteLine("[Setup] SEED FAILED: " + ex.Message);
        }

        Driver = DriverFactory.Create();
        Page   = new PageHelper(Driver);
        Console.WriteLine("[Setup] Browser started. Ready.");
    }

    public void tearDownRun()
    {
        try { Driver?.Quit(); Driver?.Dispose(); }
        catch { }
        Console.WriteLine("[Setup] Browser closed.");
    }

    public void afterStep()
    {
        // Khởi động lại browser nếu crash
        try { _ = Driver.Url; }
        catch
        {
            Console.WriteLine("[Setup] Browser crashed — restarting...");
            try { Driver?.Dispose(); } catch { }
            Driver = DriverFactory.Create();
            Page   = new PageHelper(Driver);
        }
    }
}
