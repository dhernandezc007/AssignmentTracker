package com.example.assignmenttracker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** architecture §3.2: exportSchema = true and the schema JSON lives under app/schemas/. */
public class RoomSchemaTest {

    private static final File DIR = new File("schemas/com.example.assignmenttracker.data.db.AppDatabase");

    @Test
    public void version1SchemaIsExported() throws Exception {
        File schema = new File(DIR, "1.json");
        assertTrue("missing " + schema.getPath(), schema.exists());
        String json = new String(Files.readAllBytes(schema.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"tableName\": \"assignments\""));
        for (String col : new String[]{"id", "course", "title", "notes", "dueAt", "priority",
                "status", "completedAt", "createdAt", "updatedAt"}) {
            assertTrue("column " + col, json.contains("\"columnName\": \"" + col + "\""));
        }
    }

    /**
     * Slice 2: schema v1 was edited in place (nothing was ever installed outside the emulator).
     * The free-text {@code course} replaced {@code courseId}, and there is still only v1.
     */
    @Test
    public void courseReplacedCourseIdInVersion1() throws Exception {
        String json = new String(Files.readAllBytes(new File(DIR, "1.json").toPath()), StandardCharsets.UTF_8);
        assertFalse("courseId must be gone from schema v1", json.contains("courseId"));
        assertTrue("course is nullable TEXT",
                json.contains("`course` TEXT,") || json.contains("`course` TEXT)"));
        assertFalse("still a single schema version", new File(DIR, "2.json").exists());
    }
}
