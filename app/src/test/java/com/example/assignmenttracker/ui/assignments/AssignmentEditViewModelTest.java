package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.data.repo.AssignmentRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Slice 2: the add/edit screen's state (new item, edit item, validation, save status). */
public class AssignmentEditViewModelTest {

    private static final long NOW = 1_762_700_000_000L;

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeRepo repo = new FakeRepo();

    private AssignmentEditViewModel newVm() {
        return new AssignmentEditViewModel(repo, () -> NOW, () -> "uuid-new", null);
    }

    private AssignmentEditViewModel editVm(String id) {
        return new AssignmentEditViewModel(repo, () -> NOW, () -> "unused", id);
    }

    @Test
    public void newItemIsNewAndHasNothingToLoad() {
        AssignmentEditViewModel vm = newVm();
        assertTrue(vm.isNew());
        assertNull(vm.getOriginal());
    }

    @Test
    public void blankTitleIsRejectedAndNothingIsWritten() {
        AssignmentEditViewModel vm = newVm();
        assertEquals(AssignmentEditViewModel.SaveResult.INVALID_TITLE, vm.save("  \n ", "Bio", "x"));
        assertTrue(repo.inserted.isEmpty());
        assertTrue(repo.updates.isEmpty());
        assertEquals(AssignmentEditViewModel.SaveStatus.IDLE, vm.getSaveStatus().getValue());
    }

    @Test
    public void newSaveInsertsTrimmedFieldsAndBlankOptionalsBecomeNull() {
        AssignmentEditViewModel vm = newVm();
        assertEquals(AssignmentEditViewModel.SaveResult.STARTED, vm.save(" Essay outline ", "   ", ""));
        assertEquals(1, repo.inserted.size());
        Assignment a = repo.inserted.get(0);
        assertEquals("uuid-new", a.id);
        assertEquals("Essay outline", a.title);
        assertNull(a.course);
        assertNull(a.notes);
        assertEquals(NOW, a.createdAt);
        assertEquals(NOW, a.updatedAt);
        assertEquals(Assignment.STATUS_TODO, a.status);
    }

    @Test
    public void newSaveKeepsCourseAndNotes() {
        newVm().save("Lab report", " Biology 101 ", "Use Tuesday's data.\nTwo pages.");
        Assignment a = repo.inserted.get(0);
        assertEquals("Biology 101", a.course);
        assertEquals("Use Tuesday's data.\nTwo pages.", a.notes);
    }

    @Test
    public void successfulSaveReportsSaved() {
        AssignmentEditViewModel vm = newVm();
        vm.save("Quiz", null, null);
        assertEquals(AssignmentEditViewModel.SaveStatus.SAVING, vm.getSaveStatus().getValue());
        repo.completeLast(null);
        assertEquals(AssignmentEditViewModel.SaveStatus.SAVED, vm.getSaveStatus().getValue());
    }

    @Test
    public void failedSaveReportsFailedOnceAndAllowsARetry() {
        AssignmentEditViewModel vm = newVm();
        vm.save("Quiz", null, null);
        repo.completeLast(new RuntimeException("disk full"));
        assertEquals(AssignmentEditViewModel.SaveStatus.FAILED, vm.getSaveStatus().getValue());
        vm.onSaveFailureShown();
        assertEquals(AssignmentEditViewModel.SaveStatus.IDLE, vm.getSaveStatus().getValue());
        assertEquals(AssignmentEditViewModel.SaveResult.STARTED, vm.save("Quiz", null, null));
    }

    /** A double tap on Save must not write twice (a second row, or two updates). */
    @Test
    public void saveWhileSavingIsIgnored() {
        AssignmentEditViewModel vm = newVm();
        vm.save("Quiz", null, null);
        assertEquals(AssignmentEditViewModel.SaveResult.BUSY, vm.save("Quiz", null, null));
        assertEquals(1, repo.inserted.size());
    }

    @Test
    public void editLoadsTheOriginal() {
        Assignment original = existing();
        repo.byId.setValue(original);
        AssignmentEditViewModel vm = editVm("abc");
        assertFalse(vm.isNew());
        assertEquals("abc", repo.observedId);
        vm.getAssignment().observeForever(a -> { });
        assertSame(original, vm.getAssignment().getValue());
    }

    /** The edit-save path: the same id, the new fields and a fresh updatedAt. No insert. */
    @Test
    public void editSaveUpdatesTheRowAndUpdatedAt() {
        repo.byId.setValue(existing());
        AssignmentEditViewModel vm = editVm("abc");
        assertEquals(AssignmentEditViewModel.SaveResult.STARTED,
                vm.save(" Lab report v2 ", " Chem 110 ", " "));
        assertTrue(repo.inserted.isEmpty());
        assertEquals(1, repo.updates.size());
        Object[] u = repo.updates.get(0);
        assertEquals("abc", u[0]);
        assertEquals("Lab report v2", u[1]);
        assertEquals("Chem 110", u[2]);
        assertNull("blank notes are cleared", u[3]);
        assertEquals(NOW, u[4]);
        repo.completeLast(null);
        assertEquals(AssignmentEditViewModel.SaveStatus.SAVED, vm.getSaveStatus().getValue());
    }

    @Test
    public void editWithBlankTitleIsRejected() {
        repo.byId.setValue(existing());
        AssignmentEditViewModel vm = editVm("abc");
        assertEquals(AssignmentEditViewModel.SaveResult.INVALID_TITLE, vm.save("", "Bio", null));
        assertTrue(repo.updates.isEmpty());
    }

    /** design.md §5.2 caps: title 200, course 60, notes 2000. */
    @Test
    public void capsMatchTheDesign() {
        assertEquals(200, TitleValidator.MAX_LENGTH);
        assertEquals(60, AssignmentEditViewModel.MAX_COURSE);
        assertEquals(2000, AssignmentEditViewModel.MAX_NOTES);
    }

    @Test
    public void overlongCourseAndNotesAreCapped() {
        newVm().save("T", repeat('c', 150), repeat('n', 2500));
        Assignment a = repo.inserted.get(0);
        assertEquals(AssignmentEditViewModel.MAX_COURSE, a.course.length());
        assertEquals(AssignmentEditViewModel.MAX_NOTES, a.notes.length());
    }

    @Test
    public void notesKeepLeadingWhitespaceAndLoseTrailing() {
        newVm().save("T", null, "  - one\n  - two  \n");
        assertEquals("  - one\n  - two", repo.inserted.get(0).notes);
    }

    private static Assignment existing() {
        Assignment a = Assignment.newTodo("abc", "Lab report", 1_000L);
        a.course = "Biology 101";
        a.notes = "old notes";
        return a;
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    static final class FakeRepo implements AssignmentRepository {
        final MutableLiveData<List<Assignment>> all = new MutableLiveData<>();
        final MutableLiveData<Assignment> byId = new MutableLiveData<>();
        final List<Assignment> inserted = new ArrayList<>();
        final List<Object[]> updates = new ArrayList<>();
        final List<WriteCallback> pending = new ArrayList<>();
        @Nullable String observedId;

        @NonNull
        @Override
        public LiveData<List<Assignment>> observeAll() {
            return all;
        }

        @NonNull
        @Override
        public LiveData<Assignment> observeById(@NonNull String id) {
            observedId = id;
            return byId;
        }

        @Override
        public void insert(@NonNull Assignment assignment, @NonNull WriteCallback done) {
            inserted.add(assignment);
            pending.add(done);
        }

        @Override
        public void updateFields(@NonNull String id, @NonNull String title, @Nullable String course,
                                 @Nullable String notes, long updatedAt, @NonNull WriteCallback done) {
            updates.add(new Object[]{id, title, course, notes, updatedAt});
            pending.add(done);
        }

        @Override
        public void updateStatus(@NonNull String id, @NonNull String status, @Nullable Long completedAt,
                                 long updatedAt, @NonNull WriteCallback done) {
            throw new AssertionError("the edit screen never changes status");
        }

        @Override
        public void delete(@NonNull String id, @NonNull WriteCallback done) {
            throw new AssertionError("the edit screen never deletes");
        }

        void completeLast(@Nullable Throwable error) {
            pending.remove(pending.size() - 1).onComplete(error);
        }
    }
}
