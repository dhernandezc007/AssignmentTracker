package com.example.assignmenttracker.ui.assignments;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** The counts behind the home summary line, and design.md §3.4's content rule. Pure. */
public final class HomeSummary {

    /** One piece of the summary line, rendered from a plural in strings.xml. */
    public static final class Part {
        public enum Kind { OVERDUE, TODAY, WEEK, NOTHING_THIS_WEEK }

        @NonNull public final Kind kind;
        public final int count;

        Part(@NonNull Kind kind, int count) {
            this.kind = kind;
            this.count = count;
        }
    }

    public final int overdue;
    public final int today;
    public final int thisWeek;
    public final int open;
    public final int done;

    public HomeSummary(int overdue, int today, int thisWeek, int open, int done) {
        this.overdue = overdue;
        this.today = today;
        this.thisWeek = thisWeek;
        this.open = open;
        this.done = done;
    }

    /**
     * §3.4: overdue and due-today parts when non-zero; otherwise this week; otherwise "nothing
     * due this week · N open". Empty when nothing is open (All caught up / empty state instead).
     */
    @NonNull
    public List<Part> parts() {
        List<Part> out = new ArrayList<>(2);
        if (overdue > 0) out.add(new Part(Part.Kind.OVERDUE, overdue));
        if (today > 0) out.add(new Part(Part.Kind.TODAY, today));
        if (out.isEmpty() && thisWeek > 0) out.add(new Part(Part.Kind.WEEK, thisWeek));
        if (out.isEmpty() && open > 0) out.add(new Part(Part.Kind.NOTHING_THIS_WEEK, open));
        return out;
    }

    boolean sameAs(@NonNull HomeSummary o) {
        return overdue == o.overdue && today == o.today && thisWeek == o.thisWeek
                && open == o.open && done == o.done;
    }
}
