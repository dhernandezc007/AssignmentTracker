package com.example.assignmenttracker.data;

/**
 * Field caps (design.md §5.2), in one place (SEC-2-1). The edit screen's maxLength, the
 * edit ViewModel and the repository all use these; lengths are UTF-16 units, like
 * {@code android:maxLength}.
 */
public final class AssignmentLimits {

    public static final int MAX_TITLE = 200;
    public static final int MAX_COURSE = 60;
    public static final int MAX_NOTES = 2000;

    private AssignmentLimits() {
    }
}
