package com.example.assignmenttracker.ui.assignments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.EditorInfo;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.App;
import com.example.assignmenttracker.R;
import com.example.assignmenttracker.databinding.DialogAddAssignmentBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Slice 1 "title only" add dialog (design.md §5.3). A DialogFragment so it and the typed
 * text survive rotation. Slice 2 replaces it with AssignmentEditActivity.
 */
public class AddAssignmentDialogFragment extends DialogFragment {

    public static final String TAG = "add_assignment";

    private DialogAddAssignmentBinding binding;
    private AssignmentsViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity(),
                AssignmentsViewModel.factory(((App) requireContext().getApplicationContext()).getAssignmentRepository()))
                .get(AssignmentsViewModel.class);

        binding = DialogAddAssignmentBinding.inflate(getLayoutInflater());
        binding.etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear the error on the next keystroke (Danny's Login pattern).
                binding.tilTitle.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.etTitle.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                trySave();
                return true;
            }
            return false;
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_add_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.action_save, null) // wired in onStart so it can stay open
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        // QA-1-1/QA-1-2: do NOT ask for the keyboard while the window is being created
        // (SOFT_INPUT_STATE_VISIBLE + requestFocus here). The system then holds the dialog's
        // first frame until the IME is ready, which took ~1.5 s on the emulator. Taps in that
        // window fell through to the list behind an invisible dialog. The dialog now draws at
        // once and the keyboard is requested after it has window focus (showKeyboardWhenFocused).
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> trySave());
            showKeyboardWhenFocused(dialog);
        }
    }

    private void showKeyboardWhenFocused(@NonNull AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        View field = binding.etTitle;
        Runnable show = () -> {
            field.requestFocus();
            WindowCompat.getInsetsController(window, field).show(WindowInsetsCompat.Type.ime());
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

    private void trySave() {
        if (viewModel.addAssignment(binding.etTitle.getText())) {
            dismiss();
        } else {
            binding.tilTitle.setError(getString(R.string.field_title_error_empty));
            binding.etTitle.requestFocus();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
