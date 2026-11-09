package com.example.assignmenttracker;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SEC-1-1: production code has no wipe-all path. Tests reset the database with
 * {@code clearAllTables()} or an in-memory database instead.
 */
public class DaoSafetyTest {

    private static final File MAIN = new File("src/main/java");
    /** A DELETE on the table with no WHERE clause. */
    private static final Pattern UNSCOPED_DELETE =
            Pattern.compile("DELETE\\s+FROM\\s+`?assignments`?\\s*(\"|$)", Pattern.CASE_INSENSITIVE);

    @Test
    public void noUnscopedDeleteInProductionCode() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (File f : javaFiles(MAIN)) {
            String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            if (UNSCOPED_DELETE.matcher(src).find()) offenders.add(f.getName() + ": DELETE without WHERE");
            if (src.contains("deleteAll(")) offenders.add(f.getName() + ": deleteAll(");
            if (src.contains("clearAllTables(")) offenders.add(f.getName() + ": clearAllTables(");
        }
        assertTrue("Wipe-all paths in src/main: " + offenders, offenders.isEmpty());
    }

    private static List<File> javaFiles(File dir) {
        List<File> out = new ArrayList<>();
        File[] kids = dir.listFiles();
        if (kids == null) return out;
        for (File k : kids) {
            if (k.isDirectory()) out.addAll(javaFiles(k));
            else if (k.getName().endsWith(".java")) out.add(k);
        }
        return out;
    }
}
