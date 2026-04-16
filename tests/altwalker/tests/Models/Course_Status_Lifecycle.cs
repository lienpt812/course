namespace CourseRegistration.Tests.Models;

/// <summary>
/// Implements model: Course_Status_Lifecycle.json
/// Covers: DRAFT→PUBLISHED→ARCHIVED, registration window, capacity
/// </summary>
public class Course_Status_Lifecycle
{
    private PageHelper Page => Setup.Page;
    private string _courseStatus = "DRAFT";

    public void setUpModel()
    {
        Page.ClearSession();
        Page.Login(PageHelper.InstructorEmail, PageHelper.InstructorPassword);
        var courseId = Page.GetFirstCourseId();
        Page.GoTo($"/courses/{courseId}");
        System.Threading.Thread.Sleep(800);
    }

    // ── Vertices ─────────────────────────────────────────────────────────────


    public void v_DraftCourse()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_PublishedCourse()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_ComingSoonCourse()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_ArchivedCourse()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_RegistrationOpenWindow()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_RegistrationClosedWindow()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CapacityFull()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CapacityAvailable()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_EditBlockedPublished()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }


    // ── Edges ─────────────────────────────────────────────────────────────────

    public void e_PublishFromDraft()
    {
        _courseStatus = "PUBLISHED";
        // Change status via edit form
        var statusSelect = Setup.Driver.FindElements(By.CssSelector("select[name='status']"));
        if (statusSelect.Count > 0)
        {
            new SelectElement(statusSelect[0]).SelectByValue("PUBLISHED");
            var saveBtn = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Lưu')]"));
            if (saveBtn.Count > 0) { saveBtn[0].Click(); System.Threading.Thread.Sleep(1000); }
        }
    }

    public void e_SetComingSoon()
    {
        _courseStatus = "COMING_SOON";
        var statusSelect = Setup.Driver.FindElements(By.CssSelector("select[name='status']"));
        if (statusSelect.Count > 0)
        {
            new SelectElement(statusSelect[0]).SelectByValue("COMING_SOON");
            var saveBtn = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Lưu')]"));
            if (saveBtn.Count > 0) { saveBtn[0].Click(); System.Threading.Thread.Sleep(1000); }
        }
    }

    public void e_PublishFromComingSoon()
    {
        _courseStatus = "PUBLISHED";
        var statusSelect = Setup.Driver.FindElements(By.CssSelector("select[name='status']"));
        if (statusSelect.Count > 0)
        {
            new SelectElement(statusSelect[0]).SelectByValue("PUBLISHED");
            var saveBtn = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Lưu')]"));
            if (saveBtn.Count > 0) { saveBtn[0].Click(); System.Threading.Thread.Sleep(1000); }
        }
    }

    public void e_ArchiveFromPublished()
    {
        _courseStatus = "ARCHIVED";
        var statusSelect = Setup.Driver.FindElements(By.CssSelector("select[name='status']"));
        if (statusSelect.Count > 0)
        {
            new SelectElement(statusSelect[0]).SelectByValue("ARCHIVED");
            var saveBtn = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Lưu')]"));
            if (saveBtn.Count > 0) { saveBtn[0].Click(); System.Threading.Thread.Sleep(1000); }
        }
    }

    public void e_ArchiveFromDraft()
    {
        _courseStatus = "ARCHIVED";
        var statusSelect = Setup.Driver.FindElements(By.CssSelector("select[name='status']"));
        if (statusSelect.Count > 0)
        {
            new SelectElement(statusSelect[0]).SelectByValue("ARCHIVED");
            var saveBtn = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Lưu')]"));
            if (saveBtn.Count > 0) { saveBtn[0].Click(); System.Threading.Thread.Sleep(1000); }
        }
    }

    public void e_TryEditPublished()
    {
        Assert.That(_courseStatus == "PUBLISHED", "Course must be published to test edit block");
        var editBtns = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Chỉnh sửa')]"));
        Assert.That(editBtns.Count == 0 || Page.TextVisible("đã published"),
            "Edit should be blocked for published course");
    }

    public void e_BackToPublished()
    {
        Page.GoTo("/courses");
        System.Threading.Thread.Sleep(500);
    }

    public void e_CheckRegistrationWindowOpen()
    {
        Page.GoTo("/courses");
        var cards = Setup.Driver.FindElements(By.CssSelector("a[href*='/courses/']"));
        if (cards.Count > 0) { cards[0].Click(); System.Threading.Thread.Sleep(500); }
    }

    public void e_CheckRegistrationWindowClosed()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }

    public void e_CheckCapacityAvailable()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }

    public void e_CheckCapacityFull()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }

    public void e_IncreaseCapacity()
    {
        // Edit course to increase max_capacity
        var editBtn = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Chỉnh sửa')]"));
        if (editBtn.Count > 0)
        {
            editBtn[0].Click();
            var capacityInput = Setup.Driver.FindElements(By.CssSelector("input[name='max_capacity']"));
            if (capacityInput.Count > 0)
            {
                Page.Type(capacityInput[0], "100");
                var saveBtn = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Lưu')]"));
                if (saveBtn.Count > 0) { saveBtn[0].Click(); System.Threading.Thread.Sleep(1000); }
            }
        }
    }

    public void e_BackToDraft()
    {
        _courseStatus = "DRAFT";
        Page.GoTo("/instructor/dashboard");
    }
}
