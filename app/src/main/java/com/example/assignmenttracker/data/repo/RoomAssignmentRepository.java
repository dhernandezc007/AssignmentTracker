package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.dao.AssignmentDao;
import com.example.assignmenttracker.data.entity.Assignment;

import java.util.List;
import java.util.concurrent.ExecutorService;

/** Room implementation. Writes run on the given executor; reads are Room LiveData. */
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
        io.execute(() -> {
            int rows;
            try {
                rows = dao.updateFields(id, title, course, notes, updatedAt);
            } catch (RuntimeException e) {
                done.onComplete(e);
                return;
            }
            done.onComplete(rows == 1 ? null : new IllegalStateException("assignment not found"));
        });
    }
}
