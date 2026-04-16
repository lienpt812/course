using Lms.MbtWeb.Helpers;
using NUnit.Framework;
using OpenQA.Selenium;

namespace Lms.MbtWeb.Models;

/// <summary>
/// CourseCatalog.json — Duyệt khóa học công khai: trang chủ, filter danh mục/level, tìm kiếm, chi tiết.
/// </summary>
public class CourseCatalog
{
    private static PageHelper Page => Setup.Page;
    private int _openedCourseId;

    public void setUpModel()
    {
        Page.ClearSession();
        Page.GoTo("/");
        PageHelper.Wait(700);
    }

    // ── Vertices ──────────────────────────────────────────────────────────────

    public void v_HomePage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("localhost:3000") || Page.CurrentUrl.Contains("/courses"),
            Is.True, "Should be on home page");
        Assert.That(
             Page.TextVisible("Course") || Page.ElementExists(By.CssSelector("a[href*='/courses']")),
            Is.True, "Home page should have course links");
    }

    public void v_CoursesPage()
    {
        Assert.That(Page.CurrentUrl.Contains("/courses"), Is.True, "Should be on courses page");
        Assert.That(
            Page.ElementExists(By.CssSelector("[class*='course'], [class*='card']"))  || Page.TextVisible("Course"),
            Is.True, "Course list must be visible");
    }

    public void v_FilteredByCategory()
    {
        Assert.That(Page.CurrentUrl.Contains("/courses"), Is.True, "Should stay on courses page after filter");
    }

    public void v_FilteredByLevel()
    {
        Assert.That(Page.CurrentUrl.Contains("/courses"), Is.True, "Should stay on courses page after level filter");
    }

    public void v_SearchResults()
    {
        Assert.That(Page.CurrentUrl.Contains("/courses"), Is.True, "Should stay on courses page in search");
    }

    public void v_CourseDetailPage()
    {
        Assert.That(
            Page.CurrentUrl.Contains("/courses/")  || Page.TextVisible("Register")  || Page.TextVisible("Price")  || Page.TextVisible("Description"),
            Is.True, "Should be on course detail page");
    }

    // ── Edges ─────────────────────────────────────────────────────────────────

    public void e_GoToCoursesFromHome()
    {
        var link = Page.FindOrNull(By.CssSelector("a[href='/courses'], a[href*='/courses']"));
        if (link != null) { Page.JsClick(link); PageHelper.Wait(800); }
        else { Page.GoTo("/courses"); PageHelper.Wait(800); }
    }

    public void e_OpenFeaturedCourse()
    {
        Page.GoTo("/courses");
        PageHelper.Wait(800);
        var id = ApiHelper.GetPublishedCourseId();
        if (id > 0) { _openedCourseId = id; Page.GoTo($"/courses/{id}"); PageHelper.Wait(800); }
    }

    public void e_FilterByCategory()
    {
        Page.GoTo("/courses");
        PageHelper.Wait(600);
        var selects = Page.FindAll(By.CssSelector("select, [class*='select'], [class*='filter']"));
        if (selects.Count > 0)
        {
            try
            {
                var sel = new OpenQA.Selenium.Support.UI.SelectElement(selects[0]);
                if (sel.Options.Count > 1) { sel.SelectByIndex(1); PageHelper.Wait(600); }
            }
            catch
            {
                Page.JsClick(selects[0]);
                PageHelper.Wait(400);
                var options = Page.FindAll(By.CssSelector("[role='option'], option"));
                if (options.Count > 1) { Page.JsClick(options[1]); PageHelper.Wait(600); }
            }
        }
        else
        {
            var catBtns = Page.FindAll(
                By.XPath("//button[contains(text(),'Backend') or contains(text(),'Frontend') or contains(text(),'Testing')]"));
            if (catBtns.Count > 0) { Page.JsClick(catBtns[0]); PageHelper.Wait(600); }
        }
    }

    public void e_FilterByLevel()
    {
        Page.GoTo("/courses");
        PageHelper.Wait(600);
        var levelBtns = Page.FindAll(
            By.XPath("//button[contains(text(),'Beginner') or contains(text(),'Intermediate') or contains(text(),'Advanced')]"));
        if (levelBtns.Count > 0) { Page.JsClick(levelBtns[0]); PageHelper.Wait(600); }
        else
        {
            var selects = Page.FindAll(By.TagName("select"));
            if (selects.Count > 1)
            {
                try
                {
                    var sel = new OpenQA.Selenium.Support.UI.SelectElement(selects[1]);
                    if (sel.Options.Count > 1) { sel.SelectByIndex(1); PageHelper.Wait(600); }
                }
                catch { }
            }
        }
    }

    public void e_ClearCategoryFilter()
    {
        var clearBtn = Page.FindOrNull(
            By.XPath("//button[contains(text(),'All') or contains(text(),'Reset')]"));
        if (clearBtn != null) { Page.JsClick(clearBtn); PageHelper.Wait(600); }
        else { Page.GoTo("/courses"); PageHelper.Wait(600); }
    }

    public void e_ClearLevelFilter()
    {
        var clearBtn = Page.FindOrNull(
            By.XPath("//button[contains(text(),'All') or contains(text(),'Reset')]"));
        if (clearBtn != null) { Page.JsClick(clearBtn); PageHelper.Wait(600); }
        else { Page.GoTo("/courses"); PageHelper.Wait(600); }
    }

    public void e_SearchCourseByKeyword()
    {
        var searchInput = Page.FindOrNull(
            By.CssSelector("input[type='search'], input[placeholder*='Search'], input[name='search'], input[name='q']"));
        if (searchInput != null)
        {
            searchInput.Clear();
            searchInput.SendKeys("Python");
            searchInput.SendKeys(Keys.Return);
            PageHelper.Wait(800);
        }
    }

    public void e_ClearSearchResults()
    {
        var clearBtn = Page.FindOrNull(
            By.XPath("//button[@aria-label='clear' or contains(text(),'Clear')]"));
        if (clearBtn != null) { Page.JsClick(clearBtn); PageHelper.Wait(600); }
        else
        {
            var searchInput = Page.FindOrNull(By.CssSelector("input[type='search'], input[name='search'], input[name='q']"));
            if (searchInput != null) { searchInput.Clear(); searchInput.SendKeys(Keys.Return); PageHelper.Wait(600); }
            else { Page.GoTo("/courses"); PageHelper.Wait(600); }
        }
    }

    public void e_OpenCourseFromList()
    {
        var links = Page.FindAll(By.CssSelector("a[href*='/courses/']"));
        if (links.Count > 0)
        {
            var href = links[0].GetAttribute("href") ?? "";
            var m    = System.Text.RegularExpressions.Regex.Match(href, @"/courses/(\d+)");
            if (m.Success) _openedCourseId = int.Parse(m.Groups[1].Value);
            Page.JsClick(links[0]);
            PageHelper.Wait(800);
        }
        else
        {
            var id = ApiHelper.GetPublishedCourseId();
            if (id > 0) { _openedCourseId = id; Page.GoTo($"/courses/{id}"); PageHelper.Wait(800); }
        }
    }

    public void e_OpenCourseFromFiltered() => OpenFirstCourseLink();
    public void e_OpenCourseFromSearch()   => OpenFirstCourseLink();

    private void OpenFirstCourseLink()
    {
        var links = Page.FindAll(By.CssSelector("a[href*='/courses/']"));
        if (links.Count > 0) { Page.JsClick(links[0]); PageHelper.Wait(800); }
        else
        {
            var id = ApiHelper.GetPublishedCourseId();
            if (id > 0) { _openedCourseId = id; Page.GoTo($"/courses/{id}"); PageHelper.Wait(800); }
        }
    }

    public void e_BackToCatalog()
    {
        var btn = Page.FindOrNull(
            By.XPath("//a[not(contains(@href,'/courses/'))][contains(@href,'/courses')] | " +
                     "//button[contains(text(),'Quay') or contains(text(),'Back')]"));
        if (btn != null) { Page.JsClick(btn); PageHelper.Wait(700); }
        else { Page.GoTo("/courses"); PageHelper.Wait(700); }
    }
}
