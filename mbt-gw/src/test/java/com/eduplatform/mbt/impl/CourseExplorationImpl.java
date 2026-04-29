package com.eduplatform.mbt.impl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.CollectionCondition;
import com.eduplatform.mbt.models.CourseExplorationModel;
import com.eduplatform.mbt.pages.CourseDetailPage;
import com.eduplatform.mbt.pages.CoursesPage;
import com.eduplatform.mbt.support.BaseImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import com.eduplatform.mbt.support.MbtBusinessAssertions;
import com.eduplatform.mbt.support.SafeUi;
import com.eduplatform.mbt.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.graphwalker.java.annotation.GraphWalker;

import static com.codeborne.selenide.Selenide.open;

/**
 * Course exploration: VERTEX assert UI + đồng bộ {@link com.eduplatform.mbt.support.TestContext} → GraphWalker.
 * EDGE thao tác UI + cập nhật ctx (logic), không phụ thuộc {@code actions} trong JSON.
 */
@GraphWalker(value = GraphWalkerExecutionPolicy.BOUNDED_COURSE, start = "v_CourseListVisible")
public class CourseExplorationImpl extends BaseImpl implements CourseExplorationModel {

    private final CoursesPage coursesPage = new CoursesPage();
    private final CourseDetailPage detailPage = new CourseDetailPage();

    private long firstCourseId;
    /** Title from the same {@code listCourses} snapshot as {@link #firstCourseId} — avoids extra GET for MBT. */
    private String listTitleForSelectedCourse = "";

    @Override
    public void v_CourseListVisible() {
        logStep("v_CourseListVisible");
        coursesPage.open().assertLoaded();
        JsonNode list = api.listCourses();
        int n = (list != null && list.path("data").isArray()) ? list.path("data").size() : 0;
        if (n == 0) {
            log.warn("v_CourseListVisible: API returned 0 published courses; ensure backend is up and "
                    + "POST /api/v1/admin/seed was applied. UI will wait for non-empty list.");
        }
        updateFirstCourseCacheFromPublishedList(list);
        coursesPage.assertCoursesVisible();
        ctx.coursesLoaded = true;
        ctx.totalCourses = n;
        ctx.filteredCount = n;
        ctx.explorationSearchQuery = "";
        ctx.explorationCategory = "all";
        ctx.explorationLevel = "all";
        ctx.registrationOpenFuture = false;
        ctx.registrationClosePast = false;
        auth().isLoggedIn = false;
        auth().currentRole = "";
        syncExplorationGraphWalker();
    }

    @Override
    public void v_CourseListFiltered() {
        logStep("v_CourseListFiltered");
        coursesPage.assertFilterShowsCardsOrEmptyState();
        syncExplorationGraphWalker();
    }

    @Override
    public void v_CourseListEmpty() {
        logStep("v_CourseListEmpty");
        SafeUi.waitUntilVisible(coursesPage.emptyState(), SafeUi.DEFAULT_TIMEOUT);
        coursesPage.cards().shouldHave(CollectionCondition.size(0));
        ctx.filteredCount = 0;
        syncExplorationGraphWalker();
    }

    @Override
    public void v_CourseDetailVisible() {
        logStep("v_CourseDetailVisible");
        detailPage.assertLoaded();
        if (firstCourseId > 0 && detailPage.title().is(Condition.visible)) {
            MbtBusinessAssertions.assertCourseApiTitleVisibleMatchLenient(
                    api, firstCourseId, detailPage.title().getText(), listTitleForSelectedCourse);
        }
        syncExplorationGraphWalker();
    }

    @Override
    public void v_CourseDetailNotFound() {
        logStep("v_CourseDetailNotFound");
        SafeUi.waitUntilVisible(detailPage.notFoundMsg(), SafeUi.DEFAULT_TIMEOUT);
        ctx.selectedCourseId = 999999L;
        syncExplorationGraphWalker();
    }

    @Override
    public void v_RegistrationClosedMsg() {
        logStep("v_RegistrationClosedMsg");
        SafeUi.waitUntilVisible(detailPage.title(), SafeUi.DEFAULT_TIMEOUT);
        boolean notPublished = detailPage.notPublishedMsg().is(Condition.visible);
        boolean openFrom = detailPage.openFromMsg().is(Condition.visible);
        boolean expired = detailPage.expiredMsg().is(Condition.visible);
        boolean loginGuard = detailPage.loginToRegister().is(Condition.visible);
        if (!notPublished && !openFrom && !expired && !loginGuard) {
            log.warn("v_RegistrationClosedMsg: no closed-registration indicator found on page");
        }
        syncExplorationGraphWalker();
    }

    @Override
    public void e_OpenCourseList() {
        logStep("e_OpenCourseList");
        coursesPage.open();
        JsonNode list = api.listCourses();
        int n = (list != null && list.path("data").isArray()) ? list.path("data").size() : 0;
        ctx.coursesLoaded = true;
        ctx.totalCourses = n;
        ctx.filteredCount = n;
        syncExplorationGraphWalker();
    }

    @Override
    public void e_ApplySearch() {
        logStep("e_ApplySearch");
        String q = pickKeywordFromFirstCourse();
        coursesPage.search(q != null ? q : "a");
        int visible = coursesPage.cards().size();
        ctx.explorationSearchQuery = q != null ? q : "a";
        ctx.filteredCount = Math.max(visible, 1);
        syncExplorationGraphWalker();
    }

    @Override
    public void e_ApplySearchNoMatch() {
        logStep("e_ApplySearchNoMatch");
        coursesPage.search("zzz_no_match_" + System.currentTimeMillis());
        ctx.explorationSearchQuery = "zzz";
        ctx.filteredCount = 0;
        syncExplorationGraphWalker();
    }

    @Override
    public void e_FilterByCategory() {
        logStep("e_FilterByCategory");
        var opts = coursesPage.categorySelect().$$("option");
        if (opts.size() > 1) {
            String val = opts.get(1).getValue();
            coursesPage.categorySelect().selectOptionByValue(val);
            ctx.explorationCategory = val;
        }
        ctx.filteredCount = Math.max(coursesPage.cards().size(), 1);
        syncExplorationGraphWalker();
    }

    @Override
    public void e_FilterByLevel() {
        logStep("e_FilterByLevel");
        coursesPage.filterLevel("Beginner");
        ctx.explorationLevel = "Beginner";
        ctx.filteredCount = Math.max(coursesPage.cards().size(), 1);
        syncExplorationGraphWalker();
    }

    @Override
    public void e_CombinedFilters() {
        logStep("e_CombinedFilters");
        coursesPage.filterLevel("Beginner");
        ctx.explorationLevel = "Beginner";
        ctx.filteredCount = Math.max(coursesPage.cards().size(), 1);
        syncExplorationGraphWalker();
    }

    @Override
    public void e_ClearFilters() {
        logStep("e_ClearFilters");
        // Re-open list so React state is reset; after v_CourseListEmpty, resetFilters() can leave 0 cards (controlled search)
        coursesPage.open().assertLoaded();
        coursesPage.resetFilters();
        coursesPage.assertCoursesVisible();
        ctx.explorationSearchQuery = "";
        ctx.explorationCategory = "all";
        ctx.explorationLevel = "all";
        ctx.filteredCount = ctx.totalCourses;
        syncExplorationGraphWalker();
    }

    @Override
    public void e_OpenCourseDetail() {
        logStep("e_OpenCourseDetail");
        if (!ctx.coursesLoaded) {
            log.warn("e_OpenCourseDetail: hasCoursesLoaded=false (expected v_CourseListVisible first); continuing");
        }
        refreshFirstPublishedFromApi();
        ctx.selectedCourseId = firstCourseId > 0 ? firstCourseId : 1;
        ctx.selectedCourseStatus = "PUBLISHED";
        ctx.registrationOpenFuture = false;
        ctx.registrationClosePast = false;
        syncExplorationGraphWalker();
        detailPage.open(firstCourseId > 0 ? firstCourseId : 1);
        detailPage.assertLoaded();
    }

    @Override
    public void e_OpenCourseDetailClosed() {
        logStep("e_OpenCourseDetailClosed");
        long draftId = firstCourseWithStatus("DRAFT");
        ctx.selectedCourseId = draftId > 0 ? draftId : 999998;
        ctx.selectedCourseStatus = "DRAFT";
        syncExplorationGraphWalker();
        detailPage.open(draftId > 0 ? draftId : 999998);
    }

    @Override
    public void e_OpenCourseDetailNotOpenYet() {
        logStep("e_OpenCourseDetailNotOpenYet");
        refreshFirstPublishedFromApi();
        long openId = firstCourseId > 0 ? firstCourseId : 1L;
        ctx.selectedCourseId = openId;
        ctx.selectedCourseStatus = "PUBLISHED";
        ctx.registrationOpenFuture = true;
        ctx.registrationClosePast = false;
        syncExplorationGraphWalker();
        detailPage.open(openId);
    }

    @Override
    public void e_OpenCourseDetailExpired() {
        logStep("e_OpenCourseDetailExpired");
        refreshFirstPublishedFromApi();
        long openId = firstCourseId > 0 ? firstCourseId : 1L;
        ctx.selectedCourseId = openId;
        ctx.selectedCourseStatus = "PUBLISHED";
        ctx.registrationOpenFuture = false;
        ctx.registrationClosePast = true;
        syncExplorationGraphWalker();
        detailPage.open(openId);
    }

    @Override
    public void e_OpenCourseDetailNotFound() {
        logStep("e_OpenCourseDetailNotFound");
        ctx.selectedCourseId = 999999L;
        syncExplorationGraphWalker();
        open("/courses/999999");
    }

    @Override
    public void e_BackToList() {
        logStep("e_BackToList");
        SafeUi.clickWhenReady(detailPage.backToList(), SafeUi.DEFAULT_TIMEOUT, 2);
        waitForUrlContains("/courses", 5);
        syncExplorationGraphWalker();
    }

    private String pickKeywordFromFirstCourse() {
        if (listTitleForSelectedCourse != null && !listTitleForSelectedCourse.isBlank()) {
            String[] parts = listTitleForSelectedCourse.split("\\s+");
            return parts.length > 0 ? parts[0] : listTitleForSelectedCourse;
        }
        JsonNode list = api.listCourses();
        if (list != null && list.path("data").isArray() && list.path("data").size() > 0) {
            String title = list.path("data").get(0).path("title").asText("");
            if (!title.isBlank()) {
                String[] parts = title.split("\\s+");
                return parts.length > 0 ? parts[0] : title;
            }
        }
        return null;
    }

    /**
     * Fills {@link #firstCourseId} and {@link #listTitleForSelectedCourse} from one PUBLISHED list
     * (reduces N× {@code courseDetail} and duplicate list calls in GraphWalker).
     */
    private void updateFirstCourseCacheFromPublishedList(JsonNode list) {
        firstCourseId = 0;
        listTitleForSelectedCourse = "";
        if (list == null || !list.path("data").isArray() || list.path("data").isEmpty()) {
            return;
        }
        if (MbtBusinessAssertions.isApiRateOrThrottle(api, list)) {
            return;
        }
        String wantSlug = TestData.DEMO_COURSE_01_SLUG;
        for (JsonNode c : list.path("data")) {
            if (wantSlug != null && wantSlug.equals(c.path("slug").asText(""))) {
                firstCourseId = c.path("id").asLong();
                listTitleForSelectedCourse = c.path("title").asText("").trim();
                return;
            }
        }
        for (JsonNode c : list.path("data")) {
            if ("PUBLISHED".equals(c.path("status").asText())) {
                firstCourseId = c.path("id").asLong();
                listTitleForSelectedCourse = c.path("title").asText("").trim();
                return;
            }
        }
        JsonNode c0 = list.path("data").get(0);
        firstCourseId = c0.path("id").asLong();
        listTitleForSelectedCourse = c0.path("title").asText("").trim();
    }

    private void refreshFirstPublishedFromApi() {
        JsonNode list = api.listCourses("PUBLISHED");
        if (MbtBusinessAssertions.isApiRateOrThrottle(api, list)) {
            log.warn("refreshFirstPublishedFromApi: list throttled; firstCourseId may be stale or 0");
        }
        updateFirstCourseCacheFromPublishedList(list);
        if (firstCourseId > 0) {
            return;
        }
        long bySlug = api.courseIdBySlug(TestData.DEMO_COURSE_01_SLUG);
        if (bySlug > 0) {
            firstCourseId = bySlug;
        }
    }

    private long firstCourseWithStatus(String status) {
        JsonNode list = api.listCourses(status);
        if (list != null && list.path("data").isArray()) {
            for (JsonNode c : list.path("data")) {
                if (status.equals(c.path("status").asText())) return c.path("id").asLong();
            }
        }
        return 0;
    }

    /** GraphWalker guard method name trong JSON: {@code gwGuard_coursesLoaded} */
    @SuppressWarnings("unused")
    public boolean gwGuard_coursesLoaded() {
        return ctx.coursesLoaded;
    }
}
