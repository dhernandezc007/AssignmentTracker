package com.example.assignmenttracker.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.assignmenttracker.data.AssignmentLimits;
import com.example.assignmenttracker.data.dao.AssignmentDao;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.ui.assignments.AssignmentEditViewModel;
import com.example.assignmenttracker.ui.assignments.TitleValidator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SEC-2-1: the repository is the app's only door to the data, so it enforces the field caps
 * itself (title 200, course 60, notes 2000; title not blank). Slice 3: status writes and
 * single-row delete.
 */
public class RoomAssignmentRepositoryTest {

    private final FakeDao dao = new FakeDao();
    private final RoomAssignmentRepository repo = new RoomAssignmentRepository(dao, new InlineExecutor());

    // ---- SEC-2-1: one shared set of caps ----

    @Test
    public void capsLiveInOnePlaceAndTheEditScreenUsesThem() {
        assertEquals(200, AssignmentLimits.MAX_TITLE);
        assertEquals(60, AssignmentLimits.MAX_COURSE);
        assertEquals(2000, AssignmentLimits.MAX_NOTES);
        assertEquals(AssignmentLimits.MAX_TITLE, TitleValidator.MAX_LENGTH);
        assertEquals(AssignmentLimits.MAX_COURSE, AssignmentEditViewModel.MAX_COURSE);
        assertEquals(AssignmentLimits.MAX_NOTES, AssignmentEditViewModel.MAX_NOTES);
    }

    @Test
    public void insertAtTheCapsIsAccepted() {
        Assignment a = Assignment.newTodo("a", repeat(200), 1);
        a.course = repeat(60);
        a.notes = repeat(2000);
        assertNull(insert(a));
        assertEquals(1, dao.inserted.size());
    }

    @Test
    public void insertOverAnyCapIsRefusedAndNothingIsWritten() {
        Assignment title = Assignment.newTodo("t", repeat(201), 1);
        Assignment course = Assignment.newTodo("c", "ok", 1);
        course.course = repeat(61);
        Assignment notes = Assignment.newTodo("n", "ok", 1);
        notes.notes = repeat(2001);
        for (Assignment a : List.of(title, course, notes)) {
            Throwable e = insert(a);
            assertTrue("refused: " + a.id, e instanceof IllegalArgumentException);
        }
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    public void insertWithABlankTitleIsRefused() {
        assertTrue(insert(Assignment.newTodo("b", "   ", 1)) instanceof IllegalArgumentException);
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    public void insertWithAnUnknownStatusIsRefused() {
        Assignment a = Assignment.newTodo("s", "ok", 1);
        a.status = "ARCHIVED";
        assertTrue(insert(a) instanceof IllegalArgumentException);
    }

    @Test
    public void updateFieldsOverAnyCapIsRefusedAndNothingIsWritten() {
        assertTrue(update("id", repeat(201), null, null) instanceof IllegalArgumentException);
        assertTrue(update("id", "ok", repeat(61), null) instanceof IllegalArgumentException);
        assertTrue(update("id", "ok", null, repeat(2001)) instanceof IllegalArgumentException);
        assertTrue(update("id", " ", null, null) instanceof IllegalArgumentException);
        assertEquals(0, dao.updateCalls);
        assertNull(update("id", repeat(200), repeat(60), repeat(2000)));
        assertEquals(1, dao.updateCalls);
    }

    // ---- Slice 3: done / undo ----

    @Test
    public void markDoneWritesStatusCompletedAtAndUpdatedAt() {
        assertNull(status("id", Assignment.STATUS_DONE, 5_000L, 5_000L));
        assertEquals(List.of("id", Assignment.STATUS_DONE, 5_000L, 5_000L), dao.statusWrites.get(0));
    }

    @Test
    public void undoDoneWritesTheOpenStatusWithNoCompletedAt() {
        assertNull(status("id", Assignment.STATUS_TODO, null, 1_000L));
        assertEquals(Collections.singletonList(null), dao.statusWrites.get(0).subList(2, 3));
    }

    @Test
    public void statusWritesMustBeConsistent() {
        assertTrue("unknown status", status("id", "LATER", null, 1L) instanceof IllegalArgumentException);
        assertTrue("done needs completedAt", status("id", Assignment.STATUS_DONE, null, 1L) instanceof IllegalArgumentException);
        assertTrue("open has no completedAt", status("id", Assignment.STATUS_TODO, 1L, 1L) instanceof IllegalArgumentException);
        assertTrue(dao.statusWrites.isEmpty());
    }

    @Test
    public void statusWriteOnAMissingRowReportsAnError() {
        dao.rowsChanged = 0;
        assertNotNull(status("ghost", Assignment.STATUS_DONE, 1L, 1L));
    }

    // ---- Slice 3: delete / restore ----

    @Test
    public void deleteRemovesOneRowById() {
        assertNull(delete("id"));
        assertEquals(List.of("id"), dao.deleted);
    }

    @Test
    public void deleteOfAMissingRowReportsAnError() {
        dao.rowsChanged = 0;
        assertNotNull(delete("ghost"));
    }

    @Test
    public void restoreIsAnInsertOfTheSameEntity() {
        Assignment a = Assignment.newTodo("keep-id", "Lab report", 1_000L);
        a.course = "Biology 101";
        a.updatedAt = 4_000L;
        assertNull(insert(a));
        assertSame(a, dao.inserted.get(0));
    }

    @Test
    public void daoFailuresReachTheCallback() {
        dao.fail = new RuntimeException("disk");
        assertSame(dao.fail, delete("id"));
        assertSame(dao.fail, status("id", Assignment.STATUS_DONE, 1L, 1L));
        assertSame(dao.fail, insert(Assignment.newTodo("x", "X", 1)));
    }

    // ---- helpers ----

    @Nullable
    private Throwable insert(Assignment a) {
        AtomicReference<Throwable> out = new AtomicReference<>(new AssertionError("callback never ran"));
        repo.insert(a, out::set);
        return out.get();
    }

    @Nullable
    private Throwable update(String id, String title, @Nullable String course, @Nullable String notes) {
        AtomicReference<Throwable> out = new AtomicReference<>(new AssertionError("callback never ran"));
        repo.updateFields(id, title, course, notes, 9L, out::set);
        return out.get();
    }

    @Nullable
    private Throwable status(String id, String status, @Nullable Long completedAt, long updatedAt) {
        AtomicReference<Throwable> out = new AtomicReference<>(new AssertionError("callback never ran"));
        repo.updateStatus(id, status, completedAt, updatedAt, out::set);
        return out.get();
    }

    @Nullable
    private Throwable delete(String id) {
        AtomicReference<Throwable> out = new AtomicReference<>(new AssertionError("callback never ran"));
        repo.delete(id, out::set);
        return out.get();
    }

    private static String repeat(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append('a');
        return sb.toString();
    }

    static final class FakeDao implements AssignmentDao {
        final List<Assignment> inserted = new ArrayList<>();
        final List<List<Object>> statusWrites = new ArrayList<>();
        final List<String> deleted = new ArrayList<>();
        int updateCalls;
        int rowsChanged = 1;
        @Nullable RuntimeException fail;

        @Override
        public void insert(Assignment assignment) {
            if (fail != null) throw fail;
            inserted.add(assignment);
        }

        @Override
        public LiveData<List<Assignment>> observeAll() {
            return null;
        }

        @Override
        public LiveData<Assignment> observeById(String id) {
            return null;
        }

        @Override
        public int updateFields(String id, String title, @Nullable String course, @Nullable String notes, long updatedAt) {
            if (fail != null) throw fail;
            updateCalls++;
            return rowsChanged;
        }

        @Override
        public int updateStatus(String id, String status, @Nullable Long completedAt, long updatedAt) {
            if (fail != null) throw fail;
            statusWrites.add(java.util.Arrays.asList(id, status, completedAt, updatedAt));
            return rowsChanged;
        }

        @Override
        public int deleteById(String id) {
            if (fail != null) throw fail;
            deleted.add(id);
            return rowsChanged;
        }
    }

    /** Runs each task on the calling thread, so callbacks finish before the call returns. */
    static final class InlineExecutor extends AbstractExecutorService {
        @Override
        public void execute(Runnable r) {
            r.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}
