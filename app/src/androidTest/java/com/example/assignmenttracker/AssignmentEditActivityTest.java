package com.example.assignmenttracker;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.ui.assignments.AssignmentEditActivity;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Slice 2 add/edit screen: inline title error, rotation keeps the input, Save rides the keyboard. */
@RunWith(AndroidJUnit4.class)
public class AssignmentEditActivityTest {

    private final Context ctx = ApplicationProvider.getApplicationContext();

    @Before
    public void clearDatabase() {
        TestDb.clear();
    }

    @After
    public void unlockOrientation() {
        AssignmentEditActivity a = EditScreenWaits.resumedEditScreen();
        if (a != null) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    () -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
        }
    }

    @Test
    public void emptyTitleIsRejectedInlineAndTheErrorClearsOnTyping() {
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(newIntent())) {
            EditScreenWaits.settled(true);
            onView(withId(R.id.btnSave)).perform(click());
            assertEquals(ctx.getString(R.string.field_title_error_empty), titleError(s));
            onView(withText(R.string.edit_title_new)).check(matches(isDisplayed()));
            assertEquals("nothing saved", 0, TestDb.count());
            s.onActivity(a -> assertTrue("focus goes back to the title", a.findViewById(R.id.etTitle).hasFocus()));

            onView(withId(R.id.etTitle)).perform(typeText("E"));
            assertNull("error clears on the next keystroke", titleError(s));
        }
    }

    @Test
    public void whitespaceTitleIsRejected() {
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(newIntent())) {
            EditScreenWaits.settled(true);
            onView(withId(R.id.etTitle)).perform(replaceText("    "));
            EditScreenWaits.settled(true);
            onView(withId(R.id.btnSave)).perform(click());
            assertEquals(ctx.getString(R.string.field_title_error_empty), titleError(s));
            assertEquals(0, TestDb.count());
        }
    }

    /** Rotating mid-edit keeps the typed text in every field (and the inline error). */
    @Test
    public void rotationKeepsTypedTextOnANewAssignment() {
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(newIntent())) {
            EditScreenWaits.settled(true);
            onView(withId(R.id.etCourse)).perform(replaceText("Biology 101"));
            onView(withId(R.id.etNotes)).perform(replaceText("Two pages\nCite the lab"));
            onView(withId(R.id.btnSave)).perform(click()); // blank title: inline error
            onView(withId(R.id.etTitle)).perform(replaceText("Lab report"));
            s.onActivity(a -> ((TextInputLayout) a.findViewById(R.id.tilTitle))
                    .setError(a.getString(R.string.field_title_error_empty)));

            rotateTo(s, Configuration.ORIENTATION_LANDSCAPE);
            assertFields(s, "Lab report", "Biology 101", "Two pages\nCite the lab");
            assertEquals("error survives rotation", ctx.getString(R.string.field_title_error_empty), titleError(s));

            rotateTo(s, Configuration.ORIENTATION_PORTRAIT);
            assertFields(s, "Lab report", "Biology 101", "Two pages\nCite the lab");
        }
    }

    /** On edit, the saved values are loaded once: a rotation must not overwrite what was typed. */
    @Test
    public void rotationKeepsTypedTextOnAnEdit() {
        Assignment a = Assignment.newTodo("r1", "Original title", 1_000L);
        a.course = "Original course";
        TestDb.insert(a);
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(editIntent("r1"))) {
            EditScreenWaits.settled(false);
            onView(withId(R.id.etTitle)).check(matches(withText("Original title")));
            onView(withId(R.id.etTitle)).perform(replaceText("Typed title"));
            onView(withId(R.id.etCourse)).perform(replaceText(""));

            rotateTo(s, Configuration.ORIENTATION_LANDSCAPE);
            assertFields(s, "Typed title", "", "");

            s.recreate(); // also survives a plain recreate
            assertFields(s, "Typed title", "", "");
            assertEquals("nothing saved yet", "Original title", TestDb.row("r1")[0]);
        }
    }

    /** design.md §5.1: Save sits above the keyboard (SystemBarInsets on the root). */
    @Test
    public void saveRidesAboveTheKeyboard() {
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(newIntent())) {
            EditScreenWaits.settled(true);
            s.onActivity(a -> {
                View decor = a.getWindow().getDecorView();
                WindowInsetsCompat in = ViewCompat.getRootWindowInsets(decor);
                int imeTop = decor.getHeight() - in.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                View save = a.findViewById(R.id.btnSave);
                int[] loc = new int[2];
                save.getLocationInWindow(loc);
                int saveBottom = loc[1] + save.getHeight();
                assertTrue("Save bottom " + saveBottom + " must be above the keyboard top " + imeTop,
                        saveBottom <= imeTop);
            });
        }
    }

    /**
     * QA-1-1 lesson: a window that asks for the IME at creation has its first frame held until
     * the keyboard is ready. The screen must not, yet the keyboard still opens on a new item.
     */
    @Test
    public void newScreenOpensTheKeyboardWithoutHoldingItsFirstFrame() {
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(newIntent())) {
            s.onActivity(a -> {
                int state = a.getWindow().getAttributes().softInputMode
                        & WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE;
                assertNotEquals(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE, state);
                assertNotEquals(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE, state);
            });
            EditScreenWaits.settled(true);
            s.onActivity(a -> assertTrue(a.findViewById(R.id.etTitle).hasFocus()));
        }
    }

    /** An edit opens without the keyboard, so the filled-in fields are all visible. */
    @Test
    public void editScreenDoesNotOpenTheKeyboard() {
        TestDb.insert(Assignment.newTodo("r1", "Quiz", 1_000L));
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(editIntent("r1"))) {
            EditScreenWaits.settled(false);
        }
    }

    /** DES-0-6: unfocused outlines are warm brown (colorOutline), not MDC's grey. */
    @Test
    public void unfocusedOutlineIsWarmBrown() {
        TestDb.insert(Assignment.newTodo("r1", "Quiz", 1_000L));
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(editIntent("r1"))) {
            EditScreenWaits.settled(false);
            s.onActivity(a -> {
                TextInputLayout course = a.findViewById(R.id.tilCourse);
                assertFalse(course.getEditText().hasFocus());
                // MDC has no public getter for the resolved unfocused stroke; read its field.
                int stroke;
                try {
                    java.lang.reflect.Field f = TextInputLayout.class.getDeclaredField("defaultStrokeColor");
                    f.setAccessible(true);
                    stroke = f.getInt(course);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
                assertEquals("default stroke = mx_brown_muted", Color.parseColor("#6E5240"), stroke);
                assertEquals("focused stroke = mx_pink", Color.parseColor("#E4007C"), course.getBoxStrokeColor());
            });
        }
    }

    /** design.md §5.2: the notes counter shows only past 1800 characters. */
    @Test
    public void notesCounterAppearsLate() {
        TestDb.insert(Assignment.newTodo("r1", "Quiz", 1_000L));
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(editIntent("r1"))) {
            EditScreenWaits.settled(false);
            s.onActivity(a -> assertFalse(((TextInputLayout) a.findViewById(R.id.tilNotes)).isCounterEnabled()));
            onView(withId(R.id.etNotes)).perform(replaceText(repeat('n', 1801)));
            s.onActivity(a -> assertTrue(((TextInputLayout) a.findViewById(R.id.tilNotes)).isCounterEnabled()));
            onView(withId(R.id.etNotes)).perform(replaceText("short"));
            s.onActivity(a -> assertFalse(((TextInputLayout) a.findViewById(R.id.tilNotes)).isCounterEnabled()));
        }
    }

    /**
     * QA-2-1: once the late notes counter is on, typing at the end of a long note keeps the
     * counter in view (it used to sit below the visible area, under the pinned Save).
     */
    @Test
    public void notesCounterStaysInViewWhileTyping() {
        TestDb.insert(Assignment.newTodo("r1", "Quiz", 1_000L));
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 1795) sb.append("lab notes ");
        String start = sb.substring(0, 1795);
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(editIntent("r1"))) {
            EditScreenWaits.settled(false);
            s.onActivity(a -> {
                EditText notes = a.findViewById(R.id.etNotes);
                notes.requestFocus();
                notes.setText(start);
                notes.setSelection(notes.length());
            });
            for (int i = 0; i < 10; i++) {
                s.onActivity(a -> ((EditText) a.findViewById(R.id.etNotes)).getText().append('x'));
            }
            assertTrue("counter on", counterEnabled(s));
            assertTrue("the counter (bottom of the notes field) is inside the visible scroll area",
                    EditScreenWaits.eventually(() -> {
                        boolean[] ok = {false};
                        s.onActivity(a -> {
                            View til = a.findViewById(R.id.tilNotes);
                            View scroll = a.findViewById(R.id.scroll);
                            int[] t = new int[2];
                            int[] sc = new int[2];
                            til.getLocationOnScreen(t);
                            scroll.getLocationOnScreen(sc);
                            ok[0] = t[1] + til.getHeight() <= sc[1] + scroll.getHeight();
                        });
                        return ok[0];
                    }));
            s.onActivity(a -> {
                EditText notes = a.findViewById(R.id.etNotes);
                View scroll = a.findViewById(R.id.scroll);
                android.text.Layout layout = notes.getLayout();
                int line = layout.getLineForOffset(notes.getSelectionEnd());
                int[] n = new int[2];
                int[] sc = new int[2];
                notes.getLocationOnScreen(n);
                scroll.getLocationOnScreen(sc);
                int cursorTop = n[1] + notes.getTotalPaddingTop() + layout.getLineTop(line);
                assertTrue("the cursor line stays visible too", cursorTop >= sc[1]);
            });
        }
    }

    private static boolean counterEnabled(ActivityScenario<AssignmentEditActivity> s) {
        boolean[] on = {false};
        s.onActivity(a -> on[0] = ((TextInputLayout) a.findViewById(R.id.tilNotes)).isCounterEnabled());
        return on[0];
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    @Test
    public void unknownIdClosesTheScreen() {
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(editIntent("missing"))) {
            assertTrue(EditScreenWaits.eventually(() ->
                    s.getState() == androidx.lifecycle.Lifecycle.State.DESTROYED));
        }
    }

    // ---- helpers ----

    private Intent newIntent() {
        return AssignmentEditActivity.newIntent(ctx, null);
    }

    private Intent editIntent(String id) {
        return AssignmentEditActivity.newIntent(ctx, id);
    }

    private static String titleError(ActivityScenario<AssignmentEditActivity> s) {
        String[] out = {null};
        s.onActivity(a -> {
            CharSequence e = ((TextInputLayout) a.findViewById(R.id.tilTitle)).getError();
            out[0] = e == null ? null : e.toString();
        });
        return out[0];
    }

    private static void assertFields(ActivityScenario<AssignmentEditActivity> s,
                                     String title, String course, String notes) {
        s.onActivity(a -> {
            assertEquals(title, ((EditText) a.findViewById(R.id.etTitle)).getText().toString());
            assertEquals(course, ((EditText) a.findViewById(R.id.etCourse)).getText().toString());
            assertEquals(notes, ((EditText) a.findViewById(R.id.etNotes)).getText().toString());
        });
    }

    /** A real configuration change: request the orientation and wait for the new instance. */
    private static void rotateTo(ActivityScenario<AssignmentEditActivity> s, int orientation) {
        int[] before = {0};
        s.onActivity(a -> {
            before[0] = System.identityHashCode(a);
            a.setRequestedOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        });
        boolean rotated = EditScreenWaits.eventually(() -> {
            AssignmentEditActivity now = EditScreenWaits.resumedEditScreen();
            return now != null && System.identityHashCode(now) != before[0]
                    && now.getResources().getConfiguration().orientation == orientation;
        });
        assertTrue("activity was not recreated in orientation " + orientation, rotated);
    }
}
