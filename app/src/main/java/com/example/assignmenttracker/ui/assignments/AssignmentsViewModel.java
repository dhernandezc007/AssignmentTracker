package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.data.repo.AssignmentRepository;

import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Home list state. Activity → this → {@link AssignmentRepository} → Room (architecture §3.1). */
public class AssignmentsViewModel extends ViewModel {

    private final AssignmentRepository repository;
    private final LongSupplier clock;
    private final Supplier<String> ids;
    private final LiveData<List<HomeItem>> homeItems;
    private final MutableLiveData<Boolean> saveFailed = new MutableLiveData<>(false);

    public AssignmentsViewModel(@NonNull AssignmentRepository repository,
                                @NonNull LongSupplier clock,
                                @NonNull Supplier<String> ids) {
        this.repository = repository;
        this.clock = clock;
        this.ids = ids;
        this.homeItems = Transformations.map(repository.observeAll(), HomeItems::build);
    }

    @NonNull
    public LiveData<List<HomeItem>> getHomeItems() {
        return homeItems;
    }

    /** True once after a write fails, until {@link #onSaveFailedShown()}. */
    @NonNull
    public LiveData<Boolean> getSaveFailed() {
        return saveFailed;
    }

    public void onSaveFailedShown() {
        saveFailed.setValue(false);
    }

    /**
     * Validates and saves a new title-only assignment.
     *
     * @return false if the title is blank (nothing is saved), true if the save was queued
     */
    public boolean addAssignment(CharSequence rawTitle) {
        String title = TitleValidator.normalize(rawTitle);
        if (title == null) return false;
        Assignment a = Assignment.newTodo(ids.get(), title, clock.getAsLong());
        repository.insert(a, error -> saveFailed.postValue(true));
        return true;
    }

    /** Builds the ViewModel with the real clock and random UUIDs. */
    public static ViewModelProvider.Factory factory(@NonNull AssignmentRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AssignmentsViewModel(repository, System::currentTimeMillis,
                        () -> UUID.randomUUID().toString());
            }
        };
    }
}
