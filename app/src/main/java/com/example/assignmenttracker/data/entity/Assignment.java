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
 * <p>{@code course} is free text (brief; Slice 2). It replaced the planned {@code courseId}
 * in schema v1, edited in place before anything was installed outside the emulator.
 * "Courses as entities" is on the Later list.
 */
@Entity(tableName = "assignments",
        indices = {@Index("dueAt"), @Index("createdAt")})
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

    /** Free-text course, up to 60 characters (design.md §5.2), or null when not set. */
    @Nullable
    public String course;

    @NonNull
    public String title = "";

    /** Up to 2000 characters (design.md §5.2), or null when not set. */
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
