package com.example.assignmenttracker.data.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.List;

/**
 * SEC-1-1: there is deliberately no wipe-all method here. Tests reset the database through
 * the androidTest helper {@code TestDb}; the JVM {@code DaoSafetyTest} guards this file.
 */
@Dao
public interface AssignmentDao {

    /** Fails on a duplicate id, so an existing row is never silently replaced. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(Assignment assignment);

    /**
     * Slice 4 order (tasks.md): due date ascending with no-date rows last, then createdAt and id
     * so ties are stable. Home splits open and done rows itself (done rows sort by completedAt).
     */
    @Query("SELECT * FROM assignments ORDER BY dueAt IS NULL, dueAt, createdAt, id")
    LiveData<List<Assignment>> observeAll();

    /** One row for the edit screen; emits null if the id doesn't exist. */
    @Query("SELECT * FROM assignments WHERE id = :id")
    LiveData<Assignment> observeById(String id);

    /**
     * The edit save (Slices 2 and 4). Only the fields the screen edits change (title, course,
     * notes, dueAt), plus {@code updatedAt}; id, createdAt, status and completedAt are kept.
     *
     * @return the number of rows changed (0 if the id doesn't exist)
     */
    @Query("UPDATE assignments SET title = :title, course = :course, notes = :notes, "
            + "dueAt = :dueAt, updatedAt = :updatedAt WHERE id = :id")
    int updateFields(String id, String title, @Nullable String course, @Nullable String notes,
                     @Nullable Long dueAt, long updatedAt);

    /**
     * Mark done / reopen / undo (Slice 3). Only status, completedAt and updatedAt change.
     *
     * @return the number of rows changed (0 if the id doesn't exist)
     */
    @Query("UPDATE assignments SET status = :status, completedAt = :completedAt, "
            + "updatedAt = :updatedAt WHERE id = :id")
    int updateStatus(String id, String status, @Nullable Long completedAt, long updatedAt);

    /**
     * Swipe delete (Slice 3): exactly one row, by id. Undo re-inserts the kept entity with
     * {@link #insert}. There is no bulk delete (SEC-1-1).
     *
     * @return the number of rows deleted (0 if the id doesn't exist)
     */
    @Query("DELETE FROM assignments WHERE id = :id")
    int deleteById(String id);
}
