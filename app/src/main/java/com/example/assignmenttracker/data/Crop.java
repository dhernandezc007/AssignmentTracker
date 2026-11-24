package com.example.assignmenttracker.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * What can be planted in the milpa: maíz, frijol and calabaza (the three sisters), plus chile.
 * Stored by {@link #name()}; the order here is the order in the chooser.
 */
public enum Crop {
    MAIZ, FRIJOL, CALABAZA, CHILE;

    /** The stored name back to a crop, or null if it isn't one. */
    @Nullable
    public static Crop parse(@Nullable String stored) {
        if (stored == null) return null;
        for (Crop c : values()) {
            if (c.name().equals(stored)) return c;
        }
        return null;
    }

    /** Like {@link #parse} but never null: an unknown value draws as maíz. */
    @NonNull
    public static Crop parseOrMaiz(@Nullable String stored) {
        Crop c = parse(stored);
        return c != null ? c : MAIZ;
    }
}
