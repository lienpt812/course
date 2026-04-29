package com.eduplatform.mbt.support;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tạo đơn PENDING bằng API công khai (học viên POST /registrations) khi
 * {@code GET /registrations?status=PENDING} rỗng dù admin seed không trả mbt_pending (API tối thiểu / mbt-pending 404),
 * hoặc sau khi bước trước đã Duyệt hết mẫu.
 */
public final class PendingRegistrationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PendingRegistrationHelper.class);

    /** Thứ tự ưu tiên: hai học viên mẫu, rồi học viên MBT chính (tránh spam toàn catalog → 429). */
    private static final String[] PENDING_STUDENT_EMAILS = {
            TestData.MBT_PENDING_STUDENT_EMAIL,
            TestData.MBT_PENDING_STUDENT_2_EMAIL,
            TestData.STUDENT_EMAIL,
    };

    /** Slug seed ổn định — thử trước khi quét list (ít request hơn). */
    private static final String[] PREFERRED_PUBLISHED_SLUGS = {
            TestData.DEMO_COURSE_03_SLUG,
            TestData.DEMO_COURSE_01_SLUG,
            TestData.MBT_STUDENT_ANCHOR_SLUG,
    };

    /** Giới hạn số khóa thử mỗi tài khoản (tránh N+1 detail × mọi course → rate limit). */
    private static final int MAX_LIST_COURSE_ATTEMPTS = 12;

    private static final int RATE_LIMIT_BACKOFF_MS = 2600;

    private PendingRegistrationHelper() {}

    /**
     * Thử từng tài khoản học viên; mỗi tài khoản thử slug ưu tiên rồi một phần catalog PUBLISHED.
     *
     * @return true nếu tạo được bản ghi mới (thường PENDING)
     */
    public static boolean tryCreateOnePendingViaStudent() {
        for (String email : PENDING_STUDENT_EMAILS) {
            if (tryCreateForSingleStudent(email, LOG)) {
                return true;
            }
            pause(400);
        }
        return false;
    }

    /**
     * When self-registration could not create PENDING (full classes, or every slot tried but duplicate/409),
     * admin bumps {@code max_capacity} on each published course (if full) and we retry one course at a time
     * with each test student. Does not return early on the first course with free slots (that case may still
     * be "already registered" for all students).
     */
    public static boolean tryEnsurePendingByAdminBumpingCapacity() {
        ApiClient admin = new ApiClient();
        if (admin.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG) == null) {
            LOG.warn("admin login failed; cannot bump course capacity");
            return false;
        }
        JsonNode list = admin.listCourses("PUBLISHED");
        if (list == null || !list.path("data").isArray() || list.path("data").isEmpty()) {
            return false;
        }
        int courseIdx = 0;
        for (JsonNode c : list.path("data")) {
            if (courseIdx++ >= 16) {
                break;
            }
            long courseId = c.path("id").asLong();
            if (courseId <= 0) {
                continue;
            }
            JsonNode det = admin.courseDetail(courseId);
            if (det == null || !det.path("data").isObject()) {
                continue;
            }
            int remain = det.path("data").path("remaining_slots").asInt(0);
            if (remain <= 0) {
                int maxCap = det.path("data").path("max_capacity").asInt(1);
                if (maxCap < 1) {
                    maxCap = 1;
                }
                Map<String, Object> patch = new HashMap<>();
                patch.put("max_capacity", maxCap + 50);
                admin.patchCourse(courseId, patch);
                if (admin.lastHttpStatus() != 200) {
                    LOG.debug("bump max_capacity failed for courseId={} (lastStatus={})", courseId, admin.lastHttpStatus());
                    continue;
                }
                LOG.info("Bumped max_capacity for courseId={} (was {}) to free slots for PENDING", courseId, maxCap);
                det = admin.courseDetail(courseId);
                if (det == null || !det.path("data").isObject()) {
                    continue;
                }
                remain = det.path("data").path("remaining_slots").asInt(0);
            }
            if (remain <= 0) {
                continue;
            }
            for (String email : PENDING_STUDENT_EMAILS) {
                pause(200);
                if (tryRegisterSingleCourseAsStudent(email, courseId, LOG)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Học viên dùng một lần (email UUID) — khi mọi tài khoản MBT cố định đã 409 trên mọi khóa còn slot.
     * Admin tăng max_capacity các khóa đầy trước khi đăng ký.
     */
    public static boolean tryCreatePendingWithEphemeralStudent() {
        ApiClient admin = new ApiClient();
        if (admin.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG) == null) {
            LOG.warn("ephemeral pending: admin login failed");
            return false;
        }
        JsonNode list = admin.listCourses("PUBLISHED");
        if (list == null || !list.path("data").isArray() || list.path("data").isEmpty()) {
            return false;
        }
        int i = 0;
        for (JsonNode c : list.path("data")) {
            if (i++ >= 40) {
                break;
            }
            long courseId = c.path("id").asLong();
            if (courseId <= 0) {
                continue;
            }
            JsonNode det = admin.courseDetail(courseId);
            if (det == null || !det.path("data").isObject()) {
                continue;
            }
            if (det.path("data").path("remaining_slots").asInt(0) > 0) {
                continue;
            }
            int maxCap = det.path("data").path("max_capacity").asInt(1);
            if (maxCap < 1) {
                maxCap = 1;
            }
            admin.patchCourse(courseId, Map.of("max_capacity", maxCap + 100));
            if (admin.lastHttpStatus() != 200) {
                LOG.debug("ephemeral: bump courseId={} lastStatus={}", courseId, admin.lastHttpStatus());
            }
        }

        String email = "mbt.ephemeral." + UUID.randomUUID().toString().replace("-", "") + "@example.com";
        ApiClient once = new ApiClient();
        JsonNode acc = once.createTestAccount("MBT Ephemeral Pending", email, TestData.PWD_STRONG, "STUDENT");
        if (acc != null && acc.path("errors").isArray() && acc.path("errors").size() > 0) {
            LOG.warn("ephemeral: createTestAccount {}", acc.path("errors"));
        }
        return tryCreateForSingleStudent(email, LOG);
    }

    private static boolean tryRegisterSingleCourseAsStudent(String email, long courseId, Logger log) {
        ApiClient a = new ApiClient();
        if (a.login(email, TestData.PWD_STRONG) == null) {
            log.debug("Login failed for {} (bump+register).", email);
            return false;
        }
        return tryRegisterCourse(a, courseId, log);
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean tryCreateForSingleStudent(String email, Logger log) {
        ApiClient a = new ApiClient();
        if (a.login(email, TestData.PWD_STRONG) == null) {
            log.debug("Login failed for {} (ensure account in TestDataSeeder).", email);
            return false;
        }
        JsonNode list = a.listCourses("PUBLISHED");
        if (list == null || isRateLimited(list)) {
            log.warn("listCourses(PUBLISHED) empty or rate limited — backoff");
            pause(RATE_LIMIT_BACKOFF_MS);
            list = a.listCourses("PUBLISHED");
        }
        if (list == null || isRateLimited(list) || !list.path("data").isArray() || list.path("data").isEmpty()) {
            log.warn("No PUBLISHED courses list; cannot create PENDING");
            return false;
        }

        Set<Long> tried = new HashSet<>();

        for (String slug : PREFERRED_PUBLISHED_SLUGS) {
            long courseId = courseIdFromList(list, slug);
            if (courseId <= 0) {
                continue;
            }
            if (!tried.add(courseId)) {
                continue;
            }
            if (tryRegisterCourse(a, courseId, log)) {
                return true;
            }
        }

        int attempts = 0;
        for (JsonNode c : list.path("data")) {
            if (attempts >= MAX_LIST_COURSE_ATTEMPTS) {
                break;
            }
            long courseId = c.path("id").asLong();
            if (courseId <= 0 || !tried.add(courseId)) {
                continue;
            }
            attempts++;
            if (tryRegisterCourse(a, courseId, log)) {
                return true;
            }
        }
        return false;
    }

    private static long courseIdFromList(JsonNode list, String slug) {
        if (slug == null || slug.isBlank() || list == null || !list.path("data").isArray()) {
            return 0;
        }
        for (JsonNode c : list.path("data")) {
            if (slug.equals(c.path("slug").asText())) {
                return c.path("id").asLong();
            }
        }
        return 0;
    }

    private static int envelopeHttpStatus(JsonNode n) {
        if (n == null) {
            return 0;
        }
        return n.path("meta").path("status").asInt(200);
    }

    private static boolean isRateLimited(JsonNode n) {
        return envelopeHttpStatus(n) == 429;
    }

    /**
     * GET detail (remaining_slots) + POST registration; xử lý 429 bằng backoff một lần.
     */
    private static boolean tryRegisterCourse(ApiClient a, long courseId, Logger log) {
        JsonNode det = a.courseDetail(courseId);
        if (isRateLimited(det)) {
            log.warn("courseDetail({}) rate limited — backoff", courseId);
            pause(RATE_LIMIT_BACKOFF_MS);
            det = a.courseDetail(courseId);
        }
        if (det == null) {
            return false;
        }
        if (isRateLimited(det)) {
            log.warn("courseDetail({}) still 429 after backoff — stop this student pass", courseId);
            return false;
        }
        if (!det.path("data").isObject()) {
            return false;
        }
        int remain = det.path("data").path("remaining_slots").asInt(0);
        if (remain <= 0) {
            log.debug("skip courseId={} (remaining_slots=0)", courseId);
            return false;
        }

        JsonNode reg = a.createRegistration(courseId);
        if (isRateLimited(reg)) {
            log.warn("createRegistration({}) rate limited — backoff once", courseId);
            pause(RATE_LIMIT_BACKOFF_MS);
            reg = a.createRegistration(courseId);
        }
        if (reg != null && reg.path("data").path("id").isNumber()) {
            String st = reg.path("data").path("status").asText("?");
            log.info(
                    "Created registration id={} status={} for courseId={}",
                    reg.path("data").path("id").asText(),
                    st,
                    courseId);
            return true;
        }
        if (reg != null && reg.path("errors").isArray() && reg.path("errors").size() > 0) {
            log.debug("createRegistration course {}: {}", courseId, reg);
        }
        return false;
    }

    public static void loginAdmin(ApiClient api) {
        String t = api.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
        if (t != null) {
            api.withToken(t);
        }
    }
}
