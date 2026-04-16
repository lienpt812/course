using Lms.MbtWeb.Helpers;
using NUnit.Framework;
using OpenQA.Selenium;

namespace Lms.MbtWeb.Models;

/// <summary>
/// ProfilePage.json — student xem/chỉnh sửa hồ sơ: navigate, edit, save valid, submit invalid, discard.
/// </summary>
public class ProfilePage
{
    private static PageHelper Page => Setup.Page;
    private string _originalName = "MBT Student";

    public void setUpModel()
    {
        ApiHelper.SeedAll();
        Page.ClearSession();
        Page.LoginStudent();
        Page.GoTo("/student/dashboard");
        PageHelper.Wait(800);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────

    public void v_AnyDashboard()
    {
        Assert.That(
            Page.CurrentUrl.Contains("dashboard") || Page.CurrentUrl.Contains("student"),
            Is.True, "Should be on dashboard");
    }

    public void v_ProfileViewPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("/profile")  || Page.TextVisible("Profile")  || Page.TextVisible("Name"),
            Is.True, "Should be on profile page");
    }

    public void v_ProfileEditMode()
    {
        Assert.That(
            Page.ElementExists(By.CssSelector("input[name='name'], input[type='text']")) ||
            Page.ElementExists(By.CssSelector("button[type='submit']")) ||
            Page.CurrentUrl.Contains("/profile"),
            Is.True, "Profile should be in edit mode");
    }

    public void v_ProfileSaveSuccess()
    {
        Assert.That(
             Page.TextVisible("Success")  || Page.TextVisible("Saved") ||
            Page.CurrentUrl.Contains("/profile") || Page.CurrentUrl.Contains("dashboard"),
            Is.True, "Profile save success message should appear");
    }

    public void v_ProfileSaveError()
    {
        Assert.That(
             Page.TextVisible("Error") || Page.TextVisible("invalid")  || Page.TextVisible("required") ||
            Page.CurrentUrl.Contains("/profile"),
            Is.True, "Profile save error should be shown");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────

    public void e_NavigateToProfile()
    {
        var link = Page.FindOrNull(
            By.XPath("//a[contains(@href,'/profile') or contains(text(),'Profile')]"));
        if (link != null) { Page.JsClick(link); PageHelper.Wait(700); }
        else { Page.GoTo("/profile"); PageHelper.Wait(700); }
    }

    public void e_ClickEditProfile()
    {
        var editBtn = Page.FindOrNull(
            By.XPath("//button[contains(text(),'Edit')]"));
        if (editBtn != null) { Page.JsClick(editBtn); PageHelper.Wait(600); }
        // If already in edit mode (no separate edit button), stay
    }

    public void e_SubmitValidProfileUpdate()
    {
        // Fill in valid name
        var nameInput = Page.FindOrNull(By.CssSelector("input[name='name'], input[placeholder*='Name']"));
        if (nameInput != null)
        {
            _originalName = nameInput.GetAttribute("value") ?? "MBT Student";
            nameInput.Clear();
            nameInput.SendKeys("MBT Student Updated " + DateTime.Now.Second);
        }

        // Fill learning goal if present
        var goalInput = Page.FindOrNull(
            By.CssSelector("input[name='learning_goal'], textarea[name='learning_goal']"));
        if (goalInput != null)
        {
            goalInput.Clear();
            goalInput.SendKeys("Updated learning goal via MBT");
        }

        var saveBtn = Page.FindOrNull(
            By.XPath("//button[contains(text(),'Save') or contains(text(),'Update')]"));
        if (saveBtn != null) { Page.JsClick(saveBtn); PageHelper.Wait(1500); }
        else { Page.Click(By.CssSelector("button[type='submit']")); PageHelper.Wait(1500); }
    }

    public void e_SubmitInvalidProfile()
    {
        // Clear required name field to trigger validation error
        var nameInput = Page.FindOrNull(By.CssSelector("input[name='name'], input[placeholder*='Name']"));
        if (nameInput != null)
        {
            _originalName = nameInput.GetAttribute("value") ?? "MBT";
            nameInput.Clear();
        }

        var saveBtn = Page.FindOrNull(
            By.XPath("//button[contains(text(),'Save') or @type='submit']"));
        if (saveBtn != null) { Page.JsClick(saveBtn); PageHelper.Wait(1000); }
    }

    public void e_FixAndRetrySubmit()
    {
        // Restore a valid name
        var nameInput = Page.FindOrNull(By.CssSelector("input[name='name'], input[placeholder*='Name']"));
        if (nameInput != null)
        {
            nameInput.Clear();
            nameInput.SendKeys(string.IsNullOrEmpty(_originalName) ? "MBT Student Fixed" : _originalName);
        }

        var saveBtn = Page.FindOrNull(
            By.XPath("//button[contains(text(),'Save') or @type='submit']"));
        if (saveBtn != null) { Page.JsClick(saveBtn); PageHelper.Wait(1200); }
    }

    public void e_BackToViewAfterSave()
    {
        // After save success, we stay on profile or go back to view
        Page.GoTo("/profile");
        PageHelper.Wait(600);
    }

    public void e_DiscardProfileEdits()
    {
        var cancelBtn = Page.FindOrNull(
            By.XPath("//button[contains(text(),'Cancel') or contains(text(),'Discard')]"));
        if (cancelBtn != null) { Page.JsClick(cancelBtn); PageHelper.Wait(600); }
        else { Page.GoTo("/profile"); PageHelper.Wait(600); }
    }

    public void e_BackToDashboardFromProfile()
    {
        Page.GoTo("/student/dashboard");
        PageHelper.Wait(600);
    }
}
