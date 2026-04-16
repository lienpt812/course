using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace CourseRegistration.Tests.Helpers;

/// <summary>
/// HTTP API helper that reads test data from TestDataConfig.
/// Used in setUpRun() to seed all required data before tests start.
/// </summary>
public static class ApiHelper
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(15) };
    private static readonly string Base = TestDataConfig.ApiBase;

    // Cached IDs after seeding
    public static int PublishedCourseId { get; private set; }
    public static int DraftCourseId     { get; private set; }
    public static int FullCourseId      { get; private set; }
    public static int Section1Id        { get; private set; }
    public static int Section2Id        { get; private set; }
    public static int Lesson1Id         { get; private set; }
    public static int Lesson2Id         { get; private set; }

    // ── Token management ─────────────────────────────────────────────────────

    private static string? _adminToken;
    private static string? _instructorToken;
    private static string? _studentToken;

    public static string AdminToken      => _adminToken      ?? throw new InvalidOperationException("Admin not authenticated");
    public static string InstructorToken => _instructorToken ?? throw new InvalidOperationException("Instructor not authenticated");
    public static string StudentToken    => _studentToken    ?? throw new InvalidOperationException("Student not authenticated");

    private static bool _seeded = false;
    private static readonly string SeedFlagFile = Path.Combine(
        Path.GetTempPath(), "altwalker_seed_done.flag");

    // ── Main entry point ─────────────────────────────────────────────────────

    /// <summary>
    /// Called once in setUpRun(). Seeds all test data from TestDataConfig.
    /// Uses a temp file flag so seeding only happens once per test session,
    /// even across multiple model processes.
    /// </summary>
    public static void SeedAll()
    {
        if (_seeded || File.Exists(SeedFlagFile))
        {
            // Still need to authenticate to get tokens
            AuthenticateAll();
            // Load cached IDs from flag file
            LoadCachedIds();
            Console.WriteLine($"[ApiHelper] Using cached seed data. PublishedCourseId={PublishedCourseId}");
            _seeded = true;
            return;
        }

        Console.WriteLine("[ApiHelper] Starting test data seeding...");

        // 1. Ensure all accounts exist
        EnsureAccounts();

        // 2. Authenticate all roles
        AuthenticateAll();

        // 3. Ensure courses exist (created by instructor)
        EnsureCourses();

        // 4. Ensure course content (sections + lessons)
        EnsureContent();

        // 5. Ensure student has a confirmed registration (for learning tests)
        EnsureStudentRegistration();

        // 6. Ensure pending registrations exist (for admin approval tests)
        EnsurePendingRegistrations();

        Console.WriteLine($"[ApiHelper] Seeding complete. PublishedCourseId={PublishedCourseId}, DraftCourseId={DraftCourseId}");
        // Write flag file with cached IDs
        File.WriteAllText(SeedFlagFile, $"{PublishedCourseId},{DraftCourseId},{FullCourseId},{Section1Id},{Section2Id},{Lesson1Id},{Lesson2Id}");
        _seeded = true;
    }

    private static void LoadCachedIds()
    {
        try
        {
            var parts = File.ReadAllText(SeedFlagFile).Split(',');
            if (parts.Length >= 7)
            {
                PublishedCourseId = int.Parse(parts[0]);
                DraftCourseId     = int.Parse(parts[1]);
                FullCourseId      = int.Parse(parts[2]);
                Section1Id        = int.Parse(parts[3]);
                Section2Id        = int.Parse(parts[4]);
                Lesson1Id         = int.Parse(parts[5]);
                Lesson2Id         = int.Parse(parts[6]);
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[ApiHelper] Could not load cached IDs: {ex.Message}");
        }
    }

    /// <summary>Delete the seed flag to force re-seeding on next run.</summary>
    public static void ResetSeedFlag()
    {
        if (File.Exists(SeedFlagFile)) File.Delete(SeedFlagFile);
        _seeded = false;
        Console.WriteLine("[ApiHelper] Seed flag reset.");
    }

    // ── Step 1: Accounts ─────────────────────────────────────────────────────

    private static void EnsureAccounts()
    {
        foreach (var user in TestDataConfig.Accounts.All)
        {
            var body = BuildRegisterBody(user);
            var res = Post("/auth/register", body, token: null);

            if (res.StatusCode == System.Net.HttpStatusCode.OK)
                Console.WriteLine($"[ApiHelper] Created account: {user.Email} ({user.Role})");
            else if (res.StatusCode == System.Net.HttpStatusCode.Conflict)
                Console.WriteLine($"[ApiHelper] Account exists: {user.Email}");
            else
                Console.WriteLine($"[ApiHelper] Account {user.Email}: {(int)res.StatusCode} {ReadBody(res)}");
        }
    }

    private static object BuildRegisterBody(TestUser user)
    {
        // Build registration payload based on role
        if (user.Role == "STUDENT")
            return new
            {
                name          = user.Name,
                email         = user.Email,
                password      = user.Password,
                role          = user.Role,
                student_major = "Computer Science",
                learning_goal = "Learn programming"
            };

        if (user.Role == "INSTRUCTOR")
            return new
            {
                name      = user.Name,
                email     = user.Email,
                password  = user.Password,
                role      = user.Role,
                expertise = "Software Engineering",
                education = "Master"
            };

        return new { name = user.Name, email = user.Email, password = user.Password, role = user.Role };
    }

    // ── Step 2: Authentication ────────────────────────────────────────────────

    private static void AuthenticateAll()
    {
        _adminToken      = Login(TestDataConfig.Accounts.Admin.Email,      TestDataConfig.Accounts.Admin.Password);
        _instructorToken = Login(TestDataConfig.Accounts.Instructor.Email, TestDataConfig.Accounts.Instructor.Password);
        _studentToken    = Login(TestDataConfig.Accounts.Student.Email,    TestDataConfig.Accounts.Student.Password);

        Console.WriteLine("[ApiHelper] All accounts authenticated.");
    }

    private static string Login(string email, string password)
    {
        var res = Post("/auth/login", new { email, password }, token: null);
        var json = ParseJson(res);
        // Try data.access_token
        if (json.TryGetProperty("data", out var data) && data.TryGetProperty("access_token", out var tok))
            return tok.GetString() ?? throw new Exception($"No token for {email}");
        throw new Exception($"Login failed for {email}: {ReadBody(res)}");
    }

    // ── Step 3: Courses ───────────────────────────────────────────────────────

    private static void EnsureCourses()
    {
        // Published course
        PublishedCourseId = EnsureCourse(TestDataConfig.Courses.Published, _instructorToken!);

        // Draft course
        DraftCourseId = EnsureCourse(TestDataConfig.Courses.Draft, _instructorToken!);

        // Full course (limited capacity)
        FullCourseId = EnsureCourse(TestDataConfig.Courses.Full, _instructorToken!);
    }

    private static int EnsureCourse(TestCourse course, string token)
    {
        // Try to create; if slug conflict, fetch existing
        var body = new
        {
            title               = course.Title,
            slug                = course.Slug,
            description         = course.Description,
            level               = course.Level,
            category            = course.Category,
            max_capacity        = course.MaxCapacity,
            price               = course.Price,
            status              = course.Status,
            certificate_enabled = course.CertEnabled
        };

        var res = Post("/courses", body, token);

        if (res.StatusCode == System.Net.HttpStatusCode.OK)
        {
            var id = ParseJson(res).GetProperty("data").GetProperty("id").GetInt32();
            Console.WriteLine($"[ApiHelper] Created course '{course.Title}' id={id}");
            return id;
        }

        if (res.StatusCode == System.Net.HttpStatusCode.Conflict)
        {
            // Slug exists - find by listing courses
            var id = FindCourseBySlug(course.Slug, token);
            Console.WriteLine($"[ApiHelper] Course exists '{course.Title}' id={id}");
            return id;
        }

        throw new Exception($"Failed to create course '{course.Title}': {(int)res.StatusCode} {ReadBody(res)}");
    }

    private static int FindCourseBySlug(string slug, string token)
    {
        // List all courses (including non-published)
        var res = Get("/courses?status=DRAFT", token);
        if (!res.IsSuccessStatusCode)
            res = Get("/courses", token);

        var json = ParseJson(res);
        if (json.TryGetProperty("data", out var data))
        {
            foreach (var course in data.EnumerateArray())
            {
                if (course.TryGetProperty("slug", out var s) && s.GetString() == slug)
                    return course.GetProperty("id").GetInt32();
            }
        }

        // Try admin endpoint
        res = Get("/courses", _adminToken);
        json = ParseJson(res);
        if (json.TryGetProperty("data", out data))
        {
            foreach (var course in data.EnumerateArray())
            {
                if (course.TryGetProperty("slug", out var s) && s.GetString() == slug)
                    return course.GetProperty("id").GetInt32();
            }
        }

        throw new Exception($"Course with slug '{slug}' not found");
    }

    // ── Step 4: Content (Sections + Lessons) ──────────────────────────────────

    private static void EnsureContent()
    {
        // Add sections to published course
        Section1Id = EnsureSection(PublishedCourseId, TestDataConfig.Content.Section1, _instructorToken!);
        Section2Id = EnsureSection(PublishedCourseId, TestDataConfig.Content.Section2, _instructorToken!);

        // Add lessons to section 1
        Lesson1Id = EnsureLesson(Section1Id, TestDataConfig.Content.Lesson1, _instructorToken!);
        Lesson2Id = EnsureLesson(Section1Id, TestDataConfig.Content.Lesson2, _instructorToken!);

        // Also add content to draft course
        var draftSection = EnsureSection(DraftCourseId, TestDataConfig.Content.Section1, _instructorToken!);
        EnsureLesson(draftSection, TestDataConfig.Content.Lesson1, _instructorToken!);
    }

    private static int EnsureSection(int courseId, TestSection section, string token)
    {
        // Check if section already exists in outline
        var outline = GetOutline(courseId);
        foreach (var item in outline)
        {
            if (item.TryGetProperty("section", out var s) &&
                s.TryGetProperty("title", out var t) &&
                t.GetString() == section.Title)
            {
                var existingId = s.GetProperty("id").GetInt32();
                Console.WriteLine($"[ApiHelper] Section exists '{section.Title}' id={existingId}");
                return existingId;
            }
        }

        var res = Post("/learning/sections", new
        {
            course_id = courseId,
            title     = section.Title,
            position  = section.Position
        }, token);

        if (res.IsSuccessStatusCode)
        {
            var id = ParseJson(res).GetProperty("data").GetProperty("id").GetInt32();
            Console.WriteLine($"[ApiHelper] Created section '{section.Title}' id={id}");
            return id;
        }

        throw new Exception($"Failed to create section '{section.Title}': {ReadBody(res)}");
    }

    private static int EnsureLesson(int sectionId, TestLesson lesson, string token)
    {
        // Check outline for existing lesson
        var res = Post("/learning/lessons", new
        {
            section_id       = sectionId,
            title            = lesson.Title,
            type             = lesson.Type,
            position         = lesson.Position,
            duration_minutes = lesson.DurationMinutes,
            is_preview       = lesson.IsPreview
        }, token);

        if (res.IsSuccessStatusCode)
        {
            var id = ParseJson(res).GetProperty("data").GetProperty("id").GetInt32();
            Console.WriteLine($"[ApiHelper] Created lesson '{lesson.Title}' id={id}");
            return id;
        }

        // Lesson may already exist - just warn
        Console.WriteLine($"[ApiHelper] Lesson '{lesson.Title}': {(int)res.StatusCode} (may already exist)");
        return 0;
    }

    private static JsonElement[] GetOutline(int courseId)
    {
        var res = Get($"/learning/courses/{courseId}/outline", _instructorToken);
        if (!res.IsSuccessStatusCode) return [];
        var json = ParseJson(res);
        if (json.TryGetProperty("data", out var data))
            return data.EnumerateArray().ToArray();
        return [];
    }

    // ── Step 5: Student Registration ──────────────────────────────────────────

    private static void EnsureStudentRegistration()
    {
        // Register student in published course
        var regRes = Post("/registrations", new { course_id = PublishedCourseId }, _studentToken!);

        if (regRes.StatusCode == System.Net.HttpStatusCode.OK)
        {
            var regId = ParseJson(regRes).GetProperty("data").GetProperty("id").GetInt32();
            Console.WriteLine($"[ApiHelper] Student registered in course {PublishedCourseId}, regId={regId}");

            // Approve the registration as admin
            var approveRes = Post($"/registrations/{regId}/approve", new { reason = "Auto-approved for testing" }, _adminToken!);
            if (approveRes.IsSuccessStatusCode)
                Console.WriteLine($"[ApiHelper] Registration {regId} approved (CONFIRMED)");
            else
                Console.WriteLine($"[ApiHelper] Approve failed: {ReadBody(approveRes)}");
        }
        else if (regRes.StatusCode == System.Net.HttpStatusCode.Conflict)
        {
            Console.WriteLine($"[ApiHelper] Student already registered in course {PublishedCourseId}");
            // Ensure it's confirmed - find and approve if pending
            EnsureRegistrationConfirmed(PublishedCourseId);
        }
        else
        {
            Console.WriteLine($"[ApiHelper] Registration: {(int)regRes.StatusCode} {ReadBody(regRes)}");
        }
    }

    private static void EnsureRegistrationConfirmed(int courseId)
    {
        var res = Get($"/registrations?course_id={courseId}", _adminToken);
        if (!res.IsSuccessStatusCode) return;

        var json = ParseJson(res);
        if (!json.TryGetProperty("data", out var data)) return;

        foreach (var reg in data.EnumerateArray())
        {
            if (!reg.TryGetProperty("user_id", out _)) continue;
            var status = reg.GetProperty("status").GetString();
            if (status == "PENDING")
            {
                var regId = reg.GetProperty("id").GetInt32();
                var approveRes = Post($"/registrations/{regId}/approve", new { reason = "Auto-approved for testing" }, _adminToken!);
                if (approveRes.IsSuccessStatusCode)
                    Console.WriteLine($"[ApiHelper] Approved pending registration {regId}");
            }
        }
    }

    // ── Step 6: Pending Registrations for Admin tests ─────────────────────────

    private static void EnsurePendingRegistrations()
    {
        // Create a second student account and register them (leave as PENDING)
        var pendingStudent = new
        {
            name          = "Pending Student",
            email         = "pending.student@test.com",
            password      = "Password123!",
            role          = "STUDENT",
            student_major = "IT",
            learning_goal = "Testing"
        };

        Post("/auth/register", pendingStudent, token: null); // ignore if exists

        var pendingToken = TryLogin("pending.student@test.com", "Password123!");
        if (pendingToken == null)
        {
            Console.WriteLine("[ApiHelper] Could not login pending student");
            return;
        }

        // Register in published course (leave as PENDING - don't approve)
        var regRes = Post("/registrations", new { course_id = PublishedCourseId }, pendingToken);
        if (regRes.StatusCode == System.Net.HttpStatusCode.OK)
            Console.WriteLine("[ApiHelper] Created pending registration for admin approval tests");
        else
            Console.WriteLine($"[ApiHelper] Pending registration: {(int)regRes.StatusCode} (may already exist)");
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private static HttpResponseMessage Post(string path, object body, string? token)
    {
        var req = new HttpRequestMessage(HttpMethod.Post, $"{Base}{path}");
        req.Content = new StringContent(JsonSerializer.Serialize(body), Encoding.UTF8, "application/json");
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return Http.Send(req);
    }

    private static HttpResponseMessage Get(string path, string? token)
    {
        var req = new HttpRequestMessage(HttpMethod.Get, $"{Base}{path}");
        if (token != null)
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return Http.Send(req);
    }

    private static JsonElement ParseJson(HttpResponseMessage res)
    {
        var body = res.Content.ReadAsStringAsync().Result;
        return JsonSerializer.Deserialize<JsonElement>(body);
    }

    private static string ReadBody(HttpResponseMessage res) =>
        res.Content.ReadAsStringAsync().Result;

    private static string? TryLogin(string email, string password)
    {
        try
        {
            return Login(email, password);
        }
        catch
        {
            return null;
        }
    }

    // ── Public helpers for use in test models ─────────────────────────────────

    /// <summary>Get access token for a role</summary>
    public static string GetToken(string role) => role.ToUpper() switch
    {
        "ADMIN"      => AdminToken,
        "INSTRUCTOR" => InstructorToken,
        "STUDENT"    => StudentToken,
        _            => throw new ArgumentException($"Unknown role: {role}")
    };

    /// <summary>Approve all pending registrations for a course (for admin tests)</summary>
    public static void ApproveAllPending(int courseId)
    {
        Post($"/registrations/bulk-approve", new { course_id = courseId, reason = "Test bulk approve" }, _adminToken!);
    }

    /// <summary>Check if backend is reachable</summary>
    public static bool IsBackendReachable()
    {
        try
        {
            var res = Http.GetAsync($"{Base}/auth/me").Result;
            return true; // any response means backend is up
        }
        catch
        {
            return false;
        }
    }
}
