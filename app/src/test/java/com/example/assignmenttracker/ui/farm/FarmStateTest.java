package com.example.assignmenttracker.ui.farm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.assignmenttracker.data.Crop;
import com.example.assignmenttracker.data.entity.FarmPlot;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Milpa rules: 3 assignments earn a seed; plants grow over 7 days and stay. */
public class FarmStateTest {

    private static final long NOW = 1_795_000_000_000L;
    private static final long DAY = Growth.DAY_MS;

    @Test
    public void everyThreeCreditsEarnASeed() {
        assertEquals(0, FarmState.of(0, null, NOW).seeds);
        assertEquals(0, FarmState.of(2, null, NOW).seeds);
        assertEquals(1, FarmState.of(3, null, NOW).seeds);
        assertEquals(2, FarmState.of(7, null, NOW).seeds);
        assertEquals("1 toward the next", 1, FarmState.of(7, null, NOW).progress);
    }

    @Test
    public void plantingSpendsSeeds() {
        List<FarmPlot> one = Collections.singletonList(FarmPlot.of("p", "MAIZ", NOW));
        assertEquals(1, FarmState.of(6, one, NOW).seeds);
        assertEquals(0, FarmState.of(3, one, NOW).seeds);
    }

    /** An Undo after planting can leave fewer credits than the field needs: no negative seeds. */
    @Test
    public void seedsNeverGoNegative() {
        List<FarmPlot> two = Arrays.asList(FarmPlot.of("a", "MAIZ", NOW), FarmPlot.of("b", "CHILE", NOW));
        assertEquals(0, FarmState.of(5, two, NOW).seeds);
    }

    @Test
    public void plotsGrowOverSevenDaysAndAreCountedWhenGrown() {
        List<FarmPlot> plots = Arrays.asList(
                FarmPlot.of("new", "FRIJOL", NOW),
                FarmPlot.of("mid", "CALABAZA", NOW - 3 * DAY),
                FarmPlot.of("old", "MAIZ", NOW - 30 * DAY));
        FarmState s = FarmState.of(9, plots, NOW);
        assertEquals(Growth.Stage.SEED, s.plots.get(0).stage);
        assertEquals(1, s.plots.get(0).day);
        assertEquals(Growth.Stage.GROWING, s.plots.get(1).stage);
        assertEquals(4, s.plots.get(1).day);
        assertEquals(Growth.Stage.GROWN, s.plots.get(2).stage);
        assertEquals("grown plants stay grown", 1f, s.plots.get(2).growth, 0f);
        assertEquals(1, s.grown);
    }

    @Test
    public void stagesFollowTheGrowthFraction() {
        assertEquals(Growth.Stage.SEED, Growth.stage(Growth.fraction(NOW, NOW)));
        assertEquals(Growth.Stage.SPROUT, Growth.stage(Growth.fraction(NOW - DAY, NOW)));
        assertEquals(Growth.Stage.FLOWERING, Growth.stage(Growth.fraction(NOW - 5 * DAY, NOW)));
        assertEquals(Growth.Stage.GROWN, Growth.stage(Growth.fraction(NOW - 7 * DAY, NOW)));
        assertEquals("a clock set back reads as just planted", 0f, Growth.fraction(NOW, NOW - DAY), 0f);
    }

    @Test
    public void unknownCropsDrawAsMaiz() {
        assertNull(Crop.parse("TOMATE"));
        assertEquals(Crop.MAIZ, Crop.parseOrMaiz("TOMATE"));
        FarmState s = FarmState.of(3, Collections.singletonList(FarmPlot.of("x", "TOMATE", NOW)), NOW);
        assertEquals(Crop.MAIZ, s.plots.get(0).crop);
        assertTrue(s.plots.get(0).growth >= 0f);
    }
}
