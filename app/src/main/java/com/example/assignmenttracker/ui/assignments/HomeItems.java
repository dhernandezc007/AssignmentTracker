package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Turns the assignment list into adapter items. Pure, so it is unit-tested on the JVM. */
public final class HomeItems {

    /** Done rows: newest completedAt first (tasks.md Slice 4), ties by id so the order is stable. */
    private static final Comparator<Assignment> NEWEST_DONE_FIRST = (a, b) -> {
        long ca = a.completedAt == null ? Long.MIN_VALUE : a.completedAt;
        long cb = b.completedAt == null ? Long.MIN_VALUE : b.completedAt;
        if (ca != cb) return Long.compare(cb, ca);
        return a.id.compareTo(b.id);
    };

    private HomeItems() {
    }

    /** Same as {@link #build(List, boolean)} with the Done section collapsed. */
    @NonNull
    public static List<HomeItem> build(@Nullable List<Assignment> assignments) {
        return build(assignments, false);
    }

    /**
     * Slice 3 layout (design.md §3.3, before buckets): the open rows in one card with no header,
     * in the given order, then the Done header, then (only when expanded) the done rows in their
     * own card. With nothing at all, a single EMPTY item.
     */
    @NonNull
    public static List<HomeItem> build(@Nullable List<Assignment> assignments, boolean doneExpanded) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.singletonList(HomeItem.empty());
        }
        List<Assignment> open = new ArrayList<>();
        List<Assignment> done = new ArrayList<>();
        for (Assignment a : assignments) {
            (a.isDone() ? done : open).add(a);
        }
        List<HomeItem> out = new ArrayList<>(assignments.size() + 1);
        addCard(out, open);
        if (!done.isEmpty()) {
            out.add(HomeItem.doneHeader(done.size(), doneExpanded));
            if (doneExpanded) {
                done.sort(NEWEST_DONE_FIRST);
                addCard(out, done);
            }
        }
        return out;
    }

    /**
     * True when {@code after} starts with a row that wasn't in {@code before} at all (a new
     * assignment at the top), so Home scrolls it into view. Expanding Done, a delete or an
     * undo further down don't count.
     */
    public static boolean newRowAtTop(@NonNull List<HomeItem> before, @NonNull List<HomeItem> after) {
        if (after.isEmpty() || after.get(0).getType() != HomeItem.TYPE_ROW) return false;
        boolean hadRows = false;
        String firstId = after.get(0).getAssignment().id;
        for (HomeItem i : before) {
            if (i.getType() != HomeItem.TYPE_ROW) continue;
            hadRows = true;
            if (i.getAssignment().id.equals(firstId)) return false;
        }
        return hadRows;
    }

    private static void addCard(@NonNull List<HomeItem> out, @NonNull List<Assignment> rows) {
        int n = rows.size();
        for (int i = 0; i < n; i++) {
            RowPosition p = n == 1 ? RowPosition.SINGLE
                    : i == 0 ? RowPosition.TOP
                    : i == n - 1 ? RowPosition.BOTTOM
                    : RowPosition.MIDDLE;
            out.add(HomeItem.row(rows.get(i), p));
        }
    }
}
