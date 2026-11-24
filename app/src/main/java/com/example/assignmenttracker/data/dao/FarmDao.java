package com.example.assignmenttracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.assignmenttracker.data.entity.FarmCredit;
import com.example.assignmenttracker.data.entity.FarmPlot;

import java.util.List;

/** The milpa (schema v2). Like {@link AssignmentDao}, there is no wipe-all method (SEC-1-1). */
@Dao
public abstract class FarmDao {

    /** Assignments it takes to earn one seed. */
    public static final int CREDITS_PER_SEED = 3;

    @Query("SELECT COUNT(*) FROM farm_credits")
    public abstract LiveData<Integer> observeCreditCount();

    /** Oldest first, so the field fills in planting order. */
    @Query("SELECT * FROM farm_plots ORDER BY plantedAt, id")
    public abstract LiveData<List<FarmPlot>> observePlots();

    /** A second mark done of the same assignment keeps the first credit (and its time). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertCredit(FarmCredit credit);

    /**
     * Undo of a mark done: removes the credit only if that mark done created it (earned at or
     * after {@code since}), never an older one from an earlier completion.
     */
    @Query("DELETE FROM farm_credits WHERE assignmentId = :assignmentId AND earnedAt >= :since")
    public abstract int deleteCreditSince(String assignmentId, long since);

    @Query("SELECT COUNT(*) FROM farm_credits")
    abstract int creditCount();

    @Query("SELECT COUNT(*) FROM farm_plots")
    abstract int plotCount();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract void insertPlot(FarmPlot plot);

    /**
     * Plants {@code plot} if a seed is available, checked and written in one transaction so two
     * fast taps can't spend the same seed.
     *
     * @return whether it was planted
     */
    @Transaction
    public boolean plantIfSeedAvailable(FarmPlot plot) {
        if (creditCount() / CREDITS_PER_SEED <= plotCount()) return false;
        insertPlot(plot);
        return true;
    }
}
