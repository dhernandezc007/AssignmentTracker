package com.example.assignmenttracker.ui.assignments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.App;
import com.example.assignmenttracker.R;
import com.example.assignmenttracker.SystemBarInsets;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.databinding.ActivityAssignmentEditBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Add / Edit screen (design.md §5). Opened by the FAB (new) and by tapping a row (edit, with
 * {@link #EXTRA_ASSIGNMENT_ID}). Close (✕) and Back finish without saving. Save finishes only
 * after the write succeeds; on failure it stays and shows {@code error_save}.
 */
public class AssignmentEditActivity extends AppCompatActivity {

    public static final String EXTRA_ASSIGNMENT_ID = "com.example.assignmenttracker.extra.ASSIGNMENT_ID";

    /** design.md §5.2: the notes counter appears only past this many characters. */
    static final int NOTES_COUNTER_FROM = 1800;

    private static final String KEY_PREFILLED = "prefilled";
    private static final int TITLE_MAX_LINES = 3;

    private ActivityAssignmentEditBinding binding;
    private AssignmentEditViewModel viewModel;
    /** Edit mode: true once the saved values are in the fields, so they are never re-applied. */
    private boolean prefilled;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @Nullable String assignmentId) {
        Intent i = new Intent(context, AssignmentEditActivity.class);
        if (assignmentId != null) i.putExtra(EXTRA_ASSIGNMENT_ID, assignmentId);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityAssignmentEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // System bars → padding, keyboard → bottom margin: Save rides above the IME (§5.1).
        SystemBarInsets.apply(binding.root);

        String id = getIntent().getStringExtra(EXTRA_ASSIGNMENT_ID);
        viewModel = new ViewModelProvider(this,
                AssignmentEditViewModel.factory(((App) getApplication()).getAssignmentRepository(), id))
                .get(AssignmentEditViewModel.class);

        binding.toolbar.setTitle(viewModel.isNew() ? R.string.edit_title_new : R.string.edit_title_edit);
        binding.toolbar.setNavigationOnClickListener(v -> finish()); // discard, no prompt

        // Enter stays "Next" (no textMultiLine) while a long title still wraps to 3 lines.
        binding.etTitle.setHorizontallyScrolling(false);
        binding.etTitle.setMaxLines(TITLE_MAX_LINES);
        binding.etNotes.addTextChangedListener(new AfterChange(s ->
                binding.tilNotes.setCounterEnabled(s.length() > NOTES_COUNTER_FROM)));

        binding.btnSave.setOnClickListener(v -> attemptSave());

        viewModel.getSaveStatus().observe(this, status -> {
            if (status == AssignmentEditViewModel.SaveStatus.SAVED) {
                finish(); // the row in the list is the confirmation (no toast)
            } else if (status == AssignmentEditViewModel.SaveStatus.FAILED) {
                Snackbar.make(binding.root, R.string.error_save, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.btnSave)
                        .show();
                viewModel.onSaveFailureShown();
            }
        });

        if (viewModel.isNew()) {
            prefilled = true;
            if (savedInstanceState == null) showKeyboardWhenFocused(binding.etTitle);
        } else {
            // Load once. After a rotation the views restore what was typed, so don't overwrite it.
            prefilled = savedInstanceState != null && savedInstanceState.getBoolean(KEY_PREFILLED);
            viewModel.getAssignment().observe(this, this::onOriginalLoaded);
        }
    }

    /**
     * The error-clearing watcher goes on after the views restore their state, or restoring the
     * title text on rotation would wipe the restored inline error.
     */
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        binding.etTitle.addTextChangedListener(new AfterChange(s -> binding.tilTitle.setError(null)));
    }

    private void onOriginalLoaded(@Nullable Assignment a) {
        if (a == null) {
            // Deleted elsewhere (not reachable in the MVP): leave quietly.
            if (viewModel.getSaveStatus().getValue() != AssignmentEditViewModel.SaveStatus.SAVED) finish();
            return;
        }
        if (prefilled) return;
        binding.etTitle.setText(a.title);
        binding.etCourse.setText(a.course);
        binding.etNotes.setText(a.notes);
        prefilled = true;
    }

    private void attemptSave() {
        if (!prefilled) return; // edit mode, values not loaded yet (a few ms at most)
        AssignmentEditViewModel.SaveResult r = viewModel.save(
                binding.etTitle.getText(), binding.etCourse.getText(), binding.etNotes.getText());
        if (r == AssignmentEditViewModel.SaveResult.INVALID_TITLE) {
            binding.tilTitle.setError(getString(R.string.field_title_error_empty));
            binding.etTitle.requestFocus();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_PREFILLED, prefilled);
    }

    /**
     * New mode: focus Title and open the keyboard once the window has focus. Asking for the
     * IME at window creation (SOFT_INPUT_STATE_VISIBLE) holds the first frame until the
     * keyboard is ready (the Slice 1 QA-1-1 lesson), so the manifest says stateHidden.
     */
    private void showKeyboardWhenFocused(@NonNull View field) {
        Runnable show = () -> {
            field.requestFocus();
            WindowCompat.getInsetsController(getWindow(), field).show(WindowInsetsCompat.Type.ime());
        };
        if (field.hasWindowFocus()) {
            field.post(show);
            return;
        }
        field.getViewTreeObserver().addOnWindowFocusChangeListener(
                new ViewTreeObserver.OnWindowFocusChangeListener() {
                    @Override
                    public void onWindowFocusChanged(boolean hasFocus) {
                        if (!hasFocus) return;
                        field.getViewTreeObserver().removeOnWindowFocusChangeListener(this);
                        field.post(show);
                    }
                });
    }

    /** A TextWatcher that only cares about the text after each change. */
    private static final class AfterChange implements TextWatcher {
        private final java.util.function.Consumer<Editable> onChange;

        AfterChange(java.util.function.Consumer<Editable> onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.accept(s);
        }
    }
}
