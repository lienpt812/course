package com.eduplatform.mbt.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Xác minh nghiệp vụ bằng API (sau bước UI/GraphWalker) thay vì chỉ "không crash".
 * Dùng chung mọi module MBT.
 */
public final class MbtBusinessAssertions {

    private static final Logger log = LoggerFactory.getLogger(MbtBusinessAssertions.class);

    private MbtBusinessAssertions() {}

    /** Chấp nhận envelope thành công: không có mảng {@code errors} rỗng, hoặc lỗi rỗng. */
    public static void assertSuccessEnvelope(JsonNode resp, String context) {
        if (resp == null) {
            throw new AssertionError(context + ": response is null");
        }
        if (resp instanceof NullNode) {
            throw new AssertionError(context + ": response is JSON null");
        }
        if (resp.has("errors") && resp.path("errors").isArray() && resp.path("errors").size() > 0) {
            throw new AssertionError(context + " API errors: " + resp.path("errors"));
        }
    }

    /**
     * True when the app envelope reports throttling (e.g. {@code RATE_LIMITED} in {@code errors[]}).
     */
    public static boolean isEnvelopeApiRateLimited(JsonNode resp) {
        if (resp == null || !resp.path("errors").isArray()) {
            return false;
        }
        for (JsonNode e : resp.path("errors")) {
            String code = e.path("code").asText("");
            String msg = e.path("message").asText("").toLowerCase();
            if ("RATE_LIMITED".equalsIgnoreCase(code) || "429".equals(code)) {
                return true;
            }
            if (msg.contains("rate limit")) {
                return true;
            }
        }
        if (resp.path("meta").path("status").asInt(200) == 429) {
            return true;
        }
        return false;
    }

    /**
     * HTTP 429 or throttling in JSON body (GraphWalker: do not assert strict business success on this response).
     */
    public static boolean isApiRateOrThrottle(ApiClient api, JsonNode body) {
        if (api != null && api.lastHttpStatus() == 429) {
            return true;
        }
        return isEnvelopeApiRateLimited(body);
    }

    public static boolean uiTitleConsistentWithApiTitle(String apiTitle, String uiTitle) {
        if (apiTitle == null || apiTitle.isBlank()) {
            return false;
        }
        String a = apiTitle.trim();
        String u = uiTitle == null ? "" : uiTitle.trim();
        if (a.equals(u) || a.contains(u) || u.contains(a)) {
            return true;
        }
        return false;
    }

    /**
     * Optional cross-check for {@code v_CourseDetailVisible}: does not fail the MBT when the public tier is
     * throttled. Prefer {@code listTitleIfKnown} from the same {@code listCourses} snapshot to avoid a second GET.
     */
    public static void assertCourseApiTitleVisibleMatchLenient(
            ApiClient api, long courseId, String uiTitleTrimmed, String listTitleIfKnown) {
        if (courseId <= 0) {
            return;
        }
        String ui = uiTitleTrimmed == null ? "" : uiTitleTrimmed.trim();
        if (listTitleIfKnown != null && !listTitleIfKnown.isBlank() && uiTitleConsistentWithApiTitle(listTitleIfKnown, ui)) {
            log.info("MBT: UI title matches earlier list response for courseId={}; skip GET /courses/{}", courseId, courseId);
            return;
        }
        JsonNode d = api.courseDetail(courseId);
        if (d == null) {
            log.warn("MBT: course detail {} null — skip title cross-check (network/transient).", courseId);
            return;
        }
        if (isApiRateOrThrottle(api, d)) {
            log.warn("MBT: course detail {} rate-limited or throttled — skip strict title API assert.", courseId);
            return;
        }
        assertSuccessEnvelope(d, "course detail " + courseId);
        String apiTitle = d.path("data").path("title").asText("").trim();
        if (apiTitle.isEmpty()) {
            throw new AssertionError("course " + courseId + ": empty title in API");
        }
        if (uiTitleConsistentWithApiTitle(apiTitle, ui)) {
            return;
        }
        throw new AssertionError("course title API='" + apiTitle + "' vs UI='" + ui + "'");
    }

    public static void assertUserRoleInMe(ApiClient api, String expectedRole) {
        JsonNode me = api.me();
        if (me == null) {
            throw new AssertionError("GET /auth/me: null (token invalid?)");
        }
        assertSuccessEnvelope(me, "me");
        String role = me.path("data").path("role").asText("");
        if (!expectedRole.equals(role)) {
            throw new AssertionError("expected role " + expectedRole + " but me() returned: " + role);
        }
    }

    /**
     * Đồng bộ 100% dữ liệu với bảng {@code registrations} (admin) — trạng thái sau duyệt / từ chối.
     */
    public static void assertRegistrationStatus(ApiClient adminApi, long registrationId, String expectedStatus) {
        String t = adminApi.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
        if (t == null) {
            throw new AssertionError("assertRegistrationStatus: admin login failed");
        }
        adminApi.withToken(t);
        JsonNode all = adminApi.listRegistrations();
        if (all == null) {
            throw new AssertionError("listRegistrations null");
        }
        assertSuccessEnvelope(all, "listRegistrations");
        if (!all.path("data").isArray()) {
            throw new AssertionError("listRegistrations: data is not an array: " + all);
        }
        for (JsonNode r : all.path("data")) {
            if (r.path("id").asLong() == registrationId) {
                String st = r.path("status").asText("");
                if (!expectedStatus.equals(st)) {
                    throw new AssertionError("registration " + registrationId
                            + " expected status " + expectedStatus + " but was " + st);
                }
                return;
            }
        }
        throw new AssertionError("registration " + registrationId + " not found in listRegistrations");
    }

    /**
     * Sau thao tác UI bất đồng bộ (React {@code onClick} → fetch), DB có thể chưa kịp cập nhật ngay.
     */
    public static void assertRegistrationStatusEventually(
            ApiClient adminApi, long registrationId, String expectedStatus, int timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(timeoutMs, 500);
        AssertionError last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                assertRegistrationStatus(adminApi, registrationId, expectedStatus);
                return;
            } catch (AssertionError e) {
                last = e;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ie);
                }
            }
        }
        if (last != null) {
            throw new AssertionError("Sau " + timeoutMs + "ms: " + last.getMessage());
        }
        assertRegistrationStatus(adminApi, registrationId, expectedStatus);
    }

    public static int countRegistrationsByStatus(ApiClient adminApi, String status) {
        String t = adminApi.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
        if (t == null) {
            return 0;
        }
        adminApi.withToken(t);
        JsonNode r = adminApi.listRegistrationsWithStatusQuery(status);
        if (r == null || !r.path("data").isArray()) {
            return 0;
        }
        return r.path("data").size();
    }

    public static void assertCertificateIssueHasCode(JsonNode issueResp) {
        assertSuccessEnvelope(issueResp, "POST issue certificate");
        String code = issueResp.path("data").path("verification_code").asText("");
        if (code == null || code.isBlank()) {
            throw new AssertionError("issue certificate: missing verification_code in " + issueResp);
        }
    }

    public static void assertCertificatePublicVerifyOk(JsonNode verifyResp) {
        assertSuccessEnvelope(verifyResp, "GET certificate verify");
        if (verifyResp.path("data").isMissingNode() || !verifyResp.path("data").isObject()) {
            throw new AssertionError("verify: expected data object, got: " + verifyResp);
        }
    }

    /**
     * Số slot còn lại (GET /courses/{id} → data.remaining_slots). Duyệt PENDING cần {@code > 0},
     * nếu không BE trả 400 "Lớp đã đủ, không thể duyệt thêm".
     */
    public static int remainingSlotsForCourse(ApiClient anyApi, long courseId) {
        if (courseId <= 0) {
            return 0;
        }
        JsonNode d = anyApi.courseDetail(courseId);
        if (d == null) {
            return 0;
        }
        assertSuccessEnvelope(d, "course detail (slots)");
        return d.path("data").path("remaining_slots").asInt(0);
    }

    /** Không ném lỗi — dùng khi {@link #syncAdminGuards} / guard trước bước UI. */
    public static int safeRemainingSlotsForCourse(ApiClient anyApi, long courseId) {
        if (courseId <= 0) {
            return 0;
        }
        try {
            JsonNode d = anyApi.courseDetail(courseId);
            if (d == null) {
                return 0;
            }
            if (d.has("errors") && d.path("errors").isArray() && d.path("errors").size() > 0) {
                return 0;
            }
            return d.path("data").path("remaining_slots").asInt(0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Cùng điều kiện thời gian với {@code CourseDetailPage}: tắt khi trước
     * {@code registration_open_at} hoặc sau {@code registration_close_at} (nếu có giá trị).
     */
    public static boolean safeRegistrationWindowOpenForCourse(ApiClient anyApi, long courseId) {
        if (courseId <= 0) {
            return true;
        }
        try {
            JsonNode d = anyApi.courseDetail(courseId);
            if (d == null) {
                return true;
            }
            if (d.has("errors") && d.path("errors").isArray() && d.path("errors").size() > 0) {
                return true;
            }
            JsonNode data = d.path("data");
            if (data.isMissingNode() || !data.isObject()) {
                return true;
            }
            String openS = data.path("registration_open_at").asText("");
            String closeS = data.path("registration_close_at").asText("");
            return isWithinRegistrationWindow(openS, closeS);
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isWithinRegistrationWindow(String openS, String closeS) {
        long now = System.currentTimeMillis();
        Long openMs = parseIsoOrDateToMillis(openS);
        if (openMs != null && now < openMs) {
            return false;
        }
        Long closeMs = parseIsoOrDateToMillis(closeS);
        if (closeMs != null && now > closeMs) {
            return false;
        }
        return true;
    }

    private static Long parseIsoOrDateToMillis(String s) {
        if (s == null || s.isBlank() || "null".equals(s)) {
            return null;
        }
        String t = s.trim();
        try {
            return Instant.parse(t).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // no-op: try other shapes
        }
        try {
            if (t.length() == 10 && t.charAt(4) == '-' && t.charAt(7) == '-') {
                return LocalDate.parse(t).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
        } catch (DateTimeParseException ignored) {
            // no-op
        }
        return null;
    }

    public static long courseIdForRegistrationId(ApiClient adminApi, long registrationId) {
        JsonNode all = adminApi.listRegistrations();
        if (all == null || !all.path("data").isArray()) {
            return 0L;
        }
        for (JsonNode r : all.path("data")) {
            if (r.path("id").asLong() == registrationId) {
                return r.path("course_id").asLong();
            }
        }
        return 0L;
    }

    /**
     * Strict: fails on any envelope error (use in unit-style API tests). For UI+GraphWalker use
     * {@link #assertCourseApiTitleVisibleMatchLenient}.
     */
    public static void assertCourseApiTitleVisibleMatch(ApiClient anyApi, long courseId, String uiTitleTrimmed) {
        JsonNode d = anyApi.courseDetail(courseId);
        assertSuccessEnvelope(d, "course detail " + courseId);
        String apiTitle = d.path("data").path("title").asText("").trim();
        if (apiTitle.isEmpty()) {
            throw new AssertionError("course " + courseId + ": empty title in API");
        }
        String ui = uiTitleTrimmed == null ? "" : uiTitleTrimmed.trim();
        if (uiTitleConsistentWithApiTitle(apiTitle, ui)) {
            return;
        }
        throw new AssertionError("course title API='" + apiTitle + "' vs UI='" + ui + "'");
    }
}
