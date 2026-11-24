package com.example.assignmenttracker.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One planted seed in the milpa (schema v2). It grows from {@code plantedAt} on and stays in
 * the field forever, so the field is a record of everything the student has grown.
 */
@Entity(tableName = "farm_plots")
public class FarmPlot {

    @PrimaryKey
    @NonNull
    public String id = "";

    /** A {@code Crop} name (MAIZ, FRIJOL, CALABAZA, CHILE). */
    @NonNull
    public String crop = "";

    /** Epoch millis. */
    public long plantedAt;

    @NonNull
    public static FarmPlot of(@NonNull String id, @NonNull String crop, long plantedAt) {
        FarmPlot p = new FarmPlot();
        p.id = id;
        p.crop = crop;
        p.plantedAt = plantedAt;
        return p;
    }
}
