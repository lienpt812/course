using Altom.AltWalker;
using CourseRegistration.Tests.Helpers;
using OpenQA.Selenium;

namespace CourseRegistration.Tests;

public class Setup
{
    internal static IWebDriver Driver = null!;
    internal static PageHelper Page   = null!;

    public void setUpRun()
    {
        // 1. Verify backend is reachable
        if (!ApiHelper.IsBackendReachable())
            throw new Exception(
                "[Setup] Backend không chạy tại " + ApiHelper.HealthUrl +
                ". Khởi động API + Postgres; có thể đặt API_HEALTH_URL / API_BASE_URL.");

        // 2. Seed + verify — in log rõ ràng; nếu dữ liệu vẫn thiếu thì warn nhưng không throw
        //    (throw trong setUpRun → AltWalker báo GraphWalker 500 khó đọc).
        try
        {
            ApiHelper.SeedAll();
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine("==========================================================");
            Console.Error.WriteLine("[Setup] SEED FAILED — một số test có thể fail vì thiếu data.");
            Console.Error.WriteLine("Lý do: " + ex.Message);
            Console.Error.WriteLine("==========================================================");
        }

        // 3. Start browser
        StartBrowser();
        Console.WriteLine("[Setup] setUpRun complete.");
    }

    public void tearDownRun()
    {
        try { Driver?.Quit(); Driver?.Dispose(); }
        catch { }
        Console.WriteLine("[Setup] Browser closed.");
    }

    public void afterStep()
    {
        // Verify browser is still alive; restart if crashed
        try
        {
            _ = Driver.Url; // throws if browser is dead
        }
        catch
        {
            Console.WriteLine("[Setup] Browser crashed - restarting...");
            try { Driver?.Dispose(); } catch { }
            StartBrowser();
        }
    }

    private static void StartBrowser()
    {
        Driver = DriverFactory.Create(headless: false);
        Page   = new PageHelper(Driver); // PageHelper always uses this Driver
    }
}
