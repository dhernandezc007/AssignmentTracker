package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;

import com.example.assignmenttracker.data.entity.Assignment;

/**
 * The Undo the snackbar is offering (QA-3-2). Held by {@link AssignmentsViewModel} so a rotation
 * can show the snackbar again; process death drops it, and nothing comes back by itself.
 */
public final class PendingUndo {

    public enum Kind { DELETE, MARK_DONE }

    @NonNull public final Kind kind;
    /** A copy of the row as it was before the action. Never written back as-is (SEC-3-1). */
    @NonNull public final Assignment assignment;
    /** When the snackbar first showed, by the ViewModel's clock. */
    public final long shownAt;

    PendingUndo(@NonNull Kind kind, @NonNull Assignment assignment, long shownAt) {
        this.kind = kind;
        this.assignment = assignment;
        this.shownAt = shownAt;
    }
}
