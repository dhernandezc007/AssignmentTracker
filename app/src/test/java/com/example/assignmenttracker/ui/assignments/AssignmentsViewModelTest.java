package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;

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
import java.util.List;

/** Home list state. Adding and editing moved to {@link AssignmentEditViewModel} in Slice 2. */
public class AssignmentsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeRepo repo = new FakeRepo();
    private final AssignmentsViewModel vm = new AssignmentsViewModel(repo);

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

    private static <T> T last(List<T> l) {
        return l.get(l.size() - 1);
    }

    static final class FakeRepo implements AssignmentRepository {
        final MutableLiveData<List<Assignment>> data = new MutableLiveData<>();

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
            done.onComplete(null);
        }

        @Override
        public void updateFields(@NonNull String id, @NonNull String title, @Nullable String course,
                                 @Nullable String notes, long updatedAt, @NonNull WriteCallback done) {
            done.onComplete(null);
        }
    }
}
