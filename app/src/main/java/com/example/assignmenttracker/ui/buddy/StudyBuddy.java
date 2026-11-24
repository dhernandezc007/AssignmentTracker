package com.example.assignmenttracker.ui.buddy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.ui.assignments.HomeItem;
import com.example.assignmenttracker.ui.assignments.HomeSummary;

import java.util.List;
import java.util.Random;

/** What the study buddy is, and what they're in the mood to say. Pure, so it's unit-tested. */
public final class StudyBuddy {

    public enum Who {
        ABUELITA, ABUELO;

        @NonNull
        public Who other() {
            return this == ABUELITA ? ABUELO : ABUELITA;
        }

        /** Stored value → Who; anything unknown is Abuelita. */
        @NonNull
        public static Who parse(@Nullable String stored) {
            return ABUELO.name().equals(stored) ? ABUELO : ABUELITA;
        }
    }

    /**
     * Which set of lines fits the home screen right now. CELEBRATE is just after a mark done,
     * SEED just after that mark done earned a milpa seed.
     */
    public enum Mood { EMPTY, CAUGHT_UP, OVERDUE, DUE_TODAY, STEADY, CELEBRATE, SEED }

    private StudyBuddy() {
    }

    /** Reads the mood off the home list: overdue beats due today beats everything else. */
    @NonNull
    public static Mood moodFor(@Nullable List<HomeItem> items) {
        if (items == null || items.isEmpty()) return Mood.EMPTY;
        for (HomeItem item : items) {
            switch (item.getType()) {
                case HomeItem.TYPE_EMPTY:
                    return Mood.EMPTY;
                case HomeItem.TYPE_CAUGHT_UP:
                    return Mood.CAUGHT_UP;
                case HomeItem.TYPE_SUMMARY:
                    HomeSummary s = item.getSummary();
                    if (s == null) return Mood.STEADY;
                    if (s.overdue > 0) return Mood.OVERDUE;
                    if (s.today > 0) return Mood.DUE_TODAY;
                    return Mood.STEADY;
                default:
                    break;
            }
        }
        return Mood.STEADY;
    }

    /** A random index in [0, size) that isn't {@code avoid} (unless there's only one line). */
    public static int pick(int size, int avoid, @NonNull Random random) {
        if (size <= 1) return 0;
        int i = random.nextInt(size - 1);
        return i >= avoid && avoid >= 0 ? i + 1 : i;
    }
}
