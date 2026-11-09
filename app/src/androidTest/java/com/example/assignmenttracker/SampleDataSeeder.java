package com.example.assignmenttracker;

import static org.junit.Assume.assumeTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.util.DueDates;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Not a test: fills the debug database with design.md §12's fictional sample data (dates
 * relative to today) for screenshots. Skipped unless asked for:
 * <pre>adb shell am instrument -w -e seed en|es|empty|caughtup
 *   -e class com.example.assignmenttracker.SampleDataSeeder
 *   com.dhernandez.assignmenttracker.test/androidx.test.runner.AndroidJUnitRunner</pre>
 */
@RunWith(AndroidJUnit4.class)
public class SampleDataSeeder {

    @Test
    public void seed() {
        String mode = InstrumentationRegistry.getArguments().getString("seed");
        assumeTrue("only runs with -e seed", mode != null);
        TestDb.clear();
        if ("empty".equals(mode)) return;
        boolean es = "es".equals(mode);
        if (!"caughtup".equals(mode)) {
            add("1", es ? "Informe de laboratorio: enzimas" : "Lab report: enzyme activity", es ? "Biología 101" : "Biology 101", -2);
            add("2", es ? "Respuesta de lectura, cap. 4" : "Reading response, ch. 4", es ? "Inglés 102" : "English 102", -1);
            add("3", es ? "Serie de problemas 7" : "Calc problem set 7", es ? "Matemáticas 151" : "Math 151", 0);
            add("4", es ? "Foro: la Reconstrucción" : "Discussion post: Reconstruction", es ? "Historia 110" : "History 110", 0);
            add("5", es ? "Esquema del ensayo" : "Essay outline", es ? "Inglés 102" : "English 102", 1);
            add("6", es ? "Guía para el examen 3" : "Quiz 3 study guide", es ? "Química 110" : "Chemistry 110", 3);
            add("7", es ? "Diapositivas de la presentación" : "Group presentation slides", es ? "Historia 110" : "History 110", 16);
            add("8", es ? "Preguntar a la Prof. Ruiz por puntos extra" : "Ask Prof. Ruiz about extra credit", null, null);
        }
        for (int i = 0; i < 3; i++) {
            Assignment d = Assignment.newTodo("d" + i, (es ? "Tarea hecha " : "Finished assignment ") + (i + 1), 100 + i);
            d.status = Assignment.STATUS_DONE;
            d.completedAt = 1000L + i;
            TestDb.insert(d);
        }
    }

    private static void add(String id, String title, String course, Integer dayOffset) {
        Assignment a = Assignment.newTodo(id, title, Long.parseLong(id));
        a.course = course;
        if (dayOffset != null) {
            TimeZone tz = TimeZone.getDefault();
            Calendar c = Calendar.getInstance(tz);
            c.add(Calendar.DAY_OF_MONTH, dayOffset);
            a.dueAt = DueDates.startOfLocalDay(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), tz);
        }
        TestDb.insert(a);
    }
}
