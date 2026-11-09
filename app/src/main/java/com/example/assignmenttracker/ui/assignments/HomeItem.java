package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.Objects;

/**
 * One entry in the home list. Slice 1 has two view types (design.md §2): EMPTY and ROW.
 * SUMMARY, CAUGHT_UP, SECTION_HEADER and DONE_HEADER arrive in Slices 3 and 4.
 */
public final class HomeItem {

    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_ROW = 1;

    private static final HomeItem EMPTY = new HomeItem(TYPE_EMPTY, null, RowPosition.SINGLE);

    private final int type;
    @Nullable private final Assignment assignment;
    @NonNull private final RowPosition position;

    private HomeItem(int type, @Nullable Assignment assignment, @NonNull RowPosition position) {
        this.type = type;
        this.assignment = assignment;
        this.position = position;
    }

    @NonNull
    static HomeItem empty() {
        return EMPTY;
    }

    @NonNull
    static HomeItem row(@NonNull Assignment a, @NonNull RowPosition p) {
        return new HomeItem(TYPE_ROW, a, p);
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

    /** DiffUtil identity: same view type and, for rows, the same assignment id. */
    public boolean sameItemAs(@NonNull HomeItem other) {
        if (type != other.type) return false;
        if (type == TYPE_EMPTY) return true;
        return assignment != null && other.assignment != null && assignment.id.equals(other.assignment.id);
    }

    /** DiffUtil content: everything the row draws. */
    public boolean sameContentAs(@NonNull HomeItem other) {
        if (!sameItemAs(other) || position != other.position) return false;
        if (type == TYPE_EMPTY) return true;
        return Objects.equals(assignment.title, other.assignment.title)
                && Objects.equals(assignment.course, other.assignment.course)
                && assignment.updatedAt == other.assignment.updatedAt;
    }
}
