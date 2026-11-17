package com.example.assignmenttracker.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Slice 4 bucket math (tasks.md, design.md §3.1): buckets use the device's local date, "this
 * week" is the next 7 days, and the clock and zone are injected so the day boundaries are exact.
 */
public class DueDatesTest {

    private static final TimeZone CHICAGO = TimeZone.getTimeZone("America/Chicago");
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** Local wall-clock time in {@code tz}, as epoch millis. Month is 1-based. */
    private static long at(TimeZone tz, int y, int mo, int d, int h, int mi, int s, int ms) {
        Calendar c = new GregorianCalendar(tz);
        c.clear();
        c.set(y, mo - 1, d, h, mi, s);
        c.set(Calendar.MILLISECOND, ms);
        return c.getTimeInMillis();
    }

    private static long day(TimeZone tz, int y, int mo, int d) {
        return DueDates.startOfLocalDay(y, mo, d, tz);
    }

    // ---- buckets ----

    @Test
    public void noDateIsNoDate() {
        assertEquals(DueBucket.NO_DATE, DueDates.bucketOf(null, at(CHICAGO, 2026, 11, 8, 12, 0, 0, 0), CHICAGO));
    }

    @Test
    public void bucketsAroundTodayFollowTheLocalDate() {
        long now = at(CHICAGO, 2026, 11, 8, 12, 0, 0, 0); // Sunday noon
        assertEquals(DueBucket.OVERDUE, DueDates.bucketOf(day(CHICAGO, 2026, 11, 7), now, CHICAGO));
        assertEquals(DueBucket.OVERDUE, DueDates.bucketOf(day(CHICAGO, 2025, 1, 1), now, CHICAGO));
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(day(CHICAGO, 2026, 11, 8), now, CHICAGO));
        assertEquals("tomorrow is this week", DueBucket.THIS_WEEK, DueDates.bucketOf(day(CHICAGO, 2026, 11, 9), now, CHICAGO));
        assertEquals("today + 7 is the last day of this week", DueBucket.THIS_WEEK,
                DueDates.bucketOf(day(CHICAGO, 2026, 11, 15), now, CHICAGO));
        assertEquals("today + 8 is later", DueBucket.LATER, DueDates.bucketOf(day(CHICAGO, 2026, 11, 16), now, CHICAGO));
    }

    /** The midnight edge: one millisecond either side of local midnight flips the bucket. */
    @Test
    public void localMidnightIsTheBoundary() {
        long due = day(CHICAGO, 2026, 11, 9);
        long lastMsOfSunday = at(CHICAGO, 2026, 11, 8, 23, 59, 59, 999);
        long firstMsOfMonday = at(CHICAGO, 2026, 11, 9, 0, 0, 0, 0);
        long lastMsOfMonday = at(CHICAGO, 2026, 11, 9, 23, 59, 59, 999);
        long firstMsOfTuesday = at(CHICAGO, 2026, 11, 10, 0, 0, 0, 0);
        assertEquals(1, DueDates.daysFromToday(due, lastMsOfSunday, CHICAGO));
        assertEquals(DueBucket.THIS_WEEK, DueDates.bucketOf(due, lastMsOfSunday, CHICAGO));
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(due, firstMsOfMonday, CHICAGO));
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(due, lastMsOfMonday, CHICAGO));
        assertEquals(DueBucket.OVERDUE, DueDates.bucketOf(due, firstMsOfTuesday, CHICAGO));
    }

    /**
     * Time zone: the same instant is a different local day in Chicago and Tokyo, and the bucket
     * follows the device's zone, not UTC.
     */
    @Test
    public void theDeviceZoneDecidesWhatTodayIs() {
        // 2026-11-09 03:00 UTC = Nov 8, 21:00 in Chicago = Nov 9, 12:00 in Tokyo.
        long now = at(UTC, 2026, 11, 9, 3, 0, 0, 0);
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(day(CHICAGO, 2026, 11, 8), now, CHICAGO));
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(day(TOKYO, 2026, 11, 9), now, TOKYO));
        assertEquals(DueBucket.THIS_WEEK, DueDates.bucketOf(day(CHICAGO, 2026, 11, 9), now, CHICAGO));
        assertEquals(DueBucket.OVERDUE, DueDates.bucketOf(day(TOKYO, 2026, 11, 8), now, TOKYO));
    }

    /** DST: Nov 1 2026 is a 25-hour day in Chicago; counting days must not use 24h steps. */
    @Test
    public void daylightSavingDaysCountAsOneDay() {
        long now = at(CHICAGO, 2026, 10, 31, 23, 30, 0, 0);
        assertEquals(1, DueDates.daysFromToday(day(CHICAGO, 2026, 11, 1), now, CHICAGO));
        assertEquals(2, DueDates.daysFromToday(day(CHICAGO, 2026, 11, 2), now, CHICAGO));
        long lateOnTheLongDay = at(CHICAGO, 2026, 11, 1, 23, 30, 0, 0);
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(day(CHICAGO, 2026, 11, 1), lateOnTheLongDay, CHICAGO));
        long spring = at(CHICAGO, 2026, 3, 8, 23, 59, 0, 0); // 23-hour day
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(day(CHICAGO, 2026, 3, 8), spring, CHICAGO));
        assertEquals(7, DueDates.daysFromToday(day(CHICAGO, 2026, 3, 15), spring, CHICAGO));
    }

    /** A zone where local midnight was skipped (São Paulo, 2018-11-04) still gets one start of day. */
    @Test
    public void aSkippedMidnightStillMapsToThatDay() {
        TimeZone sp = TimeZone.getTimeZone("America/Sao_Paulo");
        long start = DueDates.startOfLocalDay(2018, 11, 4, sp);
        long now = at(sp, 2018, 11, 4, 12, 0, 0, 0);
        assertEquals(DueBucket.TODAY, DueDates.bucketOf(start, now, sp));
        assertEquals(1, DueDates.daysFromToday(DueDates.startOfLocalDay(2018, 11, 5, sp), now, sp));
    }

    // ---- date picker conversions (MaterialDatePicker works in UTC midnights) ----

    @Test
    public void aPickedDateIsStoredAsTheLocalStartOfThatDay() {
        long pickedNov9 = at(UTC, 2026, 11, 9, 0, 0, 0, 0);
        assertEquals(day(CHICAGO, 2026, 11, 9), DueDates.fromPickerSelection(pickedNov9, CHICAGO));
        assertEquals(day(TOKYO, 2026, 11, 9), DueDates.fromPickerSelection(pickedNov9, TOKYO));
    }

    @Test
    public void aStoredDateGoesBackToThePickerAsTheSameDay() {
        long pickedNov9 = at(UTC, 2026, 11, 9, 0, 0, 0, 0);
        for (TimeZone tz : new TimeZone[]{CHICAGO, TOKYO, UTC}) {
            assertEquals(tz.getID(), pickedNov9, DueDates.toPickerSelection(DueDates.fromPickerSelection(pickedNov9, tz), tz));
        }
    }

    @Test
    public void sameYearIsLocal() {
        long now = at(CHICAGO, 2026, 12, 31, 20, 0, 0, 0);
        assertTrue(DueDates.isSameLocalYear(day(CHICAGO, 2026, 1, 1), now, CHICAGO));
        assertFalse(DueDates.isSameLocalYear(day(CHICAGO, 2027, 1, 5), now, CHICAGO));
    }

    @Test
    public void nextMidnightIsTheStartOfTomorrow() {
        long now = at(CHICAGO, 2026, 11, 8, 21, 15, 0, 0);
        assertEquals(day(CHICAGO, 2026, 11, 9), DueDates.nextLocalMidnight(now, CHICAGO));
        long exactlyMidnight = day(CHICAGO, 2026, 11, 9);
        assertEquals(day(CHICAGO, 2026, 11, 10), DueDates.nextLocalMidnight(exactlyMidnight, CHICAGO));
    }
}
