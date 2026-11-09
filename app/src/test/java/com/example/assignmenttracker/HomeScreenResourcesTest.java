package com.example.assignmenttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

/** Slice 4 resource rules (design.md §2.1, §3.1–§3.5, §4, §5.2, §8.1, §10, §11, §14). */
public class HomeScreenResourcesTest {

    private static final String A = "http://schemas.android.com/apk/res/android";
    private static final String APP = "http://schemas.android.com/apk/res-auto";
    private static final File RES = new File("src/main/res");
    /**
     * SHA-256 (LF line endings) of the designer's Direction A files, so the check runs on any
     * machine without the design folder and never skips (SEC-4-2). Update together with the drawable.
     */
    private static final String[][] DESIGN_SHA256 = {
            {"papel_picado_wide.xml", "51d5f6638caa09e2c88213b4111f878e8131601f7e9a88a0eb080c809747fa9d"},
            {"art_flags.xml", "c059058bb5865fa9d04e33227932b02f239a59751344ef20688b26265badaed5"},
    };

    // ---- §2.1 Direction A header ----

    @Test
    public void bannerIs58dpAndTheEmptyArtIs186dp() throws Exception {
        assertEquals("58dp", dimens().get("banner_height"));
        assertEquals("186dp", dimens().get("empty_art_width"));
        assertEquals("120dp", dimens().get("caught_up_art_width"));
    }

    /** The two ready-made drawables are the design files byte for byte (line endings aside). */
    @Test
    public void directionADrawablesAreTheDesignFiles() throws Exception {
        for (String[] f : DESIGN_SHA256) {
            String app = read(new File(RES, "drawable/" + f[0]));
            assertTrue(f[0] + " is the cut-out version (evenOdd)", app.contains("android:fillType=\"evenOdd\""));
            assertEquals(f[0] + " matches the design file", f[1],
                    sha256(app.replace("\r\n", "\n").replace('\r', '\n')));
        }
        Element wide = parse(new File(RES, "drawable/papel_picado_wide.xml")).getDocumentElement();
        assertEquals("58dp", wide.getAttributeNS(A, "height"));
        assertFalse("red belongs to overdue only (§2.1)", read(new File(RES, "drawable/papel_picado_wide.xml")).contains("mx_red"));
    }

    @Test
    public void bannerNeverStretches() throws Exception {
        Element banner = byId(parse(new File(RES, "layout/activity_main.xml")), "banner");
        assertEquals("matrix", banner.getAttributeNS(A, "scaleType"));
        assertEquals("@dimen/banner_height", banner.getAttributeNS(A, "layout_height"));
    }

    // ---- §3.1 row (QA-3-1 / DES-3-4) ----

    /** The checkbox is a sibling of the focusable row body, under a root that is not focusable. */
    @Test
    public void checkboxIsASiblingOfTheFocusableRowBody() throws Exception {
        Document d = parse(new File(RES, "layout/item_assignment.xml"));
        Element root = d.getDocumentElement();
        assertFalse("root is not focusable", "true".equals(root.getAttributeNS(A, "focusable")));
        Element body = byId(d, "rowBody");
        Element check = byId(d, "rowCheck");
        assertEquals("true", body.getAttributeNS(A, "focusable"));
        assertEquals("true", body.getAttributeNS(A, "clickable"));
        assertEquals("?attr/selectableItemBackground", body.getAttributeNS(A, "foreground"));
        assertEquals("siblings", check.getParentNode(), body.getParentNode());
        assertTrue("checkbox comes first in traversal", precedes(check, body));
        assertFalse("no traversalBefore with a shared id", read(new File(RES, "layout/item_assignment.xml"))
                .contains("accessibilityTraversal"));
    }

    /** DES-4-4: balanced title breaks. DES-4-1: the overdue icon is a span, not a compound drawable. */
    @Test
    public void rowTitleBreaksBalancedAndMetaHasNoCompoundDrawable() throws Exception {
        Document d = parse(new File(RES, "layout/item_assignment.xml"));
        Element title = byId(d, "rowTitle");
        assertEquals("balanced", title.getAttributeNS(A, "breakStrategy"));
        assertEquals("2", title.getAttributeNS(A, "maxLines"));
        assertEquals("end", title.getAttributeNS(A, "ellipsize"));
        Element meta = byId(d, "rowMeta");
        for (String attr : new String[]{"drawableStart", "drawableLeft", "drawablePadding"}) {
            assertEquals("rowMeta has no " + attr, "", meta.getAttributeNS(A, attr));
        }
        assertEquals("4dp", dimens().get("overdue_icon_gap"));
    }

    @Test
    public void overdueIconExists() throws Exception {
        Element v = parse(new File(RES, "drawable/ic_overdue.xml")).getDocumentElement();
        assertEquals("16dp", v.getAttributeNS(A, "width"));
        assertTrue(new File(RES, "drawable/ic_event.xml").exists());
    }

    // ---- §3.2 section header (DES-3-5) ----

    @Test
    public void sectionHeaderTopAlignsTheSwatchAndIsAHeading() throws Exception {
        for (String f : new String[]{"layout/item_section_header.xml", "layout/item_done_header.xml"}) {
            String xml = read(new File(RES, f));
            assertFalse(f + ": no raw mx_* colors", xml.contains("@color/mx_"));
            Document d = parse(new File(RES, f));
            assertEquals(f, "true", d.getDocumentElement().getAttributeNS(A, "accessibilityHeading"));
            if (f.contains("section")) {
                // DES-4-2 / QA-4-3: not a keyboard stop, still visited by TalkBack.
                assertEquals(f, "false", d.getDocumentElement().getAttributeNS(A, "focusable"));
                assertEquals(f, "true", d.getDocumentElement().getAttributeNS(A, "screenReaderFocusable"));
            }
            Element swatch = null;
            NodeList all = d.getElementsByTagName("ImageView");
            for (int i = 0; i < all.getLength(); i++) {
                Element e = (Element) all.item(i);
                if ("@drawable/serape_swatch".equals(e.getAttributeNS(A, "src"))) swatch = e;
            }
            assertNotNull(f, swatch);
            assertTrue(f + ": swatch pinned to the label's top", swatch.getAttributeNS(APP, "layout_constraintTop_toTopOf").endsWith("Label"));
        }
    }

    // ---- §4 snackbar (DES-3-2) ----

    @Test
    public void snackbarHas12dpCorners() throws Exception {
        String themes = read(new File(RES, "values/themes.xml"));
        String styles = read(new File(RES, "values/styles.xml"));
        assertTrue(themes.contains("<item name=\"snackbarStyle\">@style/Widget.AT.Snackbar</item>"));
        String sb = style(styles, "Widget.AT.Snackbar");
        assertTrue(sb.contains("parent=\"Widget.Material3.Snackbar\""));
        assertTrue(sb.contains("<item name=\"shapeAppearance\">@style/ShapeAppearance.AT.Card</item>"));
        String shape = style(styles, "ShapeAppearance.AT.Card");
        assertTrue(shape.contains("<item name=\"cornerSize\">@dimen/radius_card</item>"));
        assertEquals("12dp", dimens().get("radius_card"));
    }

    // ---- §8.1 rule 5: date picker in pink-deep ----

    @Test
    public void datePickerOverlayUsesPinkDeep() throws Exception {
        String s = style(read(new File(RES, "values/styles.xml")), "ThemeOverlay.AT.DatePicker");
        assertTrue(s.contains("parent=\"ThemeOverlay.Material3.MaterialCalendar\""));
        assertTrue(s.contains("<item name=\"colorPrimary\">@color/mx_pink_deep</item>"));
        assertTrue(s.contains("<item name=\"colorOnPrimary\">@color/mx_white</item>"));
    }

    // ---- §5.2 due field ----

    @Test
    public void editScreenHasAReadOnlyDueFieldBetweenCourseAndNotes() throws Exception {
        Document d = parse(new File(RES, "layout/activity_assignment_edit.xml"));
        Element til = byId(d, "tilDue");
        Element et = byId(d, "etDue");
        assertEquals("@string/field_due", til.getAttributeNS(A, "hint"));
        assertEquals("@drawable/ic_event", til.getAttributeNS(APP, "startIconDrawable"));
        assertEquals("custom", til.getAttributeNS(APP, "endIconMode"));
        assertEquals("@drawable/ic_close", til.getAttributeNS(APP, "endIconDrawable"));
        assertEquals("@string/field_due_clear_cd", til.getAttributeNS(APP, "endIconContentDescription"));
        assertEquals("never focused by touch (no cursor, no IME)", "false", et.getAttributeNS(A, "focusableInTouchMode"));
        assertEquals("none", et.getAttributeNS(A, "inputType"));
        assertEquals("false", et.getAttributeNS(A, "cursorVisible"));
        assertTrue(precedes(byId(d, "tilCourse"), til));
        assertTrue(precedes(til, byId(d, "tilNotes")));
        assertEquals("Course skips the read-only field", "@id/etNotes", byId(d, "etCourse").getAttributeNS(A, "nextFocusForward"));
    }

    // ---- §11 copy ----

    @Test
    public void slice4CopyMatchesDesign() throws Exception {
        Map<String, String> en = strings(new File(RES, "values/strings.xml"));
        Map<String, String> es = strings(new File(RES, "values-es/strings.xml"));
        String[][] want = {
                {"summary_separator", " · ", " · "},
                {"meta_separator", " · ", " · "},
                {"summary_separator_spoken", ", ", ", "},
                {"caught_up_title", "All caught up", "Todo al día"},
                {"section_overdue", "Overdue", "Vencidas"},
                {"section_today", "Today", "Hoy"},
                {"section_this_week", "This week", "Esta semana"},
                {"section_later", "Later", "Más adelante"},
                {"section_no_date", "No date", "Sin fecha"},
                {"due_overdue", "Overdue · %1$s", "Vencida · %1$s"},
                {"due_overdue_spoken", "Overdue, %1$s", "Vencida, %1$s"},
                {"due_today", "Due today", "Vence hoy"},
                {"due_tomorrow", "Due tomorrow", "Vence mañana"},
                {"field_due", "Due date", "Fecha de entrega"},
                {"field_due_empty", "No due date", "Sin fecha de entrega"},
                {"field_due_clear_cd", "Clear due date", "Quitar fecha de entrega"},
                {"date_picker_title", "Pick a due date", "Elige la fecha de entrega"},
                {"empty_body", "Tap Add assignment to write down your first one. A due date is optional.",
                        "Toca Agregar tarea para anotar la primera. La fecha de entrega es opcional."},
        };
        for (String[] w : want) {
            assertEquals("EN " + w[0], w[1], en.get(w[0]));
            assertEquals("ES " + w[0], w[2], es.get(w[0]));
        }
    }

    @Test
    public void plurals() throws Exception {
        Map<String, Map<String, String>> en = plurals(new File(RES, "values/strings.xml"));
        Map<String, Map<String, String>> es = plurals(new File(RES, "values-es/strings.xml"));
        String[][] want = {
                {"summary_overdue", "%d overdue", "%d overdue", "%d vencida", "%d vencidas"},
                {"summary_due_today", "%d due today", "%d due today", "%d vence hoy", "%d vencen hoy"},
                {"summary_due_week", "%d due this week", "%d due this week", "%d vence esta semana", "%d vencen esta semana"},
                {"summary_nothing_this_week", "Nothing due this week ·\u00A0%d open", "Nothing due this week ·\u00A0%d open",
                        "Nada vence esta semana ·\u00A0%d pendiente", "Nada vence esta semana ·\u00A0%d pendientes"},
                {"caught_up_body", "Nothing open. %d done.", "Nothing open. %d done.",
                        "Nada pendiente. %d hecha.", "Nada pendiente. %d hechas."},
                {"section_header_cd", "%1$s, %2$d assignment", "%1$s, %2$d assignments",
                        "%1$s, %2$d tarea", "%1$s, %2$d tareas"},
        };
        for (String[] w : want) {
            assertEquals("EN one " + w[0], w[1], en.get(w[0]).get("one"));
            assertEquals("EN other " + w[0], w[2], en.get(w[0]).get("other"));
            assertEquals("ES one " + w[0], w[3], es.get(w[0]).get("one"));
            assertEquals("ES other " + w[0], w[4], es.get(w[0]).get("other"));
            assertEquals("ES many = other (§11.2) " + w[0], w[4], es.get(w[0]).get("many"));
        }
        assertEquals("ES has the same plurals as EN", en.keySet(), es.keySet());
    }

    // ---- helpers ----

    private static boolean precedes(Node a, Node b) {
        return (a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING) != 0;
    }

    private static String style(String styles, String name) {
        int at = styles.indexOf("name=\"" + name + "\"");
        assertTrue("style " + name, at > 0);
        return styles.substring(at, styles.indexOf("</style>", at));
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static Map<String, String> dimens() throws Exception {
        Map<String, String> out = new HashMap<>();
        NodeList n = parse(new File(RES, "values/dimens.xml")).getElementsByTagName("dimen");
        for (int i = 0; i < n.getLength(); i++) {
            Element e = (Element) n.item(i);
            out.put(e.getAttribute("name"), e.getTextContent().trim());
        }
        return out;
    }

    /** Strings as Android reads them: surrounding double quotes are dropped. */
    private static Map<String, String> strings(File f) throws Exception {
        Map<String, String> out = new HashMap<>();
        NodeList n = parse(f).getElementsByTagName("string");
        for (int i = 0; i < n.getLength(); i++) {
            Element e = (Element) n.item(i);
            out.put(e.getAttribute("name"), unquote(e.getTextContent()));
        }
        return out;
    }

    private static Map<String, Map<String, String>> plurals(File f) throws Exception {
        Map<String, Map<String, String>> out = new HashMap<>();
        NodeList n = parse(f).getElementsByTagName("plurals");
        for (int i = 0; i < n.getLength(); i++) {
            Element p = (Element) n.item(i);
            Map<String, String> q = new HashMap<>();
            NodeList items = p.getElementsByTagName("item");
            for (int j = 0; j < items.getLength(); j++) {
                Element it = (Element) items.item(j);
                q.put(it.getAttribute("quantity"), unquote(it.getTextContent()));
            }
            out.put(p.getAttribute("name"), q);
        }
        return out;
    }

    /** Drops surrounding quotes and decodes backslash-u escapes, as aapt does. */
    private static String unquote(String s) {
        String t = s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) == '\\' && i + 5 < t.length() && t.charAt(i + 1) == 'u') {
                out.append((char) Integer.parseInt(t.substring(i + 2, i + 6), 16));
                i += 5;
            } else {
                out.append(t.charAt(i));
            }
        }
        return out.toString();
    }

    private static String sha256(String s) throws Exception {
        byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : h) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static Element byId(Document d, String id) {
        NodeList all = d.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            if (("@+id/" + id).equals(e.getAttributeNS(A, "id"))) return e;
        }
        throw new AssertionError("no view with id " + id);
    }

    private static Document parse(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(f);
    }
}
