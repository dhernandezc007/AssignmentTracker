package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Optional free-text fields (course, notes). Blank means "not set" (null), so the row's
 * line 2 hides. The cap backs up the EditText's maxLength (SEC Slice 2 note).
 */
public final class OptionalText {

    private OptionalText() {
    }

    /** Trimmed at both ends (course). */
    @Nullable
    public static String normalize(@Nullable CharSequence raw, int max) {
        if (raw == null) return null;
        return cap(raw.toString().trim(), max);
    }

    /** Trimmed at the end only, so a leading indent survives (notes, design.md §5.2). */
    @Nullable
    public static String normalizeTrailing(@Nullable CharSequence raw, int max) {
        if (raw == null) return null;
        String s = raw.toString();
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return cap(s.substring(0, end), max);
    }

    @Nullable
    private static String cap(@NonNull String s, int max) {
        if (s.trim().isEmpty()) return null;
        if (s.length() <= max) return s;
        int cut = max;
        // Never leave half of a surrogate pair at the end.
        if (Character.isHighSurrogate(s.charAt(cut - 1))) cut--;
        return s.substring(0, cut);
    }
}
