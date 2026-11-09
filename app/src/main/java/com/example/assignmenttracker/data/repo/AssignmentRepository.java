package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.List;

/**
 * The app's only door to assignment data (ADR-001). Phase 1 is Room; a Phase 2
 * Firestore-backed implementation can sit behind the same interface.
 */
public interface AssignmentRepository {

    /** Called once when a write finishes, possibly on a background thread. */
    interface WriteCallback {
        /** {@code error} is null on success. */
        void onComplete(@Nullable Throwable error);
    }

    /** Every assignment, in list order. Emits again whenever the data changes. */
    @NonNull
    LiveData<List<Assignment>> observeAll();

    /** One assignment, or null if it doesn't exist. */
    @NonNull
    LiveData<Assignment> observeById(@NonNull String id);

    /** Saves a new assignment off the main thread. */
    void insert(@NonNull Assignment assignment, @NonNull WriteCallback done);

    /**
     * Updates the edited fields of an existing assignment off the main thread. Reports an
     * error if the id no longer exists.
     */
    void updateFields(@NonNull String id, @NonNull String title, @Nullable String course,
                      @Nullable String notes, @Nullable Long dueAt, long updatedAt,
                      @NonNull WriteCallback done);

    /**
     * Sets status, completedAt and updatedAt (mark done, reopen, undo). {@code completedAt} is
     * required for DONE and must be null otherwise. Reports an error if the id doesn't exist.
     */
    void updateStatus(@NonNull String id, @NonNull String status, @Nullable Long completedAt,
                      long updatedAt, @NonNull WriteCallback done);

    /**
     * Deletes one assignment. Undo is {@link #insert} of the entity the caller kept.
     * Reports an error if the id doesn't exist.
     */
    void delete(@NonNull String id, @NonNull WriteCallback done);
}
