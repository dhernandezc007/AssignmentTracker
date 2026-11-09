package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.data.AssignmentLimits;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.data.repo.AssignmentRepository;

import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * State of the add/edit screen (design.md §5). New mode when {@code editId} is null,
 * edit mode otherwise. The typed text lives in the views (they restore it on rotation);
 * this holds the loaded original and the save status.
 */
public class AssignmentEditViewModel extends ViewModel {

    /** design.md §5.2 caps, shared with the repository (SEC-2-1; title: {@link TitleValidator#MAX_LENGTH}). */
    public static final int MAX_COURSE = AssignmentLimits.MAX_COURSE;
    public static final int MAX_NOTES = AssignmentLimits.MAX_NOTES;

    public enum SaveResult { INVALID_TITLE, STARTED, BUSY }

    public enum SaveStatus { IDLE, SAVING, SAVED, FAILED }

    private final AssignmentRepository repository;
    private final LongSupplier clock;
    private final Supplier<String> ids;
    @Nullable private final String editId;
    @Nullable private final LiveData<Assignment> original;
    private final MutableLiveData<SaveStatus> saveStatus = new MutableLiveData<>(SaveStatus.IDLE);
    /** The due date being edited (Slice 4): local start of day, or null for none. */
    private final MutableLiveData<Long> dueAt = new MutableLiveData<>(null);

    public AssignmentEditViewModel(@NonNull AssignmentRepository repository,
                                   @NonNull LongSupplier clock,
                                   @NonNull Supplier<String> ids,
                                   @Nullable String editId) {
        this.repository = repository;
        this.clock = clock;
        this.ids = ids;
        this.editId = editId;
        this.original = editId == null ? null : repository.observeById(editId);
    }

    public boolean isNew() {
        return editId == null;
    }

    /** The saved row being edited (emits null if it doesn't exist), or null in new mode. */
    @Nullable
    public LiveData<Assignment> getOriginal() {
        return original;
    }

    /** Same as {@link #getOriginal()}, for edit mode only. */
    @NonNull
    public LiveData<Assignment> getAssignment() {
        if (original == null) throw new IllegalStateException("new mode has no original");
        return original;
    }

    @NonNull
    public LiveData<SaveStatus> getSaveStatus() {
        return saveStatus;
    }

    public void onSaveFailureShown() {
        saveStatus.setValue(SaveStatus.IDLE);
    }

    /** The due date shown in the field; survives rotation with the ViewModel. */
    @NonNull
    public LiveData<Long> getDueAt() {
        return dueAt;
    }

    /** Picked (local start of day) or cleared (null). */
    public void setDueAt(@Nullable Long value) {
        dueAt.setValue(value);
    }

    /** Validates and starts the write, with the current due date. Main thread. */
    @NonNull
    public SaveResult save(@Nullable CharSequence rawTitle, @Nullable CharSequence rawCourse,
                           @Nullable CharSequence rawNotes) {
        SaveStatus status = saveStatus.getValue();
        if (status == SaveStatus.SAVING || status == SaveStatus.SAVED) {
            return SaveResult.BUSY;
        }
        String title = TitleValidator.normalize(rawTitle);
        if (title == null) return SaveResult.INVALID_TITLE;
        String course = OptionalText.normalize(rawCourse, MAX_COURSE);
        String notes = OptionalText.normalizeTrailing(rawNotes, MAX_NOTES);
        long now = clock.getAsLong();

        saveStatus.setValue(SaveStatus.SAVING);
        AssignmentRepository.WriteCallback done = error ->
                saveStatus.postValue(error == null ? SaveStatus.SAVED : SaveStatus.FAILED);
        if (editId == null) {
            Assignment a = Assignment.newTodo(ids.get(), title, now);
            a.course = course;
            a.notes = notes;
            a.dueAt = dueAt.getValue();
            repository.insert(a, done);
        } else {
            repository.updateFields(editId, title, course, notes, dueAt.getValue(), now, done);
        }
        return SaveResult.STARTED;
    }

    /** The real clock and random UUIDs. */
    @NonNull
    public static ViewModelProvider.Factory factory(@NonNull AssignmentRepository repository,
                                                    @Nullable String editId) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AssignmentEditViewModel(repository, System::currentTimeMillis,
                        () -> UUID.randomUUID().toString(), editId);
            }
        };
    }
}
