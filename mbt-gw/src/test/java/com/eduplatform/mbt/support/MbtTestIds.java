package com.eduplatform.mbt.support;

/**
 * {@code data-testid} values shared with the React app (fe_new) for stable Selenium
 * locators. Prefer these over Tailwind class selectors.
 */
public final class MbtTestIds {

    private MbtTestIds() {}

    public static final String COURSES_COUNT_BADGE = "courses-count-badge";
    public static final String COURSES_GRID = "courses-grid";
    public static final String COURSES_LOADING = "courses-loading";
    public static final String COURSES_ERROR = "courses-error";
    public static final String COURSES_EMPTY_STATE = "courses-empty-state";
    public static final String COURSES_SEARCH_INPUT = "courses-search-input";

    public static final String COURSE_CARD = "course-card";

    /** Course detail — instructor edit (DRAFT owner). */
    public static final String COURSE_EDIT_BUTTON = "course-edit-button";

    /** InstructorDashboard — course picker for section/lesson manager (not create-form selects). */
    public static final String INSTRUCTOR_MANAGE_COURSE_SELECT = "instructor-manage-course-select";

    public static final String REGISTER_FORM_ERROR = "register-form-error";
    public static final String LOGIN_FORM_ERROR = "login-form-error";
    public static final String FORGOT_PASSWORD_SUCCESS = "forgot-password-success";
    public static final String FORGOT_PASSWORD_FORM_ERROR = "forgot-password-form-error";
}
