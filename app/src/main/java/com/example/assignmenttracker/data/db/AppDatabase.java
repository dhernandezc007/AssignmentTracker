package com.example.assignmenttracker.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.assignmenttracker.data.dao.AssignmentDao;
import com.example.assignmenttracker.data.entity.Assignment;

@Database(entities = {Assignment.class}, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    /** File name of the on-device database. backup_rules.xml and data_extraction_rules.xml include exactly this file. */
    public static final String DB_NAME = "assignments.db";

    /**
     * Reserved name for any future auth/session SharedPreferences (Phase 2). The backup
     * rules exclude it so a session token is never restored onto another device (SEC-0-2).
     */
    public static final String AUTH_PREFS_FILE = "auth_session.xml";

    public abstract AssignmentDao assignmentDao();

    /**
     * Builds the on-disk database. The journal is TRUNCATE rather than WAL, so all data
     * lives in the single {@link #DB_NAME} file and an Auto Backup copy is always complete
     * (no -wal/-shm side files to keep in step).
     */
    @NonNull
    public static AppDatabase create(@NonNull Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                .setJournalMode(JournalMode.TRUNCATE)
                .build();
    }
}
