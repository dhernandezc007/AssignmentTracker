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
import java.util.TimeZone;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Home list state. Activity → this → {@link AssignmentRepository} → Room (architecture §3.1).
 * Adding and editing live in {@link AssignmentEditViewModel} (Slice 2). Slice 3 added mark done,
 * delete, their Undo, and the Done section's expanded state (collapsed on every launch, not
 * persisted; design.md §3.2). Slice 4 adds the buckets (clock and zone injected), the pending
 * Undo that outlives a rotation (QA-3-2) and the reopen announcement (DES-3-3).
 */
public class AssignmentsViewModel extends ViewModel {

    /** design.md §4: the Undo snackbar shows for 5 s. */
    public static final long UNDO_MS = 5000;
    /** QA-3-2: a snackbar shown again after a rotation stays at least this long. */
    public static final long UNDO_MIN_MS = 2000;

    private final AssignmentRepository repository;
    private final LongSupplier clock;
    private final Supplier<TimeZone> zone;
    private final MutableLiveData<Boolean> doneExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> writeFailed = new MutableLiveData<>(false);
    private final MutableLiveData<String> announcement = new MutableLiveData<>(null);
    private final MediatorLiveData<List<HomeItem>> homeItems = new MediatorLiveData<>();
    @Nullable private List<Assignment> latest;
    @Nullable private PendingUndo pendingUndo;

    private final AssignmentRepository.WriteCallback reportFailure = error -> {
        if (error != null) writeFailed.postValue(true);
    };

    public AssignmentsViewModel(@NonNull AssignmentRepository repository, @NonNull LongSupplier clock,
                                @NonNull Supplier<TimeZone> zone) {
        this.repository = repository;
        this.clock = clock;
        this.zone = zone;
        homeItems.addSource(repository.observeAll(), list -> {
            latest = list;
            rebuild();
        });
        homeItems.addSource(doneExpanded, expanded -> {
            if (latest != null) rebuild();
        });
    }

    private void rebuild() {
        homeItems.setValue(HomeItems.build(latest, Boolean.TRUE.equals(doneExpanded.getValue()),
                clock.getAsLong(), zone.get()));
    }

    @NonNull
    public LiveData<List<HomeItem>> getHomeItems() {
        return homeItems;
    }

    /** Re-buckets with the current clock and zone (on resume, and at local midnight). */
    public void refresh() {
        if (latest != null) rebuild();
    }

    /** True after a write failed, until {@link #onWriteFailureShown()}. */
    @NonNull
    public LiveData<Boolean> getWriteFailed() {
        return writeFailed;
    }

    public void onWriteFailureShown() {
        writeFailed.setValue(false);
    }

    /** The title of a row that was really reopened, for TalkBack; null once announced. */
    @NonNull
    public LiveData<String> getAnnouncement() {
        return announcement;
    }

    public void onAnnounced() {
        announcement.setValue(null);
    }

    public void toggleDoneExpanded() {
        doneExpanded.setValue(!Boolean.TRUE.equals(doneExpanded.getValue()));
    }

    /**
     * Checks or unchecks a row (design.md §4b). Done sets completedAt and updatedAt to now and
     * offers Undo; reopening clears completedAt and, once the write succeeds, is announced
     * (DES-3-3). Asking for the state it already has writes nothing.
     *
     * @return whether a write was started
     */
    public boolean setDone(@NonNull Assignment a, boolean done) {
        if (a.isDone() == done) return false;
        long now = clock.getAsLong();
        if (done) {
            pendingUndo = new PendingUndo(PendingUndo.Kind.MARK_DONE, a.copy(), now);
            repository.updateStatus(a.id, Assignment.STATUS_DONE, now, now, reportFailure);
        } else {
            String title = a.title;
            repository.updateStatus(a.id, Assignment.STATUS_TODO, null, now, error -> {
                if (error != null) writeFailed.postValue(true);
                else announcement.postValue(title);
            });
        }
        return true;
    }

    /** Swipe or TalkBack delete (design.md §4). The full row is kept in memory for Undo. */
    public void delete(@NonNull Assignment a) {
        pendingUndo = new PendingUndo(PendingUndo.Kind.DELETE, a.copy(), clock.getAsLong());
        repository.delete(a.id, reportFailure);
    }

    /** The Undo the snackbar should be offering, or null. */
    @Nullable
    public PendingUndo getPendingUndo() {
        return pendingUndo;
    }

    /** How long a snackbar shown again after a rotation should stay (QA-3-2). */
    public long undoTimeLeft(@NonNull PendingUndo p) {
        return Math.max(UNDO_MIN_MS, UNDO_MS - (clock.getAsLong() - p.shownAt));
    }

    /** The snackbar for {@code p} timed out or was swiped away. A newer Undo is left alone. */
    public void onUndoEnded(@NonNull PendingUndo p) {
        if (pendingUndo == p) pendingUndo = null;
    }

    /**
     * Runs an Undo. Both kinds write {@code updatedAt = now}, never the old value, because
     * updatedAt is the Phase 2 last-write-wins clock (SEC-3-1, design.md §4).
     */
    public void undo(@NonNull PendingUndo p) {
        if (pendingUndo == p) pendingUndo = null;
        long now = clock.getAsLong();
        if (p.kind == PendingUndo.Kind.DELETE) {
            Assignment back = p.assignment.copy();
            back.updatedAt = now;
            repository.insert(back, reportFailure);
        } else {
            // The open status it had before (TODO in the MVP), with completedAt cleared.
            repository.updateStatus(p.assignment.id, p.assignment.status, null, now, reportFailure);
        }
    }

    public static ViewModelProvider.Factory factory(@NonNull AssignmentRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AssignmentsViewModel(repository, System::currentTimeMillis, TimeZone::getDefault);
            }
        };
    }
}
