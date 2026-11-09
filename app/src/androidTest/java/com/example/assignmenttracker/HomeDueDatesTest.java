package com.example.assignmenttracker;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.ui.assignments.AssignmentEditActivity;
import com.example.assignmenttracker.ui.assignments.AssignmentListAdapter;
import com.example.assignmenttracker.ui.assignments.HomeItem;
import com.example.assignmenttracker.util.DueBucket;
import com.example.assignmenttracker.util.DueDates;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Slice 4 end to end: buckets, summary, due labels, the due field, banner, and the Slice 3 carry-overs. */
@RunWith(AndroidJUnit4.class)
public class HomeDueDatesTest {

    private final Context ctx = ApplicationProvider.getApplicationContext();
    private final TimeZone tz = TimeZone.getDefault();

    @Before
    public void clearDatabase() {
        TestDb.clear();
    }

    /** Local start of the day {@code offset} days from today. */
    private long day(int offset) {
        Calendar c = Calendar.getInstance(tz);
        c.add(Calendar.DAY_OF_MONTH, offset);
        return DueDates.startOfLocalDay(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), tz);
    }

    private static Assignment item(String id, String title, String course, Long due, long createdAt) {
        Assignment a = Assignment.newTodo(id, title, createdAt);
        a.course = course;
        a.dueAt = due;
        return a;
    }

    /** design.md §12 sample data, relative to today. */
    private void insertSample() {
        TestDb.insert(item("later", "Group presentation slides", "History 110", day(16), 1));
        TestDb.insert(item("nodate", "Ask Prof. Ruiz about extra credit", null, null, 2));
        TestDb.insert(item("today1", "Calc problem set 7", "Math 151", day(0), 3));
        TestDb.insert(item("over2", "Reading response, ch. 4", "English 102", day(-1), 4));
        TestDb.insert(item("week", "Quiz 3 study guide", "Chemistry 110", day(3), 5));
        TestDb.insert(item("over1", "Lab report: enzyme activity", "Biology 101", day(-2), 6));
        TestDb.insert(item("tomorrow", "Essay outline", "English 102", day(1), 7));
        TestDb.insert(item("today2", "Discussion post: Reconstruction", "History 110", day(0), 8));
        Assignment d = item("done1", "Done one", null, day(-5), 9);
        d.status = Assignment.STATUS_DONE;
        d.completedAt = 10L;
        TestDb.insert(d);
    }

    /**
     * tasks.md "what's due next": on launch, with no taps or scrolling, the first row is the
     * earliest open item (overdue first) and it sits inside the top 640dp of the window.
     */
    @Test
    public void theFirstVisibleRowIsTheEarliestOpenItem() {
        insertSample();
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 8);
            s.onActivity(a -> {
                RecyclerView list = a.findViewById(R.id.list);
                assertEquals("not scrolled", 0, list.computeVerticalScrollOffset());
                int first = -1;
                List<HomeItem> items = adapter(a).getCurrentList();
                for (int i = 0; i < items.size() && first < 0; i++) {
                    if (items.get(i).getType() == HomeItem.TYPE_ROW) first = i;
                }
                assertEquals("earliest open item first", "over1", items.get(first).getAssignment().id);
                RecyclerView.ViewHolder h = list.findViewHolderForAdapterPosition(first);
                assertNotNull("the first row is laid out on screen", h);
                int[] loc = new int[2];
                h.itemView.getLocationInWindow(loc);
                float dp = a.getResources().getDisplayMetrics().density;
                float bottomDp = (loc[1] + h.itemView.getHeight()) / dp;
                assertTrue("row ends at " + bottomDp + "dp, inside 640dp", bottomDp <= 640f);
                assertTrue(h.itemView.isShown());
            });
        }
    }

    @Test
    public void openItemsAreBucketedWithHeadersInOrderAndTheSummaryCountsThem() {
        insertSample();
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 8);
            s.onActivity(a -> {
                List<HomeItem> items = adapter(a).getCurrentList();
                assertEquals(HomeItem.TYPE_SUMMARY, items.get(0).getType());
                List<DueBucket> headers = new ArrayList<>();
                for (HomeItem i : items) if (i.getType() == HomeItem.TYPE_SECTION_HEADER) headers.add(i.getBucket());
                assertEquals(java.util.Arrays.asList(DueBucket.OVERDUE, DueBucket.TODAY, DueBucket.THIS_WEEK,
                        DueBucket.LATER, DueBucket.NO_DATE), headers);
                assertEquals(java.util.Arrays.asList("over1", "over2", "today1", "today2", "tomorrow", "week",
                        "later", "nodate"), HomeDoneDeleteTest.rowIds(items));
                assertEquals("Done stays collapsed at the bottom", HomeItem.TYPE_DONE_HEADER,
                        items.get(items.size() - 1).getType());

                TextView summary = (TextView) view(a, 0);
                String sep = a.getString(R.string.summary_separator);
                String want = a.getResources().getQuantityString(R.plurals.summary_overdue, 2, 2) + sep
                        + a.getResources().getQuantityString(R.plurals.summary_due_today, 2, 2);
                assertEquals(want, summary.getText().toString());
                assertEquals("TalkBack hears a pause, not a middle dot",
                        want.replace(sep, a.getString(R.string.summary_separator_spoken)),
                        summary.getContentDescription().toString());

                View overdueHeader = view(a, 1);
                assertTrue(ViewCompat.isAccessibilityHeading(overdueHeader));
                assertEquals(a.getResources().getQuantityString(R.plurals.section_header_cd, 2,
                        a.getString(R.string.section_overdue), 2), overdueHeader.getContentDescription().toString());
            });
        }
    }

    /** design.md §3.1: overdue is a word plus an icon, today and tomorrow are labelled. */
    @Test
    public void rowsShowTheDueLabelBeforeTheCourse() {
        insertSample();
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 8);
            s.onActivity(a -> {
                String sep = a.getString(R.string.meta_separator);
                TextView over = meta(a, "over1");
                String overdueWord = a.getString(R.string.due_overdue, "");
                // DES-4-1: the icon is an inline span on line 1 (its placeholder is the first char).
                CharSequence overText = over.getText();
                assertTrue(overText.toString(), overText.toString().substring(1).startsWith(overdueWord));
                assertTrue(overText.toString().endsWith(sep + "Biology 101"));
                assertNull("no compound drawable", over.getCompoundDrawablesRelative()[0]);
                assertEquals("wrapped lines return to the text edge", over.getPaddingStart(), over.getCompoundPaddingStart());
                assertTrue(overText instanceof Spanned);
                ImageSpan[] icons = ((Spanned) overText).getSpans(0, overText.length(), ImageSpan.class);
                assertEquals("overdue rows carry one icon", 1, icons.length);
                assertEquals("at the start", 0, ((Spanned) overText).getSpanStart(icons[0]));
                int iconPx = Math.round(over.getTextSize() * 1.15f);
                int gapPx = a.getResources().getDimensionPixelSize(R.dimen.overdue_icon_gap);
                Drawable icon = icons[0].getDrawable();
                assertEquals("height = textSize x 1.15", iconPx, icon.getBounds().height());
                assertEquals("width = icon + 4dp gap", iconPx + gapPx, icon.getBounds().width());
                assertEquals(android.os.Build.VERSION.SDK_INT >= 29 ? DynamicDrawableSpan.ALIGN_CENTER
                        : DynamicDrawableSpan.ALIGN_BASELINE, icons[0].getVerticalAlignment());
                String spokenOverdue = a.getString(R.string.due_overdue_spoken, "");
                String body = String.valueOf(rowBody(a, "over1").getContentDescription());
                assertTrue(body, body.startsWith("Lab report: enzyme activity, Biology 101, " + spokenOverdue));

                assertEquals(a.getString(R.string.due_today) + sep + "Math 151", meta(a, "today1").getText().toString());
                CharSequence todayText = meta(a, "today1").getText();
                assertFalse("only overdue rows carry the icon", todayText instanceof Spanned
                        && ((Spanned) todayText).getSpans(0, todayText.length(), ImageSpan.class).length > 0);
                assertEquals(a.getString(R.string.due_tomorrow) + sep + "English 102", meta(a, "tomorrow").getText().toString());
                assertEquals("no date, no course: line 2 hidden", View.GONE, meta(a, "nodate").getVisibility());
            });
        }
    }

    /** QA-3-2: rotating within 5 s of a delete keeps Undo, and Undo still restores the row. */
    @Test
    public void undoSurvivesARecreateAfterADelete() {
        TestDb.insert(item("r1", "Quiz 3 study guide", null, day(2), 1));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 1);
            s.onActivity(a -> {
                View body = rowBody(a, "r1");
                assertTrue(ViewCompat.performAccessibilityAction(body, deleteAction(body, a), null));
            });
            assertTrue(EditScreenWaits.eventually(() -> TestDb.count() == 0));
            s.recreate();
            onView(withText(R.string.snackbar_deleted)).check(matches(isDisplayed()));
            SystemClock.sleep(600); // fade-in
            onView(withText(R.string.action_undo)).perform(click());
            assertTrue("restored after the recreate", EditScreenWaits.eventually(() -> TestDb.count() == 1));
        }
    }

    /** QA-3-2 for mark done, too. */
    @Test
    public void undoSurvivesARecreateAfterMarkDone() {
        TestDb.insert(item("r1", "Essay outline", null, day(1), 1));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 1);
            onView(withContentDescription(ctx.getString(R.string.checkbox_cd, "Essay outline"))).perform(click());
            assertTrue(EditScreenWaits.eventually(() -> Assignment.STATUS_DONE.equals(TestDb.fullRow("r1").get("status"))));
            s.recreate();
            onView(withText(R.string.snackbar_marked_done)).check(matches(isDisplayed()));
            SystemClock.sleep(600);
            onView(withText(R.string.action_undo)).perform(click());
            assertTrue(EditScreenWaits.eventually(() -> Assignment.STATUS_TODO.equals(TestDb.fullRow("r1").get("status"))));
        }
    }

    /** DES-3-2: 16dp side margins and 12dp corners, like the cards. */
    @Test
    public void snackbarLinesUpWithTheCards() {
        TestDb.insert(item("r1", "Quiz", null, null, 1));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            waitForRows(s, 1);
            s.onActivity(a -> {
                View body = rowBody(a, "r1");
                ViewCompat.performAccessibilityAction(body, deleteAction(body, a), null);
            });
            onView(withText(R.string.snackbar_deleted)).check(matches(isDisplayed()));
            SystemClock.sleep(600);
            s.onActivity(a -> {
                View text = findShown(a, a.getString(R.string.snackbar_deleted));
                View bar = text;
                while (!(bar.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams)) {
                    bar = (View) bar.getParent();
                }
                float dp = a.getResources().getDisplayMetrics().density;
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bar.getLayoutParams();
                assertEquals(Math.round(16 * dp), lp.leftMargin);
                assertEquals(Math.round(16 * dp), lp.rightMargin);
                Drawable bg = bar.getBackground();
                if (bg instanceof MaterialShapeDrawable) {
                    RectF r = new RectF(0, 0, bar.getWidth(), bar.getHeight());
                    float corner = ((MaterialShapeDrawable) bg).getShapeAppearanceModel().getTopLeftCornerSize().getCornerSize(r);
                    assertEquals(12 * dp, corner, 0.5f);
                }
            });
        }
    }

    // ---- the due date field (design.md §5.2) ----

    @Test
    public void aNewAssignmentStartsWithNoDueDateAndPickingTodaySavesTodaysStart() {
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(AssignmentEditActivity.newIntent(ctx, null))) {
            EditScreenWaits.settled(true);
            onView(withId(R.id.etTitle)).perform(replaceText("Calc problem set 7"));
            s.onActivity(a -> {
                assertEquals(a.getString(R.string.field_due_empty), ((TextView) a.findViewById(R.id.etDue)).getText().toString());
                assertFalse("no clear icon without a date", ((TextInputLayout) a.findViewById(R.id.tilDue)).isEndIconVisible());
            });
            androidx.test.espresso.Espresso.closeSoftKeyboard();
            onView(withId(R.id.etDue)).perform(click());
            onView(withText(R.string.date_picker_title)).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withId(com.google.android.material.R.id.confirm_button)).inRoot(isDialog()).perform(click());
            s.onActivity(a -> {
                assertTrue(((TextInputLayout) a.findViewById(R.id.tilDue)).isEndIconVisible());
                assertFalse(a.getString(R.string.field_due_empty).equals(
                        ((TextView) a.findViewById(R.id.etDue)).getText().toString()));
            });
            s.recreate();
            s.onActivity(a -> assertTrue("the date survives a recreate",
                    ((TextInputLayout) a.findViewById(R.id.tilDue)).isEndIconVisible()));
            onView(withId(R.id.btnSave)).perform(click());
            assertTrue(EditScreenWaits.eventually(() -> TestDb.count() == 1));
            Map<String, Object> row = onlyRow();
            assertEquals("stored as the local start of today", day(0), row.get("dueAt"));
        }
    }

    /** design.md §5.2 (Designer's condition): Space and Enter on the keyboard-focused due field open the picker. */
    @Test
    public void spaceAndEnterOnTheFocusedDueFieldOpenThePicker() {
        TestDb.insert(item("r1", "Lab report", null, day(3), 1));
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(AssignmentEditActivity.newIntent(ctx, "r1"))) {
            EditScreenWaits.settled(false);
            android.app.Instrumentation inst = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
            for (int key : new int[]{KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER}) {
                // Leave touch mode, as a hardware-keyboard user does (each Cancel tap re-enters it).
                inst.setInTouchMode(false);
                inst.waitForIdleSync();
                s.onActivity(a -> {
                    View due = a.findViewById(R.id.etDue);
                    assertTrue("keyboard-focusable", due.requestFocus());
                    assertTrue(due.isFocused());
                });
                onView(withId(R.id.etDue)).perform(pressKey(key));
                onView(withText(R.string.date_picker_title)).inRoot(isDialog()).check(matches(isDisplayed()));
                onView(withId(com.google.android.material.R.id.cancel_button)).inRoot(isDialog()).perform(click());
                assertTrue("picker closed after Cancel", EditScreenWaits.eventually(() -> {
                    boolean[] gone = {false};
                    s.onActivity(a -> gone[0] = a.getSupportFragmentManager().findFragmentByTag("due_picker") == null);
                    return gone[0];
                }));
            }
        } finally {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(true);
        }
    }

    @Test
    public void clearingTheDueDateOnAnEditSavesNoDate() {
        TestDb.insert(item("r1", "Lab report", null, day(3), 1));
        try (ActivityScenario<AssignmentEditActivity> s = ActivityScenario.launch(AssignmentEditActivity.newIntent(ctx, "r1"))) {
            EditScreenWaits.settled(false);
            s.onActivity(a -> assertTrue(((TextInputLayout) a.findViewById(R.id.tilDue)).isEndIconVisible()));
            onView(withContentDescription(R.string.field_due_clear_cd)).perform(click());
            s.onActivity(a -> {
                assertEquals(a.getString(R.string.field_due_empty), ((TextView) a.findViewById(R.id.etDue)).getText().toString());
                assertFalse(((TextInputLayout) a.findViewById(R.id.tilDue)).isEndIconVisible());
            });
            onView(withId(R.id.btnSave)).perform(click());
            assertTrue(EditScreenWaits.eventually(() -> TestDb.fullRow("r1").get("dueAt") == null));
        }
    }

    // ---- Direction A header (design.md §2.1) ----

    @Test
    public void theCutOutBannerRendersAt58dpAndTheEmptyArtAt186dp() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.empty_title)).check(matches(isDisplayed()));
            s.onActivity(a -> {
                float dp = a.getResources().getDisplayMetrics().density;
                ImageView banner = a.findViewById(R.id.banner);
                assertEquals(Math.round(58 * dp), banner.getHeight());
                assertNotNull(banner.getDrawable());
                assertEquals("drawn at true size, never stretched", Math.round(1080 * dp),
                        banner.getDrawable().getIntrinsicWidth(), 1);
                ImageView art = a.findViewById(R.id.emptyArt);
                assertEquals(Math.round(186 * dp), art.getWidth(), 1);
                assertEquals(Math.round(58 * dp), art.getHeight(), 2);
            });
            onView(withText(R.string.empty_body)).check(matches(isDisplayed()));
        }
    }

    // ---- helpers ----

    private static AssignmentListAdapter adapter(MainActivity a) {
        return (AssignmentListAdapter) ((RecyclerView) a.findViewById(R.id.list)).getAdapter();
    }

    private static List<HomeItem> items(ActivityScenario<MainActivity> s) {
        Object[] out = {null};
        s.onActivity(a -> out[0] = adapter(a).getCurrentList());
        @SuppressWarnings("unchecked") List<HomeItem> l = (List<HomeItem>) out[0];
        return l;
    }

    private static void waitForRows(ActivityScenario<MainActivity> s, int n) {
        assertTrue("list has " + n + " rows", EditScreenWaits.eventually(() ->
                HomeDoneDeleteTest.rowIds(items(s)).size() == n));
        SystemClock.sleep(300);
    }

    private static View view(MainActivity a, int pos) {
        RecyclerView.ViewHolder h = ((RecyclerView) a.findViewById(R.id.list)).findViewHolderForAdapterPosition(pos);
        assertNotNull("no view at " + pos, h);
        return h.itemView;
    }

    private static View rowView(MainActivity a, String id) {
        List<HomeItem> items = adapter(a).getCurrentList();
        for (int i = 0; i < items.size(); i++) {
            HomeItem it = items.get(i);
            if (it.getType() == HomeItem.TYPE_ROW && id.equals(it.getAssignment().id)) return view(a, i);
        }
        throw new AssertionError("no row " + id);
    }

    private static TextView meta(MainActivity a, String id) {
        return rowView(a, id).findViewById(R.id.rowMeta);
    }

    private static View rowBody(MainActivity a, String id) {
        return rowView(a, id).findViewById(R.id.rowBody);
    }

    private static int deleteAction(View v, Context c) {
        AccessibilityNodeInfo info = v.createAccessibilityNodeInfo();
        for (AccessibilityNodeInfo.AccessibilityAction act : info.getActionList()) {
            if (act.getLabel() != null && c.getString(R.string.action_delete).contentEquals(act.getLabel())) return act.getId();
        }
        throw new AssertionError("no Delete action");
    }

    private static View findShown(MainActivity a, String text) {
        ArrayList<View> found = new ArrayList<>();
        a.getWindow().getDecorView().findViewsWithText(found, text, View.FIND_VIEWS_WITH_TEXT);
        for (View f : found) if (f.isShown()) return f;
        throw new AssertionError("not shown: " + text);
    }

    private static Map<String, Object> onlyRow() {
        android.database.Cursor c = ((App) ApplicationProvider.getApplicationContext()).getDatabase()
                .query("SELECT id FROM assignments", null);
        try {
            c.moveToFirst();
            return TestDb.fullRow(c.getString(0));
        } finally {
            c.close();
        }
    }
}
