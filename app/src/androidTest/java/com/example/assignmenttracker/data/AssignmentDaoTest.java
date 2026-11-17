package com.example.assignmenttracker.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.assignmenttracker.data.dao.AssignmentDao;
import com.example.assignmenttracker.data.db.AppDatabase;
import com.example.assignmenttracker.data.entity.Assignment;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Slice 1: in-memory Room. Insert, then the list query returns it. */
@RunWith(AndroidJUnit4.class)
public class AssignmentDaoTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private AppDatabase db;
    private AssignmentDao dao;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase.class).allowMainThreadQueries().build();
        dao = db.assignmentDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void emptyDatabaseReturnsEmptyList() {
        assertTrue(valueOf(dao.observeAll()).isEmpty());
    }

    @Test
    public void insertThenQueryReturnsItWithEveryField() {
        Assignment a = Assignment.newTodo("9b2c-uuid", "Lab report: enzyme activity", 1_762_600_000_000L);
        a.notes = "Use Tuesday's data";
        a.course = "Biology 101";
        a.dueAt = 1_762_700_000_000L;
        dao.insert(a);

        List<Assignment> all = valueOf(dao.observeAll());
        assertEquals(1, all.size());
        Assignment b = all.get(0);
        assertEquals("9b2c-uuid", b.id);
        assertEquals("Lab report: enzyme activity", b.title);
        assertEquals("Use Tuesday's data", b.notes);
        assertEquals(Long.valueOf(1_762_700_000_000L), b.dueAt);
        assertEquals(Assignment.STATUS_TODO, b.status);
        assertEquals(Assignment.PRIORITY_NORMAL, b.priority);
        assertEquals(1_762_600_000_000L, b.createdAt);
        assertEquals(1_762_600_000_000L, b.updatedAt);
        assertEquals(null, b.completedAt);
        assertEquals("Biology 101", b.course);
    }

    /** Slice 4 (tasks.md): ORDER BY dueAt IS NULL, dueAt; ties by createdAt, then id. */
    @Test
    public void listIsByDueDateWithNoDateLast() {
        Assignment late = Assignment.newTodo("late", "Late", 1);
        late.dueAt = 9_000L;
        Assignment soon = Assignment.newTodo("soon", "Soon", 2);
        soon.dueAt = 1_000L;
        Assignment tieNew = Assignment.newTodo("tieNew", "Tie new", 5);
        tieNew.dueAt = 5_000L;
        Assignment tieOld = Assignment.newTodo("tieOld", "Tie old", 4);
        tieOld.dueAt = 5_000L;
        dao.insert(Assignment.newTodo("none2", "No date 2", 8));
        dao.insert(late);
        dao.insert(Assignment.newTodo("none1", "No date 1", 7));
        dao.insert(tieNew);
        dao.insert(soon);
        dao.insert(tieOld);
        List<String> ids = new java.util.ArrayList<>();
        for (Assignment a : valueOf(dao.observeAll())) ids.add(a.id);
        assertEquals(java.util.Arrays.asList("soon", "tieOld", "tieNew", "late", "none1", "none2"), ids);
    }

    @Test
    public void duplicateIdIsRejected() {
        dao.insert(Assignment.newTodo("same", "A", 1));
        try {
            dao.insert(Assignment.newTodo("same", "B", 2));
            fail("expected a constraint failure");
        } catch (SQLiteConstraintException expected) {
            // ABORT keeps the first row intact.
        }
        assertEquals("A", valueOf(dao.observeAll()).get(0).title);
    }

    @Test
    public void observeByIdReturnsTheRowOrNull() {
        dao.insert(Assignment.newTodo("x", "X", 1));
        assertEquals("X", valueOf(dao.observeById("x")).title);
        AtomicReference<Assignment> missing = new AtomicReference<>(Assignment.newTodo("sentinel", "", 0));
        dao.observeById("nope").observeForever(missing::set);
        assertNull(missing.get());
    }

    /** Slices 2 and 4: the edit save changes title, course, notes, dueAt and updatedAt, and nothing else. */
    @Test
    public void updateFieldsChangesOnlyTheEditedFieldsAndUpdatedAt() {
        Assignment a = Assignment.newTodo("u", "Lab report", 1_000L);
        a.course = "Biology 101";
        a.notes = "old";
        a.dueAt = 5_000L;
        dao.insert(a);
        dao.insert(Assignment.newTodo("other", "Untouched", 2_000L));

        assertEquals(1, dao.updateFields("u", "Lab report v2", "Chem 110", null, 7_000L, 9_000L));

        Assignment b = valueOf(dao.observeById("u"));
        assertEquals("Lab report v2", b.title);
        assertEquals("Chem 110", b.course);
        assertNull(b.notes);
        assertEquals(9_000L, b.updatedAt);
        assertEquals("createdAt kept", 1_000L, b.createdAt);
        assertEquals("dueAt written", Long.valueOf(7_000L), b.dueAt);
        assertEquals("status kept", Assignment.STATUS_TODO, b.status);
        Assignment other = valueOf(dao.observeById("other"));
        assertEquals("the other row is untouched", "Untouched", other.title);
        assertEquals(2_000L, other.updatedAt);
    }

    /** Slice 4: clearing the due date in the edit screen writes null. */
    @Test
    public void updateFieldsCanClearTheDueDate() {
        Assignment a = Assignment.newTodo("c", "Quiz", 1_000L);
        a.dueAt = 5_000L;
        dao.insert(a);
        assertEquals(1, dao.updateFields("c", "Quiz", null, null, null, 2_000L));
        assertNull(valueOf(dao.observeById("c")).dueAt);
    }

    @Test
    public void updateOfAMissingIdChangesNothing() {
        assertEquals(0, dao.updateFields("ghost", "T", null, null, null, 1L));
        assertTrue(valueOf(dao.observeAll()).isEmpty());
    }

    /** Slice 3: mark done sets status, completedAt and updatedAt; undo puts the row back exactly. */
    @Test
    public void markDoneThenUndoRestoresTheOpenRow() {
        Assignment a = Assignment.newTodo("d", "Quiz 3 study guide", 1_000L);
        a.course = "Chemistry 110";
        a.notes = "ch. 5";
        a.dueAt = 8_000L;
        a.updatedAt = 2_000L;
        dao.insert(a);
        dao.insert(Assignment.newTodo("other", "Untouched", 3_000L));

        assertEquals(1, dao.updateStatus("d", Assignment.STATUS_DONE, 5_000L, 5_000L));
        Assignment done = valueOf(dao.observeById("d"));
        assertEquals(Assignment.STATUS_DONE, done.status);
        assertEquals(Long.valueOf(5_000L), done.completedAt);
        assertEquals(5_000L, done.updatedAt);
        assertEquals("title kept", "Quiz 3 study guide", done.title);
        assertEquals("dueAt kept", Long.valueOf(8_000L), done.dueAt);

        assertEquals(1, dao.updateStatus("d", Assignment.STATUS_TODO, null, 2_000L));
        assertSameRow(a, valueOf(dao.observeById("d")));
        Assignment other = valueOf(dao.observeById("other"));
        assertEquals(Assignment.STATUS_TODO, other.status);
        assertNull(other.completedAt);
    }

    @Test
    public void statusUpdateOfAMissingIdChangesNothing() {
        assertEquals(0, dao.updateStatus("ghost", Assignment.STATUS_DONE, 1L, 1L));
    }

    /** Slice 3: delete removes exactly one row; restoring re-inserts the same id and fields. */
    @Test
    public void deleteThenRestoreBringsBackTheSameRow() {
        Assignment a = Assignment.newTodo("del-uuid", "Lab report: enzyme activity", 1_000L);
        a.course = "Biology 101";
        a.notes = "Use Tuesday's data";
        a.dueAt = 9_000L;
        a.priority = Assignment.PRIORITY_HIGH;
        a.status = Assignment.STATUS_DONE;
        a.completedAt = 4_000L;
        a.updatedAt = 4_000L;
        dao.insert(a);
        dao.insert(Assignment.newTodo("stay", "Stays", 2_000L));

        assertEquals(1, dao.deleteById("del-uuid"));
        List<Assignment> left = valueOf(dao.observeAll());
        assertEquals(1, left.size());
        assertEquals("stay", left.get(0).id);
        AtomicReference<Assignment> gone = new AtomicReference<>(Assignment.newTodo("sentinel", "", 0));
        dao.observeById("del-uuid").observeForever(gone::set);
        assertNull(gone.get());

        dao.insert(a);
        assertSameRow(a, valueOf(dao.observeById("del-uuid")));
        assertEquals(2, valueOf(dao.observeAll()).size());
    }

    @Test
    public void deleteOfAMissingIdChangesNothing() {
        dao.insert(Assignment.newTodo("x", "X", 1));
        assertEquals(0, dao.deleteById("ghost"));
        assertEquals(1, valueOf(dao.observeAll()).size());
    }

    private static void assertSameRow(Assignment want, Assignment got) {
        assertEquals(want.id, got.id);
        assertEquals(want.title, got.title);
        assertEquals(want.course, got.course);
        assertEquals(want.notes, got.notes);
        assertEquals(want.dueAt, got.dueAt);
        assertEquals(want.priority, got.priority);
        assertEquals(want.status, got.status);
        assertEquals(want.completedAt, got.completedAt);
        assertEquals(want.createdAt, got.createdAt);
        assertEquals(want.updatedAt, got.updatedAt);
    }

    private static <T> T valueOf(LiveData<T> live) {
        AtomicReference<T> ref = new AtomicReference<>();
        live.observeForever(ref::set);
        T v = ref.get();
        assertNotNull("LiveData had no value", v);
        return v;
    }
}
