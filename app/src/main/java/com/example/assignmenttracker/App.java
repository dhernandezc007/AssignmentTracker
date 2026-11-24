package com.example.assignmenttracker;

import android.app.Application;

import androidx.annotation.NonNull;

import com.example.assignmenttracker.data.db.AppDatabase;
import com.example.assignmenttracker.data.repo.AssignmentRepository;
import com.example.assignmenttracker.data.repo.FarmRepository;
import com.example.assignmenttracker.data.repo.RoomAssignmentRepository;
import com.example.assignmenttracker.data.repo.RoomFarmRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Holds the app-wide singletons: the Room database, its write executor and the repositories.
 * Both repositories share the one write thread, so a mark done and its milpa credit land in order.
 */
public class App extends Application {

    private AppDatabase database;
    private ExecutorService io;
    private AssignmentRepository repository;
    private FarmRepository farmRepository;

    @NonNull
    public synchronized AppDatabase getDatabase() {
        if (database == null) {
            database = AppDatabase.create(this);
        }
        return database;
    }

    @NonNull
    private synchronized ExecutorService io() {
        if (io == null) {
            io = Executors.newSingleThreadExecutor();
        }
        return io;
    }

    @NonNull
    public synchronized AssignmentRepository getAssignmentRepository() {
        if (repository == null) {
            repository = new RoomAssignmentRepository(getDatabase().assignmentDao(), io());
        }
        return repository;
    }

    @NonNull
    public synchronized FarmRepository getFarmRepository() {
        if (farmRepository == null) {
            farmRepository = new RoomFarmRepository(getDatabase().farmDao(), io());
        }
        return farmRepository;
    }
}
