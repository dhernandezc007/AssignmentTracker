package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.example.assignmenttracker.data.entity.Assignment;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** design.md §2: one EMPTY item when there is nothing, otherwise rows grouped in one card. */
public class HomeItemsTest {

    @Test
    public void noAssignmentsGivesASingleEmptyItem() {
        List<HomeItem> items = HomeItems.build(Collections.emptyList());
        assertEquals(1, items.size());
        assertEquals(HomeItem.TYPE_EMPTY, items.get(0).getType());
    }

    @Test
    public void nullIsTreatedAsEmpty() {
        assertEquals(HomeItem.TYPE_EMPTY, HomeItems.build(null).get(0).getType());
    }

    @Test
    public void oneAssignmentIsASingleCard() {
        Assignment a = Assignment.newTodo("1", "A", 10L);
        List<HomeItem> items = HomeItems.build(List.of(a));
        assertEquals(1, items.size());
        assertEquals(HomeItem.TYPE_ROW, items.get(0).getType());
        assertSame(a, items.get(0).getAssignment());
        assertEquals(RowPosition.SINGLE, items.get(0).getPosition());
    }

    @Test
    public void severalAssignmentsKeepOrderAndGetTopMiddleBottom() {
        List<Assignment> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) list.add(Assignment.newTodo("id" + i, "T" + i, i));
        List<HomeItem> items = HomeItems.build(list);
        assertEquals(4, items.size());
        assertEquals(RowPosition.TOP, items.get(0).getPosition());
        assertEquals(RowPosition.MIDDLE, items.get(1).getPosition());
        assertEquals(RowPosition.MIDDLE, items.get(2).getPosition());
        assertEquals(RowPosition.BOTTOM, items.get(3).getPosition());
        for (int i = 0; i < 4; i++) assertEquals("id" + i, items.get(i).getAssignment().id);
    }

    @Test
    public void twoAssignmentsAreTopAndBottom() {
        List<HomeItem> items = HomeItems.build(List.of(
                Assignment.newTodo("a", "A", 1), Assignment.newTodo("b", "B", 2)));
        assertEquals(RowPosition.TOP, items.get(0).getPosition());
        assertEquals(RowPosition.BOTTOM, items.get(1).getPosition());
    }

    @Test
    public void sameIdAndContentAreDiffEqual() {
        HomeItem x = HomeItems.build(List.of(Assignment.newTodo("a", "A", 1))).get(0);
        HomeItem y = HomeItems.build(List.of(Assignment.newTodo("a", "A", 1))).get(0);
        assertEquals(true, x.sameItemAs(y));
        assertEquals(true, x.sameContentAs(y));
        HomeItem z = HomeItems.build(List.of(Assignment.newTodo("a", "Changed", 1))).get(0);
        assertEquals(true, x.sameItemAs(z));
        assertEquals(false, x.sameContentAs(z));
    }

    /** Slice 2: the row draws the course on line 2, so a course edit must redraw it. */
    @Test
    public void courseChangeIsAContentChange() {
        Assignment a = Assignment.newTodo("a", "A", 1);
        Assignment b = Assignment.newTodo("a", "A", 1);
        b.course = "Biology 101";
        HomeItem x = HomeItems.build(List.of(a)).get(0);
        HomeItem y = HomeItems.build(List.of(b)).get(0);
        assertEquals(true, x.sameItemAs(y));
        assertEquals(false, x.sameContentAs(y));
    }

    /** design.md §3.1: line 2 shows the course, and is hidden when there is none. */
    @Test
    public void metaLineIsTheCourseOrNothing() {
        Assignment a = Assignment.newTodo("a", "A", 1);
        assertEquals(null, RowMeta.text(a, " · "));
        a.course = "Math 151";
        assertEquals("Math 151", RowMeta.text(a, " · "));
        assertEquals("parts join with the separator", "Due today · Math 151",
                RowMeta.join(" · ", "Due today", null, "", "Math 151"));
    }

    // ---- Slice 3: the Done section (design.md §3.3) ----

    private static Assignment done(String id, long completedAt) {
        Assignment a = Assignment.newTodo(id, "T" + id, 1);
        a.status = Assignment.STATUS_DONE;
        a.completedAt = completedAt;
        return a;
    }

    @Test
    public void doneItemsGoUnderACollapsedDoneHeaderBelowTheOpenOnes() {
        List<HomeItem> items = HomeItems.build(List.of(
                Assignment.newTodo("o1", "Open 1", 3), done("d1", 10), Assignment.newTodo("o2", "Open 2", 2)));
        assertEquals("two open rows, then the header; done rows hidden", 3, items.size());
        assertEquals("o1", items.get(0).getAssignment().id);
        assertEquals(RowPosition.TOP, items.get(0).getPosition());
        assertEquals("o2", items.get(1).getAssignment().id);
        assertEquals(RowPosition.BOTTOM, items.get(1).getPosition());
        HomeItem header = items.get(2);
        assertEquals(HomeItem.TYPE_DONE_HEADER, header.getType());
        assertEquals(1, header.getDoneCount());
        assertEquals(false, header.isExpanded());
    }

    @Test
    public void expandedDoneShowsItsOwnCardNewestCompletedFirst() {
        List<HomeItem> items = HomeItems.build(List.of(
                Assignment.newTodo("o1", "Open", 3), done("old", 10), done("new", 30), done("mid", 20)), true);
        assertEquals(5, items.size());
        assertEquals(RowPosition.SINGLE, items.get(0).getPosition());
        assertEquals(HomeItem.TYPE_DONE_HEADER, items.get(1).getType());
        assertEquals(3, items.get(1).getDoneCount());
        assertEquals(true, items.get(1).isExpanded());
        assertEquals("new", items.get(2).getAssignment().id);
        assertEquals(RowPosition.TOP, items.get(2).getPosition());
        assertEquals("mid", items.get(3).getAssignment().id);
        assertEquals(RowPosition.MIDDLE, items.get(3).getPosition());
        assertEquals("old", items.get(4).getAssignment().id);
        assertEquals(RowPosition.BOTTOM, items.get(4).getPosition());
    }

    @Test
    public void everythingDoneShowsOnlyTheDoneHeaderWhileCollapsed() {
        List<HomeItem> collapsed = HomeItems.build(List.of(done("d1", 1), done("d2", 2)));
        assertEquals(1, collapsed.size());
        assertEquals(HomeItem.TYPE_DONE_HEADER, collapsed.get(0).getType());
        assertEquals(2, collapsed.get(0).getDoneCount());
        List<HomeItem> expanded = HomeItems.build(List.of(done("d1", 1)), true);
        assertEquals(2, expanded.size());
        assertEquals(RowPosition.SINGLE, expanded.get(1).getPosition());
    }

    @Test
    public void noDoneItemsMeansNoDoneHeader() {
        for (HomeItem i : HomeItems.build(List.of(Assignment.newTodo("o", "O", 1)), true)) {
            assertEquals(HomeItem.TYPE_ROW, i.getType());
        }
    }

    @Test
    public void statusAndCompletedAtChangesAreContentChanges() {
        Assignment open = Assignment.newTodo("a", "A", 1);
        Assignment closed = done("a", 5);
        closed.title = "A";
        HomeItem x = HomeItems.build(List.of(open)).get(0);
        HomeItem y = HomeItems.build(List.of(closed), true).get(1);
        assertEquals(true, x.sameItemAs(y));
        assertEquals(false, x.sameContentAs(y));
    }

    @Test
    public void doneHeaderIsOneItemWhoseContentIsCountAndExpanded() {
        HomeItem a = HomeItems.build(List.of(done("d1", 1))).get(0);
        HomeItem b = HomeItems.build(List.of(done("d1", 1), done("d2", 2))).get(0);
        HomeItem c = HomeItems.build(List.of(done("d1", 1)), true).get(0);
        assertEquals(true, a.sameItemAs(b));
        assertEquals(false, a.sameContentAs(b));
        assertEquals(false, a.sameContentAs(c));
        assertEquals(true, a.sameContentAs(HomeItems.build(List.of(done("d9", 9))).get(0)));
    }

    /** design.md §4: a removal changes the neighbours' corners, which must count as a change. */
    @Test
    public void removingTheBottomRowRebindsTheNewBottom() {
        Assignment a = Assignment.newTodo("a", "A", 3);
        Assignment b = Assignment.newTodo("b", "B", 2);
        Assignment c = Assignment.newTodo("c", "C", 1);
        HomeItem before = HomeItems.build(List.of(a, b, c)).get(1);
        HomeItem after = HomeItems.build(List.of(a, b)).get(1);
        assertEquals(RowPosition.MIDDLE, before.getPosition());
        assertEquals(RowPosition.BOTTOM, after.getPosition());
        assertEquals(false, before.sameContentAs(after));
    }

    /** Home scrolls to the top only for a row that is new at the top, not for an expand. */
    @Test
    public void newRowAtTopDetectsOnlyANewFirstRow() {
        List<HomeItem> one = HomeItems.build(List.of(Assignment.newTodo("a", "A", 1), done("d", 1)));
        List<HomeItem> added = HomeItems.build(List.of(Assignment.newTodo("n", "N", 2),
                Assignment.newTodo("a", "A", 1), done("d", 1)));
        List<HomeItem> expanded = HomeItems.build(List.of(Assignment.newTodo("a", "A", 1), done("d", 1)), true);
        assertEquals(true, HomeItems.newRowAtTop(one, added));
        assertEquals(false, HomeItems.newRowAtTop(one, expanded));
        assertEquals(false, HomeItems.newRowAtTop(Collections.emptyList(), one));
        assertEquals(false, HomeItems.newRowAtTop(added, one));
    }
}
