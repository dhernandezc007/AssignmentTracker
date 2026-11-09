package com.example.assignmenttracker;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** architecture §3.2: exportSchema = true and the schema JSON lives under app/schemas/. */
public class RoomSchemaTest {

    @Test
    public void version1SchemaIsExported() throws Exception {
        File schema = new File("schemas/com.example.assignmenttracker.data.db.AppDatabase/1.json");
        assertTrue("missing " + schema.getPath(), schema.exists());
        String json = new String(Files.readAllBytes(schema.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"tableName\": \"assignments\""));
        for (String col : new String[]{"id", "courseId", "title", "notes", "dueAt", "priority",
                "status", "completedAt", "createdAt", "updatedAt"}) {
            assertTrue("column " + col, json.contains("\"columnName\": \"" + col + "\""));
        }
    }
}
