package com.eduplatform.mbt.support;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trạng thái nghiệp vụ auth/session dùng cho MBT Auth và các module khác (token, role).
 * Đồng bộ với biến guard trong model GraphWalker qua {@link BaseImpl#setGw(String, Object)}.
 */
public final class AuthState {

    public boolean isLoggedIn;
    /** STUDENT | INSTRUCTOR | ADMIN | rỗng khi guest (khớp guard JSON {@code currentRole == 'STUDENT'}). */
    public String currentRole = "";
    public String accessToken;
    public String refreshToken;

    public boolean emailExists = true;
    public boolean emailNotExists = true;
    public boolean passwordCorrect = true;
    public boolean passwordStrong = true;
    public boolean emailValid = true;
    public boolean studentMajorOrGoalSet = true;
    public boolean expertiseSet = true;

    public String resetToken = "";
    /** Set when last forgot-password submit saw HTTP 429 / rate limit copy in the UI. */
    public boolean forgotPasswordRateLimited;
    /** Set when last forgot-password saw transport/backend failure (e.g. {@code "Request failed"} in the FE). */
    public boolean forgotPasswordInfrastructureFailure;
    public boolean tokenUsed;
    public boolean tokenExpired;
    public boolean tokenNotFound;
    public boolean profileUpdateValid = true;

    public void clearSession() {
        isLoggedIn = false;
        currentRole = "";
        accessToken = null;
        refreshToken = null;
        forgotPasswordRateLimited = false;
        forgotPasswordInfrastructureFailure = false;
    }

    /** Giá trị guard “happy path” mặc định trước luồng dương. */
    public void armHappyPathGuards() {
        emailExists = true;
        emailNotExists = true;
        passwordCorrect = true;
        passwordStrong = true;
        emailValid = true;
        studentMajorOrGoalSet = true;
        expertiseSet = true;
        profileUpdateValid = true;
        forgotPasswordRateLimited = false;
        forgotPasswordInfrastructureFailure = false;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("isLoggedIn", isLoggedIn);
        m.put("currentRole", currentRole);
        m.put("hasToken", accessToken != null && !accessToken.isBlank());
        m.put("emailExists", emailExists);
        m.put("emailNotExists", emailNotExists);
        m.put("passwordCorrect", passwordCorrect);
        m.put("passwordStrong", passwordStrong);
        m.put("emailValid", emailValid);
        m.put("studentMajorOrGoalSet", studentMajorOrGoalSet);
        m.put("expertiseSet", expertiseSet);
        m.put("resetToken.len", resetToken == null ? 0 : resetToken.length());
        m.put("tokenUsed", tokenUsed);
        m.put("forgotPasswordRateLimited", forgotPasswordRateLimited);
        m.put("forgotPasswordInfrastructureFailure", forgotPasswordInfrastructureFailure);
        m.put("profileUpdateValid", profileUpdateValid);
        return m;
    }

    @Override
    public String toString() {
        return snapshot().toString();
    }
}
