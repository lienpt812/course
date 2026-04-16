using System.Net.Http.Headers;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace CourseRegistration.Tests.Helpers;

/// <summary>
/// Direct HTTP calls to the backend API.
/// Used for: seeding test data, bypassing UI for setup steps, ban user, etc.
/// </summary>
public static class ApiHelper
{
    // ── Base config ──────────────────────────────────────────────────────────
    /// <summary>Backend API (không qua Vite). Mặc định khớp proxy <c>fe_new</c>: vite → localhost:8000.</summary>
    private static string ApiBase => Environment.GetEnvironmentVariable("API_BASE_URL") ?? "http://localhost:8000/api/v1";

    public static string HealthUrl =>
        Environment.GetEnvironmentVariable("API_HEALTH_URL") ?? "http://localhost:8000/health";

    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(30) };

    // ── Cached tokens ────────────────────────────────────────────────────────
    private static string? _adminToken;
    private static string? _instructorToken;
    private static string? _studentToken;

    // ── Authentication ───────────────────────────────────────────────────────
    public static string GetAdminToken()
    {
        if (_adminToken != null) return _adminToken;
        _adminToken = Login(PageHelper.AdminEmail, PageHelper.AdminPassword);
        return _adminToken;
    }

    public static string GetInstructorToken()
    {
        if (_instructorToken != null) return _instructorToken;
        _instructorToken = Login(PageHelper.InstructorEmail, PageHelper.InstructorPassword);
        return _instructorToken;
    }

    public static string GetStudentToken()
    {
        if (_studentToken != null) return _studentToken;
        _studentToken = Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        return _studentToken;
    }

    public static void ResetTokenCache()
    {
        _adminToken = null;
        _instructorToken = null;
        _studentToken = null;
    }

    private static string Login(string email, string password)
    {
        var body = JsonConvert.SerializeObject(new { email, password });
        var resp = Post("/auth/login", body, token: null);
        var obj  = JObject.Parse(resp);
        return obj["data"]?["access_token"]?.ToString()
            ?? obj["access_token"]?.ToString()
            ?? obj["token"]?.ToString()
            ?? obj["data"]?["token"]?.ToString()
            ?? throw new Exception($"Cannot parse token from login response: {resp}");
    }

    /// <summary>Kiểm tra backend (health) trước khi seed / mở browser.</summary>
    public static bool IsBackendReachable()
    {
        try
        {
            var req = new HttpRequestMessage(HttpMethod.Get, HealthUrl);
            using var resp = Http.Send(req);
            return resp.IsSuccessStatusCode;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>Slug khóa học do <c>ensure_altwalker_e2e_fixtures</c> tạo — MBT/AltWalker cần có trước khi chạy model.</summary>
    public const string AltwalkerSeedCourseSlug = "altwalker-e2e-cert";

    // ── Seed helpers ─────────────────────────────────────────────────────────
    /// <summary>
    /// Re-seed all test data via <c>POST /admin/seed</c> (backend creates student/instructor/admin@test.com,
    /// course <c>altwalker-e2e-cert</c> with lessons, CONFIRMED student with incomplete progress, pending sample for admin MBT).
    /// Requires <c>admin@test.com</c> to exist (see tests/altwalker README). Call from <see cref="Setup.setUpRun"/>.
    /// Sau khi gọi API, <b>xác minh</b> dữ liệu (khóa publish + ≥2 lesson + login student); nếu vẫn lỗi sau 1 lần seed lại → throw.
    /// </summary>
    public static void SeedAll()
    {
        ResetTokenCache();
        RunSeedPostWithFallback();

        if (TryVerifyTestSeedData(out var err))
            return;

        Console.WriteLine($"[SeedAll] Verification failed: {err}. Retrying POST /admin/seed once...");
        ResetTokenCache();
        RunSeedPostWithFallback();

        if (!TryVerifyTestSeedData(out err))
            throw new InvalidOperationException(
                "[SeedAll] Dữ liệu test chưa sẵn sàng sau khi seed. " + err +
                " Kiểm tra API (POST /admin/seed), DB, và tài khoản admin@test.com.");
    }

    private static void RunSeedPostWithFallback()
    {
        try
        {
            Post("/admin/seed", "{}", GetAdminToken());
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[SeedAll] POST /admin/seed failed: {ex.Message}");
            try
            {
                SeedPendingRegistrations();
            }
            catch
            {
                /* no legacy endpoint */
            }
        }
    }

    /// <summary>
    /// Kiểm tra điều kiện tối thiểu cho MBT: admin/student đăng nhập được, có khóa published <see cref="AltwalkerSeedCourseSlug"/>,
    /// outline có ít nhất 2 lesson (học viên đã CONFIRMED theo seed).
    /// </summary>
    public static bool TryVerifyTestSeedData(out string reason)
    {
        reason = "";
        try
        {
            _ = GetAdminToken();
        }
        catch (Exception ex)
        {
            reason = "Admin login: " + ex.Message;
            return false;
        }

        try
        {
            var json   = Get("/courses?status=PUBLISHED&limit=100", GetAdminToken());
            var courses = TryParseArray(json);
            var alt     = courses.FirstOrDefault(c => string.Equals(
                c["slug"]?.ToString(), AltwalkerSeedCourseSlug, StringComparison.Ordinal));
            if (alt == null)
            {
                reason = $"Không thấy khóa published slug '{AltwalkerSeedCourseSlug}' trong danh sách courses.";
                return false;
            }

            var courseId = alt["id"]!.ToObject<int>();

            try
            {
                _ = GetStudentToken();
            }
            catch (Exception ex)
            {
                reason = "Student login: " + ex.Message;
                return false;
            }

            var outlineJson = Get($"/learning/courses/{courseId}/outline", GetStudentToken());
            var lessonCount = CountLessonsInOutlineJson(outlineJson);
            if (lessonCount < 2)
            {
                reason = $"Khóa '{AltwalkerSeedCourseSlug}' cần ≥2 lesson (outline hiện có {lessonCount}).";
                return false;
            }

            return true;
        }
        catch (Exception ex)
        {
            reason = ex.Message;
            return false;
        }
    }

    private static int CountLessonsInOutlineJson(string json)
    {
        var obj  = JObject.Parse(json);
        var data = obj["data"] as JArray;
        if (data == null) return 0;
        var n = 0;
        foreach (var sec in data)
            n += (sec?["lessons"] as JArray)?.Count ?? 0;
        return n;
    }

    /// <summary>Ensure at least N pending registrations exist for the test course.</summary>
    public static void SeedPendingRegistrations(int count = 5)
    {
        try
        {
            var body = JsonConvert.SerializeObject(new { count });
            Post("/admin/test/seed/registrations", body, GetAdminToken());
        }
        catch { /* best-effort */ }
    }

    // ── Course ───────────────────────────────────────────────────────────────
    public static int CreateCourse(string title, string slug, string description = "Test course",
        string category = "Backend", string level = "Beginner", int maxCapacity = 10,
        decimal price = 0, bool certificateEnabled = true)
    {
        var body = JsonConvert.SerializeObject(new
        {
            title, slug, description, category, level,
            max_capacity       = maxCapacity,
            price,
            certificate_enabled = certificateEnabled,
            status             = "DRAFT"
        });
        var resp = Post("/courses", body, GetInstructorToken());
        var obj  = JObject.Parse(resp);
        return obj["id"]?.ToObject<int>()
            ?? obj["data"]?["id"]?.ToObject<int>()
            ?? throw new Exception($"Cannot parse course id: {resp}");
    }

    public static void PublishCourse(int courseId)
    {
        Patch($"/courses/{courseId}", JsonConvert.SerializeObject(new { status = "PUBLISHED" }), GetInstructorToken());
    }

    public static int GetOrCreateTestCourseId()
    {
        try
        {
            var resp = Get("/courses?status=DRAFT&limit=1", GetInstructorToken());
            var arr  = JArray.Parse(resp);
            if (arr.Count > 0) return arr[0]["id"]!.ToObject<int>();
        }
        catch { }
        return CreateCourse("Test Course " + DateTime.Now.Ticks, "test-course-" + DateTime.Now.Ticks);
    }

    // ── Section & Lesson ─────────────────────────────────────────────────────
    public static int CreateSection(int courseId, string title = "Test Section", int position = 1)
    {
        var body = JsonConvert.SerializeObject(new { course_id = courseId, title, position });
        var resp = Post("/learning/sections", body, GetInstructorToken());
        var obj  = JObject.Parse(resp);
        return obj["id"]?.ToObject<int>() ?? obj["data"]?["id"]?.ToObject<int>()
            ?? throw new Exception($"Cannot parse section id: {resp}");
    }

    public static int CreateLesson(int sectionId, string title = "Test Lesson",
        string type = "TEXT", int position = 1, int durationMinutes = 5)
    {
        var body = JsonConvert.SerializeObject(new
        {
            section_id       = sectionId,
            title,
            type,
            position,
            duration_minutes = durationMinutes,
            is_preview       = false
        });
        var resp = Post("/learning/lessons", body, GetInstructorToken());
        var obj  = JObject.Parse(resp);
        return obj["id"]?.ToObject<int>() ?? obj["data"]?["id"]?.ToObject<int>()
            ?? throw new Exception($"Cannot parse lesson id: {resp}");
    }

    // ── Registration ─────────────────────────────────────────────────────────
    public static int RegisterStudent(int courseId)
    {
        var body = JsonConvert.SerializeObject(new { course_id = courseId });
        var resp = Post("/registrations", body, GetStudentToken());
        var obj  = JObject.Parse(resp);
        return obj["id"]?.ToObject<int>() ?? obj["data"]?["id"]?.ToObject<int>()
            ?? throw new Exception($"Cannot parse registration id: {resp}");
    }

    public static void ApproveRegistration(int registrationId)
    {
        Post($"/registrations/{registrationId}/approve", "{}", GetAdminToken());
    }

    public static void RejectRegistration(int registrationId)
    {
        Post($"/registrations/{registrationId}/reject",
            JsonConvert.SerializeObject(new { reason = "Test rejection" }),
            GetAdminToken());
    }

    public static void CancelRegistration(int registrationId)
    {
        Post($"/registrations/{registrationId}/cancel", "{}", GetStudentToken());
    }

    public static List<int> GetPendingRegistrationIds(int? courseId = null)
    {
        var query = courseId.HasValue ? $"?course_id={courseId}&status=PENDING" : "?status=PENDING";
        var resp  = Get($"/registrations{query}", GetAdminToken());
        var arr   = TryParseArray(resp);
        return arr.Select(r => r["id"]!.ToObject<int>()).ToList();
    }

    // ── User / Ban ───────────────────────────────────────────────────────────
    public static int GetFirstActiveUserId()
    {
        try
        {
            var resp = Get("/admin/users?status=ACTIVE&role=STUDENT&limit=1", GetAdminToken());
            var arr  = TryParseArray(resp);
            if (arr.Count > 0) return arr[0]["id"]!.ToObject<int>();
        }
        catch { }
        return 0;
    }

    public static void BanUser(int userId)
    {
        Post($"/admin/users/{userId}/ban", "{}", GetAdminToken());
    }

    // ── Learning Progress ────────────────────────────────────────────────────
    public static void MarkLessonComplete(int lessonId)
    {
        var body = JsonConvert.SerializeObject(new { lesson_id = lessonId, completion_pct = 100 });
        Post("/learning/progress", body, GetStudentToken());
    }

    public static void CompleteAllLessons(int courseId)
    {
        try
        {
            var resp    = Get($"/learning/courses/{courseId}/outline", GetStudentToken());
            var outline = JObject.Parse(resp);
            var lessons = outline["lessons"]?.ToObject<JArray>()
                       ?? outline["data"]?["lessons"]?.ToObject<JArray>()
                       ?? new JArray();
            foreach (var lesson in lessons)
            {
                var lessonId = lesson["id"]!.ToObject<int>();
                MarkLessonComplete(lessonId);
            }
        }
        catch { /* best-effort */ }
    }

    // ── Certificate ──────────────────────────────────────────────────────────
    public static string IssueCertificate(int courseId)
    {
        var resp = Post($"/certificates/issue/{courseId}", "{}", GetStudentToken());
        var obj  = JObject.Parse(resp);
        return obj["verification_code"]?.ToString()
            ?? obj["code"]?.ToString()
            ?? obj["data"]?["verification_code"]?.ToString()
            ?? string.Empty;
    }

    public static bool VerifyCertificate(string code)
    {
        try
        {
            var resp = Get($"/certificates/verify/{code}", token: null);
            var obj  = JObject.Parse(resp);
            return obj["valid"]?.ToObject<bool>() ?? obj["data"]?["valid"]?.ToObject<bool>() ?? false;
        }
        catch { return false; }
    }

    // ── Notifications ────────────────────────────────────────────────────────
    public static List<JToken> GetMyNotifications()
    {
        var resp = Get("/notifications/me", GetStudentToken());
        return TryParseArray(resp);
    }

    public static void MarkNotificationRead(int notifId)
    {
        Post($"/notifications/{notifId}/read", "{}", GetStudentToken());
    }

    // ── Admin Jobs ───────────────────────────────────────────────────────────
    public static void RunExpirePendingJob()
    {
        Post("/admin/jobs/expire-pending", "{}", GetAdminToken());
    }

    // ── HTTP primitives ──────────────────────────────────────────────────────
    public static string Get(string path, string? token)
    {
        var req = new HttpRequestMessage(HttpMethod.Get, $"{ApiBase}{path}");
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var resp = Http.Send(req);
        var body = resp.Content.ReadAsStringAsync().Result;
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"GET {path} → {(int)resp.StatusCode}: {body}");
        return body;
    }

    public static string Post(string path, string jsonBody, string? token)
    {
        var req = new HttpRequestMessage(HttpMethod.Post, $"{ApiBase}{path}")
        {
            Content = new StringContent(jsonBody, Encoding.UTF8, "application/json")
        };
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var resp = Http.Send(req);
        var body = resp.Content.ReadAsStringAsync().Result;
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"POST {path} → {(int)resp.StatusCode}: {body}");
        return body;
    }

    public static string Patch(string path, string jsonBody, string? token)
    {
        var req = new HttpRequestMessage(HttpMethod.Patch, $"{ApiBase}{path}")
        {
            Content = new StringContent(jsonBody, Encoding.UTF8, "application/json")
        };
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var resp = Http.Send(req);
        var body = resp.Content.ReadAsStringAsync().Result;
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"PATCH {path} → {(int)resp.StatusCode}: {body}");
        return body;
    }

    public static string Delete(string path, string? token)
    {
        var req = new HttpRequestMessage(HttpMethod.Delete, $"{ApiBase}{path}");
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var resp = Http.Send(req);
        var body = resp.Content.ReadAsStringAsync().Result;
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"DELETE {path} → {(int)resp.StatusCode}: {body}");
        return body;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static List<JToken> TryParseArray(string json)
    {
        json = json.Trim();
        if (json.StartsWith("["))
            return JArray.Parse(json).ToList();
        var obj = JObject.Parse(json);
        // common wrappers: { data: [...] } or { items: [...] } or { registrations: [...] }
        foreach (var key in new[] { "data", "items", "registrations", "users", "courses", "lessons", "sections", "notifications" })
        {
            if (obj[key] is JArray arr) return arr.ToList();
        }
        return new List<JToken>();
    }
}