package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.data.repo.AssignmentRepository;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Home list state. Activity → this → {@link AssignmentRepository} → Room (architecture §3.1).
 * Adding and editing live in {@link AssignmentEditViewModel} (Slice 2). Slice 3 adds mark done,
 * delete, their Undo, and the Done section's expanded state (collapsed on every launch, not
 * persisted; design.md §3.2).
 */
public class AssignmentsViewModel extends ViewModel {

    private final AssignmentRepository repository;
    private final LongSupplier clock;
    private final MutableLiveData<Boolean> doneExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> writeFailed = new MutableLiveData<>(false);
    private final MediatorLiveData<List<HomeItem>> homeItems = new MediatorLiveData<>();
    @Nullable private List<Assignment> latest;

    private final AssignmentRepository.WriteCallback reportFailure = error -> {
        if (error != null) writeFailed.postValue(true);
    };

    public AssignmentsViewModel(@NonNull AssignmentRepository repository, @NonNull LongSupplier clock) {
        this.repository = repository;
        this.clock = clock;
        homeItems.addSource(repository.observeAll(), list -> {
            latest = list;
            rebuild();
        });
        homeItems.addSource(doneExpanded, expanded -> {
            if (latest != null) rebuild();
        });
    }

    private void rebuild() {
        homeItems.setValue(HomeItems.build(latest, Boolean.TRUE.equals(doneExpanded.getValue())));
    }

    @NonNull
    public LiveData<List<HomeItem>> getHomeItems() {
        return homeItems;
    }

    /** True after a write failed, until {@link #onWriteFailureShown()}. */
    @NonNull
    public LiveData<Boolean> getWriteFailed() {
        return writeFailed;
    }

    public void onWriteFailureShown() {
        writeFailed.setValue(false);
    }

    public void toggleDoneExpanded() {
        doneExpanded.setValue(!Boolean.TRUE.equals(doneExpanded.getValue()));
    }

    /**
     * Checks or unchecks a row (design.md §4b). Done sets completedAt and updatedAt to now;
     * reopening clears completedAt. Asking for the state it already has writes nothing.
     *
     * @return a copy of the row as it was, for Undo
     */
    @NonNull
    public Assignment setDone(@NonNull Assignment a, boolean done) {
        Assignment before = a.copy();
        if (a.isDone() == done) return before;
        long now = clock.getAsLong();
        if (done) {
            repository.updateStatus(a.id, Assignment.STATUS_DONE, now, now, reportFailure);
        } else {
            repository.updateStatus(a.id, Assignment.STATUS_TODO, null, now, reportFailure);
        }
        return before;
    }

    /** Undo after mark done (Proposal 1, approved 2026-11-09): status, completedAt and updatedAt as they were. */
    public void undoStatus(@NonNull Assignment before) {
        repository.updateStatus(before.id, before.status, before.completedAt, before.updatedAt, reportFailure);
    }

    /**
     * Swipe or TalkBack delete (design.md §4).
     *
     * @return the full row, kept in memory for Undo
     */
    @NonNull
    public Assignment delete(@NonNull Assignment a) {
        Assignment kept = a.copy();
        repository.delete(a.id, reportFailure);
        return kept;
    }

    /** Undo after delete: re-inserts the same entity (same id, same fields, same updatedAt). */
    public void restore(@NonNull Assignment kept) {
        repository.insert(kept.copy(), reportFailure);
    }

    public static ViewModelProvider.Factory factory(@NonNull AssignmentRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AssignmentsViewModel(repository, System::currentTimeMillis);
            }
        };
    }
}
