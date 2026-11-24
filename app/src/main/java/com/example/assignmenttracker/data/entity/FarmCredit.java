package com.example.assignmenttracker.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One finished assignment counted toward the milpa (schema v2). Every 3 credits earn a seed.
 *
 * <p>An assignment counts once, ever: re-checking it after a reopen adds nothing. Undo of the
 * mark done takes the credit back. Deleting the assignment later does not, because the work
 * was done. That is why there is no foreign key to {@code assignments}.
 */
@Entity(tableName = "farm_credits")
public class FarmCredit {

    @PrimaryKey
    @NonNull
    public String assignmentId = "";

    /** When it was first marked done (epoch millis). */
    public long earnedAt;

    @NonNull
    public static FarmCredit of(@NonNull String assignmentId, long earnedAt) {
        FarmCredit c = new FarmCredit();
        c.assignmentId = assignmentId;
        c.earnedAt = earnedAt;
        return c;
    }
}
