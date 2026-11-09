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

import javax.xml.parsers.DocumentBuilderFactory;

/** Slice 2 resource rules: the add/edit screen, the tappable row and the design carry-overs. */
public class EditScreenResourcesTest {

    private static final String A = "http://schemas.android.com/apk/res/android";
    private static final String APP = "http://schemas.android.com/apk/res-auto";
    private static final File RES = new File("src/main/res");

    @Test
    public void editActivityIsDeclaredAndNotExported() throws Exception {
        NodeList acts = parse(new File("src/main/AndroidManifest.xml")).getElementsByTagName("activity");
        Element edit = null;
        for (int i = 0; i < acts.getLength(); i++) {
            Element a = (Element) acts.item(i);
            if (a.getAttributeNS(A, "name").endsWith(".AssignmentEditActivity")) edit = a;
        }
        assertNotNull("AssignmentEditActivity must be in the manifest", edit);
        assertEquals("false", edit.getAttributeNS(A, "exported"));
        assertEquals(0, edit.getElementsByTagName("intent-filter").getLength());
    }

    /** QA-1-5: the Slice 1 dialog is gone, replaced by the full screen. */
    @Test
    public void slice1DialogIsGone() {
        assertFalse(new File(RES, "layout/dialog_add_assignment.xml").exists());
        assertFalse(new File("src/main/java/com/example/assignmenttracker/ui/assignments/AddAssignmentDialogFragment.java").exists());
    }

    @Test
    public void editLayoutFollowsDesign() throws Exception {
        File f = new File(RES, "layout/activity_assignment_edit.xml");
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertFalse("no raw mx_* colors in new layouts", xml.contains("@color/mx_"));
        Document d = parse(f);
        Element title = byId(d, "etTitle");
        assertEquals("200", title.getAttributeNS(A, "maxLength"));
        assertEquals("actionNext", title.getAttributeNS(A, "imeOptions"));
        Element tilTitle = byId(d, "tilTitle");
        assertEquals("true", tilTitle.getAttributeNS(APP, "counterEnabled"));
        assertEquals("200", tilTitle.getAttributeNS(APP, "counterMaxLength"));
        assertEquals("60", byId(d, "etCourse").getAttributeNS(A, "maxLength"));
        assertEquals("@id/etNotes", byId(d, "etCourse").getAttributeNS(A, "nextFocusForward"));
        assertEquals("2000", byId(d, "etNotes").getAttributeNS(A, "maxLength"));
        assertEquals("2000", byId(d, "tilNotes").getAttributeNS(APP, "counterMaxLength"));
        assertTrue(byId(d, "etNotes").getAttributeNS(A, "inputType").contains("textMultiLine"));
        assertEquals("@string/field_course_placeholder", byId(d, "tilCourse").getAttributeNS(APP, "placeholderText"));
        assertEquals("@style/Widget.AT.Button.Pill", byId(d, "btnSave").getAttribute("style"));
        assertEquals("no", byId(d, "ribbon").getAttributeNS(A, "importantForAccessibility"));
    }

    /** DES-0-6: unfocused outlines come from colorOutline (warm brown), focused from colorPrimary. */
    @Test
    public void textFieldStrokeIsAStateListWithAWarmDefault() throws Exception {
        String styles = new String(Files.readAllBytes(new File(RES, "values/styles.xml").toPath()), StandardCharsets.UTF_8);
        assertTrue(styles.contains("<item name=\"boxStrokeColor\">@color/field_stroke</item>"));
        NodeList items = parse(new File(RES, "color/field_stroke.xml")).getElementsByTagName("item");
        Element last = (Element) items.item(items.getLength() - 1);
        assertEquals("default (unfocused) stroke", "?attr/colorOutline", last.getAttributeNS(A, "color"));
        Element first = (Element) items.item(0);
        assertEquals("true", first.getAttributeNS(A, "state_focused"));
        assertEquals("?attr/colorPrimary", first.getAttributeNS(A, "color"));
    }

    /** DES-1-2 */
    @Test
    public void emptyBodyHasNoLetterSpacing() throws Exception {
        String styles = new String(Files.readAllBytes(new File(RES, "values/styles.xml").toPath()), StandardCharsets.UTF_8);
        int start = styles.indexOf("name=\"TextAppearance.AT.StateBody\"");
        assertTrue(start > 0);
        String block = styles.substring(start, styles.indexOf("</style>", start));
        assertTrue(block.contains("<item name=\"android:letterSpacing\">0</item>"));
    }

    /** DES-1-3 */
    @Test
    public void emptyBodyBreaksBalanced() throws Exception {
        Element body = byId(parse(new File(RES, "layout/item_empty.xml")), "emptyBody");
        assertEquals("balanced", body.getAttributeNS(A, "breakStrategy"));
    }

    /** DES-1-4: rows are tappable with a ripple and a focus state, and the list highlight is back. */
    @Test
    public void rowsHaveARippleAndFocusState() throws Exception {
        // QA-3-1 (Slice 4): the tap/focus target is the row body; the card root clips it.
        Document doc = parse(new File(RES, "layout/item_assignment.xml"));
        Element root = doc.getDocumentElement();
        Element row = byId(doc, "rowBody");
        assertEquals("true", row.getAttributeNS(A, "clickable"));
        assertEquals("true", row.getAttributeNS(A, "focusable"));
        assertEquals("?attr/selectableItemBackground", row.getAttributeNS(A, "foreground"));
        assertEquals("ripple clipped to the row shape", "true", root.getAttributeNS(A, "clipToOutline"));
        Element list = byId(parse(new File(RES, "layout/activity_main.xml")), "list");
        assertFalse("list focus highlight must not be disabled",
                list.hasAttributeNS(A, "defaultFocusHighlightEnabled"));
    }

    // ---- helpers ----

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
