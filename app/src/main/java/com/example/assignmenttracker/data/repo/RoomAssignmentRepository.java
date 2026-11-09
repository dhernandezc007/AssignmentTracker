package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.dao.AssignmentDao;
import com.example.assignmenttracker.data.entity.Assignment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

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

    @Override
    public void insert(@NonNull Assignment assignment, @NonNull Consumer<Throwable> onError) {
        io.execute(() -> {
            try {
                dao.insert(assignment);
            } catch (RuntimeException e) {
                onError.accept(e);
            }
        });
    }
}
