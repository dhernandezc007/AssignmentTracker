package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.List;
import java.util.function.Consumer;

/**
 * The app's only door to assignment data (ADR-001). Phase 1 is Room; a Phase 2
 * Firestore-backed implementation can sit behind the same interface.
 */
public interface AssignmentRepository {

    /** Every assignment, in list order. Emits again whenever the data changes. */
    @NonNull
    LiveData<List<Assignment>> observeAll();

    /**
     * Saves a new assignment off the main thread. {@code onError} is called, possibly on
     * a background thread, if the write fails.
     */
    void insert(@NonNull Assignment assignment, @NonNull Consumer<Throwable> onError);
}
