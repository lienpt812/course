package com.eduplatform.mbt.pages;

import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.SafeUi;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/** Header nav rendered by fe_new/src/app/components/RootLayout.tsx. */
public class NavBar {

    public SelenideElement root() { return $("header nav"); }

    public SelenideElement brand()        { return root().$(byText("EduPlatform")); }
    public SelenideElement linkCourses()  { return root().$(byText("Khóa Học")); }
    public SelenideElement linkDashboardStudent()    { return root().$(byText("Dashboard")); }
    public SelenideElement linkDashboardInstructor() { return root().$(byText("Giảng Viên")); }
    public SelenideElement linkDashboardAdmin()      { return root().$(byText("Quản Trị")); }
    public SelenideElement buttonLogin()  { return root().$(byText("Đăng nhập")); }
    public SelenideElement buttonLogout() { return root().$(byText("Logout")); }
    public SelenideElement profileLink()  { return root().$("a[title='Trang cá nhân']"); }

    public void clickLogin() {
        SafeUi.clickWhenReady(buttonLogin(), Duration.ofSeconds(8), 2);
    }

    public void clickLogout() {
        SafeUi.clickWhenReady(buttonLogout(), Duration.ofSeconds(8), 2);
    }
    public void clickCourses() { linkCourses().click(); }
}
