package com.example.assignmenttracker;

import android.database.Cursor;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.example.assignmenttracker.data.entity.Assignment;

/**
 * Test-only access to the app's real database. Runs on the instrumentation thread (Room
 * refuses the main thread). SEC-1-1: the wipe lives here, not in the production DAO.
 */
public final class TestDb {

    private TestDb() {
    }

    private static App app() {
        return ApplicationProvider.getApplicationContext();
    }

    public static void clear() {
        app().getDatabase().clearAllTables();
    }

    public static void insert(Assignment a) {
        app().getDatabase().assignmentDao().insert(a);
    }

    public static int count() {
        try (Cursor c = app().getDatabase().query("SELECT COUNT(*) FROM assignments", null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    /** {title, course, notes, updatedAt} for one row, or null if it doesn't exist. */
    @Nullable
    public static Object[] row(String id) {
        try (Cursor c = app().getDatabase().query(
                "SELECT title, course, notes, updatedAt, createdAt FROM assignments WHERE id = ?",
                new Object[]{id})) {
            if (!c.moveToFirst()) return null;
            return new Object[]{c.getString(0), c.isNull(1) ? null : c.getString(1),
                    c.isNull(2) ? null : c.getString(2), c.getLong(3), c.getLong(4)};
        }
    }
}
