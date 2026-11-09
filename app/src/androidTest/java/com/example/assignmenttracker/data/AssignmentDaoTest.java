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

    @Test
    public void listIsNewestFirst() {
        dao.insert(Assignment.newTodo("old", "Old", 1));
        dao.insert(Assignment.newTodo("new", "New", 3));
        dao.insert(Assignment.newTodo("mid", "Mid", 2));
        List<Assignment> all = valueOf(dao.observeAll());
        assertEquals("new", all.get(0).id);
        assertEquals("mid", all.get(1).id);
        assertEquals("old", all.get(2).id);
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

    /** Slice 2: the edit save changes title, course, notes and updatedAt, and nothing else. */
    @Test
    public void updateFieldsChangesOnlyTheEditedFieldsAndUpdatedAt() {
        Assignment a = Assignment.newTodo("u", "Lab report", 1_000L);
        a.course = "Biology 101";
        a.notes = "old";
        a.dueAt = 5_000L;
        dao.insert(a);
        dao.insert(Assignment.newTodo("other", "Untouched", 2_000L));

        assertEquals(1, dao.updateFields("u", "Lab report v2", "Chem 110", null, 9_000L));

        Assignment b = valueOf(dao.observeById("u"));
        assertEquals("Lab report v2", b.title);
        assertEquals("Chem 110", b.course);
        assertNull(b.notes);
        assertEquals(9_000L, b.updatedAt);
        assertEquals("createdAt kept", 1_000L, b.createdAt);
        assertEquals("dueAt kept", Long.valueOf(5_000L), b.dueAt);
        assertEquals("status kept", Assignment.STATUS_TODO, b.status);
        Assignment other = valueOf(dao.observeById("other"));
        assertEquals("the other row is untouched", "Untouched", other.title);
        assertEquals(2_000L, other.updatedAt);
    }

    @Test
    public void updateOfAMissingIdChangesNothing() {
        assertEquals(0, dao.updateFields("ghost", "T", null, null, 1L));
        assertTrue(valueOf(dao.observeAll()).isEmpty());
    }

    private static <T> T valueOf(LiveData<T> live) {
        AtomicReference<T> ref = new AtomicReference<>();
        live.observeForever(ref::set);
        T v = ref.get();
        assertNotNull("LiveData had no value", v);
        return v;
    }
}
