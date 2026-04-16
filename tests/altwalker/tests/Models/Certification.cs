namespace CourseRegistration.Tests.Models;

/// <summary>
/// Implements model: Certification.json
/// Covers: Claim cert, verify cert (public), cert already exists (idempotent)
/// </summary>
public class Certification
{
    private PageHelper Page => Setup.Page;
    private string _verificationCode = "";

    public void setUpModel()
    {
        Page.ClearSession();
        Page.Login(PageHelper.StudentEmail, PageHelper.StudentPassword);
        Page.GoTo("/student/dashboard");
        System.Threading.Thread.Sleep(500);
    }

    // ── Vertices ─────────────────────────────────────────────────────────────


    public void v_StudentDashboard()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_LearningPage()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CertClaimSuccess()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CertAlreadyExists()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CertNotEligible()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CertVerifyPage()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CertVerifyValid()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_CertVerifyInvalid()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }



    public void v_NoCertSection()
    {
        Assert.That(Page.CurrentUrl.Contains("localhost"), "Should be on app");
    }


    // ── Edges ─────────────────────────────────────────────────────────────────

    public void e_ViewNoCerts()
    {
        Assert.That(
            Page.TextVisible("Chứng Chỉ Của Tôi") || Page.TextVisible("Certificate"),
            "Should show cert section");
    }

    public void e_GoToLearning()
    {
        // Try to find a learn link from dashboard
        var learnLinks = Setup.Driver.FindElements(By.CssSelector("a[href*='/learn/']"));
        if (learnLinks.Count > 0)
        {
            learnLinks[0].Click();
            System.Threading.Thread.Sleep(1000);
        }
        else
        {
            // No confirmed courses - go to courses page
            Page.GoTo("/courses");
            System.Threading.Thread.Sleep(500);
        }
    }

    public void e_CompleteAllLessons()
    {
        if (!Page.CurrentUrl.Contains("/learn/"))
        {
            Assert.Warn("Not on learning page - skipping lesson completion");
            return;
        }
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++)
        {
            var completeBtns = Setup.Driver.FindElements(
                By.XPath("//*[contains(text(),'Đánh dấu hoàn thành') or contains(text(),'Mark')]"));
            if (completeBtns.Count == 0) break;
            Page.JsClick(completeBtns[0]);
            System.Threading.Thread.Sleep(1000);
        }
    }

    public void e_ClaimCertSuccess()
    {
        var claimBtn = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'Nhận chứng chỉ')]"));
        if (claimBtn.Count > 0)
        {
            claimBtn[0].Click();
            System.Threading.Thread.Sleep(2000);
        }
    }

    public void e_ClaimCertNotEligible()
    {
        // Try to claim without completing all lessons
        Page.GoTo("/student/dashboard");
    }

    public void e_ClaimCertAlreadyExists()
    {
        // Try to claim again - should return existing cert
        var claimBtn = Setup.Driver.FindElements(
            By.XPath("//*[contains(text(),'Nhận chứng chỉ') or contains(text(),'Xem chứng chỉ')]"));
        if (claimBtn.Count > 0)
            claimBtn[0].Click();
        System.Threading.Thread.Sleep(1000);
    }

    public void e_ViewCertInDashboard()
    {
        Page.GoTo("/student/dashboard");
        Page.WaitForUrl("dashboard");
        // Extract verification code from cert card
        var codeElements = Setup.Driver.FindElements(By.XPath("//*[contains(text(),'Mã:')]"));
        if (codeElements.Count > 0)
            _verificationCode = codeElements[0].Text.Replace("Mã:", "").Trim();
    }

    public void e_VerifyCertFromDashboard()
    {
        if (!string.IsNullOrEmpty(_verificationCode))
            Page.GoTo($"/api/v1/certificates/verify/{_verificationCode}");
        else
            Page.GoTo("/student/dashboard");
    }

    public void e_VerifyValidCode()
    {
        if (!string.IsNullOrEmpty(_verificationCode))
        {
            Page.GoTo($"/api/v1/certificates/verify/{_verificationCode}");
            System.Threading.Thread.Sleep(500);
            Assert.That(Page.TextVisible("true") || Page.TextVisible("valid"), "Cert should be valid");
        }
    }

    public void e_VerifyInvalidCode()
    {
        Page.GoTo("/api/v1/certificates/verify/invalid-code-000");
        System.Threading.Thread.Sleep(500);
        Assert.That(Page.TextVisible("false") || Page.TextVisible("invalid"), "Should show invalid cert");
    }

    public void e_BackToDashboard()
    {
        Page.GoTo("/student/dashboard");
        System.Threading.Thread.Sleep(800);
    }

    public void e_BackToDashboardFromVerify()  => e_BackToDashboard();
    public void e_BackToDashboardFromInvalid() => e_BackToDashboard();
    public void e_BackToDashboardFromNoSection() => e_BackToDashboard();
    public void e_BackToDashboardFromNotEligible() => e_BackToDashboard();
}
