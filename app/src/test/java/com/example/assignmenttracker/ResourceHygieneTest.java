package com.example.assignmenttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Slice 0 guard rails, run as plain JVM tests (working dir = the app module):
 * every UI string is a resource, every English key has a Spanish twin,
 * and the theme is light-only for v1.
 */
public class ResourceHygieneTest {

    private static final File RES = new File("src/main/res");
    private static final File JAVA = new File("src/main/java");
    private static final List<String> TEXT_ATTRS =
            Arrays.asList("android:text", "android:hint", "android:contentDescription");

    @Test
    public void appNameIsAssignmentTracker() throws Exception {
        assertEquals("Assignment Tracker", strings(new File(RES, "values/strings.xml"), true).get("app_name"));
    }

    @Test
    public void everyTranslatableStringHasASpanishEntry() throws Exception {
        Map<String, String> en = strings(new File(RES, "values/strings.xml"), false);
        Map<String, String> es = strings(new File(RES, "values-es/strings.xml"), false);
        assertEquals("values-es keys must match values keys", en.keySet(), es.keySet());
        for (Map.Entry<String, String> e : es.entrySet()) {
            assertFalse("Spanish entry is empty: " + e.getKey(), e.getValue().trim().isEmpty());
        }
    }

    @Test
    public void layoutsHaveNoHardcodedText() throws Exception {
        List<String> offenders = new ArrayList<>();
        File[] layouts = new File(RES, "layout").listFiles((d, n) -> n.endsWith(".xml"));
        assertTrue(layouts != null && layouts.length > 0);
        for (File layout : layouts) {
            collectLiteralText(parse(layout).getDocumentElement(), layout.getName(), offenders);
        }
        assertTrue("Hardcoded text in layouts: " + offenders, offenders.isEmpty());
    }

    @Test
    public void javaHasNoHardcodedUiStrings() throws Exception {
        // String literals passed to setError / setText / Toast.makeText.
        Pattern p = Pattern.compile("(setError|setText|makeText\\([^,]+,)\\s*\\(?\\s*\"");
        List<String> offenders = new ArrayList<>();
        for (File f : javaFiles(JAVA)) {
            String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Matcher m = p.matcher(src);
            while (m.find()) offenders.add(f.getName() + ": " + m.group());
        }
        assertTrue("Hardcoded UI strings in Java: " + offenders, offenders.isEmpty());
    }

    @Test
    public void themeIsLightOnly() throws Exception {
        assertFalse("values-night theme must not exist in v1 (light only)",
                new File(RES, "values-night/themes.xml").exists());
        NodeList styles = parse(new File(RES, "values/themes.xml")).getElementsByTagName("style");
        boolean found = false;
        for (int i = 0; i < styles.getLength(); i++) {
            Element s = (Element) styles.item(i);
            if (s.getAttribute("name").equals("Base.Theme.AssignmentTracker")) {
                found = true;
                assertTrue("Base theme must extend a Light theme",
                        s.getAttribute("parent").contains(".Light"));
            }
        }
        assertTrue(found);
    }

    /** design.md §7.1: every role is mapped, so no stock M3 purple/grey leaks into widgets. */
    @Test
    public void themeMapsPaletteToMaterialAttributes() throws Exception {
        Map<String, String> items = themeItems();
        String[][] expected = {
                {"colorPrimary", "@color/mx_pink"},
                {"colorOnPrimary", "@color/mx_white"},
                {"colorPrimaryContainer", "@color/mx_pink_container"},
                {"colorOnPrimaryContainer", "@color/mx_on_pink_container"},
                {"colorSecondary", "@color/mx_green"},
                {"colorOnSecondary", "@color/mx_white"},
                {"colorSecondaryContainer", "@color/mx_green_container"},
                {"colorOnSecondaryContainer", "@color/mx_on_green_container"},
                {"colorTertiary", "@color/mx_marigold"},
                {"colorOnTertiary", "@color/mx_brown"},
                {"colorError", "@color/mx_red"},
                {"colorOnError", "@color/mx_white"},
                {"colorErrorContainer", "@color/mx_red_container"},
                {"colorOnErrorContainer", "@color/mx_on_red_container"},
                {"android:colorBackground", "@color/mx_cream"},
                {"colorOnBackground", "@color/mx_brown"},
                {"colorSurface", "@color/mx_white"},
                {"colorOnSurface", "@color/mx_brown"},
                {"colorOnSurfaceVariant", "@color/mx_brown_muted"},
                {"colorSurfaceContainerLow", "@color/mx_surface_high"},
                {"colorSurfaceContainerHigh", "@color/mx_surface_high"},
                {"colorSurfaceContainerHighest", "@color/mx_surface_high"},
                {"colorOutline", "@color/mx_brown_muted"},
                {"colorOutlineVariant", "@color/mx_rule"},
                {"colorSurfaceInverse", "@color/mx_brown"},
                {"colorOnSurfaceInverse", "@color/mx_cream"},
                {"colorPrimaryInverse", "@color/mx_yellow"},
        };
        for (String[] e : expected) {
            assertEquals("themes.xml " + e[0], e[1], items.get(e[0]));
        }
    }

    @Test
    public void designTokensExistWithSpecHex() throws Exception {
        Map<String, String> colors = new TreeMap<>();
        NodeList nodes = parse(new File(RES, "values/colors.xml")).getElementsByTagName("color");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element c = (Element) nodes.item(i);
            colors.put(c.getAttribute("name"), c.getTextContent().trim().toUpperCase());
        }
        String[][] expected = {
                {"mx_pink_deep", "#B0005E"}, {"mx_brown_muted", "#6E5240"}, {"mx_rule", "#EADBC4"},
                {"mx_white", "#FFFFFF"}, {"mx_surface_high", "#FFFBF3"},
                {"mx_pink_container", "#FFD8E9"}, {"mx_on_pink_container", "#5C0030"},
                {"mx_green_container", "#CDEBDD"}, {"mx_on_green_container", "#002114"},
                {"mx_red_container", "#FFDAD9"}, {"mx_on_red_container", "#410007"},
        };
        for (String[] e : expected) {
            assertEquals("colors.xml " + e[0], e[1], colors.get(e[0]));
        }
    }

    private static Map<String, String> themeItems() throws Exception {
        Map<String, String> out = new TreeMap<>();
        NodeList styles = parse(new File(RES, "values/themes.xml")).getElementsByTagName("style");
        for (int i = 0; i < styles.getLength(); i++) {
            Element s = (Element) styles.item(i);
            if (!s.getAttribute("name").equals("Base.Theme.AssignmentTracker")) continue;
            NodeList items = s.getElementsByTagName("item");
            for (int j = 0; j < items.getLength(); j++) {
                Element it = (Element) items.item(j);
                out.put(it.getAttribute("name"), it.getTextContent().trim());
            }
        }
        return out;
    }

    // ---- helpers ----

    private static void collectLiteralText(Element el, String file, List<String> out) {
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node a = attrs.item(i);
            if (TEXT_ATTRS.contains(a.getNodeName())) {
                String v = a.getNodeValue();
                if (!v.startsWith("@") && !v.startsWith("?")) out.add(file + " " + a.getNodeName() + "=" + v);
            }
        }
        NodeList kids = el.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            if (kids.item(i) instanceof Element) collectLiteralText((Element) kids.item(i), file, out);
        }
    }

    private static Map<String, String> strings(File f, boolean includeUntranslatable) throws Exception {
        assertTrue("missing " + f, f.exists());
        Map<String, String> out = new TreeMap<>();
        NodeList nodes = parse(f).getElementsByTagName("string");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element s = (Element) nodes.item(i);
            if (includeUntranslatable || !"false".equals(s.getAttribute("translatable"))) {
                out.put(s.getAttribute("name"), s.getTextContent());
            }
        }
        return out;
    }

    private static Document parse(File f) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
    }

    private static List<File> javaFiles(File dir) throws IOException {
        List<File> out = new ArrayList<>();
        TreeSet<File> stack = new TreeSet<>(Arrays.asList(dir));
        while (!stack.isEmpty()) {
            File d = stack.pollFirst();
            File[] kids = d.listFiles();
            if (kids == null) continue;
            for (File k : kids) {
                if (k.isDirectory()) stack.add(k);
                else if (k.getName().endsWith(".java")) out.add(k);
            }
        }
        return out;
    }
}
