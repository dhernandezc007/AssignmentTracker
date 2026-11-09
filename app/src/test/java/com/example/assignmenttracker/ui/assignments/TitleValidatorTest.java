package com.example.assignmenttracker.ui.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TitleValidatorTest {

    @Test
    public void nullEmptyAndBlankAreRejected() {
        assertNull(TitleValidator.normalize(null));
        assertNull(TitleValidator.normalize(""));
        assertNull(TitleValidator.normalize("   \t\n "));
    }

    @Test
    public void validTitleIsTrimmed() {
        assertEquals("Essay outline", TitleValidator.normalize("  Essay outline \n"));
    }

    @Test
    public void maxLengthIsTwoHundred() {
        String ok = repeat('a', TitleValidator.MAX_LENGTH);
        assertEquals(ok, TitleValidator.normalize(ok));
        assertNull(TitleValidator.normalize(ok + "b"));
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
