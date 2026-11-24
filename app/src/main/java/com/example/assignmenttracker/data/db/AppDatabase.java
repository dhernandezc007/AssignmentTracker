package com.example.assignmenttracker.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.assignmenttracker.data.dao.AssignmentDao;
import com.example.assignmenttracker.data.dao.FarmDao;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.data.entity.FarmCredit;
import com.example.assignmenttracker.data.entity.FarmPlot;

@Database(entities = {Assignment.class, FarmCredit.class, FarmPlot.class}, version = 2, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    /** File name of the on-device database. backup_rules.xml and data_extraction_rules.xml include exactly this file. */
    public static final String DB_NAME = "assignments.db";

    /**
     * Reserved name for any future auth/session SharedPreferences (Phase 2). The backup
     * rules exclude it so a session token is never restored onto another device (SEC-0-2).
     */
    public static final String AUTH_PREFS_FILE = "auth_session.xml";

    /**
     * v1 → v2: adds the milpa (farm_credits, farm_plots). Assignments are untouched. The SQL
     * matches schemas/…/2.json exactly, or Room refuses to open the database.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `farm_credits` (`assignmentId` TEXT NOT NULL, "
                    + "`earnedAt` INTEGER NOT NULL, PRIMARY KEY(`assignmentId`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `farm_plots` (`id` TEXT NOT NULL, `crop` TEXT NOT NULL, "
                    + "`plantedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            // Work finished before the milpa existed still counts toward seeds.
            db.execSQL("INSERT OR IGNORE INTO `farm_credits` (`assignmentId`, `earnedAt`) "
                    + "SELECT `id`, `completedAt` FROM `assignments` "
                    + "WHERE `status` = 'DONE' AND `completedAt` IS NOT NULL");
        }
    };

    public abstract AssignmentDao assignmentDao();

    public abstract FarmDao farmDao();

    /**
     * Builds the on-disk database. The journal is TRUNCATE rather than WAL, so all data
     * lives in the single {@link #DB_NAME} file and an Auto Backup copy is always complete
     * (no -wal/-shm side files to keep in step).
     */
    @NonNull
    public static AppDatabase create(@NonNull Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                .setJournalMode(JournalMode.TRUNCATE)
                .addMigrations(MIGRATION_1_2)
                .build();
    }
}
