package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.data.repo.AssignmentRepository;
import com.example.assignmenttracker.util.DueBucket;
import com.example.assignmenttracker.util.DueDates;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * Home list state. Adding and editing moved to {@link AssignmentEditViewModel} in Slice 2.
 * Slice 3: mark done / undo, delete / restore, and the collapsible Done section.
 * Slice 4: SEC-3-1 (Undo writes updatedAt = now), QA-3-2 (pending Undo in the ViewModel),
 * DES-3-3 (announce only a real reopen) and re-bucketing on refresh.
 */
public class AssignmentsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private static final long NOW = 1_762_600_000_000L;
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Chicago");

    private long now = NOW;
    private final FakeRepo repo = new FakeRepo();
    private final AssignmentsViewModel vm = new AssignmentsViewModel(repo, () -> now, () -> TZ);

    @Test
    public void homeItemsFollowTheRepository() {
        List<List<HomeItem>> seen = observe();
        repo.data.setValue(new ArrayList<>());
        assertEquals(HomeItem.TYPE_EMPTY, last(seen).get(0).getType());
        repo.data.setValue(List.of(Assignment.newTodo("a", "A", 1)));
        assertEquals(HomeItem.TYPE_SUMMARY, last(seen).get(0).getType());
        assertEquals(HomeItem.TYPE_SECTION_HEADER, last(seen).get(1).getType());
        assertEquals(HomeItem.TYPE_ROW, last(seen).get(2).getType());
    }

    @Test
    public void nothingIsEmittedBeforeTheFirstLoad() {
        List<List<HomeItem>> seen = observe();
        vm.toggleDoneExpanded();
        vm.refresh();
        assertTrue(seen.isEmpty());
    }

    @Test
    public void markDoneSetsDoneCompletedAtAndUpdatedAtToNow() {
        Assignment a = Assignment.newTodo("a", "Quiz", 1_000L);
        assertTrue(vm.setDone(a, true));
        assertEquals(Arrays.asList("a", Assignment.STATUS_DONE, NOW, NOW), repo.statusWrites.get(0));
    }

    @Test
    public void uncheckSetsOpenAndClearsCompletedAt() {
        Assignment a = Assignment.newTodo("a", "Quiz", 1_000L);
        a.status = Assignment.STATUS_DONE;
        a.completedAt = 5_000L;
        assertTrue(vm.setDone(a, false));
        assertEquals(Arrays.asList("a", Assignment.STATUS_TODO, null, NOW), repo.statusWrites.get(0));
        assertNull("unchecking offers no Undo (design.md §4b)", vm.getPendingUndo());
    }

    @Test
    public void setDoneToTheCurrentStateWritesNothing() {
        assertFalse(vm.setDone(Assignment.newTodo("a", "Quiz", 1L), false));
        assertTrue(repo.statusWrites.isEmpty());
    }

    /** SEC-3-1: Undo after mark done reopens the row and stamps updatedAt = now, never the old value. */
    @Test
    public void undoDoneRestoresTheOpenStatusClearsCompletedAtAndWritesUpdatedAtNow() {
        Assignment a = Assignment.newTodo("a", "Quiz", 1_000L);
        a.updatedAt = 2_000L;
        assertTrue(vm.setDone(a, true));
        PendingUndo p = vm.getPendingUndo();
        assertNotNull(p);
        assertEquals(PendingUndo.Kind.MARK_DONE, p.kind);
        assertNotSame("the snapshot is a copy", a, p.assignment);
        now = NOW + 3_000L;
        vm.undo(p);
        assertEquals(Arrays.asList("a", Assignment.STATUS_TODO, null, NOW + 3_000L), repo.statusWrites.get(1));
        assertNull("used up", vm.getPendingUndo());
    }

    /** SEC-3-1: Undo after delete re-inserts the same entity, but updatedAt is now. */
    @Test
    public void deleteRemovesByIdAndUndoRestoresTheEntityWithUpdatedAtNow() {
        Assignment a = Assignment.newTodo("keep", "Lab report", 1_000L);
        a.course = "Biology 101";
        a.notes = "n";
        a.dueAt = 7_000L;
        a.updatedAt = 3_000L;
        vm.delete(a);
        assertEquals(List.of("keep"), repo.deleted);
        PendingUndo p = vm.getPendingUndo();
        assertEquals(PendingUndo.Kind.DELETE, p.kind);
        now = NOW + 4_000L;
        vm.undo(p);
        Assignment back = repo.inserted.get(0);
        assertEquals("keep", back.id);
        assertEquals("Lab report", back.title);
        assertEquals("Biology 101", back.course);
        assertEquals("n", back.notes);
        assertEquals(Long.valueOf(7_000L), back.dueAt);
        assertEquals(1_000L, back.createdAt);
        assertEquals(Assignment.STATUS_TODO, back.status);
        assertEquals("updatedAt = now (SEC-3-1)", NOW + 4_000L, back.updatedAt);
        assertEquals("the kept copy itself is untouched", 3_000L, p.assignment.updatedAt);
    }

    /** QA-3-2: the pending Undo lives in the ViewModel, so a rotation can show it again. */
    @Test
    public void pendingUndoSurvivesUntilTheSnackbarEndsAndReportsTheTimeLeft() {
        vm.delete(Assignment.newTodo("a", "A", 1));
        PendingUndo p = vm.getPendingUndo();
        assertEquals(AssignmentsViewModel.UNDO_MS, vm.undoTimeLeft(p));
        now = NOW + 1_500L;
        assertEquals(AssignmentsViewModel.UNDO_MS - 1_500L, vm.undoTimeLeft(p));
        now = NOW + 4_900L;
        assertEquals("never shorter than the floor", AssignmentsViewModel.UNDO_MIN_MS, vm.undoTimeLeft(p));
        vm.onUndoEnded(p);
        assertNull(vm.getPendingUndo());
    }

    @Test
    public void aNewUndoReplacesTheOldAndTheOldOnesEndDoesNotClearIt() {
        vm.delete(Assignment.newTodo("a", "A", 1));
        PendingUndo first = vm.getPendingUndo();
        vm.delete(Assignment.newTodo("b", "B", 1));
        PendingUndo second = vm.getPendingUndo();
        assertNotSame(first, second);
        vm.onUndoEnded(first);
        assertSame(second, vm.getPendingUndo());
    }

    /** DES-3-3: mark done is announced by its snackbar; only a real reopen is announced. */
    @Test
    public void onlyARealReopenIsAnnounced() {
        Assignment a = Assignment.newTodo("a", "Quiz", 1L);
        vm.setDone(a, true);
        assertNull("mark done: the snackbar speaks", vm.getAnnouncement().getValue());
        a.status = Assignment.STATUS_DONE;
        a.completedAt = 2L;
        assertTrue(vm.setDone(a, false));
        assertEquals("Quiz", vm.getAnnouncement().getValue());
        vm.onAnnounced();
        assertNull(vm.getAnnouncement().getValue());
        assertFalse("ignored tap: no write", vm.setDone(Assignment.newTodo("b", "B", 1L), false));
        assertNull(vm.getAnnouncement().getValue());
    }

    @Test
    public void aFailedReopenIsNotAnnounced() {
        repo.fail = new RuntimeException("disk");
        Assignment a = Assignment.newTodo("a", "Quiz", 1L);
        a.status = Assignment.STATUS_DONE;
        a.completedAt = 2L;
        vm.setDone(a, false);
        assertNull(vm.getAnnouncement().getValue());
    }

    @Test
    public void anIgnoredCheckOffersNoUndo() {
        Assignment d = Assignment.newTodo("a", "Quiz", 1L);
        d.status = Assignment.STATUS_DONE;
        d.completedAt = 2L;
        assertFalse(vm.setDone(d, true));
        assertNull(vm.getPendingUndo());
    }

    /** The day can change while Home is open: a refresh re-buckets with the new clock. */
    @Test
    public void refreshRebucketsWithTheCurrentClock() {
        List<List<HomeItem>> seen = observe();
        long tomorrow = DueDates.nextLocalMidnight(NOW, TZ);
        Assignment a = Assignment.newTodo("a", "A", 1);
        a.dueAt = tomorrow;
        repo.data.setValue(List.of(a));
        assertEquals(DueBucket.THIS_WEEK, last(seen).get(1).getBucket());
        now = tomorrow + 1;
        vm.refresh();
        assertEquals(DueBucket.TODAY, last(seen).get(1).getBucket());
    }

    @Test
    public void doneSectionIsCollapsedByDefaultAndToggles() {
        List<List<HomeItem>> seen = observe();
        Assignment d = Assignment.newTodo("d", "Done one", 1);
        d.status = Assignment.STATUS_DONE;
        d.completedAt = 2L;
        repo.data.setValue(List.of(d));
        assertEquals("caught up + Done header", 2, last(seen).size());
        assertEquals(HomeItem.TYPE_CAUGHT_UP, last(seen).get(0).getType());
        assertFalse(last(seen).get(1).isExpanded());
        vm.toggleDoneExpanded();
        assertEquals(3, last(seen).size());
        assertTrue(last(seen).get(1).isExpanded());
        vm.toggleDoneExpanded();
        assertEquals(2, last(seen).size());
    }

    @Test
    public void aFailedWriteIsReportedOnceUntilShown() {
        repo.fail = new RuntimeException("disk");
        vm.delete(Assignment.newTodo("a", "A", 1));
        assertEquals(Boolean.TRUE, vm.getWriteFailed().getValue());
        vm.onWriteFailureShown();
        assertEquals(Boolean.FALSE, vm.getWriteFailed().getValue());
    }

    @Test
    public void successfulWritesReportNoFailure() {
        vm.setDone(Assignment.newTodo("a", "A", 1), true);
        assertEquals(Boolean.FALSE, vm.getWriteFailed().getValue());
    }

    @Test
    public void copyKeepsEveryField() {
        Assignment a = Assignment.newTodo("x", "T", 1);
        a.course = "C";
        a.notes = "N";
        a.dueAt = 2L;
        a.priority = Assignment.PRIORITY_HIGH;
        a.status = Assignment.STATUS_DONE;
        a.completedAt = 3L;
        a.updatedAt = 4L;
        Assignment b = a.copy();
        assertNotSame(a, b);
        assertEquals(a.id, b.id);
        assertEquals(a.title, b.title);
        assertEquals(a.course, b.course);
        assertEquals(a.notes, b.notes);
        assertEquals(a.dueAt, b.dueAt);
        assertEquals(a.priority, b.priority);
        assertEquals(a.status, b.status);
        assertEquals(a.completedAt, b.completedAt);
        assertEquals(a.createdAt, b.createdAt);
        assertEquals(a.updatedAt, b.updatedAt);
        assertNull(Assignment.newTodo("y", "Y", 1).copy().completedAt);
    }

    private List<List<HomeItem>> observe() {
        List<List<HomeItem>> seen = new ArrayList<>();
        Observer<List<HomeItem>> o = seen::add;
        vm.getHomeItems().observeForever(o);
        return seen;
    }

    private static <T> T last(List<T> l) {
        return l.get(l.size() - 1);
    }

    static final class FakeRepo implements AssignmentRepository {
        final MutableLiveData<List<Assignment>> data = new MutableLiveData<>();
        final List<List<Object>> statusWrites = new ArrayList<>();
        final List<String> deleted = new ArrayList<>();
        final List<Assignment> inserted = new ArrayList<>();
        @Nullable Throwable fail;

        @NonNull
        @Override
        public LiveData<List<Assignment>> observeAll() {
            return data;
        }

        @NonNull
        @Override
        public LiveData<Assignment> observeById(@NonNull String id) {
            return new MutableLiveData<>();
        }

        @Override
        public void insert(@NonNull Assignment assignment, @NonNull WriteCallback done) {
            inserted.add(assignment);
            done.onComplete(fail);
        }

        @Override
        public void updateFields(@NonNull String id, @NonNull String title, @Nullable String course,
                                 @Nullable String notes, @Nullable Long dueAt, long updatedAt,
                                 @NonNull WriteCallback done) {
            done.onComplete(fail);
        }

        @Override
        public void updateStatus(@NonNull String id, @NonNull String status, @Nullable Long completedAt,
                                 long updatedAt, @NonNull WriteCallback done) {
            statusWrites.add(Arrays.asList(id, status, completedAt, updatedAt));
            done.onComplete(fail);
        }

        @Override
        public void delete(@NonNull String id, @NonNull WriteCallback done) {
            deleted.add(id);
            done.onComplete(fail);
        }
    }
}
