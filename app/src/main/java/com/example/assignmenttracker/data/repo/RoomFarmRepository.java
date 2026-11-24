package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.Crop;
import com.example.assignmenttracker.data.dao.FarmDao;
import com.example.assignmenttracker.data.entity.FarmCredit;
import com.example.assignmenttracker.data.entity.FarmPlot;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/** Room implementation. Writes run on the given executor; reads are Room LiveData. */
public final class RoomFarmRepository implements FarmRepository {

    private final FarmDao dao;
    private final ExecutorService io;

    public RoomFarmRepository(@NonNull FarmDao dao, @NonNull ExecutorService io) {
        this.dao = dao;
        this.io = io;
    }

    @NonNull
    @Override
    public LiveData<Integer> observeCreditCount() {
        return dao.observeCreditCount();
    }

    @NonNull
    @Override
    public LiveData<List<FarmPlot>> observePlots() {
        return dao.observePlots();
    }

    @Override
    public void credit(@NonNull String assignmentId, long at) {
        io.execute(() -> {
            try {
                dao.insertCredit(FarmCredit.of(assignmentId, at));
            } catch (RuntimeException ignored) {
                // Farm progress is a bonus; the assignment write is what matters.
            }
        });
    }

    @Override
    public void uncredit(@NonNull String assignmentId, long since) {
        io.execute(() -> {
            try {
                dao.deleteCreditSince(assignmentId, since);
            } catch (RuntimeException ignored) {
                // As above.
            }
        });
    }

    @Override
    public void plant(@NonNull String crop, long at, @NonNull PlantCallback done) {
        if (Crop.parse(crop) == null) {
            done.onComplete(false, new IllegalArgumentException("unknown crop"));
            return;
        }
        io.execute(() -> {
            boolean planted;
            try {
                planted = dao.plantIfSeedAvailable(FarmPlot.of(UUID.randomUUID().toString(), crop, at));
            } catch (RuntimeException e) {
                done.onComplete(false, e);
                return;
            }
            done.onComplete(planted, null);
        });
    }
}
