package com.example.assignmenttracker.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Due-date math (Slice 4). Pure Java so it is unit-tested with a fixed clock and zone.
 *
 * <p>Storage: {@code dueAt} is the epoch millis of the <b>local start of the due day</b> in the
 * zone the date was picked in (design.md §5.2 leaves start- or end-of-day to the Engineer).
 * Buckets compare <b>local calendar dates</b> (never 24-hour steps), so DST days and the
 * midnight edge behave. Known gap: if the device later moves to a zone west of where the date
 * was picked, the stored instant falls on the previous local day there.
 */
public final class DueDates {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long DAY_MS = 86_400_000L;
    /** tasks.md: "this week" means the next 7 days (tomorrow through today + 7). */
    public static final int WEEK_DAYS = 7;

    private DueDates() {
    }

    /** Whole local days from today to the due date: 0 today, 1 tomorrow, -1 yesterday. */
    public static int daysFromToday(long dueAt, long now, @NonNull TimeZone tz) {
        return (int) (localEpochDay(dueAt, tz) - localEpochDay(now, tz));
    }

    @NonNull
    public static DueBucket bucketOf(@Nullable Long dueAt, long now, @NonNull TimeZone tz) {
        if (dueAt == null) return DueBucket.NO_DATE;
        int d = daysFromToday(dueAt, now, tz);
        if (d < 0) return DueBucket.OVERDUE;
        if (d == 0) return DueBucket.TODAY;
        if (d <= WEEK_DAYS) return DueBucket.THIS_WEEK;
        return DueBucket.LATER;
    }

    /** The first instant of the local day {@code year-month-day} (month 1-based) in {@code tz}. */
    public static long startOfLocalDay(int year, int month, int day, @NonNull TimeZone tz) {
        Calendar c = new GregorianCalendar(tz);
        c.clear();
        c.set(year, month - 1, day, 0, 0, 0);
        // Lenient: where midnight was skipped by DST, this lands on the first existing instant.
        return c.getTimeInMillis();
    }

    /** MaterialDatePicker returns UTC midnight of the picked date; store that date's local start. */
    public static long fromPickerSelection(long utcMidnight, @NonNull TimeZone tz) {
        Calendar u = new GregorianCalendar(UTC);
        u.setTimeInMillis(utcMidnight);
        return startOfLocalDay(u.get(Calendar.YEAR), u.get(Calendar.MONTH) + 1, u.get(Calendar.DAY_OF_MONTH), tz);
    }

    /** The picker's selection (UTC midnight) for a stored due date. */
    public static long toPickerSelection(long dueAt, @NonNull TimeZone tz) {
        return localEpochDay(dueAt, tz) * DAY_MS;
    }

    public static boolean isSameLocalYear(long a, long b, @NonNull TimeZone tz) {
        Calendar c = new GregorianCalendar(tz);
        c.setTimeInMillis(a);
        int ya = c.get(Calendar.YEAR);
        c.setTimeInMillis(b);
        return ya == c.get(Calendar.YEAR);
    }

    /** The start of the local day after the one {@code now} falls in (when Home re-buckets). */
    public static long nextLocalMidnight(long now, @NonNull TimeZone tz) {
        Calendar c = new GregorianCalendar(tz);
        c.setTimeInMillis(now);
        c.add(Calendar.DAY_OF_MONTH, 1);
        return startOfLocalDay(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), tz);
    }

    /** Days since 1970-01-01 of the local calendar date of {@code t}. */
    private static long localEpochDay(long t, @NonNull TimeZone tz) {
        Calendar c = new GregorianCalendar(tz);
        c.setTimeInMillis(t);
        Calendar u = new GregorianCalendar(UTC);
        u.clear();
        u.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        return Math.floorDiv(u.getTimeInMillis(), DAY_MS);
    }
}
