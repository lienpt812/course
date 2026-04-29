package com.eduplatform.mbt.support;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures the 3 canonical test accounts + demo courses exist in the backend DB
 * before a run. Safe to call multiple times (idempotent backend endpoints).
 */
public final class TestDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TestDataSeeder.class);
    private static volatile boolean seeded = false;

    private TestDataSeeder() {}

    public static synchronized void seedOnce() {
        if (seeded) return;
        ApiClient api = new ApiClient();

        ensureAccount(api, TestData.ADMIN_EMAIL,      TestData.ADMIN_NAME,      "ADMIN");
        ensureAccount(api, TestData.INSTRUCTOR_EMAIL, TestData.INSTRUCTOR_NAME, "INSTRUCTOR");
        ensureAccount(api, TestData.STUDENT_EMAIL,    TestData.STUDENT_NAME,    "STUDENT");
        ensureAccount(api, TestData.MBT_PENDING_STUDENT_EMAIL, "MBT Pending Student", "STUDENT");
        ensureAccount(api, TestData.MBT_PENDING_STUDENT_2_EMAIL, "MBT Pending Student 2", "STUDENT");

        String adminToken = api.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
        if (adminToken != null) {
            JsonNode seedResp = api.seed();
            validateMbtGraphwalkerBlock(seedResp);
            if (!mbtPendingSamplePresent(seedResp)) {
                log.warn("POST /admin/seed: no mbt_pending in data; trying POST /admin/seed/mbt-pending");
                JsonNode refill = api.seedMbtPending();
                if (isValidMbtPendingEnvelope(refill)) {
                    ensureMbtPendingBlockPresent(
                            refill.path("data").path("mbt_pending"), refill, "POST /admin/seed/mbt-pending");
                } else {
                    if (isNotFoundError(refill) || (refill == null && api.lastHttpStatus() == 404)) {
                        log.warn(
                                "POST /admin/seed/mbt-pending: 404 (chưa deploy). Thử POST /admin/seed/class-full-pending, "
                                        + "sau đó GET /registrations?status=PENDING");
                    } else {
                        log.warn("POST /admin/seed/mbt-pending: unexpected body {}", refill);
                    }
                    if (!recoverPendingFromClassFullOrVerify(api)) {
                        requireAtLeastOnePendingOrThrow(api, seedResp, refill);
                    }
                }
            } else {
                ensureMbtPendingBlockPresent(seedResp.path("data").path("mbt_pending"), seedResp, "POST /admin/seed");
            }
            log.info("Seeded demo courses and fixtures");
        } else {
            log.warn("Admin login failed, skipping seed (backend may be down)");
        }

        seeded = true;
    }

    /** Có nối mbt_pending hợp lệ từ POST /admin/seed. */
    private static boolean mbtPendingSamplePresent(JsonNode seedResp) {
        if (seedResp == null) return false;
        JsonNode data = seedResp.path("data");
        if (data == null || data.isNull() || !data.isObject()) return false;
        return !data.path("mbt_pending").isMissingNode();
    }

    private static void validateMbtGraphwalkerBlock(JsonNode seedResp) {
        if (seedResp == null || seedResp.path("data").isMissingNode() || seedResp.path("data").isNull()
                || !seedResp.path("data").isObject()) {
            log.warn("Seed response empty or data not an object; MBT graphwalker may be missing: {}", seedResp);
            return;
        }
        JsonNode data = seedResp.path("data");
        JsonNode mbt = data.path("mbt_graphwalker");
        if (mbt.path("skipped").asBoolean(false)) {
            String reason = mbt.path("reason").asText("unknown");
            log.warn(
                    "POST /admin/seed: mbt_graphwalker skipped ({}) — UI/MBT may be degraded. Accounts: {}, {}",
                    reason, TestData.STUDENT_EMAIL, TestData.INSTRUCTOR_EMAIL);
        }
    }

    private static void ensureMbtPendingBlockPresent(JsonNode pending, JsonNode fullResponse, String where) {
        if (skipPendingRequirement()) {
            log.warn("{}: skipping strict mbt_pending JSON checks (mbt.seed.skipPendingRequirement=true)", where);
            return;
        }
        if (pending == null || pending.isMissingNode() || pending.isNull()) {
            throw new IllegalStateException(where + ": missing mbt_pending after seed. Response: " + fullResponse);
        }
        if (pending.path("mbt_pending").isTextual() && pending.path("mbt_pending").asText("").contains("skipped")) {
            throw new IllegalStateException(
                    "mbt_pending PENDING sample not created (" + where + "): " + pending
                            + ". Need at least one PUBLISHED course in DB.");
        }
        if (!pending.path("mbt_pending_registration_id").isNumber()) {
            throw new IllegalStateException(
                    "mbt_pending_registration_id missing (" + where + "): " + pending);
        }
    }

    /** Thân lồng JSON chuẩn (POST /admin/seed/mbt-pending). */
    private static boolean isValidMbtPendingEnvelope(JsonNode n) {
        if (n == null) {
            return false;
        }
        return n.path("data").path("mbt_pending").path("mbt_pending_registration_id").isNumber();
    }

    private static boolean isNotFoundError(JsonNode n) {
        if (n == null) {
            return false;
        }
        if (n.path("detail").isTextual()) {
            return n.path("detail").asText("").toLowerCase().contains("not found");
        }
        return false;
    }

    private static int countPendingFromAdminList(ApiClient api) {
        JsonNode r = api.listRegistrationsWithStatusQuery("PENDING");
        if (r == null || !r.path("data").isArray()) {
            return 0;
        }
        return r.path("data").size();
    }

    /**
     * BE mới: POST /admin/seed/class-full-pending tạo đơn PENDING (lớp đầy) khi route mbt-pending không có.
     */
    private static boolean recoverPendingFromClassFullOrVerify(ApiClient api) {
        JsonNode classFull = api.seedClassFullPending();
        if (classFull != null
                && classFull.path("data").path("mbt_class_full_pending").path("pending_registration_id").isNumber()) {
            if (api.accessToken() == null) {
                String t = api.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
                if (t != null) {
                    api.withToken(t);
                }
            }
            int n = countPendingFromAdminList(api);
            if (n > 0) {
                log.info(
                        "Recovered from POST /admin/seed/class-full-pending: {} PENDING (Admin MBT ok)",
                        n);
                return true;
            }
        } else {
            if (classFull == null) {
                log.debug("class-full-pending: null or HTTP error (lastStatus={})", api.lastHttpStatus());
            } else {
                log.debug("class-full-pending response: {}", classFull);
            }
        }
        if (api.accessToken() == null) {
            String t = api.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
            if (t != null) {
                api.withToken(t);
            }
        }
        return countPendingFromAdminList(api) > 0;
    }

    /**
     * Khi JSON seed không có mbt_pending (API cũ) hoặc route mbt-pending 404, vẫn chấp nhận nếu admin thấy ≥1
     * đơn PENDING qua GET (DB đã seed đúng).
     */
    private static void requireAtLeastOnePendingOrThrow(ApiClient api, JsonNode fullSeed, JsonNode mbtRefill) {
        if (api.accessToken() == null) {
            String t = api.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
            if (t == null) {
                throw new IllegalStateException("Cannot login admin to verify PENDING");
            }
            api.withToken(t);
        }
        api.clearReadCache();
        int n = countPendingFromAdminList(api);
        if (n > 0) {
            log.info("Verified {} PENDING registration(s) via GET /registrations?status=PENDING (Admin MBT ok)", n);
            return;
        }
        log.info("0 PENDING; creating one via student POST /registrations (works with minimal /admin/seed)");
        if (PendingRegistrationHelper.tryCreateOnePendingViaStudent()) {
            PendingRegistrationHelper.loginAdmin(api);
            n = countPendingFromAdminList(api);
        }
        if (n > 0) {
            log.info("Verified {} PENDING after student self-registration (Admin MBT ok)", n);
            return;
        }
        log.info("Still 0 PENDING; admin bumps max_capacity on a full course, then student POST /registrations");
        if (PendingRegistrationHelper.tryEnsurePendingByAdminBumpingCapacity()) {
            PendingRegistrationHelper.loginAdmin(api);
            n = countPendingFromAdminList(api);
        }
        if (n > 0) {
            log.info("Verified {} PENDING after admin capacity bump (Admin MBT ok)", n);
            return;
        }
        log.info("Still 0 PENDING; ephemeral student (UUID email) after bumping full courses");
        if (PendingRegistrationHelper.tryCreatePendingWithEphemeralStudent()) {
            PendingRegistrationHelper.loginAdmin(api);
            n = countPendingFromAdminList(api);
        }
        if (n > 0) {
            log.info("Verified {} PENDING after ephemeral student (Admin MBT ok)", n);
            return;
        }
        log.warn("PENDING still 0 — full re-seed + one more student pass (rate limit / race recovery)");
        api.clearReadCache();
        api.seed();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (PendingRegistrationHelper.tryCreateOnePendingViaStudent()) {
            PendingRegistrationHelper.loginAdmin(api);
            n = countPendingFromAdminList(api);
        }
        if (n > 0) {
            log.info("Verified {} PENDING after re-seed retry (ok)", n);
            return;
        }
        if (skipPendingRequirement()) {
            log.warn(
                    "No PENDING in DB after student fallback; continuing (mbt.seed.skipPendingRequirement=true). "
                            + "Admin MBT runs need PENDING or a fresh seed.");
            return;
        }
        throw new IllegalStateException(
                "No PENDING registrations. POST /admin/seed had no mbt_pending: "
                + fullSeed
                + "; mbt-pending: "
                + mbtRefill
                + ". Tried "
                + TestData.MBT_PENDING_STUDENT_EMAIL
                + " POST /registrations. Cần API_BASE_URL trùng server, khóa PUBLISHED, "
                + "và tài khoản "
                + TestData.MBT_PENDING_STUDENT_EMAIL
                + " tạo được bằng /auth/test/create-account-with-role. "
                + "Hoặc deploy BE có POST /admin/seed trả mbt_pending + route /admin/seed/mbt-pending hoặc /admin/seed/class-full-pending.");
    }

    /**
     * When true, missing PENDING after seed + student fallback does not fail bootstrap (e.g. course-only MBT
     * under strict rate limits). Set via {@code -Dmbt.seed.skipPendingRequirement=true} or static init on a runner.
     */
    private static boolean skipPendingRequirement() {
        return Boolean.parseBoolean(System.getProperty("mbt.seed.skipPendingRequirement", "false"));
    }

    private static void ensureAccount(ApiClient api, String email, String name, String role) {
        JsonNode created = api.createTestAccount(name, email, TestData.PWD_STRONG, role);
        if (created != null) {
            log.info("Ensured account: {} ({}) -> {}", email, role, created.path("data"));
        } else {
            log.debug("Account {} likely already exists", email);
        }
    }
}
