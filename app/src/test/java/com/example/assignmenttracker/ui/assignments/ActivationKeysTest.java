package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;

/** design.md §5.2 (Designer's condition): Enter and Space on the focused due field open the picker. */
public class ActivationKeysTest {

    @Test
    public void enterSpaceAndDpadCenterActivate() {
        assertTrue(ActivationKeys.activates(KeyEvent.KEYCODE_ENTER));
        assertTrue(ActivationKeys.activates(KeyEvent.KEYCODE_NUMPAD_ENTER));
        assertTrue(ActivationKeys.activates(KeyEvent.KEYCODE_SPACE));
        assertTrue(ActivationKeys.activates(KeyEvent.KEYCODE_DPAD_CENTER));
    }

    @Test
    public void otherKeysDoNot() {
        assertFalse(ActivationKeys.activates(KeyEvent.KEYCODE_TAB));
        assertFalse(ActivationKeys.activates(KeyEvent.KEYCODE_A));
        assertFalse(ActivationKeys.activates(KeyEvent.KEYCODE_DPAD_DOWN));
        assertFalse(ActivationKeys.activates(KeyEvent.KEYCODE_ESCAPE));
    }
}
