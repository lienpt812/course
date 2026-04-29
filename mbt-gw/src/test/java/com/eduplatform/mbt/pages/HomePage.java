package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Selenide;

/** / — HomePage.tsx */
public class HomePage {

    public HomePage open() {
        Selenide.open("/");
        return this;
    }
}
