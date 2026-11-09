package com.example.assignmenttracker;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.ui.assignments.HomeItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Home end to end: empty state, FAB → new screen, row tap → edit screen, save and discard. */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Before
    public void clearDatabase() {
        TestDb.clear();
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
    public void fabOpensTheNewAssignmentScreenAndSaveAddsTheRowWithItsCourse() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());
            EditScreenWaits.settled(true);
            onView(withText(R.string.edit_title_new)).check(matches(isDisplayed()));
            onView(withId(R.id.etTitle)).perform(replaceText("Essay outline"));
            onView(withId(R.id.etCourse)).perform(replaceText("English 101"));
            EditScreenWaits.settled(true);
            onView(withId(R.id.btnSave)).perform(click());

            assertTrue("the list holds the new row", EditScreenWaits.eventually(() -> firstType(s) == HomeItem.TYPE_ROW));
            onView(withText("Essay outline")).check(matches(isDisplayed()));
            onView(withText("English 101")).check(matches(isDisplayed()));
            assertEquals(1, TestDb.count());
        }
    }

    @Test
    public void rowWithoutACourseHidesLineTwo() {
        TestDb.insert(Assignment.newTodo("a", "Quiz", 1));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            assertTrue(EditScreenWaits.eventually(() -> firstType(s) == HomeItem.TYPE_ROW));
            s.onActivity(a -> {
                RecyclerView list = a.findViewById(R.id.list);
                View row = list.getChildAt(0);
                assertEquals(View.GONE, row.findViewById(R.id.rowMeta).getVisibility());
            });
        }
    }

    @Test
    public void tappingARowOpensItForEditingWithTheFieldsFilledIn() {
        Assignment a = Assignment.newTodo("r1", "Lab report: enzyme activity", 1_000L);
        a.course = "Biology 101";
        a.notes = "Use Tuesday's data.";
        TestDb.insert(a);
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Lab report: enzyme activity")).perform(click());
            EditScreenWaits.settled(false);
            onView(withText(R.string.edit_title_edit)).check(matches(isDisplayed()));
            onView(withId(R.id.etTitle)).check(matches(withText("Lab report: enzyme activity")));
            onView(withId(R.id.etCourse)).check(matches(withText("Biology 101")));
            onView(withId(R.id.etNotes)).check(matches(withText("Use Tuesday's data.")));
        }
    }

    /** The edit-save path: the row and its updatedAt change; the id and createdAt stay. */
    @Test
    public void editSaveUpdatesTheRowAndUpdatedAt() {
        Assignment a = Assignment.newTodo("r1", "Lab report", 1_000L);
        a.course = "Biology 101";
        TestDb.insert(a);
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Lab report")).perform(click());
            EditScreenWaits.settled(false);
            onView(withId(R.id.etTitle)).perform(replaceText("Lab report v2"));
            onView(withId(R.id.etCourse)).perform(replaceText("Chem 110"));
            closeSoftKeyboard();
            EditScreenWaits.settled(false);
            onView(withId(R.id.btnSave)).perform(click());

            assertTrue("row updated in the DB", EditScreenWaits.eventually(() -> {
                Object[] r = TestDb.row("r1");
                return r != null && "Lab report v2".equals(r[0]);
            }));
            Object[] r = TestDb.row("r1");
            assertEquals("Chem 110", r[1]);
            assertTrue("updatedAt moved forward", (long) r[3] > 1_000L);
            assertEquals("createdAt kept", 1_000L, (long) r[4]);
            assertEquals("no new row", 1, TestDb.count());
            // The change animation cross-fades an old and a new view for a moment, so check the
            // adapter's data rather than the absence of the old view.
            assertTrue("the list shows the edit", EditScreenWaits.eventually(() -> {
                String[] t = {null};
                s.onActivity(act -> {
                    RecyclerView list = act.findViewById(R.id.list);
                    com.example.assignmenttracker.ui.assignments.AssignmentListAdapter ad =
                            (com.example.assignmenttracker.ui.assignments.AssignmentListAdapter) list.getAdapter();
                    if (ad != null && ad.getItemCount() == 1 && ad.getCurrentList().get(0).getAssignment() != null) {
                        t[0] = ad.getCurrentList().get(0).getAssignment().title;
                    }
                });
                return "Lab report v2".equals(t[0]);
            }));
            onView(withText("Lab report v2")).check(matches(isDisplayed()));
            onView(withText("Chem 110")).check(matches(isDisplayed()));
        }
    }

    /** Cancel discards: the ✕ leaves without saving. */
    @Test
    public void closeLeavesAnEditWithoutSaving() {
        TestDb.insert(Assignment.newTodo("r1", "Lab report", 1_000L));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Lab report")).perform(click());
            EditScreenWaits.settled(false);
            onView(withId(R.id.etTitle)).perform(replaceText("Should not be saved"));
            onView(withContentDescription(R.string.edit_close_cd)).perform(click());

            onView(withText("Lab report")).check(matches(isDisplayed()));
            onView(withText("Should not be saved")).check(doesNotExist());
            Object[] r = TestDb.row("r1");
            assertEquals("Lab report", r[0]);
            assertEquals("updatedAt untouched", 1_000L, (long) r[3]);
        }
    }

    /** Cancel discards: system Back leaves without saving. */
    @Test
    public void backLeavesAnEditWithoutSaving() {
        TestDb.insert(Assignment.newTodo("r1", "Lab report", 1_000L));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Lab report")).perform(click());
            EditScreenWaits.settled(false);
            onView(withId(R.id.etTitle)).perform(replaceText("Should not be saved"));
            closeSoftKeyboard();
            pressBack();

            onView(withText("Lab report")).check(matches(isDisplayed()));
            assertEquals("Lab report", TestDb.row("r1")[0]);
            assertEquals(1_000L, (long) TestDb.row("r1")[3]);
        }
    }

    /** Cancel discards on a new item too: nothing is inserted. */
    @Test
    public void closingANewAssignmentSavesNothing() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());
            EditScreenWaits.settled(true);
            onView(withId(R.id.etTitle)).perform(replaceText("Draft I gave up on"));
            onView(withContentDescription(R.string.edit_close_cd)).perform(click());

            onView(withText(R.string.empty_title)).check(matches(isDisplayed()));
            assertEquals(0, TestDb.count());
        }
    }

    /** DES-1-4: rows are focusable and clickable so a keyboard user can reach and open them. */
    @Test
    public void rowsAreFocusableAndClickable() {
        TestDb.insert(Assignment.newTodo("a", "Quiz", 1));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            assertTrue(EditScreenWaits.eventually(() -> firstType(s) == HomeItem.TYPE_ROW));
            s.onActivity(a -> {
                View row = ((RecyclerView) a.findViewById(R.id.list)).getChildAt(0);
                assertTrue(row.isFocusable());
                assertTrue(row.isClickable());
                assertNotNull("ripple / focus state", row.getForeground());
                assertTrue("ripple clipped to the row shape", row.getClipToOutline());
            });
        }
    }

    /**
     * DES-1-4 follow-up: the list container itself never takes focus, so the platform's grey
     * default focus highlight can't cover the whole list (seen with a hardware keyboard).
     * Keyboard focus lands on the rows, which draw their own focus state.
     */
    @Test
    public void listContainerIsNotFocusableSoOnlyRowsShowFocus() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            s.onActivity(a -> {
                View list = a.findViewById(R.id.list);
                assertTrue(!list.isFocusable() && !list.isFocused());
            });
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

    @Test
    public void bannerIsDecorativeAndTitleIsAHeading() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            s.onActivity(a -> {
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                        a.findViewById(R.id.banner).getImportantForAccessibility());
                assertEquals(true, ViewCompat.isAccessibilityHeading(a.findViewById(R.id.title)));
            });
        }
    }

    private static int firstType(ActivityScenario<MainActivity> s) {
        int[] t = {-1};
        s.onActivity(a -> {
            RecyclerView.Adapter<?> ad = ((RecyclerView) a.findViewById(R.id.list)).getAdapter();
            t[0] = ad != null && ad.getItemCount() > 0 ? ad.getItemViewType(0) : -1;
        });
        return t[0];
    }
}
