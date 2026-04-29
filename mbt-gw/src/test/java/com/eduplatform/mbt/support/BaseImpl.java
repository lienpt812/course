package com.eduplatform.mbt.support;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.eduplatform.mbt.pages.AdminDashboardPage;
import com.eduplatform.mbt.pages.LoginPage;
import com.fasterxml.jackson.databind.JsonNode;
import org.graalvm.polyglot.Value;
import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.core.machine.MachineException;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Edge.RuntimeEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * Common base for all model implementations. Holds a shared {@link TestContext}
 * plus API/Selenide helpers. Each concrete subclass extends this + implements the
 * generated GraphWalker model interface.
 *
 * <p>Quy ước model JSON: {@code guard} và phần tử trong {@code actions} chỉ là <strong>tên method</strong>
 * (public, không tham số) — ví dụ {@code gwGuard_loggedIn} hoặc tùy chọn {@code gwGuard_loggedIn()}.
 * Method guard phải trả về {@code boolean}; method action thường {@code void}. Biểu thức Graal/JS chỉ dùng
 * khi cần {@code global.*} (fallback {@link #isAvailable} / {@link #execute(Action)} gốc).</p>
 */
public abstract class BaseImpl extends ExecutionContext {

    private static final Pattern SIMPLE_METHOD_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\(\\))?$");

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final TestContext ctx = new TestContext();
    protected final ApiClient api = new ApiClient();

    /** Throttle: repeated identical GW element back-to-back (rate limit / UI flake mitigation). */
    private String mbtLastElementName;
    private long mbtLastElementAtMs;

    /** Session + guard variables cho module Auth (và token dùng chung). */
    protected AuthState auth() {
        return TestContext.getAuthState();
    }

    @Override
    public boolean isAvailable(RuntimeEdge edge) {
        if (!edge.hasGuard()) {
            return true;
        }
        String script = edge.getGuard().getScript().trim();
        if (script.contains("global.")) {
            return super.isAvailable(edge);
        }
        String method = simpleMethodNameOrNull(script);
        if (method != null) {
            return invokeBooleanGuard(method);
        }
        return super.isAvailable(edge);
    }

    @Override
    public void execute(Action action) {
        String script = action.getScript().trim();
        if (script.contains("global.")) {
            super.execute(action);
            return;
        }
        String method = simpleMethodNameOrNull(script);
        if (method != null) {
            invokeVoidAction(method);
            return;
        }
        super.execute(action);
    }

    /**
     * Wraps every vertex/edge method: updates {@link TestContext} MBT fields, throttles hot repeats,
     * and logs a clear line on {@link MachineException} (fail-fast visibility).
     */
    @Override
    public void execute(Element element) {
        if (element == null || !element.hasName()) {
            return;
        }
        String name = element.getName();
        ctx.mbtCurrentModelElement = name;
        if (name.startsWith("v_")) {
            ctx.mbtCurrentPage = name;
        } else if (name.startsWith("e_")) {
            ctx.mbtLastEdge = name;
        }
        mbtDelayIfHotRepeat(name);
        try {
            super.execute(element);
        } catch (MachineException ex) {
            String pageUrl = "";
            try {
                if (WebDriverRunner.hasWebDriverStarted()) {
                    pageUrl = WebDriverRunner.url();
                }
            } catch (Exception ignored) { }
            log.error("MBT fail-fast: element='{}' model='{}' url='{}' cause='{}'", name,
                    getModel() != null ? getModel().getName() : "?",
                    pageUrl,
                    ex.getCause() != null ? ex.getCause() : ex);
            throw ex;
        } finally {
            mbtAfterElement(name);
        }
    }

    private void mbtDelayIfHotRepeat(String name) {
        long now = System.currentTimeMillis();
        if (name.equals(mbtLastElementName) && (now - mbtLastElementAtMs) < 450) {
            int d = 200 + (int) (Math.random() * 300);
            log.warn("MBT throttle: repeated element '{}' within {}ms — sleep {}ms", name,
                    (now - mbtLastElementAtMs), d);
            try {
                Thread.sleep(d);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        mbtLastElementName = name;
        mbtLastElementAtMs = System.currentTimeMillis();
    }

    private void mbtAfterElement(String name) {
        if (name.startsWith("e_")) {
            int d = 200 + (int) (Math.random() * 150);
            try {
                Thread.sleep(d);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Soft guard for edges that assume a session. GraphWalker should block invalid edges via
     * {@code guard} in the model; this is only a log line when the walk desynchronizes.
     */
    @SuppressWarnings("unused")
    protected boolean mbtEdgeExpectLoggedIn(String edgeName) {
        if (auth().isLoggedIn) {
            return true;
        }
        log.warn("MBT: edge '{}' expected isLoggedIn; page='{}' — likely model/guard desync", edgeName, ctx.mbtCurrentPage);
        return false;
    }

    private static String simpleMethodNameOrNull(String script) {
        if (!SIMPLE_METHOD_NAME.matcher(script).matches()) {
            return null;
        }
        if (script.endsWith("()")) {
            return script.substring(0, script.length() - 2);
        }
        return script;
    }

    private boolean invokeBooleanGuard(String name) {
        try {
            Method m = getClass().getMethod(name);
            Class<?> rt = m.getReturnType();
            if (rt != boolean.class && rt != Boolean.class) {
                throw new MachineException(this, new IllegalStateException(
                        "Guard must return boolean: " + name + " in " + getClass().getName()));
            }
            Object r = m.invoke(this);
            if (r == null) {
                return false;
            }
            return (boolean) r;
        } catch (NoSuchMethodException e) {
            throw new MachineException(this, new IllegalStateException(
                    "Missing public no-arg guard " + name + " on " + getClass().getName(), e));
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            throw new MachineException(this, c);
        } catch (ReflectiveOperationException e) {
            throw new MachineException(this, e);
        }
    }

    private void invokeVoidAction(String name) {
        try {
            Method m = getClass().getMethod(name);
            m.invoke(this);
        } catch (NoSuchMethodException e) {
            throw new MachineException(this, new IllegalStateException(
                    "Missing public no-arg action " + name + " on " + getClass().getName(), e));
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            throw new MachineException(this, c);
        } catch (ReflectiveOperationException e) {
            throw new MachineException(this, e);
        }
    }

    protected void logStep(String name) {
        if (log.isInfoEnabled()) {
            log.info("[STEP] {} | beforeAuth={}", name, auth().snapshot());
        }
    }

    protected void logStepAfter(String name) {
        if (log.isInfoEnabled()) {
            log.info("[STEP/DONE] {} | afterAuth={}", name, auth().snapshot());
        }
    }

    /**
     * Đẩy biến vào context Graal (dữ liệu phụ trợ; guard chính trong JSON là tên method {@code gwGuard_*}).
     */
    protected void setGw(String name, Object value) {
        setAttribute(name, Value.asValue(value));
    }

    /**
     * Đẩy toàn bộ {@link AuthState} vào context Graal (nếu cần cho guard dạng {@code global.*} hoặc debug).
     */
    protected void syncGraphWalkerAuthVars() {
        AuthState a = auth();
        setGw("isLoggedIn", a.isLoggedIn);
        setGw("currentRole", a.currentRole == null ? "" : a.currentRole);
        setGw("accessToken", a.accessToken != null && !a.accessToken.isBlank() ? "valid" : "");
        setGw("emailExists", a.emailExists);
        setGw("emailNotExists", a.emailNotExists);
        setGw("passwordCorrect", a.passwordCorrect);
        setGw("passwordStrong", a.passwordStrong);
        setGw("emailValid", a.emailValid);
        setGw("studentMajorOrGoalSet", a.studentMajorOrGoalSet);
        setGw("expertiseSet", a.expertiseSet);
        setGw("resetToken", a.resetToken == null ? "" : a.resetToken);
        setGw("tokenUsed", a.tokenUsed);
        setGw("profileUpdateValid", a.profileUpdateValid);
    }

    protected void armHappyAuthGuards() {
        auth().armHappyPathGuards();
        syncGraphWalkerAuthVars();
    }

    /** CourseExplorationModel — đồng bộ {@link TestContext} → GraphWalker. */
    protected void syncExplorationGraphWalker() {
        setGw("coursesLoaded", ctx.coursesLoaded);
        setGw("totalCourses", ctx.totalCourses);
        setGw("filteredCount", ctx.filteredCount);
        setGw("selectedCategory", ctx.explorationCategory);
        setGw("selectedLevel", ctx.explorationLevel);
        setGw("searchQuery", ctx.explorationSearchQuery);
        setGw("selectedCourseId", ctx.selectedCourseId);
        setGw("selectedCourseStatus", ctx.selectedCourseStatus);
        setGw("registrationOpenFuture", ctx.registrationOpenFuture);
        setGw("registrationClosePast", ctx.registrationClosePast);
        setGw("isLoggedIn", auth().isLoggedIn);
        String role = auth().currentRole == null || auth().currentRole.isEmpty() ? "GUEST" : auth().currentRole;
        setGw("currentRole", role);
    }

    /** RegistrationModel */
    protected void syncRegistrationGraphWalker() {
        setGw("hasRemainingSlotOnTargetCourse", ctx.hasRemainingSlotOnTargetCourse);
        setGw("regPipelineHasPending", ctx.regPipelineHasPending);
        setGw("isLoggedIn", auth().isLoggedIn);
        setGw("currentRole", auth().currentRole == null ? "" : auth().currentRole);
    }

    /** LearningModel */
    protected void syncLearningGraphWalker() {
        setGw("isLoggedIn", auth().isLoggedIn);
        setGw("currentRole", auth().currentRole == null ? "" : auth().currentRole);
        setGw("registrationStatus", ctx.registrationStatus);
        setGw("totalLessons", ctx.totalLessons);
        setGw("completedLessons", ctx.completedLessons);
        setGw("completionPct", ctx.completionPct);
        setGw("currentLessonType", ctx.currentLessonType);
        setGw("currentLessonIsPreview", ctx.currentLessonIsPreview);
        setGw("currentLessonCompletionPct", ctx.currentLessonCompletionPct);
    }

    /** CertificateModel */
    protected void syncCertificateGraphWalker() {
        setGw("isLoggedIn", auth().isLoggedIn);
        setGw("currentRole", auth().currentRole == null ? "" : auth().currentRole);
        setGw("hasExistingCertificate", ctx.hasExistingCertificate);
        setGw("hasVerificationCode", ctx.hasVerificationCode);
        setGw("verificationCode", ctx.verificationCode == null ? "" : ctx.verificationCode);
    }

    /** InstructorContentModel — cờ validation + ownership (API sync trong impl). */
    protected void syncInstructorGraphWalker() {
        setGw("isLoggedIn", auth().isLoggedIn);
        setGw("currentRole", auth().currentRole == null ? "" : auth().currentRole);
        setGw("ownedCourseExists", ctx.ownedCourseExists);
        setGw("ownedDraftExists", ctx.ownedDraftExists);
        setGw("ownedPublishedExists", ctx.ownedPublishedExists);
        setGw("foreignCourseExists", ctx.foreignCourseExists);
        setGw("selectedSectionExists", ctx.selectedSectionExists);
        setGw("titleValid", ctx.titleValid);
        setGw("descriptionValid", ctx.descriptionValid);
        setGw("slugValid", ctx.slugValid);
        setGw("imageUrlValid", ctx.imageUrlValid);
        setGw("maxCapacityValid", ctx.maxCapacityValid);
        setGw("estimatedHoursValid", ctx.estimatedHoursValid);
        setGw("sectionTitleValid", ctx.sectionTitleValid);
        setGw("sectionPositionValid", ctx.sectionPositionValid);
        setGw("lessonTitleValid", ctx.lessonTitleValid);
        setGw("lessonPositionValid", ctx.lessonPositionValid);
        setGw("lessonDurationValid", ctx.lessonDurationValid);
    }

    /** AdminManagementModel */
    protected void syncAdminGraphWalker() {
        setGw("isLoggedIn", auth().isLoggedIn);
        setGw("currentRole", auth().currentRole == null ? "" : auth().currentRole);
        setGw("adminHasPending", ctx.adminHasPending);
        setGw("adminHasApprovablePending", ctx.adminHasApprovablePending);
        setGw("lastAdminAction", ctx.lastAdminAction == null ? "INIT" : ctx.lastAdminAction);
    }

    protected void armHappyInstructorCourseFormGuards() {
        ctx.titleValid = true;
        ctx.descriptionValid = true;
        ctx.slugValid = true;
        ctx.imageUrlValid = true;
        ctx.maxCapacityValid = true;
        ctx.estimatedHoursValid = true;
        syncInstructorGraphWalker();
    }

    protected void armHappyInstructorSectionLessonGuards() {
        ctx.sectionTitleValid = true;
        ctx.sectionPositionValid = true;
        ctx.lessonTitleValid = true;
        ctx.lessonPositionValid = true;
        ctx.lessonDurationValid = true;
        syncInstructorGraphWalker();
    }

    /**
     * Poll the current URL until it contains {@code fragment} or the timeout elapses.
     * Returns true if matched. Useful after form submits where FE navigation is async.
     */
    protected boolean waitForUrlContains(String fragment, int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                String url = WebDriverRunner.url();
                if (url != null && url.contains(fragment)) return true;
            } catch (Exception ignored) {}
            try { Thread.sleep(200); } catch (InterruptedException ignored) { return false; }
        }
        return false;
    }

    /** Navigate with a simple wait-or-fallback: let FE drive SPA routing, else force open. */
    protected void navigateWithFallback(String urlFragment, String fallbackPath, int seconds) {
        if (!waitForUrlContains(urlFragment, seconds)) {
            log.warn("URL did not change to '{}' within {}s; forcing open('{}')", urlFragment, seconds, fallbackPath);
            Selenide.open(fallbackPath);
        }
    }

    /**
     * API login plus full SPA session (tokens + {@code auth_user} JSON), then {@link Selenide#open(String)}.
     * Token-only injection leaves React {@code RootLayout} without {@code auth_user} and often redirects to {@code /courses}.
     */
    /**
     * Đăng nhập qua API với vài lần thử (mạng/BE tạm lỗi) — dùng cho MBT dài, tránh lệch guard/SPA.
     */
    protected String loginWithRetry(String email, String password) {
        final int max = 5;
        for (int i = 0; i < max; i++) {
            String t = api.login(email, password);
            if (t != null) {
                return t;
            }
            try {
                Thread.sleep(350L * (i + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (i < max - 1) {
                log.warn("API login attempt {} failed for {} — retrying", i + 1, email);
            }
        }
        log.error("API login failed for {} after {} attempts", email, max);
        return null;
    }

    /**
     * Writes tokens + {@code auth_user} into the browser (matches FE {@code RootLayout} / {@code api.ts}).
     * Real {@code refresh_token} is required so a 401 on {@code /auth/refresh} does not wipe the session.
     */
    protected void writeAuthToBrowserLocalStorage(String accessToken, String refreshToken, String meJson) {
        String refresh = refreshToken == null ? "" : refreshToken;
        try { Selenide.clearBrowserCookies(); } catch (Exception ignored) {}
        Selenide.executeJavaScript(
                "localStorage.clear(); sessionStorage.clear();" +
                "localStorage.setItem('access_token', arguments[0]);" +
                "localStorage.setItem('refresh_token', arguments[1]);" +
                "localStorage.setItem('auth_user', arguments[2]);",
                accessToken, refresh, meJson);
    }

    /**
     * Re-injects {@link AuthState} into the browser and opens {@code path} without POST /auth/login.
     * Avoids rate limits when GraphWalker revisits an entry vertex.
     *
     * @return false if there is no token, /auth/me fails, or the URL never matches {@code path}
     */
    protected boolean injectBrowserSessionAndOpen(String path) {
        if (auth().accessToken == null || auth().accessToken.isBlank()) {
            return false;
        }
        api.withToken(auth().accessToken);
        JsonNode meResp = api.me();
        if (meResp == null || meResp.path("data").isMissingNode()) {
            log.warn("injectBrowserSessionAndOpen: /auth/me failed — session may be stale");
            return false;
        }
        String meJson = meResp.path("data").toString();
        String refresh = auth().refreshToken != null ? auth().refreshToken : "";
        writeAuthToBrowserLocalStorage(auth().accessToken, refresh, meJson);
        Selenide.open(path);
        String frag = path.startsWith("/") ? path.substring(1) : path;
        if (!waitForUrlContains(frag, 12)) {
            log.warn("injectBrowserSessionAndOpen: expected URL fragment '{}' not found; url={} — retry open", frag,
                    WebDriverRunner.url());
            Selenide.open(path);
            if (!waitForUrlContains(frag, 10)) {
                return false;
            }
        }
        return true;
    }

    protected void openAuthenticatedWithApi(String email, String password, String path) {
        // Land on a non-protected route first so clearing storage in resetBrowserState does not
        // trigger RootLayout's "no token -> navigate('/courses')" while still on /admin/dashboard etc.
        Selenide.open("/login");
        SelenideSetup.resetBrowserState();
        String token = loginWithRetry(email, password);
        if (token == null) {
            auth().accessToken = null;
            auth().refreshToken = null;
            auth().isLoggedIn = false;
            Selenide.open(path);
            return;
        }
        auth().accessToken = token;
        auth().isLoggedIn = true;
        api.withToken(token);
        JsonNode meResp = api.me();
        String meJson = meResp != null && !meResp.path("data").isMissingNode()
                ? meResp.path("data").toString()
                : "{}";
        String refresh = api.lastRefreshToken();
        if (refresh == null) {
            refresh = "";
        }
        auth().refreshToken = refresh.isBlank() ? null : refresh;
        writeAuthToBrowserLocalStorage(token, refresh, meJson);
        Selenide.open(path);
        String frag = path.startsWith("/") ? path.substring(1) : path;
        if (!waitForUrlContains(frag, 12)) {
            log.warn("openAuthenticatedWithApi: expected URL fragment '{}' not found; url={} — mở lại", frag,
                    WebDriverRunner.url());
            Selenide.open(path);
            if (!waitForUrlContains(frag, 10)) {
                log.warn("openAuthenticatedWithApi: sau lần mở lại vẫn url={}", WebDriverRunner.url());
            }
        }
    }

    /**
     * Ensures the browser shows the admin dashboard (h1 + filter selects), with the same strategy as
     * {@code AdminManagementImpl#assertAdminDashboard}: fix URL + re-inject session, then full UI login if the
     * SPA never renders the admin view (e.g. stale session or {@code me()} failure after a long run).
     *
     * <p>Call after {@code asAdmin()}; pass a no-arg re-injection of the same session (e.g. {@code () -> asAdmin()}).
     */
    protected void ensureAdminDashboardPage(AdminDashboardPage adminDash, Runnable reInjectAsAdmin) {
        try {
            if (!waitForUrlContains("/admin/dashboard", 3)) {
                log.info("ensureAdminDashboardPage: not on /admin, url={} — re-injecting session", WebDriverRunner.url());
                reInjectAsAdmin.run();
            }
            if (!waitForUrlContains("/admin/dashboard", 8)) {
                log.warn("ensureAdminDashboardPage: still not on /admin after re-inject, url={}", WebDriverRunner.url());
            }
            adminDash.waitForAdminFilterControls();
        } catch (Throwable t) {
            log.warn("ensureAdminDashboardPage: retry full UI login: {}", t.getMessage());
            Selenide.open("/login");
            SelenideSetup.resetBrowserState();
            Selenide.open("/login");
            LoginPage login = new LoginPage();
            login.assertLoaded();
            login.login(TestData.ADMIN_EMAIL, TestData.PWD_STRONG);
            adminDash.open().assertLoaded();
        }
    }
}
