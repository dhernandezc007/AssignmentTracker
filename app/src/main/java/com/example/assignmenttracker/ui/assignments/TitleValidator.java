package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.Nullable;

/** Title rule for the add/edit screen: trimmed, not blank, at most 200 characters. */
public final class TitleValidator {

    public static final int MAX_LENGTH = 200;

    private TitleValidator() {
    }

    /** Returns the trimmed title, or null if it is blank or longer than {@link #MAX_LENGTH}. */
    @Nullable
    public static String normalize(@Nullable CharSequence raw) {
        if (raw == null) return null;
        String t = raw.toString().trim();
        if (t.isEmpty() || t.length() > MAX_LENGTH) return null;
        return t;
    }
}
