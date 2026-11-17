package com.example.assignmenttracker.util;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Due dates in the device locale (design.md §3.1): "Thu, Nov 6" / "jue, 6 nov" on rows (year
 * added outside the current year), "Thursday, November 6" / "jueves, 6 de noviembre" spoken,
 * and always with the year in the edit field. Patterns come from the platform's best pattern
 * for each skeleton, so the order and punctuation follow the locale.
 */
public final class DateFormats {

    private DateFormats() {
    }

    /** Row text: no year in the current year. */
    @NonNull
    public static String visible(long dueAt, long now, @NonNull Locale locale, @NonNull TimeZone tz) {
        return format(DueDates.isSameLocalYear(dueAt, now, tz) ? "EEEMMMd" : "EEEMMMdy", dueAt, locale, tz);
    }

    /** Row content description. */
    @NonNull
    public static String spoken(long dueAt, long now, @NonNull Locale locale, @NonNull TimeZone tz) {
        return format(DueDates.isSameLocalYear(dueAt, now, tz) ? "EEEEMMMMd" : "EEEEMMMMdy", dueAt, locale, tz);
    }

    /** Edit field: always with the year ("Thu, Nov 6, 2026" / "jue, 6 nov 2026"). */
    @NonNull
    public static String withYear(long dueAt, @NonNull Locale locale, @NonNull TimeZone tz) {
        return format("EEEMMMdy", dueAt, locale, tz);
    }

    @NonNull
    private static String format(@NonNull String skeleton, long t, @NonNull Locale locale, @NonNull TimeZone tz) {
        SimpleDateFormat f = new SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale);
        f.setTimeZone(tz);
        return f.format(new Date(t));
    }
}
