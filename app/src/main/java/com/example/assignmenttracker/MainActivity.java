package com.example.assignmenttracker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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
import com.example.assignmenttracker.ui.assignments.PendingUndo;
import com.example.assignmenttracker.ui.assignments.SwipeToDeleteCallback;
import com.example.assignmenttracker.util.DueDates;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.TimeZone;

/** Home: the bucketed assignment list (design.md §2, §3). The app's only launcher. */
public class MainActivity extends AppCompatActivity {

    /** Re-bucket a moment after local midnight, so "Due tomorrow" becomes "Due today". */
    private static final long AFTER_MIDNIGHT_MS = 1000;

    private ActivityMainBinding binding;
    private AssignmentsViewModel viewModel;
    private AssignmentListAdapter adapter;
    @Nullable private Snackbar snackbar;
    private boolean openingEditor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable midnightTick = new Runnable() {
        @Override
        public void run() {
            viewModel.refresh();
            scheduleMidnightTick();
        }
    };

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
            // A just-added row lands in its bucket; bring it (and its header) into view. The
            // first load, an expand or a removal doesn't move the list (what's due next stays first).
            int target = HomeItems.scrollTargetForNewOpenRow(adapter.getCurrentList(), items);
            adapter.submitList(items, target < 0 ? null : () -> binding.list.scrollToPosition(target));
        });
        vm.getWriteFailed().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            // A failed delete leaves the swiped row off-screen; rebind so it slides back.
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            // The Undo on offer no longer matches what's in the database.
            PendingUndo p = vm.getPendingUndo();
            if (p != null) vm.onUndoEnded(p);
            showSnackbar(Snackbar.make(binding.main, R.string.error_save, Snackbar.LENGTH_LONG));
            vm.onWriteFailureShown();
        });
        // DES-3-3: only a reopen that really happened is spoken (mark done has its snackbar).
        vm.getAnnouncement().observe(this, title -> {
            if (title == null) return;
            announce(getString(R.string.announce_marked_open, title));
            vm.onAnnounced();
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

        // QA-3-2: a rotation within the 5 s must not lose Undo. The ViewModel kept it.
        PendingUndo pending = vm.getPendingUndo();
        if (pending != null) showUndo(pending, vm.undoTimeLeft(pending));
    }

    @Override
    protected void onResume() {
        super.onResume();
        openingEditor = false;
        // The day may have changed while Home was in the background.
        viewModel.refresh();
        scheduleMidnightTick();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(midnightTick);
        super.onPause();
    }

    private void scheduleMidnightTick() {
        handler.removeCallbacks(midnightTick);
        long now = System.currentTimeMillis();
        long delay = DueDates.nextLocalMidnight(now, TimeZone.getDefault()) - now + AFTER_MIDNIGHT_MS;
        handler.postDelayed(midnightTick, delay);
    }

    @SuppressWarnings("deprecation") // announceForAccessibility: §4b; no non-deprecated equivalent on API 24+
    private void announce(@NonNull String text) {
        binding.list.announceForAccessibility(text);
    }

    /**
     * Mark done or reopen (design.md §4b). Marking done offers Undo (Proposal 1), and that
     * snackbar is the only announcement (DES-3-3). An ignored tap does nothing.
     */
    private void toggleDone(@NonNull Assignment a, boolean done) {
        if (!viewModel.setDone(a, done) || !done) return;
        PendingUndo p = viewModel.getPendingUndo();
        if (p != null) showUndo(p, AssignmentsViewModel.UNDO_MS);
    }

    /** Swipe or TalkBack delete (design.md §4): gone from Room now, back on Undo. */
    private void delete(@NonNull Assignment a) {
        viewModel.delete(a);
        PendingUndo p = viewModel.getPendingUndo();
        if (p != null) showUndo(p, AssignmentsViewModel.UNDO_MS);
    }

    /** The Undo snackbar for {@code p}. Its timeout or a swipe-away ends the Undo in the ViewModel. */
    private void showUndo(@NonNull PendingUndo p, long durationMs) {
        int text = p.kind == PendingUndo.Kind.DELETE ? R.string.snackbar_deleted : R.string.snackbar_marked_done;
        Snackbar s = Snackbar.make(binding.main, text, (int) durationMs)
                .setAction(R.string.action_undo, v -> viewModel.undo(p));
        s.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar bar, int event) {
                // MANUAL (replaced, or the activity going away for a rotation) keeps the Undo.
                if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE) viewModel.onUndoEnded(p);
            }
        });
        showSnackbar(s);
    }

    /**
     * Above the FAB. A new snackbar replaces the one showing; the earlier action stays done.
     * Material extends the timeout for accessibility users on its own. DES-3-2: 16dp side
     * margins (the 12dp corners come from Widget.AT.Snackbar).
     */
    private void showSnackbar(@NonNull Snackbar s) {
        if (snackbar != null) snackbar.dismiss();
        snackbar = s;
        View v = s.getView();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        int side = getResources().getDimensionPixelSize(R.dimen.space_screen);
        lp.leftMargin = side;
        lp.rightMargin = side;
        v.setLayoutParams(lp);
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
