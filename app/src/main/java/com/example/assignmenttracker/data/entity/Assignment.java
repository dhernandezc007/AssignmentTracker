package com.example.assignmenttracker.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One assignment (architecture §3.3). Ids are UUID strings and every date is epoch
 * millis (UTC), so a later Firestore sync needs no key migration. {@code updatedAt}
 * doubles as last-write-wins for that sync.
 *
 * <p>{@code courseId} is a plain nullable column for now: the Course table is not built
 * yet, so there is no foreign key in schema v1.
 */
@Entity(tableName = "assignments",
        indices = {@Index("courseId"), @Index("dueAt"), @Index("createdAt")})
public class Assignment {

    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE = "DONE";

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH = 2;

    @PrimaryKey
    @NonNull
    public String id = "";

    @Nullable
    public String courseId;

    @NonNull
    public String title = "";

    @Nullable
    public String notes;

    @Nullable
    public Long dueAt;

    @ColumnInfo(defaultValue = "1")
    public int priority = PRIORITY_NORMAL;

    @NonNull
    @ColumnInfo(defaultValue = STATUS_TODO)
    public String status = STATUS_TODO;

    @Nullable
    public Long completedAt;

    public long createdAt;

    public long updatedAt;

    /** A new open assignment with only a title, created and updated at {@code now}. */
    @NonNull
    public static Assignment newTodo(@NonNull String id, @NonNull String title, long now) {
        Assignment a = new Assignment();
        a.id = id;
        a.title = title;
        a.createdAt = now;
        a.updatedAt = now;
        return a;
    }
}
