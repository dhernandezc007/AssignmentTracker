package com.example.assignmenttracker;

import android.app.Application;

import androidx.annotation.NonNull;

import com.example.assignmenttracker.data.db.AppDatabase;
import com.example.assignmenttracker.data.repo.AssignmentRepository;
import com.example.assignmenttracker.data.repo.RoomAssignmentRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Holds the app-wide singletons: the Room database, its write executor and the repository. */
public class App extends Application {

    private AppDatabase database;
    private AssignmentRepository repository;

    @NonNull
    public synchronized AppDatabase getDatabase() {
        if (database == null) {
            database = AppDatabase.create(this);
        }
        return database;
    }

    @NonNull
    public synchronized AssignmentRepository getAssignmentRepository() {
        if (repository == null) {
            ExecutorService io = Executors.newSingleThreadExecutor();
            repository = new RoomAssignmentRepository(getDatabase().assignmentDao(), io);
        }
        return repository;
    }
}
