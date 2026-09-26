package com.bs.lotp.desktop.ui.plugintable;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.util.List;
import java.util.Locale;

/**
 * Model of the plugin table: all rows plus search/category filter state and
 * sort order. Pure collections, no controls — safe to use and unit-test
 * without the JavaFX toolkit running.
 */
public class PluginTableModel {

    public static final String ALL_CATEGORIES = "All categories";

    private final ObservableList<PluginRow> rows = FXCollections.observableArrayList();
    private final FilteredList<PluginRow> filtered = new FilteredList<>(rows, row -> true);
    private final SortedList<PluginRow> sorted = new SortedList<>(filtered);

    private String query = "";
    private String category = ALL_CATEGORIES;

    /**
     * Live-filtered view of the rows, for the {@code TableView} to observe.
     */
    public FilteredList<PluginRow> filtered() {
        return filtered;
    }

    /**
     * Filtered rows in table sort order. The view binds this list's comparator
     * to the {@code TableView} comparator, so clicking a column header sorts.
     */
    public SortedList<PluginRow> sorted() {
        return sorted;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public void setRows(List<PluginRow> fresh) {
        rows.setAll(fresh);
    }

    public void replaceRow(PluginRow replacement) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).plugin().uid() == replacement.plugin().uid()) {
                rows.set(i, replacement);
                return;
            }
        }
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        refilter();
    }

    public void setCategory(String category) {
        this.category = category == null ? ALL_CATEGORIES : category;
        refilter();
    }

    private void refilter() {
        String q = query.toLowerCase(Locale.ROOT);
        String cat = category;
        filtered.setPredicate(row -> {
            boolean matchCategory = cat == null || cat.equals(ALL_CATEGORIES)
                    || row.plugin().category().equals(cat);
            if (!matchCategory) {
                return false;
            }
            if (q.isBlank()) {
                return true;
            }
            var plugin = row.plugin();
            return plugin.name().toLowerCase(Locale.ROOT).contains(q)
                    || plugin.author().toLowerCase(Locale.ROOT).contains(q)
                    || plugin.description().toLowerCase(Locale.ROOT).contains(q);
        });
    }
}
