package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.data.repo.AssignmentRepository;

import java.util.List;

/**
 * Home list state. Activity → this → {@link AssignmentRepository} → Room (architecture §3.1).
 * Adding and editing live in {@link AssignmentEditViewModel} (Slice 2).
 */
public class AssignmentsViewModel extends ViewModel {

    private final LiveData<List<HomeItem>> homeItems;

    public AssignmentsViewModel(@NonNull AssignmentRepository repository) {
        this.homeItems = Transformations.map(repository.observeAll(), HomeItems::build);
    }

    @NonNull
    public LiveData<List<HomeItem>> getHomeItems() {
        return homeItems;
    }

    public static ViewModelProvider.Factory factory(@NonNull AssignmentRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AssignmentsViewModel(repository);
            }
        };
    }
}
