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
     * Slice 1 order: newest first, so a just-added row shows at the top. Slice 4 replaces
     * this with the due-date order ({@code status != 'DONE' ORDER BY dueAt IS NULL, dueAt}).
     */
    @Query("SELECT * FROM assignments ORDER BY createdAt DESC, id")
    LiveData<List<Assignment>> observeAll();

    /** One row for the edit screen; emits null if the id doesn't exist. */
    @Query("SELECT * FROM assignments WHERE id = :id")
    LiveData<Assignment> observeById(String id);

    /**
     * The edit save (Slice 2). Only the fields the screen edits change, plus {@code updatedAt};
     * id, createdAt, status, completedAt and dueAt are kept as they are in the row.
     *
     * @return the number of rows changed (0 if the id doesn't exist)
     */
    @Query("UPDATE assignments SET title = :title, course = :course, notes = :notes, "
            + "updatedAt = :updatedAt WHERE id = :id")
    int updateFields(String id, String title, @Nullable String course, @Nullable String notes, long updatedAt);
}
