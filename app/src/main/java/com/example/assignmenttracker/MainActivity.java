package com.example.assignmenttracker;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.databinding.ActivityMainBinding;
import com.example.assignmenttracker.ui.assignments.AssignmentEditActivity;
import com.example.assignmenttracker.ui.assignments.AssignmentListAdapter;
import com.example.assignmenttracker.ui.assignments.AssignmentsViewModel;
import com.example.assignmenttracker.ui.assignments.HomeItems;
import com.example.assignmenttracker.ui.assignments.SwipeToDeleteCallback;
import com.google.android.material.snackbar.Snackbar;

/** Home: the assignment list (design.md §2). The app's only launcher. */
public class MainActivity extends AppCompatActivity {

    /** design.md §4: the Undo snackbar shows for 5 s. */
    private static final int UNDO_MS = 5000;

    private ActivityMainBinding binding;
    private AssignmentsViewModel viewModel;
    private AssignmentListAdapter adapter;
    @Nullable private Snackbar snackbar;
    private boolean openingEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setAccessibilityHeading(binding.title, true);
        applyInsets();

        AssignmentsViewModel vm = new ViewModelProvider(this,
                AssignmentsViewModel.factory(((App) getApplication()).getAssignmentRepository()))
                .get(AssignmentsViewModel.class);

        viewModel = vm;
        AssignmentListAdapter.Listener listener = new AssignmentListAdapter.Listener() {
            @Override
            public void onRowClick(@NonNull Assignment a) {
                openEditor(a.id);
            }

            @Override
            public void onToggleDone(@NonNull Assignment a, boolean done) {
                toggleDone(a, done);
            }

            @Override
            public void onDelete(@NonNull Assignment a) {
                delete(a);
            }

            @Override
            public void onToggleDoneSection() {
                vm.toggleDoneExpanded();
            }
        };
        adapter = new AssignmentListAdapter(listener);
        binding.list.setAdapter(adapter);
        new ItemTouchHelper(new SwipeToDeleteCallback(this, adapter, listener, binding.list))
                .attachToRecyclerView(binding.list);
        // DES-1-4: RecyclerView makes itself focusable in its constructor (so XML can't turn
        // it off). Focused, it draws the platform's grey highlight over the whole list in
        // keyboard mode. Rows are the focus targets and draw their own ripple focus state.
        binding.list.setFocusable(false);
        binding.list.setFocusableInTouchMode(false);
        vm.getHomeItems().observe(this, items -> {
            // Keep a just-added (newest-first) row in view; an expand or an undo doesn't jump.
            boolean newAtTop = HomeItems.newRowAtTop(adapter.getCurrentList(), items);
            adapter.submitList(items, newAtTop ? () -> binding.list.scrollToPosition(0) : null);
        });
        vm.getWriteFailed().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            // A failed delete leaves the swiped row off-screen; rebind so it slides back.
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            showSnackbar(Snackbar.make(binding.main, R.string.error_save, Snackbar.LENGTH_LONG));
            vm.onWriteFailureShown();
        });

        binding.fabAdd.setOnClickListener(v -> openEditor(null));

        // design.md §6: shrink on scroll down past 16dp, extend on scroll up.
        int threshold = getResources().getDimensionPixelSize(R.dimen.fab_scroll_threshold);
        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0 && rv.computeVerticalScrollOffset() > threshold) {
                    binding.fabAdd.shrink();
                } else if (dy < 0) {
                    binding.fabAdd.extend();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        openingEditor = false;
    }

    /**
     * Mark done or reopen (design.md §4b). The row leaves its place, so TalkBack hears where it
     * went. Marking done also offers Undo (Proposal 1, approved 2026-11-09), because the Done
     * section is collapsed and the row seems to vanish.
     */
    @SuppressWarnings("deprecation") // announceForAccessibility: §4b; no non-deprecated equivalent on API 24+
    private void toggleDone(@NonNull Assignment a, boolean done) {
        Assignment before = viewModel.setDone(a, done);
        binding.list.announceForAccessibility(getString(
                done ? R.string.announce_marked_done : R.string.announce_marked_open, a.title));
        if (done) {
            showSnackbar(Snackbar.make(binding.main, R.string.snackbar_marked_done, UNDO_MS)
                    .setAction(R.string.action_undo, v -> viewModel.undoStatus(before)));
        }
    }

    /** Swipe or TalkBack delete (design.md §4): gone from Room now, back on Undo. */
    private void delete(@NonNull Assignment a) {
        Assignment kept = viewModel.delete(a);
        showSnackbar(Snackbar.make(binding.main, R.string.snackbar_deleted, UNDO_MS)
                .setAction(R.string.action_undo, v -> viewModel.restore(kept)));
    }

    /**
     * Above the FAB. A new snackbar replaces the one showing; the earlier action stays done.
     * Material extends the timeout for accessibility users on its own.
     */
    private void showSnackbar(@NonNull Snackbar s) {
        if (snackbar != null) snackbar.dismiss();
        snackbar = s;
        s.setAnchorView(binding.fabAdd).show();
    }

    /**
     * Opens Add/Edit: new when {@code id} is null, otherwise that assignment. A fast double
     * tap would start two screens (the QA-0-4 pattern), so only the first tap counts until
     * Home is resumed again.
     */
    private void openEditor(@Nullable String id) {
        if (openingEditor) return;
        openingEditor = true;
        startActivity(AssignmentEditActivity.newIntent(this, id));
    }

    /**
     * Edge-to-edge (design.md §2). Status bar → header top padding; nav bar → list bottom
     * padding and FAB bottom margin. Side insets (landscape cutout / nav bar) go to the
     * content (title text, list cards, FAB), never to the decorative banner, which bleeds to
     * both edges (QA-1-3). Physical left/right because insets are physical.
     */
    private void applyInsets() {
        int clearance = getResources().getDimensionPixelSize(R.dimen.list_bottom_clearance);
        int side = getResources().getDimensionPixelSize(R.dimen.space_screen);
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            binding.header.setPadding(0, bars.top, 0, 0);
            binding.title.setPadding(side + bars.left, binding.title.getPaddingTop(),
                    side + bars.right, binding.title.getPaddingBottom());
            binding.list.setPadding(bars.left, 0, bars.right, clearance + bars.bottom);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.fabAdd.getLayoutParams();
            lp.leftMargin = side + bars.left;
            lp.rightMargin = side + bars.right;
            lp.bottomMargin = side + bars.bottom;
            binding.fabAdd.setLayoutParams(lp);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
