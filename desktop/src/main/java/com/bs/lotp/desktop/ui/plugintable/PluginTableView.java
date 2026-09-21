package com.bs.lotp.desktop.ui.plugintable;

import com.bs.lotp.desktop.ui.plugintable.cols.DownloadColumn;
import com.bs.lotp.desktop.ui.plugintable.cols.StatusColumn;
import com.bs.lotp.desktop.ui.plugintable.cols.TextColumn;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

import java.util.function.Consumer;

/**
 * Passive view: builds the plugin {@code TableView} and forwards selection
 * changes to the presenter. Holds no state beyond the controls themselves.
 */
public class PluginTableView {

    private final TableView<PluginRow> table = new TableView<>();

    public PluginTableView(FilteredList<PluginRow> items, Consumer<PluginRow> onSelection) {
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("plugin-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(items);

        table.getColumns().add(new TextColumn("Name", 220, row -> row.plugin().name()));
        table.getColumns().add(new TextColumn("Author", 130, row -> row.plugin().author()));
        table.getColumns().add(new TextColumn("Version", 130, row -> row.plugin().version()
                + (row.installed() == null ? "" : " / " + row.installed().version())));
        table.getColumns().add(new TextColumn("Updated", 100, row -> row.plugin().updatedText()));
        table.getColumns().add(new TextColumn("Category", 170, row -> row.plugin().category()));
        table.getColumns().add(new DownloadColumn());
        table.getColumns().add(new TextColumn("Size", 90, row -> row.plugin().sizeText()));
        table.getColumns().add(new StatusColumn());

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, row) -> onSelection.accept(row));
    }

    public TableView<PluginRow> node() {
        return table;
    }

}
