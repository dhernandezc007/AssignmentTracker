package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.entity.FarmPlot;

import java.util.List;

/** The milpa's door to its data: credits from finished assignments, and the planted plots. */
public interface FarmRepository extends CompletionLedger {

    /** Called once when planting finishes, possibly on a background thread. */
    interface PlantCallback {
        /** {@code planted} is false when no seed was available; {@code error} is null on success. */
        void onComplete(boolean planted, @Nullable Throwable error);
    }

    /** How many assignments have been credited. Emits again on every change. */
    @NonNull
    LiveData<Integer> observeCreditCount();

    /** Every plot, oldest first. */
    @NonNull
    LiveData<List<FarmPlot>> observePlots();

    /** Plants one seed of {@code crop} at {@code at}, if one is available. */
    void plant(@NonNull String crop, long at, @NonNull PlantCallback done);
}
