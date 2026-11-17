package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Which due label a row shows on line 2 (design.md §3.1). Done rows show none. */
public enum DueLabel {
    NONE, OVERDUE, TODAY, TOMORROW, DATE;

    @NonNull
    public static DueLabel forDays(@Nullable Integer days) {
        if (days == null) return NONE;
        if (days < 0) return OVERDUE;
        if (days == 0) return TODAY;
        if (days == 1) return TOMORROW;
        return DATE;
    }

    @NonNull
    public static DueLabel of(@NonNull HomeItem row) {
        return forDays(row.getDueDays());
    }
}
