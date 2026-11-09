package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.Objects;

/**
 * One entry in the home list (design.md §2). Slice 3 has three view types: EMPTY, ROW and
 * DONE_HEADER. SUMMARY, CAUGHT_UP and SECTION_HEADER arrive in Slice 4.
 */
public final class HomeItem {

    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_ROW = 1;
    public static final int TYPE_DONE_HEADER = 2;

    private static final HomeItem EMPTY = new HomeItem(TYPE_EMPTY, null, RowPosition.SINGLE, 0, false);

    private final int type;
    @Nullable private final Assignment assignment;
    @NonNull private final RowPosition position;
    private final int doneCount;
    private final boolean expanded;

    private HomeItem(int type, @Nullable Assignment assignment, @NonNull RowPosition position,
                     int doneCount, boolean expanded) {
        this.type = type;
        this.assignment = assignment;
        this.position = position;
        this.doneCount = doneCount;
        this.expanded = expanded;
    }

    @NonNull
    static HomeItem empty() {
        return EMPTY;
    }

    @NonNull
    static HomeItem row(@NonNull Assignment a, @NonNull RowPosition p) {
        return new HomeItem(TYPE_ROW, a, p, 0, false);
    }

    @NonNull
    static HomeItem doneHeader(int count, boolean expanded) {
        return new HomeItem(TYPE_DONE_HEADER, null, RowPosition.SINGLE, count, expanded);
    }

    public int getType() {
        return type;
    }

    @Nullable
    public Assignment getAssignment() {
        return assignment;
    }

    @NonNull
    public RowPosition getPosition() {
        return position;
    }

    /** DONE_HEADER: how many assignments are done. */
    public int getDoneCount() {
        return doneCount;
    }

    /** DONE_HEADER: whether the done rows are showing. */
    public boolean isExpanded() {
        return expanded;
    }

    /** DiffUtil identity: same view type and, for rows, the same assignment id. */
    public boolean sameItemAs(@NonNull HomeItem other) {
        if (type != other.type) return false;
        if (type != TYPE_ROW) return true;
        return assignment != null && other.assignment != null && assignment.id.equals(other.assignment.id);
    }

    /**
     * DiffUtil content: everything the item draws. For rows that includes the position in the
     * card, so a neighbour's removal rebinds the corners (design.md §4).
     */
    public boolean sameContentAs(@NonNull HomeItem other) {
        if (!sameItemAs(other) || position != other.position) return false;
        if (type == TYPE_EMPTY) return true;
        if (type == TYPE_DONE_HEADER) return doneCount == other.doneCount && expanded == other.expanded;
        return Objects.equals(assignment.title, other.assignment.title)
                && Objects.equals(assignment.course, other.assignment.course)
                && Objects.equals(assignment.status, other.assignment.status)
                && Objects.equals(assignment.completedAt, other.assignment.completedAt)
                && assignment.updatedAt == other.assignment.updatedAt;
    }
}
