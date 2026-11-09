package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.util.DueBucket;
import com.example.assignmenttracker.util.DueDates;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Slice 4 home list (design.md §2.1 adapter order, §3.2 buckets, §3.3 Done, §3.4 summary):
 * SUMMARY (or CAUGHT_UP) → each non-empty bucket's header and rows → DONE_HEADER → done rows.
 */
public class HomeItemsTest {

    private static final TimeZone TZ = TimeZone.getTimeZone("America/Chicago");
    /** Sunday, Nov 8 2026, 10:00 in Chicago (design.md §12 sample "today"). */
    private static final long NOW;

    static {
        Calendar c = new GregorianCalendar(TZ);
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 8, 10, 0, 0);
        NOW = c.getTimeInMillis();
    }

    private static long nov(int d) {
        return DueDates.startOfLocalDay(2026, 11, d, TZ);
    }

    private static Assignment open(String id, Long dueAt, long createdAt) {
        Assignment a = Assignment.newTodo(id, "T" + id, createdAt);
        a.dueAt = dueAt;
        return a;
    }

    private static Assignment done(String id, long completedAt) {
        Assignment a = Assignment.newTodo(id, "T" + id, 1);
        a.status = Assignment.STATUS_DONE;
        a.completedAt = completedAt;
        return a;
    }

    private static List<HomeItem> build(boolean expanded, Assignment... list) {
        return HomeItems.build(Arrays.asList(list), expanded, NOW, TZ);
    }

    private static List<String> rowIds(List<HomeItem> items) {
        List<String> out = new ArrayList<>();
        for (HomeItem i : items) if (i.getType() == HomeItem.TYPE_ROW) out.add(i.getAssignment().id);
        return out;
    }

    private static List<Integer> types(List<HomeItem> items) {
        List<Integer> out = new ArrayList<>();
        for (HomeItem i : items) out.add(i.getType());
        return out;
    }

    // ---- empty, caught up ----

    @Test
    public void noAssignmentsGivesASingleEmptyItem() {
        List<HomeItem> items = HomeItems.build(Collections.emptyList(), false, NOW, TZ);
        assertEquals(1, items.size());
        assertEquals(HomeItem.TYPE_EMPTY, items.get(0).getType());
        assertEquals(HomeItem.TYPE_EMPTY, HomeItems.build(null, false, NOW, TZ).get(0).getType());
    }

    @Test
    public void everythingDoneShowsAllCaughtUpThenTheCollapsedDoneHeader() {
        List<HomeItem> items = build(false, done("d1", 1), done("d2", 2));
        assertEquals(Arrays.asList(HomeItem.TYPE_CAUGHT_UP, HomeItem.TYPE_DONE_HEADER), types(items));
        assertEquals(2, items.get(0).getCount());
        assertEquals(2, items.get(1).getDoneCount());
        assertFalse(items.get(1).isExpanded());
    }

    // ---- buckets and order ----

    @Test
    public void openItemsGoUnderHeadersInBucketOrderAndEmptyBucketsAreHidden() {
        List<HomeItem> items = build(false,
                open("later", nov(24), 1), open("nodate", null, 2), open("over", nov(6), 3),
                open("today", nov(8), 4), open("week", nov(11), 5));
        assertEquals(HomeItem.TYPE_SUMMARY, items.get(0).getType());
        List<DueBucket> headers = new ArrayList<>();
        for (HomeItem i : items) if (i.getType() == HomeItem.TYPE_SECTION_HEADER) headers.add(i.getBucket());
        assertEquals(Arrays.asList(DueBucket.OVERDUE, DueBucket.TODAY, DueBucket.THIS_WEEK,
                DueBucket.LATER, DueBucket.NO_DATE), headers);
        assertEquals(Arrays.asList("over", "today", "week", "later", "nodate"), rowIds(items));

        List<HomeItem> some = build(false, open("today", nov(8), 1), open("nodate", null, 2));
        assertEquals(Arrays.asList(HomeItem.TYPE_SUMMARY, HomeItem.TYPE_SECTION_HEADER, HomeItem.TYPE_ROW,
                HomeItem.TYPE_SECTION_HEADER, HomeItem.TYPE_ROW), types(some));
        assertEquals(DueBucket.TODAY, some.get(1).getBucket());
        assertEquals(DueBucket.NO_DATE, some.get(3).getBucket());
    }

    /** tasks.md: ORDER BY dueAt IS NULL, dueAt; ties by createdAt then id so the order is stable. */
    @Test
    public void openItemsSortByDueDateAscendingWithNoDateLast() {
        List<HomeItem> items = build(false,
                open("n2", null, 20), open("w2", nov(12), 2), open("o1", nov(1), 9),
                open("w1", nov(10), 7), open("o2", nov(6), 1), open("n1", null, 10),
                open("tieB", nov(13), 5), open("tieA", nov(13), 5), open("tieEarly", nov(13), 4));
        assertEquals(Arrays.asList("o1", "o2", "w1", "w2", "tieEarly", "tieA", "tieB", "n1", "n2"), rowIds(items));
    }

    @Test
    public void sectionHeadersCarryTheirCount() {
        List<HomeItem> items = build(false, open("a", nov(6), 1), open("b", nov(7), 2), open("c", null, 3));
        assertEquals(DueBucket.OVERDUE, items.get(1).getBucket());
        assertEquals(2, items.get(1).getCount());
        assertEquals(DueBucket.NO_DATE, items.get(4).getBucket());
        assertEquals(1, items.get(4).getCount());
    }

    @Test
    public void eachBucketIsItsOwnCard() {
        List<HomeItem> items = build(false,
                open("o1", nov(6), 1), open("o2", nov(7), 2), open("o3", nov(7), 3), open("t", nov(8), 4));
        assertEquals(RowPosition.TOP, items.get(2).getPosition());
        assertEquals(RowPosition.MIDDLE, items.get(3).getPosition());
        assertEquals(RowPosition.BOTTOM, items.get(4).getPosition());
        assertEquals(RowPosition.SINGLE, items.get(6).getPosition());
    }

    @Test
    public void rowsCarryDaysFromTodayAndDoneRowsCarryNone() {
        List<HomeItem> items = build(true, open("o", nov(6), 1), open("t", nov(9), 2),
                open("n", null, 3), done("d", 5));
        HomeItem over = items.get(2);
        assertEquals(Integer.valueOf(-2), over.getDueDays());
        assertEquals(DueLabel.OVERDUE, DueLabel.of(over));
        HomeItem tomorrow = items.get(4);
        assertEquals(Integer.valueOf(1), tomorrow.getDueDays());
        assertEquals(DueLabel.TOMORROW, DueLabel.of(tomorrow));
        assertNull(items.get(6).getDueDays());
        assertEquals(DueLabel.NONE, DueLabel.of(items.get(6)));
        HomeItem doneRow = items.get(8);
        assertEquals("d", doneRow.getAssignment().id);
        assertEquals(DueLabel.NONE, DueLabel.of(doneRow));
    }

    @Test
    public void dueLabelKinds() {
        assertEquals(DueLabel.NONE, DueLabel.forDays(null));
        assertEquals(DueLabel.OVERDUE, DueLabel.forDays(-1));
        assertEquals(DueLabel.TODAY, DueLabel.forDays(0));
        assertEquals(DueLabel.TOMORROW, DueLabel.forDays(1));
        assertEquals(DueLabel.DATE, DueLabel.forDays(2));
        assertEquals(DueLabel.DATE, DueLabel.forDays(40));
    }

    // ---- Done (kept from Slice 3) ----

    @Test
    public void doneItemsGoUnderACollapsedDoneHeaderBelowTheOpenOnes() {
        List<HomeItem> items = build(false, open("o1", null, 3), done("d1", 10), open("o2", null, 4));
        HomeItem header = items.get(items.size() - 1);
        assertEquals(HomeItem.TYPE_DONE_HEADER, header.getType());
        assertEquals(1, header.getDoneCount());
        assertFalse(header.isExpanded());
        assertEquals(Arrays.asList("o1", "o2"), rowIds(items));
    }

    @Test
    public void expandedDoneShowsItsOwnCardNewestCompletedFirst() {
        List<HomeItem> items = build(true, open("o1", null, 3), done("old", 10), done("new", 30), done("mid", 20));
        int h = types(items).indexOf(HomeItem.TYPE_DONE_HEADER);
        assertTrue(items.get(h).isExpanded());
        assertEquals("new", items.get(h + 1).getAssignment().id);
        assertEquals(RowPosition.TOP, items.get(h + 1).getPosition());
        assertEquals("mid", items.get(h + 2).getAssignment().id);
        assertEquals("old", items.get(h + 3).getAssignment().id);
        assertEquals(RowPosition.BOTTOM, items.get(h + 3).getPosition());
    }

    @Test
    public void noDoneItemsMeansNoDoneHeader() {
        assertFalse(types(build(true, open("o", null, 1))).contains(HomeItem.TYPE_DONE_HEADER));
    }

    // ---- summary (design.md §3.4 content rule) ----

    private static List<String> parts(HomeSummary s) {
        List<String> out = new ArrayList<>();
        for (HomeSummary.Part p : s.parts()) out.add(p.kind + ":" + p.count);
        return out;
    }

    @Test
    public void summaryCountsOverdueTodayWeekOpenAndDone() {
        HomeSummary s = build(false, open("o1", nov(6), 1), open("o2", nov(7), 2), open("t1", nov(8), 3),
                open("t2", nov(8), 4), open("t3", nov(8), 5), open("w", nov(9), 6), open("l", nov(30), 7),
                open("n", null, 8), done("d", 9)).get(0).getSummary();
        assertEquals(2, s.overdue);
        assertEquals(3, s.today);
        assertEquals(1, s.thisWeek);
        assertEquals(8, s.open);
        assertEquals(1, s.done);
        assertEquals(Arrays.asList("OVERDUE:2", "TODAY:3"), parts(s));
    }

    @Test
    public void summaryRuleOverdueAndTodayOnlyWhenNonZero() {
        assertEquals(Arrays.asList("OVERDUE:1"), parts(new HomeSummary(1, 0, 4, 9, 0)));
        assertEquals(Arrays.asList("TODAY:2"), parts(new HomeSummary(0, 2, 4, 9, 0)));
    }

    @Test
    public void summaryFallsBackToThisWeekThenToTheOpenCount() {
        assertEquals(Arrays.asList("WEEK:4"), parts(new HomeSummary(0, 0, 4, 9, 0)));
        assertEquals(Arrays.asList("NOTHING_THIS_WEEK:9"), parts(new HomeSummary(0, 0, 0, 9, 3)));
        assertTrue("nothing open has no parts (caught up instead)", new HomeSummary(0, 0, 0, 0, 3).parts().isEmpty());
    }

    /** The midnight edge through the whole list: the same row moves from This week to Today. */
    @Test
    public void rebuildingAfterMidnightMovesTheRowToToday() {
        Assignment a = open("a", nov(9), 1);
        long beforeMidnight = DueDates.startOfLocalDay(2026, 11, 9, TZ) - 1;
        long afterMidnight = beforeMidnight + 1;
        List<HomeItem> before = HomeItems.build(List.of(a), false, beforeMidnight, TZ);
        List<HomeItem> after = HomeItems.build(List.of(a), false, afterMidnight, TZ);
        assertEquals(DueBucket.THIS_WEEK, before.get(1).getBucket());
        assertEquals(DueBucket.TODAY, after.get(1).getBucket());
        assertEquals(1, after.get(0).getSummary().today);
        assertFalse("the row must rebind (Due tomorrow → Due today)", before.get(2).sameContentAs(after.get(2)));
    }

    // ---- diffing ----

    @Test
    public void sameIdAndContentAreDiffEqual() {
        HomeItem x = build(false, open("a", null, 1)).get(2);
        HomeItem y = build(false, open("a", null, 1)).get(2);
        assertTrue(x.sameItemAs(y));
        assertTrue(x.sameContentAs(y));
        Assignment changed = open("a", null, 1);
        changed.title = "Changed";
        HomeItem z = build(false, changed).get(2);
        assertTrue(x.sameItemAs(z));
        assertFalse(x.sameContentAs(z));
    }

    @Test
    public void courseAndDueChangesAreContentChanges() {
        Assignment b = open("a", null, 1);
        b.course = "Biology 101";
        assertFalse(build(false, open("a", null, 1)).get(2).sameContentAs(build(false, b).get(2)));
        HomeItem week = build(false, open("a", nov(10), 1)).get(2);
        HomeItem week2 = build(false, open("a", nov(11), 1)).get(2);
        assertTrue(week.sameItemAs(week2));
        assertFalse(week.sameContentAs(week2));
    }

    @Test
    public void headersAreIdentifiedByBucketAndSummaryIsOneItem() {
        HomeItem over = build(false, open("a", nov(6), 1)).get(1);
        HomeItem today = build(false, open("a", nov(8), 1)).get(1);
        assertFalse(over.sameItemAs(today));
        HomeItem over2 = build(false, open("a", nov(6), 1), open("b", nov(6), 2)).get(1);
        assertTrue(over.sameItemAs(over2));
        assertFalse("count changed", over.sameContentAs(over2));
        HomeItem s1 = build(false, open("a", nov(6), 1)).get(0);
        HomeItem s2 = build(false, open("a", nov(8), 1)).get(0);
        assertTrue(s1.sameItemAs(s2));
        assertFalse(s1.sameContentAs(s2));
    }

    @Test
    public void removingTheBottomRowRebindsTheNewBottom() {
        List<HomeItem> before = build(false, open("a", null, 1), open("b", null, 2), open("c", null, 3));
        List<HomeItem> after = build(false, open("a", null, 1), open("b", null, 2));
        assertEquals(RowPosition.MIDDLE, before.get(3).getPosition());
        assertEquals(RowPosition.BOTTOM, after.get(3).getPosition());
        assertFalse(before.get(3).sameContentAs(after.get(3)));
    }

    @Test
    public void rowItemsKeepTheEntity() {
        Assignment a = open("a", null, 1);
        assertSame(a, HomeItems.build(List.of(a), false, NOW, TZ).get(2).getAssignment());
    }

    // ---- scrolling to a newly added row ----

    @Test
    public void aNewOpenRowInTheFirstBucketScrollsToTheTop() {
        List<HomeItem> before = build(false, open("a", nov(10), 1));
        List<HomeItem> after = build(false, open("n", nov(6), 2), open("a", nov(10), 1));
        assertEquals(0, HomeItems.scrollTargetForNewOpenRow(before, after));
    }

    @Test
    public void aNewRowFurtherDownTargetsItsHeaderWhenItOpensACard() {
        List<HomeItem> before = build(false, open("a", nov(6), 1));
        List<HomeItem> after = build(false, open("a", nov(6), 1), open("n", null, 2));
        // summary, Overdue, a, No date, n
        assertEquals(3, HomeItems.scrollTargetForNewOpenRow(before, after));
        List<HomeItem> after2 = build(false, open("a", nov(6), 1), open("n", nov(7), 2));
        // summary, Overdue, a(top), n(bottom): n is the target itself
        assertEquals(3, HomeItems.scrollTargetForNewOpenRow(before, after2));
    }

    @Test
    public void launchesExpandsAndDoneRowsNeverScroll() {
        List<HomeItem> one = build(false, open("a", null, 1), done("d", 1));
        assertEquals("first load", -1, HomeItems.scrollTargetForNewOpenRow(Collections.emptyList(), one));
        assertEquals("expanding Done", -1,
                HomeItems.scrollTargetForNewOpenRow(one, build(true, open("a", null, 1), done("d", 1))));
        assertEquals("a removal", -1, HomeItems.scrollTargetForNewOpenRow(one, build(false, done("d", 1))));
    }
}
