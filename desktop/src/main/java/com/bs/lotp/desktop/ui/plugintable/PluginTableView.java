package com.bs.lotp.desktop.ui.plugintable;

import com.bs.lotp.desktop.ui.plugintable.cols.DownloadColumn;
import com.bs.lotp.desktop.ui.plugintable.cols.RowColumn;
import com.bs.lotp.desktop.ui.plugintable.cols.StatusColumn;
import com.bs.lotp.desktop.ui.plugintable.cols.TextColumn;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

import java.time.Instant;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Passive view: builds the plugin {@code TableView} and forwards selection
 * changes to the presenter. Holds no state beyond the controls themselves.
 * Every column is sortable; the items comparator is bound to the table's so
 * header clicks reorder the rows.
 */
public class PluginTableView {

    private final TableView<PluginRow> table = new TableView<>();

    public PluginTableView(SortedList<PluginRow> items, Consumer<PluginRow> onSelection) {
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("plugin-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(items);
        items.comparatorProperty().bind(table.comparatorProperty());

        table.getColumns().add(new TextColumn("Name", 220, row -> row.plugin().name()));
        table.getColumns().add(new TextColumn("Author", 130, row -> row.plugin().author()));
        table.getColumns().add(new RowColumn("Version", 130,
                row -> row.plugin().version()
                        + (row.installed() == null ? "" : " / " + row.installed().version()),
                Comparator.comparing((PluginRow row) -> row.plugin().version(), PluginRow::compareVersions)
                        .thenComparing(row -> row.installed() == null ? "" : row.installed().version(),
                                PluginRow::compareVersions)));
        table.getColumns().add(new RowColumn("Updated", 100,
                row -> row.plugin().updatedText(),
                Comparator.comparing((PluginRow row) -> row.plugin().updated(),
                        Comparator.nullsLast(Comparator.<Instant>naturalOrder()))));
        table.getColumns().add(new TextColumn("Category", 170, row -> row.plugin().category()));
        table.getColumns().add(new DownloadColumn());
        table.getColumns().add(new RowColumn("Size", 90,
                row -> row.plugin().sizeText(),
                Comparator.comparingLong(row -> row.plugin().size())));
        table.getColumns().add(new StatusColumn());

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, row) -> onSelection.accept(row));
    }

    public TableView<PluginRow> node() {
        return table;
    }

}
