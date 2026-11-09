package com.example.assignmenttracker;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * QA-0-1: with the keyboard open, the focused field must end up fully above it,
 * including a field near the bottom of the screen (SignUp's Confirm password).
 */
@RunWith(AndroidJUnit4.class)
public class SystemBarInsetsImeTest {

    /** The emulator may have a hardware keyboard attached, which hides the soft one by default. */
    @Before
    public void showSoftKeyboardEvenWithHardKeyboard() throws Exception {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put secure show_ime_with_hard_keyboard 1").close();
    }

    @Test
    public void focusedBottomFieldIsScrolledFullyAboveKeyboard() {
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            onView(withId(R.id.etConfirm)).perform(click());

            // [0] IME height, [1] field bottom (window coords), [2] window height
            final int[] m = new int[3];
            boolean visible = false;
            long deadline = SystemClock.uptimeMillis() + 8000;
            while (SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity(a -> {
                    View decor = a.getWindow().getDecorView();
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
                    m[0] = insets == null ? 0 : insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                    View field = a.findViewById(R.id.etConfirm);
                    int[] loc = new int[2];
                    field.getLocationInWindow(loc);
                    m[1] = loc[1] + field.getHeight();
                    m[2] = decor.getHeight();
                });
                if (m[0] > 0 && m[1] <= m[2] - m[0]) {
                    visible = true;
                    break;
                }
                SystemClock.sleep(200);
            }
            assertTrue("soft keyboard never appeared", m[0] > 0);
            assertTrue("Confirm password bottom " + m[1] + "px is behind the keyboard top "
                    + (m[2] - m[0]) + "px", visible);
        }
    }
}
