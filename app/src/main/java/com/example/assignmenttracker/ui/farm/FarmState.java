package com.example.assignmenttracker.ui.farm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.Crop;
import com.example.assignmenttracker.data.dao.FarmDao;
import com.example.assignmenttracker.data.entity.FarmPlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Everything the milpa screen and the home button show. Pure, so it's unit-tested. */
public final class FarmState {

    /** One plot as drawn: crop, how grown, and its day. */
    public static final class Plot {
        @NonNull public final String id;
        @NonNull public final Crop crop;
        public final long plantedAt;
        public final float growth;
        @NonNull public final Growth.Stage stage;
        public final int day;

        Plot(@NonNull String id, @NonNull Crop crop, long plantedAt, float growth, int day) {
            this.id = id;
            this.crop = crop;
            this.plantedAt = plantedAt;
            this.growth = growth;
            this.stage = Growth.stage(growth);
            this.day = day;
        }
    }

    public final int credits;
    /** Seeds earned and not planted yet. */
    public final int seeds;
    /** Assignments toward the next seed, 0..CREDITS_PER_SEED-1. */
    public final int progress;
    public final int grown;
    @NonNull public final List<Plot> plots;

    private FarmState(int credits, int seeds, int progress, int grown, @NonNull List<Plot> plots) {
        this.credits = credits;
        this.seeds = seeds;
        this.progress = progress;
        this.grown = grown;
        this.plots = plots;
    }

    @NonNull
    public static FarmState of(int credits, @Nullable List<FarmPlot> planted, long now) {
        List<FarmPlot> rows = planted != null ? planted : Collections.emptyList();
        int per = FarmDao.CREDITS_PER_SEED;
        // An Undo after planting can leave fewer credits than plots need; never show negative seeds.
        int seeds = Math.max(0, credits / per - rows.size());
        List<Plot> plots = new ArrayList<>(rows.size());
        int grown = 0;
        for (FarmPlot p : rows) {
            float g = Growth.fraction(p.plantedAt, now);
            if (g >= 1f) grown++;
            plots.add(new Plot(p.id, Crop.parseOrMaiz(p.crop), p.plantedAt, g, Growth.day(p.plantedAt, now)));
        }
        return new FarmState(credits, seeds, Math.max(0, credits) % per, grown,
                Collections.unmodifiableList(plots));
    }
}
