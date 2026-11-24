package com.example.assignmenttracker.ui.farm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.assignmenttracker.data.Crop;
import com.example.assignmenttracker.data.entity.FarmPlot;
import com.example.assignmenttracker.data.repo.FarmRepository;

import java.util.List;
import java.util.function.LongSupplier;

/** The milpa: seeds earned, the field, and planting. Used by the milpa screen and by Home. */
public class FarmViewModel extends ViewModel {

    private final FarmRepository repository;
    private final LongSupplier clock;
    private final MediatorLiveData<FarmState> state = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> plantFailed = new MutableLiveData<>(false);
    @Nullable private Integer credits;
    @Nullable private List<FarmPlot> plots;

    public FarmViewModel(@NonNull FarmRepository repository, @NonNull LongSupplier clock) {
        this.repository = repository;
        this.clock = clock;
        state.addSource(repository.observeCreditCount(), c -> {
            credits = c != null ? c : 0;
            rebuild();
        });
        state.addSource(repository.observePlots(), p -> {
            plots = p;
            rebuild();
        });
    }

    /** Emits once both sources have loaded, so nothing flashes "0 seeds" on the way in. */
    private void rebuild() {
        if (credits == null || plots == null) return;
        state.setValue(FarmState.of(credits, plots, clock.getAsLong()));
    }

    @NonNull
    public LiveData<FarmState> getState() {
        return state;
    }

    /** Re-reads the clock so plants show today's growth (on resume). */
    public void refresh() {
        rebuild();
    }

    /** True after planting failed, until {@link #onPlantFailureShown()}. */
    @NonNull
    public LiveData<Boolean> getPlantFailed() {
        return plantFailed;
    }

    public void onPlantFailureShown() {
        plantFailed.setValue(false);
    }

    /** Plants one seed of {@code crop}. No seed available is a quiet no-op (the button was stale). */
    public void plant(@NonNull Crop crop) {
        repository.plant(crop.name(), clock.getAsLong(), (planted, error) -> {
            if (error != null) plantFailed.postValue(true);
        });
    }

    @NonNull
    public static ViewModelProvider.Factory factory(@NonNull FarmRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new FarmViewModel(repository, System::currentTimeMillis);
            }
        };
    }
}
