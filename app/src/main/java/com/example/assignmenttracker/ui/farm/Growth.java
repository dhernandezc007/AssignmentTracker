package com.example.assignmenttracker.ui.farm;

import androidx.annotation.NonNull;

/** How far a plot has grown. Pure: plantedAt and now in, fraction and stage out. */
public final class Growth {

    public static final long DAY_MS = 24L * 60 * 60 * 1000;
    /** Seed to fully grown. */
    public static final long GROW_MS = 7 * DAY_MS;

    public enum Stage { SEED, SPROUT, GROWING, FLOWERING, GROWN }

    private Growth() {
    }

    /** 0 at planting, 1 when fully grown. A clock that went backwards reads as 0. */
    public static float fraction(long plantedAt, long now) {
        long elapsed = now - plantedAt;
        if (elapsed <= 0) return 0f;
        if (elapsed >= GROW_MS) return 1f;
        return elapsed / (float) GROW_MS;
    }

    /** Seed for the first ~17 h, then sprout, growing, flowering, and grown at day 7. */
    @NonNull
    public static Stage stage(float fraction) {
        if (fraction >= 1f) return Stage.GROWN;
        if (fraction >= 0.7f) return Stage.FLOWERING;
        if (fraction >= 0.35f) return Stage.GROWING;
        if (fraction >= 0.1f) return Stage.SPROUT;
        return Stage.SEED;
    }

    /** Day of growth, starting at day 1 on the day it was planted. */
    public static int day(long plantedAt, long now) {
        long elapsed = Math.max(0, now - plantedAt);
        return (int) (elapsed / DAY_MS) + 1;
    }
}
