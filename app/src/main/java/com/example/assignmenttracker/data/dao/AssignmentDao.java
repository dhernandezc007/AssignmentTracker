package com.example.assignmenttracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.List;

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

    /** Wipes every row. Used by instrumented tests to start from a known state. */
    @Query("DELETE FROM assignments")
    void deleteAll();
}
