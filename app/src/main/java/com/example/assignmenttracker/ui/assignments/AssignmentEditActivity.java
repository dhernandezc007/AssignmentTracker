package com.example.assignmenttracker.ui.assignments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.App;
import com.example.assignmenttracker.R;
import com.example.assignmenttracker.SystemBarInsets;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.databinding.ActivityAssignmentEditBinding;
import com.example.assignmenttracker.util.DateFormats;
import com.example.assignmenttracker.util.DueDates;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.TimeZone;

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
    /** Process death: the due date being edited (absent = none). Rotation keeps it in the ViewModel. */
    private static final String KEY_DUE = "due";
    private static final String TAG_PICKER = "due_picker";
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
        binding.etNotes.addTextChangedListener(new AfterChange(s -> {
            binding.tilNotes.setCounterEnabled(s.length() > NOTES_COUNTER_FROM);
            keepNotesCounterInView();
        }));

        binding.btnSave.setOnClickListener(v -> attemptSave());

        // Due date (Slice 4, design.md §5.2): tap opens the picker, the end ✕ clears it.
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_DUE)) {
            viewModel.setDueAt(savedInstanceState.getLong(KEY_DUE));
        }
        viewModel.getDueAt().observe(this, this::renderDue);
        binding.etDue.setOnClickListener(v -> openDatePicker());
        // Keyboard users (the field is focusable by keyboard only): Enter or Space opens the picker.
        binding.etDue.setOnKeyListener(ActivationKeys.clickOnActivation());
        binding.tilDue.setEndIconOnClickListener(v -> clearDue());
        Fragment shown = getSupportFragmentManager().findFragmentByTag(TAG_PICKER);
        if (shown instanceof MaterialDatePicker) {
            // Listeners aren't kept across a rotation; the picker itself is.
            @SuppressWarnings("unchecked") MaterialDatePicker<Long> picker = (MaterialDatePicker<Long>) shown;
            listenTo(picker);
        }

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

    /**
     * QA-2-1: while the late notes counter is on and the student types in Notes, scroll just
     * enough to show the counter under the field. The scroll view on its own only keeps the
     * cursor line in view, which left the counter under the pinned Save. If the field is so
     * tall that the cursor and the counter can't both fit, the cursor wins.
     */
    private void keepNotesCounterInView() {
        if (!binding.tilNotes.isCounterEnabled() || !binding.etNotes.hasFocus()) return;
        // After the text change has been laid out and the platform has scrolled to the cursor.
        binding.scroll.post(() -> {
            android.text.Layout layout = binding.etNotes.getLayout();
            if (layout == null || !binding.tilNotes.isCounterEnabled()) return;
            int viewport = binding.scroll.getHeight() - binding.scroll.getPaddingTop()
                    - binding.scroll.getPaddingBottom();
            android.graphics.Rect r = new android.graphics.Rect(0, 0, 0, binding.tilNotes.getHeight());
            binding.scroll.offsetDescendantRectToMyCoords(binding.tilNotes, r);
            int counterBottom = r.bottom;
            r.set(0, 0, 0, 0);
            binding.scroll.offsetDescendantRectToMyCoords(binding.etNotes, r);
            int line = layout.getLineForOffset(binding.etNotes.getSelectionEnd());
            int cursorTop = r.top + binding.etNotes.getTotalPaddingTop() + layout.getLineTop(line);

            int y = binding.scroll.getScrollY();
            if (counterBottom <= y + viewport) return;         // already visible
            if (counterBottom - cursorTop > viewport) return;   // can't show both: keep the cursor
            binding.scroll.scrollTo(0, counterBottom - viewport);
        });
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
        viewModel.setDueAt(a.dueAt);
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
        Long due = viewModel.getDueAt().getValue();
        if (due != null) outState.putLong(KEY_DUE, due);
    }

    /**
     * The ✕ hides itself once tapped, which would leave TalkBack focus nowhere; design.md §5.2
     * asks for focus to move back to the field, the one place it can sensibly go.
     */
    @android.annotation.SuppressLint("AccessibilityFocus")
    private void clearDue() {
        viewModel.setDueAt(null);
        binding.etDue.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
    }

    /** design.md §5.2: the date with its year, or "No due date" in the muted color; ✕ only when set. */
    private void renderDue(@Nullable Long due) {
        if (due == null) {
            binding.etDue.setText(R.string.field_due_empty);
            binding.etDue.setTextColor(MaterialColors.getColor(binding.etDue,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
        } else {
            Locale locale = getResources().getConfiguration().getLocales().get(0);
            binding.etDue.setText(DateFormats.withYear(due, locale, TimeZone.getDefault()));
            binding.etDue.setTextColor(MaterialColors.getColor(binding.etDue,
                    com.google.android.material.R.attr.colorOnSurface));
        }
        binding.tilDue.setEndIconVisible(due != null);
    }

    /**
     * MaterialDatePicker (design.md §5.2): opens on the current date or today, calendar mode,
     * past dates allowed, deep-pink overlay (§8.1 rule 5). One at a time.
     */
    private void openDatePicker() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_PICKER) != null) return;
        Long due = viewModel.getDueAt().getValue();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date_picker_title)
                .setSelection(due == null ? MaterialDatePicker.todayInUtcMilliseconds()
                        : DueDates.toPickerSelection(due, TimeZone.getDefault()))
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .setTheme(R.style.ThemeOverlay_AT_DatePicker)
                .build();
        listenTo(picker);
        picker.showNow(getSupportFragmentManager(), TAG_PICKER);
    }

    /** The picker answers in UTC midnight; store the local start of that day. */
    private void listenTo(@NonNull MaterialDatePicker<Long> picker) {
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                viewModel.setDueAt(DueDates.fromPickerSelection(selection, TimeZone.getDefault()));
            }
        });
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
