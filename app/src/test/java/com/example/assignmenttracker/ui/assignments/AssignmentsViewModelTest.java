package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import java.util.List;
import java.util.function.Consumer;

public class AssignmentsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeRepo repo = new FakeRepo();
    private final AssignmentsViewModel vm = new AssignmentsViewModel(repo, () -> 1_700_000_000_000L, () -> "uuid-1");

    @Test
    public void blankTitleIsRejectedAndNothingIsSaved() {
        assertFalse(vm.addAssignment("   "));
        assertTrue(repo.inserted.isEmpty());
    }

    @Test
    public void validTitleInsertsATrimmedTodoWithIdAndTimestamps() {
        assertTrue(vm.addAssignment("  Essay outline "));
        assertEquals(1, repo.inserted.size());
        Assignment a = repo.inserted.get(0);
        assertEquals("uuid-1", a.id);
        assertEquals("Essay outline", a.title);
        assertEquals(Assignment.STATUS_TODO, a.status);
        assertEquals(Assignment.PRIORITY_NORMAL, a.priority);
        assertEquals(1_700_000_000_000L, a.createdAt);
        assertEquals(1_700_000_000_000L, a.updatedAt);
        assertNull(a.dueAt);
        assertNull(a.completedAt);
        assertNull(a.courseId);
        assertNull(a.notes);
    }

    @Test
    public void homeItemsFollowTheRepository() {
        List<List<HomeItem>> seen = new ArrayList<>();
        Observer<List<HomeItem>> o = seen::add;
        vm.getHomeItems().observeForever(o);
        repo.data.setValue(new ArrayList<>());
        assertEquals(HomeItem.TYPE_EMPTY, last(seen).get(0).getType());
        repo.data.setValue(List.of(Assignment.newTodo("a", "A", 1)));
        assertEquals(HomeItem.TYPE_ROW, last(seen).get(0).getType());
        vm.getHomeItems().removeObserver(o);
    }

    @Test
    public void saveFailureIsReportedOnceThenCleared() {
        repo.failWith = new RuntimeException("disk full");
        vm.getSaveFailed().observeForever(b -> { });
        vm.addAssignment("Quiz");
        assertEquals(Boolean.TRUE, vm.getSaveFailed().getValue());
        vm.onSaveFailedShown();
        assertEquals(Boolean.FALSE, vm.getSaveFailed().getValue());
    }

    private static <T> T last(List<T> l) {
        return l.get(l.size() - 1);
    }

    static final class FakeRepo implements AssignmentRepository {
        final MutableLiveData<List<Assignment>> data = new MutableLiveData<>();
        final List<Assignment> inserted = new ArrayList<>();
        @Nullable RuntimeException failWith;

        @Override
        public LiveData<List<Assignment>> observeAll() {
            return data;
        }

        @Override
        public void insert(Assignment assignment, Consumer<Throwable> onError) {
            if (failWith != null) {
                onError.accept(failWith);
            } else {
                inserted.add(assignment);
            }
        }
    }
}
