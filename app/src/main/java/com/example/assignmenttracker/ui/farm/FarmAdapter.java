package com.example.assignmenttracker.ui.farm;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.data.Crop;
import com.example.assignmenttracker.data.dao.FarmDao;
import com.example.assignmenttracker.databinding.ItemFarmHeaderBinding;
import com.example.assignmenttracker.databinding.ItemFarmPlotBinding;
import com.example.assignmenttracker.util.DateFormats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/** The milpa screen: a full-width header, then the field as a grid of plots. */
final class FarmAdapter extends ListAdapter<FarmAdapter.Item, RecyclerView.ViewHolder> {

    static final int TYPE_HEADER = 0;
    static final int TYPE_PLOT = 1;
    static final int TYPE_EMPTY = 2;
    static final int COLUMNS = 3;

    interface Listener {
        void onPlant();
    }

    /** One entry: the header (with the whole state), a planted plot, or an empty furrow. */
    static final class Item {
        final int type;
        @Nullable final FarmState state;
        @Nullable final FarmState.Plot plot;
        final int emptyIndex;

        private Item(int type, @Nullable FarmState state, @Nullable FarmState.Plot plot, int emptyIndex) {
            this.type = type;
            this.state = state;
            this.plot = plot;
            this.emptyIndex = emptyIndex;
        }
    }

    /**
     * Header, every plot, then empty furrows to finish the last row (a full row when nothing
     * is planted yet), so the field always reads as a field.
     */
    @NonNull
    static List<Item> items(@NonNull FarmState s) {
        List<Item> out = new ArrayList<>(s.plots.size() + 4);
        out.add(new Item(TYPE_HEADER, s, null, 0));
        for (FarmState.Plot p : s.plots) out.add(new Item(TYPE_PLOT, null, p, 0));
        int empty = s.plots.isEmpty() ? COLUMNS : (COLUMNS - s.plots.size() % COLUMNS) % COLUMNS;
        for (int i = 0; i < empty; i++) out.add(new Item(TYPE_EMPTY, null, null, i));
        return out;
    }

    private final Listener listener;

    FarmAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            ItemFarmHeaderBinding b = ItemFarmHeaderBinding.inflate(inflater, parent, false);
            b.btnPlant.setOnClickListener(v -> listener.onPlant());
            return new HeaderHolder(b);
        }
        ItemFarmPlotBinding b = ItemFarmPlotBinding.inflate(inflater, parent, false);
        b.plot.setClipToOutline(true);
        return new PlotHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(Objects.requireNonNull(item.state));
        } else {
            ((PlotHolder) holder).bind(item.plot);
        }
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        private final ItemFarmHeaderBinding b;
        private final View[] dots;

        HeaderHolder(@NonNull ItemFarmHeaderBinding b) {
            super(b.getRoot());
            this.b = b;
            this.dots = new View[]{b.dot1, b.dot2, b.dot3};
            ViewCompat.setAccessibilityHeading(b.seeds, true);
        }

        void bind(@NonNull FarmState s) {
            Resources res = b.getRoot().getResources();
            b.seeds.setText(s.seeds > 0
                    ? res.getQuantityString(R.plurals.farm_seeds_to_plant, s.seeds, s.seeds)
                    : res.getString(R.string.farm_no_seeds));
            b.btnPlant.setVisibility(s.seeds > 0 ? View.VISIBLE : View.GONE);

            int per = FarmDao.CREDITS_PER_SEED;
            for (int i = 0; i < dots.length; i++) {
                dots[i].setBackgroundResource(i < s.progress
                        ? R.drawable.bg_seed_dot_filled : R.drawable.bg_seed_dot_empty);
                dots[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            b.progress.setText(res.getString(R.string.farm_progress, s.progress, per));

            boolean planted = !s.plots.isEmpty();
            b.stats.setVisibility(planted ? View.VISIBLE : View.GONE);
            b.stats.setText(res.getString(R.string.farm_stats, s.plots.size(), s.grown));
            b.emptyField.setVisibility(planted ? View.GONE : View.VISIBLE);
        }
    }

    static final class PlotHolder extends RecyclerView.ViewHolder {
        private final ItemFarmPlotBinding b;

        PlotHolder(@NonNull ItemFarmPlotBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(@Nullable FarmState.Plot p) {
            if (p == null) {
                // An empty furrow: just soil, nothing for TalkBack to read.
                b.art.setEmpty();
                b.cropName.setText(null);
                b.stage.setText(R.string.plot_empty);
                b.plot.setContentDescription(null);
                b.plot.setFocusable(false);
                b.plot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                return;
            }
            Resources res = b.getRoot().getResources();
            b.art.setPlot(p.crop, p.growth);
            String crop = res.getString(cropName(p.crop));
            String stage = res.getString(stageName(p.stage));
            b.cropName.setText(crop);
            b.stage.setText(p.stage == Growth.Stage.GROWN ? stage : res.getString(R.string.plot_meta, stage, p.day));
            Locale locale = res.getConfiguration().getLocales().get(0);
            String planted = DateFormats.spoken(p.plantedAt, System.currentTimeMillis(), locale, TimeZone.getDefault());
            b.plot.setContentDescription(res.getString(R.string.plot_cd, crop, stage, planted));
            b.plot.setFocusable(true);
            b.plot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    @StringRes
    static int cropName(@NonNull Crop c) {
        switch (c) {
            case FRIJOL:
                return R.string.crop_frijol;
            case CALABAZA:
                return R.string.crop_calabaza;
            case CHILE:
                return R.string.crop_chile;
            default:
                return R.string.crop_maiz;
        }
    }

    @StringRes
    static int cropChoice(@NonNull Crop c) {
        switch (c) {
            case FRIJOL:
                return R.string.crop_choice_frijol;
            case CALABAZA:
                return R.string.crop_choice_calabaza;
            case CHILE:
                return R.string.crop_choice_chile;
            default:
                return R.string.crop_choice_maiz;
        }
    }

    @StringRes
    static int stageName(@NonNull Growth.Stage s) {
        switch (s) {
            case SPROUT:
                return R.string.stage_sprout;
            case GROWING:
                return R.string.stage_growing;
            case FLOWERING:
                return R.string.stage_flowering;
            case GROWN:
                return R.string.stage_grown;
            default:
                return R.string.stage_seed;
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item a, @NonNull Item b) {
            if (a.type != b.type) return false;
            if (a.type == TYPE_PLOT) return Objects.requireNonNull(a.plot).id.equals(Objects.requireNonNull(b.plot).id);
            if (a.type == TYPE_EMPTY) return a.emptyIndex == b.emptyIndex;
            return true;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item a, @NonNull Item b) {
            if (a.type == TYPE_HEADER) {
                FarmState x = Objects.requireNonNull(a.state);
                FarmState y = Objects.requireNonNull(b.state);
                return x.seeds == y.seeds && x.progress == y.progress
                        && x.plots.size() == y.plots.size() && x.grown == y.grown;
            }
            if (a.type == TYPE_PLOT) {
                FarmState.Plot x = Objects.requireNonNull(a.plot);
                FarmState.Plot y = Objects.requireNonNull(b.plot);
                return Float.compare(x.growth, y.growth) == 0 && x.day == y.day && x.crop.equals(y.crop);
            }
            return true;
        }
    };
}
