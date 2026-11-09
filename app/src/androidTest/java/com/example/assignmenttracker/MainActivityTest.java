package com.example.assignmenttracker;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.os.SystemClock;
import android.view.View;

import android.view.WindowManager;
import android.widget.Button;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.ui.assignments.AddAssignmentDialogFragment;
import com.example.assignmenttracker.ui.assignments.HomeItem;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Slice 1 end to end: empty state, FAB, title-only dialog, inline error, new row. */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Before
    public void clearDatabase() {
        App app = ApplicationProvider.getApplicationContext();
        app.getDatabase().assignmentDao().deleteAll();
    }

    @Test
    public void emptyStateShowsWhenThereAreNoAssignments() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.empty_title)).check(matches(isDisplayed()));
            onView(withText(R.string.empty_body)).check(matches(isDisplayed()));
            onView(withId(R.id.fabAdd)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void emptyTitleIsRejectedInline() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());
            waitForDialogSettled(s);
            onView(withText(R.string.action_save)).perform(click());
            // Dialog stays open and the field shows the error.
            onView(withId(R.id.tilTitle)).check((v, e) -> {
                if (e != null) throw e;
                CharSequence err = ((TextInputLayout) v).getError();
                assertEquals(v.getContext().getString(R.string.field_title_error_empty),
                        err == null ? null : err.toString());
            });
            onView(withText(R.string.dialog_add_title)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void savingATitleAddsARowImmediately() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());
            waitForDialogSettled(s);
            // Keep the keyboard up: closing it re-centres the dialog (adjustResize), and a click
            // computed mid-move lands outside it (touch-outside dismisses, nothing saved).
            onView(withId(R.id.etTitle)).perform(typeText("Essay outline"));
            waitForDialogSettled(s);
            onView(withText(R.string.action_save)).perform(click());
            onView(withText(R.string.dialog_add_title)).check(doesNotExist());

            // The write is async (repository executor), so wait for the list to hold just the row.
            long deadline = SystemClock.uptimeMillis() + 5000;
            final int[] state = new int[2]; // [0] item count, [1] first view type
            while (SystemClock.uptimeMillis() < deadline) {
                s.onActivity(a -> {
                    RecyclerView.Adapter<?> ad = ((RecyclerView) a.findViewById(R.id.list)).getAdapter();
                    state[0] = ad.getItemCount();
                    state[1] = ad.getItemCount() > 0 ? ad.getItemViewType(0) : -1;
                });
                if (state[0] == 1 && state[1] == HomeItem.TYPE_ROW) break;
                SystemClock.sleep(100);
            }
            assertEquals("one item", 1, state[0]);
            assertEquals("it is a row, not the empty state", HomeItem.TYPE_ROW, state[1]);
            onView(withText("Essay outline")).check(matches(isDisplayed()));
        }
    }

    /** QA-1-3: in landscape the side (cutout) inset goes to the text, not the decorative banner. */
    @Test
    public void bannerBleedsToTheEdgesWhileTextTakesTheSideInsets() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            s.onActivity(a -> {
                View root = a.findViewById(R.id.main);
                WindowInsetsCompat fake = new WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.systemBars(), androidx.core.graphics.Insets.of(156, 63, 48, 0))
                        .build();
                ViewCompat.dispatchApplyWindowInsets(root, fake);
                root.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(root.getHeight(), View.MeasureSpec.EXACTLY));
                root.layout(root.getLeft(), root.getTop(), root.getRight(), root.getBottom());

                View banner = a.findViewById(R.id.banner);
                int[] loc = new int[2];
                banner.getLocationInWindow(loc);
                assertEquals("banner starts at the left edge", 0, loc[0]);
                assertEquals("banner reaches the right edge", root.getWidth(), loc[0] + banner.getWidth());

                int side = a.getResources().getDimensionPixelSize(R.dimen.space_screen);
                View title = a.findViewById(R.id.title);
                assertEquals("title clears the left cutout", side + 156, title.getPaddingLeft());
                assertEquals("title clears the right inset", side + 48, title.getPaddingRight());
                assertEquals("header clears the status bar", 63, a.findViewById(R.id.header).getPaddingTop());
                View list = a.findViewById(R.id.list);
                assertEquals("cards clear the left cutout", 156, list.getPaddingLeft());
                assertEquals(48, list.getPaddingRight());
            });
        }
    }

    /**
     * QA-1-1 root cause: a dialog window created with SOFT_INPUT_STATE_VISIBLE has its first
     * frame held until the IME is ready, so it stayed invisible (and untappable) for ~1.5 s.
     * The window must not ask for the IME at creation, yet the keyboard still opens by itself.
     */
    @Test
    public void addDialogDoesNotHoldItsFirstFrameForTheKeyboard() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());
            waitForDialogSettled(s);
            s.onActivity(a -> {
                DialogFragment f = (DialogFragment) a.getSupportFragmentManager()
                        .findFragmentByTag(AddAssignmentDialogFragment.TAG);
                int state = f.getDialog().getWindow().getAttributes().softInputMode
                        & WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE;
                assertNotEquals("dialog window must not request the IME at creation",
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE, state);
                assertNotEquals(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE, state);
            });
            final boolean[] imeUp = {false};
            long deadline = SystemClock.uptimeMillis() + 8000;
            while (!imeUp[0] && SystemClock.uptimeMillis() < deadline) {
                s.onActivity(a -> {
                    DialogFragment f = (DialogFragment) a.getSupportFragmentManager()
                            .findFragmentByTag(AddAssignmentDialogFragment.TAG);
                    WindowInsetsCompat in = ViewCompat.getRootWindowInsets(f.getDialog().getWindow().getDecorView());
                    imeUp[0] = in != null && in.isVisible(WindowInsetsCompat.Type.ime());
                });
                if (!imeUp[0]) SystemClock.sleep(100);
            }
            assertEquals("keyboard should open for the title field (design.md §5.2)", true, imeUp[0]);
        }
    }

    /**
     * Waits until the add dialog has window focus, the keyboard is up, and its Save button has
     * stopped moving.
     * The dialog re-centres when the soft keyboard arrives (adjustResize); Espresso computes
     * the click point and then injects it, so a move in between would miss (QA-1-1).
     */
    private static void waitForDialogSettled(ActivityScenario<MainActivity> s) {
        long deadline = SystemClock.uptimeMillis() + 8000;
        int[] last = {-1, -1};
        long stableSince = 0;
        while (SystemClock.uptimeMillis() < deadline) {
            final int[] now = {-1, -1};
            final boolean[] focused = {false};
            s.onActivity(a -> {
                Fragment f = a.getSupportFragmentManager().findFragmentByTag(AddAssignmentDialogFragment.TAG);
                if (!(f instanceof DialogFragment)) return;
                AlertDialog d = (AlertDialog) ((DialogFragment) f).getDialog();
                if (d == null || d.getWindow() == null) return;
                focused[0] = d.getWindow().getDecorView().hasWindowFocus();
                WindowInsetsCompat in = ViewCompat.getRootWindowInsets(d.getWindow().getDecorView());
                // The app always opens the keyboard; settled means after it has arrived.
                focused[0] = focused[0] && in != null && in.isVisible(WindowInsetsCompat.Type.ime());
                Button save = d.getButton(AlertDialog.BUTTON_POSITIVE);
                if (save != null && save.isShown()) save.getLocationOnScreen(now);
            });
            long t = SystemClock.uptimeMillis();
            if (focused[0] && now[1] >= 0 && now[0] == last[0] && now[1] == last[1]) {
                if (stableSince == 0) stableSince = t;
                if (t - stableSince >= 600) return;
            } else {
                stableSince = 0;
            }
            last = now;
            SystemClock.sleep(100);
        }
        throw new AssertionError("add dialog never settled (focus + stable Save position)");
    }

    @Test
    public void bannerIsDecorativeAndTitleIsAHeading() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            s.onActivity(a -> {
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                        a.findViewById(R.id.banner).getImportantForAccessibility());
                assertEquals(true, androidx.core.view.ViewCompat.isAccessibilityHeading(a.findViewById(R.id.title)));
            });
        }
    }
}
