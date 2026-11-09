package com.example.assignmenttracker;

import android.app.Activity;
import android.os.SystemClock;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.assignmenttracker.ui.assignments.AssignmentEditActivity;

import java.util.Collection;

/**
 * Waits for the add/edit screen to stop moving before a click. The Save button rides above
 * the keyboard, so it moves when the IME arrives; Espresso computes a click point and then
 * injects it, and a move in between would miss (the QA-1-1 lesson from Slice 1).
 */
public final class EditScreenWaits {

    private EditScreenWaits() {
    }

    /** The resumed edit screen, or null. Call from the instrumentation thread. */
    public static AssignmentEditActivity resumedEditScreen() {
        AssignmentEditActivity[] out = {null};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            for (Activity a : resumed) {
                if (a instanceof AssignmentEditActivity) out[0] = (AssignmentEditActivity) a;
            }
        });
        return out[0];
    }

    /**
     * Waits until the edit screen is resumed with window focus, the keyboard is in the expected
     * state, and Save has held still for 600 ms.
     */
    public static void settled(boolean expectIme) {
        long deadline = SystemClock.uptimeMillis() + 10_000;
        int[] last = {-1, -1};
        long stableSince = 0;
        while (SystemClock.uptimeMillis() < deadline) {
            int[] now = {-1, -1};
            boolean[] ready = {false};
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Collection<Activity> resumed =
                        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                for (Activity a : resumed) {
                    if (!(a instanceof AssignmentEditActivity)) continue;
                    View decor = a.getWindow().getDecorView();
                    WindowInsetsCompat in = ViewCompat.getRootWindowInsets(decor);
                    boolean ime = in != null && in.isVisible(WindowInsetsCompat.Type.ime());
                    ready[0] = decor.hasWindowFocus() && ime == expectIme;
                    View save = a.findViewById(R.id.btnSave);
                    if (save != null && save.isShown()) save.getLocationOnScreen(now);
                }
            });
            long t = SystemClock.uptimeMillis();
            if (ready[0] && now[1] >= 0 && now[0] == last[0] && now[1] == last[1]) {
                if (stableSince == 0) stableSince = t;
                if (t - stableSince >= 600) return;
            } else {
                stableSince = 0;
            }
            last = now;
            SystemClock.sleep(100);
        }
        throw new AssertionError("edit screen never settled (focus, keyboard=" + expectIme + ", stable Save)");
    }

    /** Polls until {@code check} is true or 5 s pass. */
    public static boolean eventually(java.util.function.BooleanSupplier check) {
        long deadline = SystemClock.uptimeMillis() + 5_000;
        while (SystemClock.uptimeMillis() < deadline) {
            if (check.getAsBoolean()) return true;
            SystemClock.sleep(100);
        }
        return check.getAsBoolean();
    }
}
