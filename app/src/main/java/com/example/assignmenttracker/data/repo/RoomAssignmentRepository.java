package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.AssignmentLimits;
import com.example.assignmenttracker.data.dao.AssignmentDao;
import com.example.assignmenttracker.data.entity.Assignment;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Room implementation. Writes run on the given executor; reads are Room LiveData.
 *
 * <p>SEC-2-1: this is the only door to the data, so it checks what comes through, whoever the
 * caller is (today the edit and home ViewModels; later sync or import). A write that breaks a
 * rule is refused with an {@link IllegalArgumentException} through the callback, and nothing
 * reaches the DAO. Callers already cap and trim, so a refusal means a bug, not user input.
 */
public final class RoomAssignmentRepository implements AssignmentRepository {

    private final AssignmentDao dao;
    private final ExecutorService io;

    public RoomAssignmentRepository(@NonNull AssignmentDao dao, @NonNull ExecutorService io) {
        this.dao = dao;
        this.io = io;
    }

    @NonNull
    @Override
    public LiveData<List<Assignment>> observeAll() {
        return dao.observeAll();
    }

    @NonNull
    @Override
    public LiveData<Assignment> observeById(@NonNull String id) {
        return dao.observeById(id);
    }

    @Override
    public void insert(@NonNull Assignment assignment, @NonNull WriteCallback done) {
        String problem = checkFields(assignment.title, assignment.course, assignment.notes);
        if (problem == null) problem = checkStatus(assignment.status, assignment.completedAt);
        if (problem != null) {
            done.onComplete(new IllegalArgumentException(problem));
            return;
        }
        io.execute(() -> {
            try {
                dao.insert(assignment);
            } catch (RuntimeException e) {
                done.onComplete(e);
                return;
            }
            done.onComplete(null);
        });
    }

    @Override
    public void updateFields(@NonNull String id, @NonNull String title, @Nullable String course,
                             @Nullable String notes, long updatedAt, @NonNull WriteCallback done) {
        String problem = checkFields(title, course, notes);
        if (problem != null) {
            done.onComplete(new IllegalArgumentException(problem));
            return;
        }
        runOneRow(() -> dao.updateFields(id, title, course, notes, updatedAt), done);
    }

    @Override
    public void updateStatus(@NonNull String id, @NonNull String status, @Nullable Long completedAt,
                             long updatedAt, @NonNull WriteCallback done) {
        String problem = checkStatus(status, completedAt);
        if (problem != null) {
            done.onComplete(new IllegalArgumentException(problem));
            return;
        }
        runOneRow(() -> dao.updateStatus(id, status, completedAt, updatedAt), done);
    }

    @Override
    public void delete(@NonNull String id, @NonNull WriteCallback done) {
        runOneRow(() -> dao.deleteById(id), done);
    }

    /** Runs a single-row write; anything other than exactly one row changed is an error. */
    private void runOneRow(@NonNull java.util.function.IntSupplier write, @NonNull WriteCallback done) {
        io.execute(() -> {
            int rows;
            try {
                rows = write.getAsInt();
            } catch (RuntimeException e) {
                done.onComplete(e);
                return;
            }
            done.onComplete(rows == 1 ? null : new IllegalStateException("assignment not found"));
        });
    }

    /** The reason a title/course/notes set is refused, or null if it's fine. No user text in it. */
    @Nullable
    static String checkFields(@Nullable String title, @Nullable String course, @Nullable String notes) {
        if (title == null || title.trim().isEmpty()) return "title is blank";
        if (title.length() > AssignmentLimits.MAX_TITLE) return "title over cap";
        if (course != null && course.length() > AssignmentLimits.MAX_COURSE) return "course over cap";
        if (notes != null && notes.length() > AssignmentLimits.MAX_NOTES) return "notes over cap";
        return null;
    }

    /** The reason a status write is refused, or null if it's fine. */
    @Nullable
    static String checkStatus(@Nullable String status, @Nullable Long completedAt) {
        if (Assignment.STATUS_DONE.equals(status)) {
            return completedAt == null ? "done without completedAt" : null;
        }
        if (Assignment.STATUS_TODO.equals(status) || Assignment.STATUS_IN_PROGRESS.equals(status)) {
            return completedAt != null ? "open with completedAt" : null;
        }
        return "unknown status";
    }
}
