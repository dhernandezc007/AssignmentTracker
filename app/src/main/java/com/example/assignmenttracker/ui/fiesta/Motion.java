package com.example.assignmenttracker.ui.fiesta;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

/** Whether decorative motion should play. "Remove animations" turns the fiesta off. */
public final class Motion {

    private Motion() {
    }

    public static boolean enabled(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return ValueAnimator.areAnimatorsEnabled();
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }
}
