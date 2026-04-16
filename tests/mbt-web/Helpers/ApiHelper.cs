using System.Net.Http.Headers;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Lms.MbtWeb.Helpers;

/// <summary>
/// HTTP trực tiếp tới API backend — dùng để seed data và lấy IDs cho các step.
/// </summary>
public static class ApiHelper
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(30) };

    // ── Token cache ──────────────────────────────────────────────────────────
    private static string? _adminToken;
    private static string? _instructorToken;
    private static string? _studentToken;

    public static void ResetTokenCache()
    {
        _adminToken = null;
        _instructorToken = null;
        _studentToken = null;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public static string GetAdminToken()
    {
        _adminToken ??= Login(TestEnv.AdminEmail, TestEnv.AdminPassword);
        return _adminToken;
    }

    public static string GetInstructorToken()
    {
        _instructorToken ??= Login(TestEnv.InstructorEmail, TestEnv.InstructorPassword);
        return _instructorToken;
    }

    public static string GetStudentToken()
    {
        _studentToken ??= Login(TestEnv.StudentEmail, TestEnv.StudentPassword);
        return _studentToken;
    }

    private static string Login(string email, string password)
    {
        var resp = Post("/auth/login", new { email, password }, null);
        var obj  = JObject.Parse(resp);
        return obj["data"]?["access_token"]?.ToString()
            ?? obj["access_token"]?.ToString()
            ?? throw new Exception($"Login failed ({email}): {resp}");
    }

    // ── Health ────────────────────────────────────────────────────────────────

    public static bool IsBackendReachable()
    {
        try
        {
            using var resp = Http.Send(new HttpRequestMessage(HttpMethod.Get, TestEnv.HealthUrl));
            return resp.IsSuccessStatusCode;
        }
        catch { return false; }
    }

    // ── Seed ──────────────────────────────────────────────────────────────────

    /// <summary>
    /// POST /admin/seed → xác minh (khoá published + ≥2 lesson + logins). Thử lại 1 lần nếu fail.
    /// In thông báo rõ nhưng KHÔNG throw — tránh AltWalker báo GraphWalker 500 khó đọc.
    /// </summary>
    public static void SeedAll()
    {
        ResetTokenCache();
        TryPostSeed();

        if (VerifyTestData(out var err))
        {
            Console.WriteLine("[ApiHelper] Seed OK.");
            return;
        }

        Console.WriteLine("[ApiHelper] Verify failed: " + err + " — retrying seed...");
        ResetTokenCache();
        TryPostSeed();

        if (!VerifyTestData(out err))
        {
            Console.Error.WriteLine(
                "=============================================================\n" +
                "[ApiHelper] SEED WARNING: " + err + "\n" +
                "Một số test có thể fail. Kiểm tra backend + DB.\n" +
                "=============================================================");
        }
        else
        {
            Console.WriteLine("[ApiHelper] Seed OK (retry).");
        }
    }

    private static void TryPostSeed()
    {
        try
        {
            Post("/admin/seed", new { }, GetAdminToken());
        }
        catch (Exception ex)
        {
            Console.WriteLine("[ApiHelper] POST /admin/seed: " + ex.Message);
        }
    }

    public static bool VerifyTestData(out string reason)
    {
        reason = "";
        try
        {
            _ = GetAdminToken();
            _ = GetStudentToken();
        }
        catch (Exception ex) { reason = "Login: " + ex.Message; return false; }

        try
        {
            var resp  = Get("/courses?status=PUBLISHED&limit=100", GetAdminToken());
            var items = ParseArray(resp);
            var seed  = items.FirstOrDefault(c =>
                string.Equals(c["slug"]?.ToString(), TestEnv.SeedCourseSlug, StringComparison.Ordinal));
            if (seed == null)
            {
                reason = $"Không thấy khóa slug '{TestEnv.SeedCourseSlug}'.";
                return false;
            }

            var cid = seed["id"]?.Value<int>() ?? 0;
            var outline = Get($"/learning/courses/{cid}/outline", GetStudentToken());
            var lessons  = CountLessonsInOutline(JObject.Parse(outline));
            if (lessons < 2)
            {
                reason = $"Khóa seed cần ≥2 lesson, outline có {lessons}.";
                return false;
            }

            return true;
        }
        catch (Exception ex) { reason = ex.Message; return false; }
    }

    private static int CountLessonsInOutline(JObject root)
    {
        var data = root["data"] as JArray;
        if (data == null) return 0;
        var n = 0;
        foreach (var sec in data)
            n += (sec?["lessons"] as JArray)?.Count ?? 0;
        return n;
    }

    // ── Courses ───────────────────────────────────────────────────────────────

    public static int GetPublishedCourseId(string? slug = null)
    {
        var url  = "/courses?status=PUBLISHED&limit=50";
        var resp = Get(url, GetAdminToken());
        var list = ParseArray(resp);
        if (!string.IsNullOrEmpty(slug))
        {
            var hit = list.FirstOrDefault(c =>
                string.Equals(c["slug"]?.ToString(), slug, StringComparison.Ordinal));
            if (hit != null) return hit["id"]?.Value<int>() ?? 0;
        }

        return list.Count > 0 ? list[0]["id"]?.Value<int>() ?? 0 : 0;
    }

    public static int CreateDraftCourse(string title, string slug)
    {
        var resp = Post("/courses", new
        {
            title,
            slug,
            description = "MBT test course — auto created.",
            category    = "Testing",
            level       = "Beginner",
            max_capacity = 30,
            price       = 0,
            status      = "DRAFT"
        }, GetInstructorToken());
        var obj = JObject.Parse(resp);
        return obj["data"]?["id"]?.Value<int>() ?? obj["id"]?.Value<int>() ?? 0;
    }

    public static void PublishCourse(int courseId) =>
        Patch($"/courses/{courseId}", new { status = "PUBLISHED" }, GetInstructorToken());

    // ── Registrations ─────────────────────────────────────────────────────────

    public static int GetFirstPendingRegistrationId(int? courseId = null)
    {
        var q    = courseId.HasValue ? $"?course_id={courseId}&status=PENDING" : "?status=PENDING";
        var resp = Get("/registrations" + q, GetAdminToken());
        var list = ParseArray(resp);
        return list.Count > 0 ? list[0]["id"]?.Value<int>() ?? 0 : 0;
    }

    public static int EnsurePendingRegistration(int courseId)
    {
        var rid = GetFirstPendingRegistrationId(courseId);
        if (rid > 0) return rid;
        // register as student if none
        try
        {
            var resp = Post("/registrations", new { course_id = courseId }, GetStudentToken());
            var obj  = JObject.Parse(resp);
            return obj["data"]?["id"]?.Value<int>() ?? 0;
        }
        catch { return 0; }
    }

    public static void ApproveRegistration(int rid) =>
        Post($"/registrations/{rid}/approve", new { reason = "MBT approved" }, GetAdminToken());

    public static void RejectRegistration(int rid) =>
        Post($"/registrations/{rid}/reject", new { reason = "MBT rejected" }, GetAdminToken());

    // ── Learning ──────────────────────────────────────────────────────────────

    public static int GetFirstLessonId(int courseId)
    {
        var resp = Get($"/learning/courses/{courseId}/outline", GetStudentToken());
        var root = JObject.Parse(resp);
        var data = root["data"] as JArray;
        if (data == null || data.Count == 0) return 0;
        var lessons = data[0]["lessons"] as JArray;
        return lessons?.Count > 0 ? lessons[0]["id"]?.Value<int>() ?? 0 : 0;
    }

    public static void MarkLessonComplete(int lessonId) =>
        Post("/learning/progress", new { lesson_id = lessonId, completion_pct = 100 }, GetStudentToken());

    // ── Certificates ──────────────────────────────────────────────────────────

    public static string? GetFirstCertificateCode()
    {
        var resp  = Get("/certificates/me", GetStudentToken());
        var certs = ParseArray(resp);
        return certs.Count > 0 ? certs[0]["verification_code"]?.ToString() : null;
    }

    public static string? IssueCertificate(int courseId)
    {
        try
        {
            var resp = Post($"/certificates/issue/{courseId}", new { }, GetStudentToken());
            var obj  = JObject.Parse(resp);
            return obj["data"]?["verification_code"]?.ToString();
        }
        catch { return null; }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public static List<JToken> GetMyNotifications() =>
        ParseArray(Get("/notifications/me", GetStudentToken()));

    public static void MarkNotificationRead(int notifId) =>
        Post($"/notifications/{notifId}/read", new { }, GetStudentToken());

    // ── Users ─────────────────────────────────────────────────────────────────

    public static int GetFirstActiveStudentId()
    {
        try
        {
            var resp = Get("/admin/users?status=ACTIVE&role=STUDENT&limit=1", GetAdminToken());
            var list = ParseArray(resp);
            return list.Count > 0 ? list[0]["id"]?.Value<int>() ?? 0 : 0;
        }
        catch { return 0; }
    }

    // ── HTTP primitives ──────────────────────────────────────────────────────

    public static string Get(string path, string? token)
    {
        using var req = new HttpRequestMessage(HttpMethod.Get, TestEnv.ApiBaseUrl + path);
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        using var resp = Http.Send(req);
        var body = resp.Content.ReadAsStringAsync().Result;
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"GET {path} → {(int)resp.StatusCode}: {body}");
        return body;
    }

    public static string Post(string path, object payload, string? token)
    {
        using var req = new HttpRequestMessage(HttpMethod.Post, TestEnv.ApiBaseUrl + path)
        {
            Content = new StringContent(JsonConvert.SerializeObject(payload), Encoding.UTF8, "application/json")
        };
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        using var resp = Http.Send(req);
        var body = resp.Content.ReadAsStringAsync().Result;
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"POST {path} → {(int)resp.StatusCode}: {body}");
        return body;
    }

    public static string Patch(string path, object payload, string? token)
    {
        using var req = new HttpRequestMessage(HttpMethod.Patch, TestEnv.ApiBaseUrl + path)
        {
            Content = new StringContent(JsonConvert.SerializeObject(payload), Encoding.UTF8, "application/json")
        };
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        using var resp = Http.Send(req);
        var body = resp.Content.ReadAsStringAsync().Result;
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"PATCH {path} → {(int)resp.StatusCode}: {body}");
        return body;
    }

    public static List<JToken> ParseArray(string json)
    {
        json = json.Trim();
        if (json.StartsWith("[")) return JArray.Parse(json).ToList();
        var obj = JObject.Parse(json);
        foreach (var key in new[] { "data", "items", "results", "courses", "registrations", "users", "notifications" })
        {
            if (obj[key] is JArray arr) return arr.ToList();
        }
        return new List<JToken>();
    }
}
