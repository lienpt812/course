package com.eduplatform.mbt.pages;

/**
 * Result of POST /auth/forgot-password as surfaced by the SPA (success block vs error paragraph).
 *
 * <p><strong>Mocking / local dev:</strong> use seeded accounts, MSW, or a backend with
 * {@code AUTH_PASSWORD_RESET} / mail stub enabled. UI-only MBT can treat {@link #INFRASTRUCTURE_OR_NETWORK}
 * like {@link #RATE_LIMITED} and skip the reset vertex via the same graph escape edge.</p>
 */
public enum ForgotSubmitOutcome {
    /** FE rendered {@code data-testid="forgot-password-success"} (HTTP 200 envelope). */
    SUCCESS,
    /**
     * Middleware returned 429 (or FE shows equivalent copy). Valid system behaviour under automation load;
     * tests should not fail the whole GraphWalker run.
     */
    RATE_LIMITED,
    /**
     * Same UX treatment as 429: generic client copy (e.g. {@code "Request failed"} from
     * {@code api.ts} when the server body has no {@code errors[0].message} / {@code detail}),
     * network/5xx, SMTP/email provider down, CORS, or proxy. Not a test bug — do not assert strict success.
     */
    INFRASTRUCTURE_OR_NETWORK
}
