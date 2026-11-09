package com.example.assignmenttracker;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.databinding.ActivityMainBinding;
import com.example.assignmenttracker.ui.assignments.AddAssignmentDialogFragment;
import com.example.assignmenttracker.ui.assignments.AssignmentListAdapter;
import com.example.assignmenttracker.ui.assignments.AssignmentsViewModel;
import com.google.android.material.snackbar.Snackbar;

/** Home: the assignment list (design.md §2). The app's only launcher. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

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

        AssignmentListAdapter adapter = new AssignmentListAdapter();
        binding.list.setAdapter(adapter);
        vm.getHomeItems().observe(this, items -> {
            boolean grew = items.size() > adapter.getItemCount() && adapter.getItemCount() > 0;
            // Keep a just-added (newest-first) row in view.
            adapter.submitList(items, grew ? () -> binding.list.scrollToPosition(0) : null);
        });

        vm.getSaveFailed().observe(this, failed -> {
            if (Boolean.TRUE.equals(failed)) {
                Snackbar.make(binding.main, R.string.error_save, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.fabAdd)
                        .show();
                vm.onSaveFailedShown();
            }
        });

        binding.fabAdd.setOnClickListener(v -> {
            if (getSupportFragmentManager().findFragmentByTag(AddAssignmentDialogFragment.TAG) == null) {
                new AddAssignmentDialogFragment().show(getSupportFragmentManager(), AddAssignmentDialogFragment.TAG);
            }
        });

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
