package com.example.assignmenttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * SEC-0-1 (JVM half): the fake Login/SignUp screens must never be on the launch path.
 * The instrumented {@code LaunchPathSecurityTest} checks the same thing on the merged,
 * installed manifest.
 */
public class LaunchPathManifestTest {

    private static final String A = "http://schemas.android.com/apk/res/android";
    private static final File MANIFEST = new File("src/main/AndroidManifest.xml");

    @Test
    public void mainActivityIsTheOnlyLauncher() throws Exception {
        List<String> launchers = new ArrayList<>();
        for (Element act : activities()) {
            NodeList cats = act.getElementsByTagName("category");
            for (int i = 0; i < cats.getLength(); i++) {
                if ("android.intent.category.LAUNCHER".equals(((Element) cats.item(i)).getAttributeNS(A, "name"))) {
                    launchers.add(act.getAttributeNS(A, "name"));
                }
            }
        }
        assertEquals("exactly one launcher, MainActivity", List.of(".MainActivity"), launchers);
    }

    @Test
    public void loginAndSignUpAreNotExportedAndHaveNoIntentFilter() throws Exception {
        int seen = 0;
        for (Element act : activities()) {
            String name = act.getAttributeNS(A, "name");
            if (name.equals(".LoginActivity") || name.equals(".SignUpActivity")) {
                seen++;
                assertEquals(name + " must be exported=false", "false", act.getAttributeNS(A, "exported"));
                assertEquals(name + " must have no intent-filter", 0, act.getElementsByTagName("intent-filter").getLength());
            }
        }
        assertEquals("Login and SignUp stay declared (kept in source)", 2, seen);
    }

    @Test
    public void onlyMainActivityIsExported() throws Exception {
        for (Element act : activities()) {
            if ("true".equals(act.getAttributeNS(A, "exported"))) {
                assertEquals(".MainActivity", act.getAttributeNS(A, "name"));
            }
        }
    }

    @Test
    public void releaseBuildDisablesTheAuthPlaceholders() throws Exception {
        String gradle = new String(Files.readAllBytes(new File("build.gradle.kts").toPath()), StandardCharsets.UTF_8);
        Matcher release = Pattern.compile("release\\s*\\{(.*?)optimization", Pattern.DOTALL).matcher(gradle);
        assertTrue("release block not found", release.find());
        assertTrue("release must set AUTH_ENABLED=false",
                release.group(1).contains("buildConfigField(\"boolean\", \"AUTH_ENABLED\", \"false\")"));
        // Both screens bail out when the flag is off.
        for (String f : new String[]{"LoginActivity.java", "SignUpActivity.java"}) {
            String src = new String(Files.readAllBytes(
                    new File("src/main/java/com/example/assignmenttracker/" + f).toPath()), StandardCharsets.UTF_8);
            assertTrue(f + " must finish() when AUTH_ENABLED is false",
                    src.contains("if (!BuildConfig.AUTH_ENABLED)"));
            assertTrue(f + " needs the SEC-0-1 marker", src.contains("SECURITY: placeholder, not authentication. See SEC-0-1"));
        }
    }

    @Test
    public void noUiPathStartsLoginOrSignUp() throws Exception {
        for (File f : new File("src/main/java/com/example/assignmenttracker").listFiles((d, n) -> n.endsWith(".java"))) {
            if (f.getName().equals("LoginActivity.java") || f.getName().equals("SignUpActivity.java")) continue;
            String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertFalse(f.getName() + " references LoginActivity", src.contains("LoginActivity"));
            assertFalse(f.getName() + " references SignUpActivity", src.contains("SignUpActivity"));
        }
    }

    private static List<Element> activities() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        NodeList nodes = f.newDocumentBuilder().parse(MANIFEST).getElementsByTagName("activity");
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) out.add((Element) nodes.item(i));
        assertFalse(out.isEmpty());
        return out;
    }
}
