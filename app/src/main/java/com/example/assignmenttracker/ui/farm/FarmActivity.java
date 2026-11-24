package com.example.assignmenttracker.ui.farm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.assignmenttracker.App;
import com.example.assignmenttracker.R;
import com.example.assignmenttracker.SystemBarInsets;
import com.example.assignmenttracker.data.Crop;
import com.example.assignmenttracker.databinding.ActivityFarmBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * The milpa. Every 3 finished assignments earn a seed; the student picks a crop and plants it,
 * and it grows over 7 days into a full plant that stays in the field for good. Opened from the
 * milpa button on Home.
 */
public class FarmActivity extends AppCompatActivity {

    private ActivityFarmBinding binding;
    private FarmViewModel viewModel;
    private FarmAdapter adapter;
    /** Plots shown last time, to bring a just-planted one into view. -1 before the first load. */
    private int shownPlots = -1;
    private boolean planting;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, FarmActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityFarmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.apply(binding.root);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this,
                FarmViewModel.factory(((App) getApplication()).getFarmRepository()))
                .get(FarmViewModel.class);

        adapter = new FarmAdapter(this::chooseCrop);
        GridLayoutManager grid = new GridLayoutManager(this, FarmAdapter.COLUMNS);
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItemViewType(position) == FarmAdapter.TYPE_HEADER ? FarmAdapter.COLUMNS : 1;
            }
        });
        binding.field.setLayoutManager(grid);
        binding.field.setAdapter(adapter);

        viewModel.getState().observe(this, state -> {
            int plots = state.plots.size();
            boolean justPlanted = planting && shownPlots >= 0 && plots > shownPlots;
            adapter.submitList(FarmAdapter.items(state), justPlanted
                    ? () -> {
                        binding.field.smoothScrollToPosition(plots); // header is 0, so the new plot is at plots
                        binding.confetti.celebrate();
                    }
                    : null);
            if (justPlanted) planting = false;
            shownPlots = plots;
        });
        viewModel.getPlantFailed().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            planting = false;
            Snackbar.make(binding.root, R.string.farm_plant_failed, Snackbar.LENGTH_LONG).show();
            viewModel.onPlantFailureShown();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Plants grow while the screen is away; show today's growth.
        viewModel.refresh();
    }

    /** "What will you plant?": maíz, frijol, calabaza or chile. */
    private void chooseCrop() {
        Crop[] crops = Crop.values();
        CharSequence[] names = new CharSequence[crops.length];
        for (int i = 0; i < crops.length; i++) names[i] = getString(FarmAdapter.cropChoice(crops[i]));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.farm_choose_crop)
                .setItems(names, (dialog, which) -> {
                    planting = true;
                    viewModel.plant(crops[which]);
                })
                .show();
    }
}
