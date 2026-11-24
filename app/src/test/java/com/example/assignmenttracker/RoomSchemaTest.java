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
    }

    /**
     * The milpa (v2) adds two tables and leaves assignments alone, so v1 → v2 is a real
     * migration (AppDatabase.MIGRATION_1_2), not an edit in place: v1 has been installed.
     */
    @Test
    public void version2AddsTheMilpaTablesAndKeepsAssignments() throws Exception {
        File schema = new File(DIR, "2.json");
        assertTrue("missing " + schema.getPath(), schema.exists());
        String json = new String(Files.readAllBytes(schema.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"version\": 2"));
        for (String table : new String[]{"assignments", "farm_credits", "farm_plots"}) {
            assertTrue("table " + table, json.contains("\"tableName\": \"" + table + "\""));
        }
        assertFalse("no v3 yet", new File(DIR, "3.json").exists());
    }

    /** The hand-written migration SQL must match what Room generates, or Room won't open the DB. */
    @Test
    public void migrationSqlMatchesTheExportedSchema() throws Exception {
        String json = new String(Files.readAllBytes(new File(DIR, "2.json").toPath()), StandardCharsets.UTF_8);
        String src = new String(Files.readAllBytes(new File(
                "src/main/java/com/example/assignmenttracker/data/db/AppDatabase.java").toPath()), StandardCharsets.UTF_8)
                .replaceAll("\"\\s*\\+\\s*\"", "");
        for (String table : new String[]{"farm_credits", "farm_plots"}) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "\"tableName\": \"" + table + "\",\\s*\"createSql\": \"([^\"]+)\"").matcher(json);
            assertTrue("createSql for " + table, m.find());
            String expected = m.group(1).replace("${TABLE_NAME}", table);
            assertTrue("MIGRATION_1_2 must run: " + expected, src.contains(expected));
        }
    }
}
