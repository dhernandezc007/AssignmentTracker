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
}
