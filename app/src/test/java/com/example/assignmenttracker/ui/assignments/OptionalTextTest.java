package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Course and notes (Slice 2): optional, trimmed, blank means "not set", length-capped. */
public class OptionalTextTest {

    @Test
    public void nullEmptyAndBlankAreNull() {
        assertNull(OptionalText.normalize(null, 10));
        assertNull(OptionalText.normalize("", 10));
        assertNull(OptionalText.normalize(" \t\n", 10));
    }

    @Test
    public void textIsTrimmed() {
        assertEquals("Biology 101", OptionalText.normalize("  Biology 101 ", 100));
    }

    @Test
    public void innerLineBreaksAreKept() {
        assertEquals("a\nb", OptionalText.normalize("\na\nb\n", 100));
    }

    @Test
    public void textIsCappedAtMax() {
        assertEquals("abcde", OptionalText.normalize("abcdefgh", 5));
    }

    /** design.md §5.2: notes are trimmed at the end only, so a leading indent stays. */
    @Test
    public void trailingTrimKeepsLeadingWhitespace() {
        assertEquals("  - step one\n  - step two", OptionalText.normalizeTrailing("  - step one\n  - step two \n\n", 2000));
        assertNull(OptionalText.normalizeTrailing(" \n ", 2000));
        assertNull(OptionalText.normalizeTrailing(null, 2000));
        assertEquals("abc", OptionalText.normalizeTrailing("abcdef", 3));
    }

    /** Never cut a surrogate pair in half (an emoji at the cap). */
    @Test
    public void capDoesNotSplitASurrogatePair() {
        String s = "abcd🌼"; // "abcd" + 🌼 (2 chars)
        assertEquals("abcd", OptionalText.normalize(s, 5));
    }
}
