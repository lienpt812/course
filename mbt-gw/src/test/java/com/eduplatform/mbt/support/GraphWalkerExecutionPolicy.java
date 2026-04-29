package com.eduplatform.mbt.support;

/**
 * Central {@link org.graphwalker.java.annotation.GraphWalker#value} strings.
 *
 * <p>We use {@code random(edge_coverage(100) || length(N))} — <strong>not</strong> {@code weighted_random}:
 * GraphWalker 4.3’s {@code WeightedRandomPath} applies {@code weight} to the <em>current</em> list of
 * guard-unblocked edges; if that subset’s positive weights do not cover the probability mass (e.g. sum
 * &lt; 1 with no zero-weight edge to take the rest), the walker throws
 * “Could not calculate which weighted edge to choose”. Uniform random is stable with our guarded models.
 * Stops when full edge coverage is reached or the path length cap is hit.
 *
 * <p>Override at runtime: {@code -Dgraphwalker.generator=...} (see
 * {@link com.eduplatform.mbt.runners.AbstractGraphWalkerRunner}).
 */
public final class GraphWalkerExecutionPolicy {

    private GraphWalkerExecutionPolicy() {}

    /** Auth + registration-style flows. */
    public static final String BOUNDED_DEFAULT = "random(edge_coverage(100) || length(50))";
    /** Auth model only: longer cap so random walks can reach more branches (forgot/reset/instructor). */
    public static final String BOUNDED_AUTH = "random(edge_coverage(100))";
    public static final String BOUNDED_COURSE = "random(edge_coverage(100) || length(45))";
    public static final String BOUNDED_REG = "random(edge_coverage(100) || length(45))";
    public static final String BOUNDED_LEARN = "random(edge_coverage(100) || length(45))";
    public static final String BOUNDED_CERT = "random(edge_coverage(100) || length(40))";
    /** Instructor: slightly shorter path to reduce form churn. */
    public static final String BOUNDED_INSTRUCTOR = "random(edge_coverage(100) || length(40))";
    public static final String BOUNDED_ADMIN = "random(edge_coverage(100) || length(45))";

    // --- Deterministic (short) journeys: a* to one goal vertex; guards + seed data must allow the path. ---

    public static final String JOURNEY_AUTH_LOGIN_STUDENT =
            "a_star(reached_vertex(v_StudentDashboard))";
    public static final String JOURNEY_COURSE_TO_DETAIL = "a_star(reached_vertex(v_CourseDetailVisible))";
    public static final String JOURNEY_INSTRUCTOR_SECTION_MANAGER = "a_star(reached_vertex(v_SectionManagerOpen))";
    public static final String JOURNEY_LEARNING_PAGE = "a_star(reached_vertex(v_LearningPageLoaded))";
    public static final String JOURNEY_CERTIFICATE_ISSUED = "a_star(reached_vertex(v_CertificateIssued))";
    public static final String JOURNEY_REG_PENDING = "a_star(reached_vertex(v_PendingRegistration))";
}
