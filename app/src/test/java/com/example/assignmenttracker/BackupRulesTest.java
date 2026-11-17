package com.example.assignmenttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.assignmenttracker.data.db.AppDatabase;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * SEC-0-2: backup is explicit. Only the Room database is included, the same way in
 * backup_rules.xml (API 30 and below) and in both sections of data_extraction_rules.xml
 * (API 31+), and auth/session prefs are explicitly excluded.
 */
public class BackupRulesTest {

    private static final File XML = new File("src/main/res/xml");

    @Test
    public void fullBackupContentIncludesOnlyTheRoomDatabase() throws Exception {
        Element root = parse("backup_rules.xml");
        assertRules("full-backup-content", root);
    }

    @Test
    public void cloudBackupAndDeviceTransferIncludeOnlyTheRoomDatabase() throws Exception {
        Element root = parse("data_extraction_rules.xml");
        for (String section : new String[]{"cloud-backup", "device-transfer"}) {
            NodeList s = root.getElementsByTagName(section);
            assertEquals("one <" + section + ">", 1, s.getLength());
            assertRules(section, (Element) s.item(0));
        }
    }

    private static void assertRules(String where, Element el) {
        List<String> includes = rules(el, "include");
        assertEquals(where + " includes", List.of("database:" + AppDatabase.DB_NAME), includes);
        List<String> excludes = rules(el, "exclude");
        assertTrue(where + " must exclude the future auth/session prefs: " + excludes,
                excludes.contains("sharedpref:" + AppDatabase.AUTH_PREFS_FILE));
    }

    private static List<String> rules(Element el, String tag) {
        List<String> out = new ArrayList<>();
        NodeList kids = el.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n instanceof Element && ((Element) n).getTagName().equals(tag)) {
                out.add(((Element) n).getAttribute("domain") + ":" + ((Element) n).getAttribute("path"));
            }
        }
        return out;
    }

    private static Element parse(String name) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File(XML, name)).getDocumentElement();
    }
}
