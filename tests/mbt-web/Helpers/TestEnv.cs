namespace Lms.MbtWeb.Helpers;

/// <summary>Tập trung toàn bộ biến môi trường và giá trị mặc định.</summary>
public static class TestEnv
{
    // ── URLs ─────────────────────────────────────────────────────────────────
    public static string AppBaseUrl  => E("APP_BASE_URL",  "http://localhost:3000");
    public static string ApiBaseUrl  => E("API_BASE_URL",  "http://localhost:8000/api/v1");
    public static string HealthUrl   => E("API_HEALTH_URL","http://localhost:8000/health");

    // ── Credentials ───────────────────────────────────────────────────────────
    public static string AdminEmail     => E("ADMIN_EMAIL",       "admin@test.com");
    public static string AdminPassword  => E("ADMIN_PASSWORD",    "Password123!");
    public static string InstructorEmail    => E("INSTRUCTOR_EMAIL",    "instructor@test.com");
    public static string InstructorPassword => E("INSTRUCTOR_PASSWORD", "Password123!");
    public static string StudentEmail    => E("STUDENT_EMAIL",    "student@test.com");
    public static string StudentPassword => E("STUDENT_PASSWORD", "Password123!");

    // ── Selenium ──────────────────────────────────────────────────────────────
    public static bool Headless           => Flag("HEADLESS");
    public static int  DefaultWaitSeconds => int.TryParse(E("WAIT_SECONDS", "15"), out var v) ? v : 15;

    // ── Seed ──────────────────────────────────────────────────────────────────
    public const string SeedCourseSlug = "altwalker-e2e-cert";

    private static string E(string key, string def)
    {
        var v = Environment.GetEnvironmentVariable(key);
        return string.IsNullOrWhiteSpace(v) ? def : v.Trim();
    }

    private static bool Flag(string key) =>
        string.Equals(E(key, "0"), "1", StringComparison.Ordinal);
}
