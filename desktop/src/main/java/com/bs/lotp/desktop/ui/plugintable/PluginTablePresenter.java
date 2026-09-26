package com.bs.lotp.desktop.ui.plugintable;

import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Consumer;

/**
 * Presenter mediating between {@link PluginTableModel} and {@link PluginTableView}:
 * filter input and row updates go through here, selection events come out.
 * Construct on the JavaFX thread (it builds controls).
 */
public class PluginTablePresenter {

    private final PluginTableModel model = new PluginTableModel();
    private final PluginTableView view;
    private Consumer<PluginRow> selectionListener = row -> {};

    public PluginTablePresenter() {
        view = new PluginTableView(model.sorted(), this::onSelection);
    }

    public TableView<PluginRow> node() {
        return view.node();
    }

    /**
     * Called for every selection change, including deselect ({@code null}).
     */
    public void onSelectionChanged(Consumer<PluginRow> listener) {
        this.selectionListener = listener == null ? row -> {} : listener;
    }

    public void setRows(List<PluginRow> rows) {
        model.setRows(rows);
    }

    public void replaceRow(PluginRow replacement) {
        model.replaceRow(replacement);
        var items = model.sorted();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).plugin().uid() == replacement.plugin().uid()) {
                view.node().getSelectionModel().select(i);
                return;
            }
        }
    }

    public PluginRow selected() {
        return view.node().getSelectionModel().getSelectedItem();
    }

    public boolean isEmpty() {
        return model.isEmpty();
    }

    public void setSearchQuery(String query) {
        model.setQuery(query);
    }

    public void setCategoryFilter(String category) {
        model.setCategory(category);
    }

    private void onSelection(PluginRow row) {
        selectionListener.accept(row);
    }
}
