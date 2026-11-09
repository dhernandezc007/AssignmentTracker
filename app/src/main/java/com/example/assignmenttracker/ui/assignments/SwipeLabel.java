package com.example.assignmenttracker.ui.assignments;

/** DES-3-1: when the swipe background has room for the "Delete" label. Pure, unit-tested. */
final class SwipeLabel {

    private SwipeLabel() {
    }

    /** True when the revealed width holds the whole group: inset + icon + gap + label. */
    static boolean fits(float dX, int inset, int iconWidth, int gap, float textWidth) {
        return Math.abs(dX) >= inset + iconWidth + gap + textWidth;
    }
}
