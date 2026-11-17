package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.util.DueBucket;
import com.example.assignmenttracker.util.DueDates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/** Turns the assignment list into adapter items. Pure, so it is unit-tested on the JVM. */
public final class HomeItems {

    /**
     * Open rows (tasks.md Slice 4): {@code ORDER BY dueAt IS NULL, dueAt}, then createdAt and id
     * so ties are stable. The DAO uses the same order; this keeps the list right whatever the source.
     */
    static final Comparator<Assignment> BY_DUE = (a, b) -> {
        if (a.dueAt == null && b.dueAt != null) return 1;
        if (a.dueAt != null && b.dueAt == null) return -1;
        if (a.dueAt != null && !a.dueAt.equals(b.dueAt)) return Long.compare(a.dueAt, b.dueAt);
        if (a.createdAt != b.createdAt) return Long.compare(a.createdAt, b.createdAt);
        return a.id.compareTo(b.id);
    };

    /** Done rows: newest completedAt first (tasks.md Slice 4), ties by id so the order is stable. */
    private static final Comparator<Assignment> NEWEST_DONE_FIRST = (a, b) -> {
        long ca = a.completedAt == null ? Long.MIN_VALUE : a.completedAt;
        long cb = b.completedAt == null ? Long.MIN_VALUE : b.completedAt;
        if (ca != cb) return Long.compare(cb, ca);
        return a.id.compareTo(b.id);
    };

    private HomeItems() {
    }

    /**
     * design.md §2.1 order: SUMMARY (or CAUGHT_UP when nothing is open) → for each non-empty
     * bucket (Overdue, Today, This week, Later, No date) its SECTION_HEADER and ROWs → DONE_HEADER
     * (if anything is done) → done ROWs (only when expanded). Nothing at all: a single EMPTY.
     * Buckets use {@code now} in the device zone {@code tz}.
     */
    @NonNull
    public static List<HomeItem> build(@Nullable List<Assignment> assignments, boolean doneExpanded,
                                       long now, @NonNull TimeZone tz) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.singletonList(HomeItem.empty());
        }
        Map<DueBucket, List<Assignment>> buckets = new EnumMap<>(DueBucket.class);
        for (DueBucket b : DueBucket.values()) buckets.put(b, new ArrayList<>());
        List<Assignment> done = new ArrayList<>();
        int open = 0;
        for (Assignment a : assignments) {
            if (a.isDone()) {
                done.add(a);
            } else {
                buckets.get(DueDates.bucketOf(a.dueAt, now, tz)).add(a);
                open++;
            }
        }

        List<HomeItem> out = new ArrayList<>(assignments.size() + 8);
        if (open == 0) {
            out.add(HomeItem.caughtUp(done.size()));
        } else {
            out.add(HomeItem.summary(new HomeSummary(buckets.get(DueBucket.OVERDUE).size(),
                    buckets.get(DueBucket.TODAY).size(), buckets.get(DueBucket.THIS_WEEK).size(),
                    open, done.size())));
        }
        for (DueBucket b : DueBucket.values()) {
            List<Assignment> rows = buckets.get(b);
            if (rows.isEmpty()) continue;
            rows.sort(BY_DUE);
            out.add(HomeItem.sectionHeader(b, rows.size()));
            addCard(out, rows, b, now, tz);
        }
        if (!done.isEmpty()) {
            out.add(HomeItem.doneHeader(done.size(), doneExpanded));
            if (doneExpanded) {
                done.sort(NEWEST_DONE_FIRST);
                addCard(out, done, null, now, tz);
            }
        }
        return out;
    }

    /**
     * After an add, where Home should scroll so the new open row shows: its section header when
     * it opens a card, the top of the list when it lands in the first bucket, otherwise the row.
     * -1 for the first load, an expand, a removal, or a done row.
     */
    public static int scrollTargetForNewOpenRow(@NonNull List<HomeItem> before, @NonNull List<HomeItem> after) {
        if (before.isEmpty()) return -1;
        Set<String> had = new HashSet<>();
        for (HomeItem i : before) if (i.getType() == HomeItem.TYPE_ROW) had.add(i.getAssignment().id);
        for (int p = 0; p < after.size(); p++) {
            HomeItem i = after.get(p);
            if (i.getType() != HomeItem.TYPE_ROW || i.getAssignment().isDone()) continue;
            if (had.contains(i.getAssignment().id)) continue;
            RowPosition pos = i.getPosition();
            int target = (pos == RowPosition.TOP || pos == RowPosition.SINGLE) ? p - 1 : p;
            for (int k = 0; k < target; k++) {
                int t = after.get(k).getType();
                if (t != HomeItem.TYPE_SUMMARY && t != HomeItem.TYPE_SECTION_HEADER) return target;
            }
            return 0;
        }
        return -1;
    }

    private static void addCard(@NonNull List<HomeItem> out, @NonNull List<Assignment> rows,
                                @Nullable DueBucket bucket, long now, @NonNull TimeZone tz) {
        int n = rows.size();
        for (int i = 0; i < n; i++) {
            RowPosition p = n == 1 ? RowPosition.SINGLE
                    : i == 0 ? RowPosition.TOP
                    : i == n - 1 ? RowPosition.BOTTOM
                    : RowPosition.MIDDLE;
            Assignment a = rows.get(i);
            Integer days = bucket == null || a.dueAt == null ? null : DueDates.daysFromToday(a.dueAt, now, tz);
            out.add(HomeItem.row(a, p, bucket, days));
        }
    }
}
