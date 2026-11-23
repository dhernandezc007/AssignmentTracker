package com.example.assignmenttracker.ui.buddy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.assignmenttracker.ui.assignments.HomeItems;

import org.junit.Test;

import java.util.Collections;
import java.util.Random;
import java.util.TimeZone;

public class StudyBuddyTest {

    @Test
    public void whoDefaultsToAbuelitaAndSwitches() {
        assertEquals(StudyBuddy.Who.ABUELITA, StudyBuddy.Who.parse(null));
        assertEquals(StudyBuddy.Who.ABUELITA, StudyBuddy.Who.parse("nonsense"));
        assertEquals(StudyBuddy.Who.ABUELO, StudyBuddy.Who.parse("ABUELO"));
        assertEquals(StudyBuddy.Who.ABUELO, StudyBuddy.Who.ABUELITA.other());
        assertEquals(StudyBuddy.Who.ABUELITA, StudyBuddy.Who.ABUELO.other());
    }

    @Test
    public void noAssignmentsIsTheEmptyMood() {
        assertEquals(StudyBuddy.Mood.EMPTY, StudyBuddy.moodFor(null));
        assertEquals(StudyBuddy.Mood.EMPTY, StudyBuddy.moodFor(Collections.emptyList()));
        assertEquals(StudyBuddy.Mood.EMPTY, StudyBuddy.moodFor(
                HomeItems.build(Collections.emptyList(), false, 0L, TimeZone.getTimeZone("UTC"))));
    }

    @Test
    public void pickNeverRepeatsTheLastLine() {
        Random r = new Random(42);
        for (int avoid = 0; avoid < 5; avoid++) {
            for (int i = 0; i < 200; i++) {
                int p = StudyBuddy.pick(5, avoid, r);
                assertNotEquals(avoid, p);
                assertTrue(p >= 0 && p < 5);
            }
        }
        assertEquals(0, StudyBuddy.pick(1, 0, r));
    }
}
