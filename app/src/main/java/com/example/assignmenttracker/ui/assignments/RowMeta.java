package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.entity.Assignment;

/**
 * Row line 2 (design.md §3.1): the parts that exist, joined with {@code meta_separator}.
 * Slice 2 has only the course; Slice 4 puts the due label in front of it.
 */
public final class RowMeta {

    private RowMeta() {
    }

    /** The line 2 text, or null when there is nothing to show (line 2 is hidden). */
    @Nullable
    public static String text(@NonNull Assignment a, @NonNull String separator) {
        return join(separator, a.course);
    }

    @Nullable
    static String join(@NonNull String separator, @Nullable String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(separator);
            sb.append(p);
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
