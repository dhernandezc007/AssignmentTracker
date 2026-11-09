package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.util.DueBucket;

import java.util.Objects;

/**
 * One entry in the home list (design.md §2.1 adapter view types): SUMMARY, CAUGHT_UP,
 * SECTION_HEADER, ROW, DONE_HEADER and EMPTY.
 */
public final class HomeItem {

    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_ROW = 1;
    public static final int TYPE_DONE_HEADER = 2;
    public static final int TYPE_SUMMARY = 3;
    public static final int TYPE_CAUGHT_UP = 4;
    public static final int TYPE_SECTION_HEADER = 5;

    private static final HomeItem EMPTY = new HomeItem(TYPE_EMPTY, null, RowPosition.SINGLE, 0, false, null, null, null);

    private final int type;
    @Nullable private final Assignment assignment;
    @NonNull private final RowPosition position;
    private final int count;
    private final boolean expanded;
    @Nullable private final DueBucket bucket;
    @Nullable private final Integer dueDays;
    @Nullable private final HomeSummary summary;

    private HomeItem(int type, @Nullable Assignment assignment, @NonNull RowPosition position, int count,
                     boolean expanded, @Nullable DueBucket bucket, @Nullable Integer dueDays,
                     @Nullable HomeSummary summary) {
        this.type = type;
        this.assignment = assignment;
        this.position = position;
        this.count = count;
        this.expanded = expanded;
        this.bucket = bucket;
        this.dueDays = dueDays;
        this.summary = summary;
    }

    @NonNull
    static HomeItem empty() {
        return EMPTY;
    }

    /** {@code dueDays}: days from today for an open row with a due date, else null. */
    @NonNull
    static HomeItem row(@NonNull Assignment a, @NonNull RowPosition p, @Nullable DueBucket bucket,
                        @Nullable Integer dueDays) {
        return new HomeItem(TYPE_ROW, a, p, 0, false, bucket, dueDays, null);
    }

    @NonNull
    static HomeItem doneHeader(int count, boolean expanded) {
        return new HomeItem(TYPE_DONE_HEADER, null, RowPosition.SINGLE, count, expanded, null, null, null);
    }

    @NonNull
    static HomeItem sectionHeader(@NonNull DueBucket bucket, int count) {
        return new HomeItem(TYPE_SECTION_HEADER, null, RowPosition.SINGLE, count, false, bucket, null, null);
    }

    @NonNull
    static HomeItem summary(@NonNull HomeSummary s) {
        return new HomeItem(TYPE_SUMMARY, null, RowPosition.SINGLE, 0, false, null, null, s);
    }

    @NonNull
    static HomeItem caughtUp(int doneCount) {
        return new HomeItem(TYPE_CAUGHT_UP, null, RowPosition.SINGLE, doneCount, false, null, null, null);
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

    /** SECTION_HEADER: rows in the bucket. CAUGHT_UP and DONE_HEADER: done items. */
    public int getCount() {
        return count;
    }

    /** DONE_HEADER: how many assignments are done. */
    public int getDoneCount() {
        return count;
    }

    /** DONE_HEADER: whether the done rows are showing. */
    public boolean isExpanded() {
        return expanded;
    }

    /** SECTION_HEADER and open ROW: the bucket. */
    @Nullable
    public DueBucket getBucket() {
        return bucket;
    }

    /** Open ROW with a due date: whole local days from today (negative when overdue). */
    @Nullable
    public Integer getDueDays() {
        return dueDays;
    }

    /** SUMMARY: the counts. */
    @Nullable
    public HomeSummary getSummary() {
        return summary;
    }

    /** DiffUtil identity: same view type; rows by id, section headers by bucket. */
    public boolean sameItemAs(@NonNull HomeItem other) {
        if (type != other.type) return false;
        if (type == TYPE_SECTION_HEADER) return bucket == other.bucket;
        if (type != TYPE_ROW) return true;
        return assignment != null && other.assignment != null && assignment.id.equals(other.assignment.id);
    }

    /**
     * DiffUtil content: everything the item draws. Rows include the card position (a neighbour's
     * removal rebinds the corners, design.md §4) and the days-from-today (the label changes at
     * midnight).
     */
    public boolean sameContentAs(@NonNull HomeItem other) {
        if (!sameItemAs(other) || position != other.position) return false;
        switch (type) {
            case TYPE_EMPTY:
                return true;
            case TYPE_DONE_HEADER:
                return count == other.count && expanded == other.expanded;
            case TYPE_SECTION_HEADER:
            case TYPE_CAUGHT_UP:
                return count == other.count;
            case TYPE_SUMMARY:
                return summary != null && other.summary != null && summary.sameAs(other.summary);
            default:
                return Objects.equals(assignment.title, other.assignment.title)
                        && Objects.equals(assignment.course, other.assignment.course)
                        && Objects.equals(assignment.status, other.assignment.status)
                        && Objects.equals(assignment.completedAt, other.assignment.completedAt)
                        && Objects.equals(assignment.dueAt, other.assignment.dueAt)
                        && Objects.equals(dueDays, other.dueDays)
                        && assignment.updatedAt == other.assignment.updatedAt;
        }
    }
}
