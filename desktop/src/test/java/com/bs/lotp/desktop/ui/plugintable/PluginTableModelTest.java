package com.bs.lotp.desktop.ui.plugintable;

import com.bs.lotp.desktop.feed.PluginInfo;
import com.bs.lotp.desktop.local.InstalledStore;
import com.bs.lotp.desktop.ui.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginTableModelTest {

    private static PluginInfo feed(long uid, String name, String author, String version, String category) {
        return new PluginInfo(uid, name, author, version, null, 0, category,
                name + " " + author + " description", "", "", 0, "");
    }

    private static InstalledStore.InstalledEntry installed(long uid, String version, long feedUpdated) {
        return new InstalledStore.InstalledEntry(
                uid, "name", version, feedUpdated, Instant.parse("2026-08-12T00:00:00Z"), List.of("Dir"));
    }

    private static PluginTableModel modelWithRows() {
        PluginTableModel model = new PluginTableModel();
        model.setRows(List.of(
                PluginRow.of(feed(1, "TitanBar", "Fanchen", "1.45", "UI"), null),
                PluginRow.of(feed(2, "Deed Tracker", "Cube", "3.3.0", "Quests"), null),
                PluginRow.of(feed(3, "Broke Legs", "Nolemir", "1.0.2", "Other"), null)));
        return model;
    }

    @Test
    void filtersByNameAuthorAndDescription() {
        PluginTableModel model = modelWithRows();

        model.setQuery("titan");
        assertEquals(1, model.filtered().size());
        assertEquals("TitanBar", model.filtered().get(0).plugin().name());

        model.setQuery("cube");
        assertEquals(1, model.filtered().size());
        assertEquals("Deed Tracker", model.filtered().get(0).plugin().name());

        model.setQuery("FANCHEN");
        assertEquals(1, model.filtered().size());

        model.setQuery("");
        assertEquals(3, model.filtered().size());
    }

    @Test
    void filtersByCategory() {
        PluginTableModel model = modelWithRows();

        model.setCategory("Quests");
        assertEquals(1, model.filtered().size());
        assertEquals("Deed Tracker", model.filtered().get(0).plugin().name());

        model.setCategory(PluginTableModel.ALL_CATEGORIES);
        assertEquals(3, model.filtered().size());
    }

    @Test
    void combinesQueryAndCategory() {
        PluginTableModel model = modelWithRows();

        model.setCategory("UI");
        model.setQuery("deed");
        assertTrue(model.filtered().isEmpty());

        model.setCategory(PluginTableModel.ALL_CATEGORIES);
        assertEquals(1, model.filtered().size());
    }

    @Test
    void replaceRowKeepsOtherRows() {
        PluginTableModel model = modelWithRows();

        var plugin = feed(2, "Deed Tracker", "Cube", "3.4.0", "Quests");
        model.replaceRow(new PluginRow(plugin, installed(2, "3.4.0", 0), Status.INSTALLED));

        assertEquals(3, model.filtered().size());
        assertEquals("3.4.0", model.filtered().stream()
                .filter(r -> r.plugin().uid() == 2)
                .findFirst().orElseThrow().plugin().version());
    }

    @Test
    void statusDerivation() {
        // not installed
        assertEquals(Status.NOT_INSTALLED, PluginRow.of(feed(1, "A", "B", "1.0", "C"), null).status());

        // same version installed
        assertEquals(Status.INSTALLED,
                PluginRow.of(feed(1, "A", "B", "1.0", "C"), installed(1, "1.0", 100)).status());

        // newer feed version
        assertEquals(Status.UPDATE_AVAILABLE,
                PluginRow.of(feed(1, "A", "B", "1.1", "C"), installed(1, "1.0", 100)).status());

        // newer feed timestamp, same version text
        var updated = new PluginInfo(1, "A", "B", "1.0",
                Instant.ofEpochSecond(200), 0, "C", "d", "", "", 0, "");
        assertEquals(Status.UPDATE_AVAILABLE,
                PluginRow.of(updated, installed(1, "1.0", 100)).status());

        // "v" prefix is insignificant
        assertEquals(Status.INSTALLED,
                PluginRow.of(feed(1, "A", "B", "v1.45", "C"), installed(1, "1.45", 100)).status());
    }

    @Test
    void sortedFollowsComparator() {
        PluginTableModel model = modelWithRows();

        // No comparator: insertion order.
        assertEquals(List.of("TitanBar", "Deed Tracker", "Broke Legs"),
                model.sorted().stream().map(r -> r.plugin().name()).toList());

        model.sorted().setComparator(
                Comparator.comparing((PluginRow r) -> r.plugin().name(), String.CASE_INSENSITIVE_ORDER));
        assertEquals(List.of("Broke Legs", "Deed Tracker", "TitanBar"),
                model.sorted().stream().map(r -> r.plugin().name()).toList());
    }

    @Test
    void sortingAppliesToFilteredRows() {
        PluginTableModel model = new PluginTableModel();
        model.setRows(List.of(
                PluginRow.of(feed(1, "Zulu", "Smith", "1.0", "UI"), null),
                PluginRow.of(feed(2, "Alpha", "Jones", "1.0", "UI"), null),
                PluginRow.of(feed(3, "Mike", "Smith", "1.0", "UI"), null)));
        model.setQuery("smith"); // matches Zulu + Mike via author
        model.sorted().setComparator(
                Comparator.comparing((PluginRow r) -> r.plugin().name(), String.CASE_INSENSITIVE_ORDER));

        assertEquals(List.of("Mike", "Zulu"),
                model.sorted().stream().map(r -> r.plugin().name()).toList());

        model.setQuery("");
        assertEquals(3, model.sorted().size());
    }

    @Test
    void versionsCompareNumerically() {
        assertTrue(PluginRow.compareVersions("1.9", "1.10") < 0);
        assertTrue(PluginRow.compareVersions("1.10", "1.9") > 0);
        assertEquals(0, PluginRow.compareVersions("v1.45", "1.45"));
        assertEquals(0, PluginRow.compareVersions("3.3.0", "3.3.0"));
        assertTrue(PluginRow.compareVersions("1.0.2", "1.0.10") < 0);
        assertTrue(PluginRow.compareVersions("2.0", "10.0") < 0);
        assertTrue(PluginRow.compareVersions("", "1.0") < 0);
        assertEquals(0, PluginRow.compareVersions(null, ""));
    }

    @Test
    void sortsBySizeNumerically() {
        PluginTableModel model = new PluginTableModel();
        model.setRows(List.of(
                rowWithSize(1, 10_000),
                rowWithSize(2, 9_000),
                rowWithSize(3, 2_000_000)));
        model.sorted().setComparator(Comparator.comparingLong(r -> r.plugin().size()));

        // Lexicographic order would put "10 KB" before "9 KB" — numeric must not.
        assertEquals(List.of(2L, 1L, 3L),
                model.sorted().stream().map(r -> r.plugin().uid()).toList());
    }

    private static PluginRow rowWithSize(long uid, long size) {
        return PluginRow.of(
                new PluginInfo(uid, "P" + uid, "A", "1.0", null, 0, "C", "d", "", "", size, ""),
                null);
    }
}
