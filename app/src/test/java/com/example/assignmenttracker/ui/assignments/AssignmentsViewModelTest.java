package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.data.repo.AssignmentRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Home list state. Adding and editing moved to {@link AssignmentEditViewModel} in Slice 2.
 * Slice 3: mark done / undo, delete / restore, and the collapsible Done section.
 */
public class AssignmentsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private static final long NOW = 1_762_600_000_000L;

    private final FakeRepo repo = new FakeRepo();
    private final AssignmentsViewModel vm = new AssignmentsViewModel(repo, () -> NOW);

    @Test
    public void homeItemsFollowTheRepository() {
        List<List<HomeItem>> seen = observe();
        repo.data.setValue(new ArrayList<>());
        assertEquals(HomeItem.TYPE_EMPTY, last(seen).get(0).getType());
        repo.data.setValue(List.of(Assignment.newTodo("a", "A", 1)));
        assertEquals(HomeItem.TYPE_ROW, last(seen).get(0).getType());
    }

    @Test
    public void nothingIsEmittedBeforeTheFirstLoad() {
        List<List<HomeItem>> seen = observe();
        vm.toggleDoneExpanded();
        assertTrue(seen.isEmpty());
    }

    @Test
    public void markDoneSetsDoneCompletedAtAndUpdatedAtToNow() {
        Assignment a = Assignment.newTodo("a", "Quiz", 1_000L);
        vm.setDone(a, true);
        assertEquals(Arrays.asList("a", Assignment.STATUS_DONE, NOW, NOW), repo.statusWrites.get(0));
    }

    @Test
    public void uncheckSetsOpenAndClearsCompletedAt() {
        Assignment a = Assignment.newTodo("a", "Quiz", 1_000L);
        a.status = Assignment.STATUS_DONE;
        a.completedAt = 5_000L;
        vm.setDone(a, false);
        assertEquals(Arrays.asList("a", Assignment.STATUS_TODO, null, NOW), repo.statusWrites.get(0));
    }

    @Test
    public void setDoneToTheCurrentStateWritesNothing() {
        vm.setDone(Assignment.newTodo("a", "Quiz", 1L), false);
        assertTrue(repo.statusWrites.isEmpty());
    }

    /** Proposal 1 (approved 2026-11-09): Undo after mark done puts the row back as it was. */
    @Test
    public void undoDoneRestoresTheOpenStatusClearsCompletedAtAndKeepsUpdatedAt() {
        Assignment a = Assignment.newTodo("a", "Quiz", 1_000L);
        a.updatedAt = 2_000L;
        Assignment before = vm.setDone(a, true);
        assertNotSame("the snapshot is a copy", a, before);
        vm.undoStatus(before);
        assertEquals(Arrays.asList("a", Assignment.STATUS_TODO, null, 2_000L), repo.statusWrites.get(1));
    }

    @Test
    public void deleteRemovesByIdAndReturnsTheFullEntityForUndo() {
        Assignment a = Assignment.newTodo("keep", "Lab report", 1_000L);
        a.course = "Biology 101";
        a.notes = "n";
        a.dueAt = 7_000L;
        a.updatedAt = 3_000L;
        Assignment kept = vm.delete(a);
        assertEquals(List.of("keep"), repo.deleted);
        vm.restore(kept);
        Assignment back = repo.inserted.get(0);
        assertEquals("keep", back.id);
        assertEquals("Lab report", back.title);
        assertEquals("Biology 101", back.course);
        assertEquals("n", back.notes);
        assertEquals(Long.valueOf(7_000L), back.dueAt);
        assertEquals(1_000L, back.createdAt);
        assertEquals("same updatedAt (design.md §4)", 3_000L, back.updatedAt);
        assertEquals(Assignment.STATUS_TODO, back.status);
    }

    @Test
    public void doneSectionIsCollapsedByDefaultAndToggles() {
        List<List<HomeItem>> seen = observe();
        Assignment d = Assignment.newTodo("d", "Done one", 1);
        d.status = Assignment.STATUS_DONE;
        d.completedAt = 2L;
        repo.data.setValue(List.of(d));
        assertEquals(1, last(seen).size());
        assertFalse(last(seen).get(0).isExpanded());
        vm.toggleDoneExpanded();
        assertEquals(2, last(seen).size());
        assertTrue(last(seen).get(0).isExpanded());
        vm.toggleDoneExpanded();
        assertEquals(1, last(seen).size());
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
                                 @Nullable String notes, long updatedAt, @NonNull WriteCallback done) {
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
