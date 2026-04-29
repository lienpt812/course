package com.eduplatform.mbt.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin HTTP client against the FastAPI backend.
 *
 * Used for:
 *   - Seeding demo / test data (admin/seed*)
 *   - Creating/deleting test accounts (/auth/test/create-account-with-role)
 *   - Direct API probing to cross-check DB state matches what the UI shows
 *
 * All methods return the raw response body parsed as JsonNode.
 * Callers use MbtBusinessAssertions to validate the envelope structure.
 *
 * <p>Resilience: all HTTP verbs retry on HTTP 429 with exponential backoff; optional short-TTL
 * read cache and minimum interval for hot GET paths (see {@code mbt.api.getCacheTtlMs},
 * {@code mbt.api.minIntervalMs} — default 4000 and 50 ms if unset).
 *
 * NOTE: InstructorContent uses PATCH for course updates.
 * The backend PATCH /courses/{id} handler is in courses.py and accepts partial updates.
 */
public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String baseUrl;
    private final HttpClient http;
    private String accessToken;
    /** Set on successful {@link #login}; used by Selenium to populate {@code localStorage.refresh_token}. */
    private String lastRefreshToken;
    /** Last HTTP status from {@link #send} (0 if no response / parse error). */
    private int lastHttpStatus;

    private final ConcurrentHashMap<String, CacheEntry> readCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastGetThrottleAt = new ConcurrentHashMap<>();

    private static final class CacheEntry {
        final JsonNode node;
        final long untilMs;

        CacheEntry(JsonNode node, long untilMs) {
            this.node = node;
            this.untilMs = untilMs;
        }
    }

    public ApiClient() {
        this(TestConfig.apiBaseUrl());
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String accessToken() { return accessToken; }

    /** Refresh token from the last successful login, or null. */
    public String lastRefreshToken() { return lastRefreshToken; }

    public int lastHttpStatus() {
        return lastHttpStatus;
    }

    public boolean isLastResponseRateLimited() {
        return lastHttpStatus == 429;
    }

    /** Clears GET caches for {@link #listCourses} / {@link #courseDetail} (e.g. after admin seed or mutations). */
    public void clearReadCache() {
        readCache.clear();
    }

    public ApiClient withToken(String token) {
        this.accessToken = token;
        return this;
    }

    // ==================== Auth ====================

    /** POST /auth/login → returns access_token (or null on failure). */
    public String login(String email, String password) {
        this.lastRefreshToken = null;
        JsonNode body = post("/api/v1/auth/login",
                Map.of("email", email, "password", password), false);
        if (body == null || body.path("data").path("access_token").isMissingNode()) return null;
        this.accessToken = body.path("data").path("access_token").asText();
        JsonNode ref = body.path("data").path("refresh_token");
        if (!ref.isMissingNode()) {
            String r = ref.asText("");
            if (!r.isBlank()) {
                this.lastRefreshToken = r;
            }
        }
        return accessToken;
    }

    /** POST /auth/test/create-account-with-role — test-only endpoint */
    public JsonNode createTestAccount(String name, String email, String password, String role) {
        String qs = "?name=" + enc(name) + "&email=" + enc(email)
                + "&password=" + enc(password) + "&role=" + enc(role);
        return post("/api/v1/auth/test/create-account-with-role" + qs, Map.of(), false);
    }

    /** DELETE /auth/test/delete-user-by-email */
    public JsonNode deleteUserByEmail(String email) {
        return send("DELETE", "/api/v1/auth/test/delete-user-by-email?email=" + enc(email), null, false);
    }

    /** GET /auth/me */
    public JsonNode me() { return get("/api/v1/auth/me", true); }

    // ==================== Admin / Seed ====================

    /** POST /admin/seed — full seed */
    public JsonNode seed() { return post("/api/v1/admin/seed", Map.of(), true); }

    /** POST /admin/seed/mbt-pending — lightweight seed for MBT pending registration tests */
    public JsonNode seedMbtPending() { return post("/api/v1/admin/seed/mbt-pending", Map.of(), true); }

    /** POST /admin/seed/courses */
    public JsonNode seedCourses() { return post("/api/v1/admin/seed/courses", Map.of(), true); }

    /** POST /admin/seed/class-full-pending — optional MBT scenario (test-web01 seeder recovery). */
    public JsonNode seedClassFullPending() {
        return post("/api/v1/admin/seed/class-full-pending", Map.of(), true);
    }

    /** POST /admin/jobs/expire-pending */
    public JsonNode expirePending() { return post("/api/v1/admin/jobs/expire-pending", Map.of(), true); }

    // ==================== Courses ====================

    /**
     * GET /courses?status=PUBLISHED — returns published courses list.
     */
    public JsonNode listCourses() {
        return listCourses("PUBLISHED");
    }

    /**
     * GET /courses?status={status}
     * @param status "PUBLISHED" | "DRAFT" | "COMING_SOON" | "ARCHIVED" | null (all)
     */
    public JsonNode listCourses(String status) {
        String path = "/api/v1/courses";
        if (status != null && !status.isBlank()) {
            path += "?status=" + enc(status.trim());
        }
        String cacheKey = "list:" + (status == null ? "" : status.trim());
        long ttlMs = readCacheTtlMs();
        if (ttlMs > 0) {
            CacheEntry cached = readCache.get(cacheKey);
            if (cached != null && cached.untilMs > System.currentTimeMillis()) {
                return cached.node;
            }
        }
        throttleGet(cacheKey);
        JsonNode n = get(path, false);
        if (ttlMs > 0 && okToCacheGetResponse(n)) {
            readCache.put(cacheKey, new CacheEntry(n, System.currentTimeMillis() + ttlMs));
        }
        return n;
    }

    /**
     * Resolve course ID by slug — iterates across all statuses for reliability.
     */
    public long courseIdBySlug(String slug) {
        if (slug == null || slug.isBlank()) return 0;
        for (String st : new String[]{"PUBLISHED", "DRAFT", "COMING_SOON", "ARCHIVED"}) {
            JsonNode list = listCourses(st);
            long id = findCourseIdInList(list, slug);
            if (id > 0) return id;
        }
        return 0;
    }

    private static long findCourseIdInList(JsonNode list, String slug) {
        if (list == null || !list.path("data").isArray()) return 0;
        for (JsonNode c : list.path("data")) {
            if (slug.equals(c.path("slug").asText())) return c.path("id").asLong();
        }
        return 0;
    }

    /** GET /courses/{id} (cached briefly when {@code mbt.api.getCacheTtlMs} &gt; 0). */
    public JsonNode courseDetail(long id) {
        if (id <= 0) {
            return null;
        }
        String path = "/api/v1/courses/" + id;
        String cacheKey = "cd:" + id;
        long ttlMs = readCacheTtlMs();
        if (ttlMs > 0) {
            CacheEntry cached = readCache.get(cacheKey);
            if (cached != null && cached.untilMs > System.currentTimeMillis()) {
                return cached.node;
            }
        }
        throttleGet(cacheKey);
        JsonNode n = get(path, false);
        if (ttlMs > 0 && okToCacheGetResponse(n)) {
            readCache.put(cacheKey, new CacheEntry(n, System.currentTimeMillis() + ttlMs));
        }
        return n;
    }

    /**
     * PATCH /courses/{id} — update course fields (owner or admin only).
     * Backend: courses.py @router.patch("/{course_id}")
     */
    public JsonNode patchCourse(long courseId, Map<String, Object> fields) {
        return send("PATCH", "/api/v1/courses/" + courseId, serializeBody(fields), true);
    }

    // ==================== Registrations ====================

    /** GET /registrations — student gets own; admin/instructor see all */
    public JsonNode listMyRegistrations() { return get("/api/v1/registrations", true); }

    /** GET /registrations — admin/instructor full view */
    public JsonNode listRegistrations() { return get("/api/v1/registrations", true); }

    /**
     * GET /registrations?status={status}
     */
    public JsonNode listRegistrationsWithStatusQuery(String status) {
        String path = "/api/v1/registrations";
        if (status != null && !status.isBlank()) {
            path += "?status=" + enc(status);
        }
        return get(path, true);
    }

    /** POST /registrations { course_id } */
    public JsonNode createRegistration(long courseId) {
        return post("/api/v1/registrations", Map.of("course_id", courseId), true);
    }

    /** POST /registrations/{id}/approve { reason } */
    public JsonNode approve(long registrationId) {
        return post("/api/v1/registrations/" + registrationId + "/approve",
                Map.of("reason", "Approved by MBT"), true);
    }

    /** POST /registrations/{id}/reject { reason } */
    public JsonNode reject(long registrationId, String reason) {
        return post("/api/v1/registrations/" + registrationId + "/reject",
                Map.of("reason", reason), true);
    }

    /** POST /registrations/bulk-approve { reason, course_id? } */
    public JsonNode bulkApprove(Long courseId) {
        var body = new java.util.HashMap<String, Object>();
        body.put("reason", "Bulk approved by MBT");
        if (courseId != null) body.put("course_id", courseId);
        return post("/api/v1/registrations/bulk-approve", body, true);
    }

    /** POST /registrations/{id}/cancel { reason } */
    public JsonNode cancel(long registrationId, String reason) {
        return post("/api/v1/registrations/" + registrationId + "/cancel",
                Map.of("reason", reason), true);
    }

    // ==================== Certificates ====================

    /** POST /certificates/issue/{courseId} */
    public JsonNode issueCertificate(long courseId) {
        return post("/api/v1/certificates/issue/" + courseId, Map.of(), true);
    }

    /** GET /certificates/verify/{code} — public endpoint */
    public JsonNode verifyCertificate(String code) {
        return get("/api/v1/certificates/verify/" + code, false);
    }

    // ==================== Low-level ====================

    public JsonNode get(String path, boolean auth) {
        return sendWith429Retry("GET", path, null, auth);
    }

    public JsonNode post(String path, Object body, boolean auth) {
        return send("POST", path, serializeBody(body), auth);
    }

    public JsonNode patch(String path, Object body, boolean auth) {
        return send("PATCH", path, serializeBody(body), auth);
    }

    /**
     * General-purpose method for any HTTP verb (GET, POST, PATCH, DELETE), with 429 retry.
     * Does not throw on rate limit after retries; returns last body (or null) and {@link #lastHttpStatus}.
     */
    public JsonNode send(String method, String path, String body, boolean auth) {
        return sendWith429Retry(method, path, body, auth);
    }

    private JsonNode sendWith429Retry(String method, String path, String body, boolean auth) {
        JsonNode n = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            n = sendOnce(method, path, body, auth);
            if (lastHttpStatus != 429) {
                return n;
            }
            log.warn("API {} {}{} -> HTTP 429 (rate limited), retry {}/4 — not failing test here",
                    method, baseUrl, path, attempt + 1);
            if (attempt < 4) {
                long backoff = 300L * (1L << attempt);
                try {
                    Thread.sleep(Math.min(backoff, 10_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return n;
                }
            }
        }
        return n;
    }

    private JsonNode sendOnce(String method, String path, String body, boolean auth) {
        lastHttpStatus = 0;
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
            if (auth && accessToken != null) {
                req.header("Authorization", "Bearer " + accessToken);
            }
            HttpRequest.BodyPublisher bp = (body == null)
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
            req.method(method, bp);

            HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            lastHttpStatus = resp.statusCode();
            log.debug("[{} {}{}] -> {}", method, baseUrl, path, resp.statusCode());
            if (resp.body() == null || resp.body().isBlank()) {
                return null;
            }
            JsonNode n = JSON.readTree(resp.body());
            if (log.isTraceEnabled()) {
                String snippet = n.toString();
                if (snippet.length() > 800) {
                    snippet = snippet.substring(0, 800) + "…";
                }
                log.trace("API response body: {}", snippet);
            }
            return n;
        } catch (Exception ex) {
            log.warn("API call {} {} failed: {}", method, path, ex.toString());
            return null;
        }
    }

    private String serializeBody(Object body) {
        if (body == null) return null;
        try {
            return JSON.writeValueAsString(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize request body", ex);
        }
    }

    private static String enc(String v) {
        return java.net.URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static long readCacheTtlMs() {
        return Long.parseLong(System.getProperty("mbt.api.getCacheTtlMs", "4000"));
    }

    private static long minGetIntervalMs() {
        return Long.parseLong(System.getProperty("mbt.api.minIntervalMs", "50"));
    }

    private void throttleGet(String opKey) {
        long min = minGetIntervalMs();
        if (min <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Long prev = lastGetThrottleAt.put(opKey, now);
        if (prev != null) {
            long elapsed = now - prev;
            if (elapsed < min) {
                long nap = min - elapsed;
                try {
                    Thread.sleep(Math.min(nap, 10_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private boolean okToCacheGetResponse(JsonNode n) {
        if (n == null) {
            return false;
        }
        if (lastHttpStatus == 429) {
            return false;
        }
        if (lastHttpStatus < 200 || lastHttpStatus >= 300) {
            return false;
        }
        return !MbtBusinessAssertions.isEnvelopeApiRateLimited(n);
    }

}