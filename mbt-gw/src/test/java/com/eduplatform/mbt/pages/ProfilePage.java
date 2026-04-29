package com.eduplatform.mbt.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.UiText;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/** /profile — ProfilePage.tsx */
public class ProfilePage {

    public ProfilePage open() {
        Selenide.open("/profile");
        return this;
    }

    public ProfilePage assertLoaded() {
        SafeUi.waitUntilVisible($("h1"), SafeUi.DEFAULT_TIMEOUT).shouldHave(Condition.text(UiText.PROFILE_H1));
        return this;
    }

    public SelenideElement saveButton()  { return $(byText("Lưu")); }
    public SelenideElement errorText()   { return $$("p.text-red-600, div.text-red-600").first(); }
    public SelenideElement successText() { return $$("p.text-emerald-700, div.text-emerald-700").first(); }
}
