package com.example.assignmenttracker.ui.assignments;

import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Keyboard activation for read-only fields that open something on tap (design.md §5.2): Enter,
 * numpad Enter, Space and D-pad center act like a click.
 */
public final class ActivationKeys {

    private ActivationKeys() {
    }

    public static boolean activates(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                || keyCode == KeyEvent.KEYCODE_SPACE
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER;
    }

    /** Clicks {@code v} on the key up of an activation key; the key down is consumed too. */
    @NonNull
    public static View.OnKeyListener clickOnActivation() {
        return (v, keyCode, event) -> {
            if (!activates(keyCode)) return false;
            if (event.getAction() == KeyEvent.ACTION_UP && event.getRepeatCount() == 0) v.performClick();
            return true;
        };
    }
}
