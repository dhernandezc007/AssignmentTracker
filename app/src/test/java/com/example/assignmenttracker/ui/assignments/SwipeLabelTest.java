package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** DES-3-1: the swipe label is drawn only when the revealed width holds inset + icon + gap + label. */
public class SwipeLabelTest {

    private static final int INSET = 20;
    private static final int ICON = 24;
    private static final int GAP = 8;
    private static final float TEXT = 60f; // needs 112 in all

    @Test
    public void theLabelWaitsUntilTheWholeGroupFits() {
        assertFalse(SwipeLabel.fits(0f, INSET, ICON, GAP, TEXT));
        assertFalse(SwipeLabel.fits(111.9f, INSET, ICON, GAP, TEXT));
        assertTrue(SwipeLabel.fits(112f, INSET, ICON, GAP, TEXT));
        assertTrue(SwipeLabel.fits(300f, INSET, ICON, GAP, TEXT));
    }

    @Test
    public void bothDirectionsUseTheRevealedWidth() {
        assertFalse(SwipeLabel.fits(-111.9f, INSET, ICON, GAP, TEXT));
        assertTrue(SwipeLabel.fits(-112f, INSET, ICON, GAP, TEXT));
    }

    /** At 200% font in ES ("Eliminar" at 28sp) the label needs much more room than in EN. */
    @Test
    public void aLongerLabelNeedsAWiderReveal() {
        assertTrue(SwipeLabel.fits(150f, INSET, ICON, GAP, 60f));
        assertFalse(SwipeLabel.fits(150f, INSET, ICON, GAP, 130f));
    }
}
