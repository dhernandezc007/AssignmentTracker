package com.example.assignmenttracker;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.ui.assignments.AssignmentListAdapter;
import com.example.assignmenttracker.ui.assignments.HomeItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

/**
 * Slice 3 end to end: mark done (and Undo), the Done section, swipe delete (and Undo).
 * Slice 4: the list starts with the summary and bucket headers, both Undo paths stamp
 * updatedAt = now (SEC-3-1), and the checkbox is a sibling of the row body (QA-3-1).
 */
@RunWith(AndroidJUnit4.class)
public class HomeDoneDeleteTest {

    private final Context ctx = ApplicationProvider.getApplicationContext();

    @Before
    public void clearDatabase() {
        TestDb.clear();
    }

    @Test
    public void checkingARowMarksItDoneMovesItUnderDoneAndUndoRestoresIt() {
        Assignment quiz = Assignment.newTodo("r1", "Quiz", 2_000L);
        quiz.updatedAt = 2_500L;
        TestDb.insert(quiz);
        TestDb.insert(Assignment.newTodo("r2", "Essay", 1_000L));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 2);
            long beforeUndo = System.currentTimeMillis();
            onView(withContentDescription(cd("Quiz"))).perform(click());

            assertTrue("DONE in the DB", EditScreenWaits.eventually(() ->
                    Assignment.STATUS_DONE.equals(col("r1", "status"))));
            assertNotNull("completedAt set", col("r1", "completedAt"));
            assertTrue("open row, then a collapsed Done header", EditScreenWaits.eventually(() -> {
                List<HomeItem> items = items(s);
                HomeItem last = items.get(items.size() - 1);
                return rowIds(items).equals(java.util.Arrays.asList("r2"))
                        && last.getType() == HomeItem.TYPE_DONE_HEADER
                        && last.getDoneCount() == 1 && !last.isExpanded();
            }));

            onView(withText(R.string.snackbar_marked_done)).check(matches(isDisplayed()));
            clickUndo();

            assertTrue("open again", EditScreenWaits.eventually(() ->
                    Assignment.STATUS_TODO.equals(col("r1", "status"))));
            assertNull("completedAt cleared", col("r1", "completedAt"));
            assertTrue("SEC-3-1: Undo writes updatedAt = now", (long) col("r1", "updatedAt") >= beforeUndo);
            assertTrue(EditScreenWaits.eventually(() -> rowIds(items(s)).size() == 2));
        }
    }

    @Test
    public void doneIsCollapsedByDefaultAndUncheckingAnExpandedRowMovesItBack() {
        Assignment d = Assignment.newTodo("d1", "Reading response", 1_000L);
        d.status = Assignment.STATUS_DONE;
        d.completedAt = 3_000L;
        TestDb.insert(d);
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            assertTrue("All caught up, then the Done header", EditScreenWaits.eventually(() ->
                    items(s).size() == 2 && items(s).get(0).getType() == HomeItem.TYPE_CAUGHT_UP));
            onView(withText(R.string.caught_up_title)).check(matches(isDisplayed()));
            onView(withText("Reading response")).check(doesNotExist());

            onView(withText(R.string.section_done)).perform(click());
            assertTrue(EditScreenWaits.eventually(() -> items(s).size() == 3));
            waitStill(R.id.rowCheck);
            s.onActivity(a -> {
                View row = rowView(a, 2);
                CheckBox cb = row.findViewById(R.id.rowCheck);
                assertTrue("checked", cb.isChecked());
                TextView title = row.findViewById(R.id.rowTitle);
                assertEquals("done title is muted (colorOnSurfaceVariant)", Color.parseColor("#6E5240"),
                        title.getCurrentTextColor());
                assertEquals("no strikethrough", 0,
                        title.getPaintFlags() & android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            });

            onView(withContentDescription(cd("Reading response"))).perform(click());
            assertTrue("open in the DB", EditScreenWaits.eventually(() ->
                    Assignment.STATUS_TODO.equals(col("d1", "status"))));
            assertNull(col("d1", "completedAt"));
            assertTrue("back with the open rows; no Done header left", EditScreenWaits.eventually(() -> {
                List<HomeItem> l = items(s);
                return l.size() == 3 && l.get(0).getType() == HomeItem.TYPE_SUMMARY
                        && l.get(2).getType() == HomeItem.TYPE_ROW;
            }));
            waitStill(R.id.rowCheck);
            s.onActivity(a -> assertEquals(Color.parseColor("#3E1F0D"),
                    ((TextView) rowView(a, 2).findViewById(R.id.rowTitle)).getCurrentTextColor()));
        }
    }

    @Test
    public void swipingARowDeletesItAndUndoRestoresTheSameRow() {
        Assignment a = Assignment.newTodo("del-1", "Lab report: enzyme activity", 1_000L);
        a.course = "Biology 101";
        a.notes = "Use Tuesday's data";
        a.updatedAt = 1_500L;
        TestDb.insert(a);
        Map<String, Object> before = TestDb.fullRow("del-1");
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 1);
            long beforeUndo = System.currentTimeMillis();
            onView(withText("Lab report: enzyme activity")).perform(swipeLeft());

            assertTrue("deleted", EditScreenWaits.eventually(() -> TestDb.count() == 0));
            onView(withText(R.string.snackbar_deleted)).check(matches(isDisplayed()));
            clickUndo();

            assertTrue("restored", EditScreenWaits.eventually(() -> TestDb.count() == 1));
            Map<String, Object> after = TestDb.fullRow("del-1");
            assertTrue("SEC-3-1: Undo writes updatedAt = now", (long) after.get("updatedAt") >= beforeUndo);
            after.put("updatedAt", before.get("updatedAt"));
            assertEquals("same id and every other field", before, after);
            assertTrue(EditScreenWaits.eventually(() -> rowIds(items(s)).equals(java.util.Arrays.asList("del-1"))));
        }
    }

    @Test
    public void aDeletedRowIsGoneAfterRelaunchWithoutUndo() {
        TestDb.insert(Assignment.newTodo("gone", "Quiz 3 study guide", 1_000L));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 1);
            onView(withText("Quiz 3 study guide")).perform(swipeRight());
            assertTrue(EditScreenWaits.eventually(() -> TestDb.count() == 0));
        }
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.empty_title)).check(matches(isDisplayed()));
            onView(withText("Quiz 3 study guide")).check(doesNotExist());
            assertEquals(0, TestDb.count());
        }
    }

    /** TalkBack users can't swipe: the row's "Delete" action does the same (design.md §4). */
    @Test
    public void deleteAccessibilityActionDeletesAndOffersUndo() {
        TestDb.insert(Assignment.newTodo("a1", "Group presentation slides", 1_000L));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 1);
            s.onActivity(a -> {
                View row = rowView(a, 2).findViewById(R.id.rowBody);
                int id = actionId(row, ctx.getString(R.string.action_delete));
                assertTrue("row offers Delete", id != -1);
                assertTrue(ViewCompat.performAccessibilityAction(row, id, null));
            });
            assertTrue(EditScreenWaits.eventually(() -> TestDb.count() == 0));
            onView(withText(R.string.snackbar_deleted)).check(matches(isDisplayed()));
        }
    }

    /** The checkbox reads "Done: <title>" plus its checked state; it's a 48dp target at 4dp in. */
    @Test
    public void checkboxReadsTheTitleAndStateAndTextStartsAt52dp() {
        TestDb.insert(Assignment.newTodo("c1", "Calc problem set 7", 1_000L));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 1);
            s.onActivity(a -> {
                View row = rowView(a, 2);
                CheckBox cb = row.findViewById(R.id.rowCheck);
                assertEquals("Done: Calc problem set 7", cb.getContentDescription().toString());
                AccessibilityNodeInfo info = cb.createAccessibilityNodeInfo();
                assertTrue(info.isCheckable());
                assertFalse(info.isChecked());
                assertTrue("checkbox is its own focus stop", cb.isFocusable());
                // QA-3-1: the checkbox and the focusable body are siblings, checkbox first, under a
                // card root that isn't focusable, so TalkBack visits the checkbox first.
                View body = row.findViewById(R.id.rowBody);
                assertTrue(body.isFocusable());
                assertFalse("card root is not a focus stop", row.isFocusable());
                android.view.ViewGroup parent = (android.view.ViewGroup) cb.getParent();
                assertEquals("siblings", parent, body.getParent());
                assertTrue("checkbox first", parent.indexOfChild(cb) < parent.indexOfChild(body));
                assertTrue("the body holds the Delete action",
                        actionId(body, a.getString(R.string.action_delete)) != -1);
                assertEquals("checkbox has no Delete of its own", -1,
                        actionId(cb, a.getString(R.string.action_delete)));
                float dp = a.getResources().getDisplayMetrics().density;
                assertEquals(Math.round(48 * dp), cb.getWidth());
                assertEquals(Math.round(48 * dp), cb.getHeight());
                int[] r = new int[2];
                int[] t = new int[2];
                int[] c = new int[2];
                row.getLocationInWindow(r);
                row.findViewById(R.id.rowTitle).getLocationInWindow(t);
                cb.getLocationInWindow(c);
                assertEquals("checkbox 4dp in", Math.round(4 * dp), c[0] - r[0]);
                assertEquals("title 52dp in", Math.round(52 * dp), t[0] - r[0], 1);
            });
        }
    }

    @Test
    public void doneHeaderIsAHeadingThatSaysCollapsedOrExpanded() {
        Assignment d = Assignment.newTodo("d1", "Essay outline", 1_000L);
        d.status = Assignment.STATUS_DONE;
        d.completedAt = 2_000L;
        TestDb.insert(d);
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForItems(s, 2);
            s.onActivity(a -> {
                View h = rowView(a, 1);
                assertTrue(ViewCompat.isAccessibilityHeading(h));
                assertEquals(a.getString(R.string.done_header_cd_collapsed, 1), h.getContentDescription().toString());
                if (Build.VERSION.SDK_INT >= 30) {
                    assertEquals(a.getString(R.string.state_collapsed), String.valueOf(ViewCompat.getStateDescription(h)));
                }
                assertEquals("the count sits next to the label", "1",
                        ((TextView) h.findViewById(R.id.doneCount)).getText().toString());
            });
            onView(withText(R.string.section_done)).perform(click());
            assertTrue(EditScreenWaits.eventually(() -> items(s).size() == 3));
            s.onActivity(a -> {
                View h = rowView(a, 1);
                assertEquals(a.getString(R.string.done_header_cd_expanded, 1), h.getContentDescription().toString());
                if (Build.VERSION.SDK_INT >= 30) {
                    assertEquals(a.getString(R.string.state_expanded), String.valueOf(ViewCompat.getStateDescription(h)));
                }
            });
        }
    }

    // ---- helpers ----

    private String cd(String title) {
        return ctx.getString(R.string.checkbox_cd, title);
    }

    private static Object col(String id, String column) {
        Map<String, Object> r = TestDb.fullRow(id);
        return r == null ? null : r.get(column);
    }

    private static List<HomeItem> items(ActivityScenario<MainActivity> s) {
        Object[] out = {null};
        s.onActivity(a -> out[0] = ((AssignmentListAdapter) ((RecyclerView) a.findViewById(R.id.list))
                .getAdapter()).getCurrentList());
        @SuppressWarnings("unchecked") List<HomeItem> l = (List<HomeItem>) out[0];
        return l;
    }

    /** Assignment ids of the ROW items, in list order. */
    static List<String> rowIds(List<HomeItem> items) {
        List<String> out = new java.util.ArrayList<>();
        for (HomeItem i : items) if (i.getType() == HomeItem.TYPE_ROW) out.add(i.getAssignment().id);
        return out;
    }

    /** Waits until the list shows {@code n} assignment rows. */
    private static void waitForRows(ActivityScenario<MainActivity> s, int n) {
        assertTrue("list has " + n + " rows", EditScreenWaits.eventually(() -> rowIds(items(s)).size() == n));
        SystemClock.sleep(300); // let the first layout and item animations finish
    }

    private static void waitForItems(ActivityScenario<MainActivity> s, int n) {
        assertTrue("list has " + n + " items", EditScreenWaits.eventually(() -> {
            List<HomeItem> l = items(s);
            return l.size() == n && l.get(0).getType() != HomeItem.TYPE_EMPTY;
        }));
        SystemClock.sleep(300); // let the first layout and item animations finish
    }

    /** The view bound to adapter position {@code pos}. */
    private static View rowView(MainActivity a, int pos) {
        RecyclerView list = a.findViewById(R.id.list);
        RecyclerView.ViewHolder h = list.findViewHolderForAdapterPosition(pos);
        assertNotNull("no view at " + pos, h);
        return h.itemView;
    }

    private static int actionId(View v, String label) {
        AccessibilityNodeInfo info = v.createAccessibilityNodeInfo();
        for (AccessibilityNodeInfo.AccessibilityAction act : info.getActionList()) {
            if (act.getLabel() != null && label.contentEquals(act.getLabel())) return act.getId();
        }
        return -1;
    }

    /** Waits for the snackbar's Undo to stop sliding in, then taps it. */
    private void clickUndo() {
        waitStillText(ctx.getString(R.string.action_undo));
        onView(withText(R.string.action_undo)).perform(click());
    }

    /** The supplier runs on the main thread (inside waitStill's runOnMainSync). */
    private static void waitStill(int viewId) {
        waitStill(() -> {
            android.app.Activity act = resumed();
            return act == null ? null : act.findViewById(viewId);
        });
    }

    private static void waitStillText(String text) {
        waitStill(() -> {
            android.app.Activity act = resumed();
            if (act == null) return null;
            java.util.ArrayList<View> found = new java.util.ArrayList<>();
            act.getWindow().getDecorView().findViewsWithText(found, text, View.FIND_VIEWS_WITH_TEXT);
            View shown = null;
            for (View f : found) if (f.isShown()) shown = f;
            return shown;
        });
    }

    private static void waitStill(java.util.function.Supplier<View> find) {
        long deadline = SystemClock.uptimeMillis() + 8_000;
        int[] last = {-1, -1};
        long since = 0;
        while (SystemClock.uptimeMillis() < deadline) {
            int[] now = {-1, -1};
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                View v = find.get();
                if (v != null && v.isShown() && v.getAlpha() == 1f) v.getLocationOnScreen(now);
            });
            long t = SystemClock.uptimeMillis();
            if (now[1] >= 0 && now[0] == last[0] && now[1] == last[1]) {
                if (since == 0) since = t;
                if (t - since >= 400) return;
            } else {
                since = 0;
            }
            last = now;
            SystemClock.sleep(100);
        }
        throw new AssertionError("view never held still");
    }

    private static android.app.Activity resumed() {
        for (android.app.Activity a : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
            return a;
        }
        return null;
    }
}
