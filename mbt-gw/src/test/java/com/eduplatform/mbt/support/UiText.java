package com.eduplatform.mbt.support;

/**
 * Chuỗi hiển thị trên FE (UTF-8), khớp {@code fe_new}. Dùng thống nhất để tránh lệch encoding
 * hoặc nhầm "Khoa Hoc" / "Khóa học".
 */
public final class UiText {

    private UiText() {}

    public static final String COURSES_H1 = "Khóa Học";
    public static final String LOGIN_H1 = "Đăng nhập";
    public static final String REGISTER_H1 = "Đăng ký";
    public static final String FORGOT_PASSWORD_H1 = "Quên mật khẩu";
    public static final String RESET_PASSWORD_H1 = "Đặt lại mật khẩu";

    public static final String STUDENT_DASHBOARD_H1 = "Dashboard Học Viên";
    public static final String INSTRUCTOR_DASHBOARD_H1 = "Dashboard Giảng Viên";
    public static final String ADMIN_DASHBOARD_H1 = "Quản Trị Hệ Thống";

    public static final String PROFILE_H1 = "Profile";
}
