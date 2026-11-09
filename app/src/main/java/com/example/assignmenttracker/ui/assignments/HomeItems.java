package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignmenttracker.data.entity.Assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Turns the assignment list into adapter items. Pure, so it is unit-tested on the JVM. */
public final class HomeItems {

    private HomeItems() {
    }

    @NonNull
    public static List<HomeItem> build(@Nullable List<Assignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.singletonList(HomeItem.empty());
        }
        // Before Slice 4 every row sits in one card under no header (design.md §1).
        int n = assignments.size();
        List<HomeItem> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            RowPosition p = n == 1 ? RowPosition.SINGLE
                    : i == 0 ? RowPosition.TOP
                    : i == n - 1 ? RowPosition.BOTTOM
                    : RowPosition.MIDDLE;
            out.add(HomeItem.row(assignments.get(i), p));
        }
        return out;
    }
}
