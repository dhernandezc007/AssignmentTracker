package com.example.assignmenttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

/** Slice 3 resource rules (design.md §3.1–§3.3, §4, §4b, §11, §14). */
public class DoneAndDeleteResourcesTest {

    private static final String A = "http://schemas.android.com/apk/res/android";
    private static final String APP = "http://schemas.android.com/apk/res-auto";
    private static final File RES = new File("src/main/res");

    /** §3.1: 48dp checkbox target at a 4dp margin, so the text starts 52dp in. */
    @Test
    public void rowHasA48dpCheckboxAndTextStartsAt52dp() throws Exception {
        Document d = parse(new File(RES, "layout/item_assignment.xml"));
        Element cb = byId(d, "rowCheck");
        assertEquals("com.google.android.material.checkbox.MaterialCheckBox", cb.getTagName());
        assertEquals("@dimen/checkbox_target", cb.getAttributeNS(A, "layout_width"));
        assertEquals("@dimen/checkbox_target", cb.getAttributeNS(A, "layout_height"));
        assertEquals("@dimen/checkbox_margin_start", cb.getAttributeNS(A, "layout_marginStart"));
        assertEquals("@color/checkbox_tint", cb.getAttributeNS(APP, "buttonTint"));
        assertFalse("no text on the checkbox itself", cb.hasAttributeNS(A, "text"));

        Map<String, String> dimens = dimens();
        assertEquals("48dp", dimens.get("checkbox_target"));
        assertEquals("4dp", dimens.get("checkbox_margin_start"));
        assertEquals("52dp", dimens.get("text_start_in_card"));
        assertFalse("the Slice 1 16dp text start is retired", dimens.containsKey("row_text_start"));
        assertEquals("rule is inset to the text start", "@dimen/text_start_in_card",
                byId(d, "rowRule").getAttributeNS(A, "layout_marginStart"));
    }

    /** §3.1: green when checked, outline otherwise. */
    @Test
    public void checkboxTintIsGreenWhenChecked() throws Exception {
        Element sel = parse(new File(RES, "color/checkbox_tint.xml")).getDocumentElement();
        NodeList items = sel.getElementsByTagName("item");
        Element first = (Element) items.item(0);
        assertEquals("true", first.getAttributeNS(A, "state_checked"));
        assertEquals("@color/mx_green", first.getAttributeNS(A, "color"));
        Element fallback = (Element) items.item(items.getLength() - 1);
        assertEquals("?attr/colorOutline", fallback.getAttributeNS(A, "color"));
    }

    /** §3.2: the Done header has the swatch, the label, the count and the chevron. */
    @Test
    public void doneHeaderLayoutFollowsDesign() throws Exception {
        File f = new File(RES, "layout/item_done_header.xml");
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertFalse("no raw mx_* colors", xml.contains("@color/mx_"));
        Document d = parse(f);
        Element root = d.getDocumentElement();
        assertEquals("48dp target", "@dimen/touch_target", root.getAttributeNS(A, "minHeight"));
        assertEquals("true", root.getAttributeNS(A, "accessibilityHeading"));
        assertEquals("@drawable/serape_swatch", byId(d, "doneSwatch").getAttributeNS(A, "src"));
        assertEquals("no", byId(d, "doneSwatch").getAttributeNS(A, "importantForAccessibility"));
        assertEquals("@string/section_done", byId(d, "doneLabel").getAttributeNS(A, "text"));
        assertNotNull(byId(d, "doneCount"));
        assertEquals("@drawable/ic_expand_more", byId(d, "doneChevron").getAttributeNS(A, "src"));
        assertEquals("48dp", dimens().get("touch_target"));
    }

    @Test
    public void newDrawablesExist() {
        for (String n : new String[]{"serape_swatch", "ic_delete", "ic_expand_more"}) {
            assertTrue(n, new File(RES, "drawable/" + n + ".xml").exists());
        }
    }

    /** §4: the snackbar action is sentence case. */
    @Test
    public void snackbarActionIsNotAllCaps() throws Exception {
        String styles = new String(Files.readAllBytes(new File(RES, "values/styles.xml").toPath()), StandardCharsets.UTF_8);
        String themes = new String(Files.readAllBytes(new File(RES, "values/themes.xml").toPath()), StandardCharsets.UTF_8);
        assertTrue(themes.contains("<item name=\"snackbarButtonStyle\">@style/Widget.AT.Button.TextButton.Snackbar</item>"));
        int at = styles.indexOf("name=\"Widget.AT.Button.TextButton.Snackbar\"");
        assertTrue(at > 0);
        String style = styles.substring(at, styles.indexOf("</style>", at));
        assertTrue(style.contains("<item name=\"android:textAllCaps\">false</item>"));
    }

    /** §11.1 copy, EN and ES, plus the approved Undo-after-done snackbar. */
    @Test
    public void slice3CopyMatchesDesign() throws Exception {
        Map<String, String> en = strings(new File(RES, "values/strings.xml"));
        Map<String, String> es = strings(new File(RES, "values-es/strings.xml"));
        String[][] want = {
                {"section_done", "Done", "Hechas"},
                {"checkbox_cd", "Done: %1$s", "Hecha: %1$s"},
                {"announce_marked_done", "Marked done: %1$s", "Marcada como hecha: %1$s"},
                {"announce_marked_open", "Moved back to open: %1$s", "Regresó a pendientes: %1$s"},
                {"action_delete", "Delete", "Eliminar"},
                {"snackbar_deleted", "Assignment deleted", "Tarea eliminada"},
                {"snackbar_marked_done", "Marked done", "Marcada como hecha"},
                {"action_undo", "Undo", "Deshacer"},
                {"done_header_cd_collapsed", "Done, %1$d. Double-tap to show.", "Hechas, %1$d. Toca dos veces para mostrar."},
                {"done_header_cd_expanded", "Done, %1$d. Double-tap to hide.", "Hechas, %1$d. Toca dos veces para ocultar."},
                {"state_collapsed", "Collapsed", "Contraído"},
                {"state_expanded", "Expanded", "Expandido"},
        };
        for (String[] w : want) {
            assertEquals("EN " + w[0], w[1], en.get(w[0]));
            assertEquals("ES " + w[0], w[2], es.get(w[0]));
        }
    }

    // ---- helpers ----

    private static Map<String, String> dimens() throws Exception {
        Map<String, String> out = new HashMap<>();
        NodeList n = parse(new File(RES, "values/dimens.xml")).getElementsByTagName("dimen");
        for (int i = 0; i < n.getLength(); i++) {
            Element e = (Element) n.item(i);
            out.put(e.getAttribute("name"), e.getTextContent().trim());
        }
        return out;
    }

    private static Map<String, String> strings(File f) throws Exception {
        Map<String, String> out = new HashMap<>();
        NodeList n = parse(f).getElementsByTagName("string");
        for (int i = 0; i < n.getLength(); i++) {
            Element e = (Element) n.item(i);
            out.put(e.getAttribute("name"), e.getTextContent());
        }
        return out;
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
