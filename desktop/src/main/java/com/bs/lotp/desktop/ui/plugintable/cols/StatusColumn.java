package com.bs.lotp.desktop.ui.plugintable.cols;

import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class StatusColumn extends AbstractPluginColumn<String> {

    public StatusColumn() {
        super("Status");
        this.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().status().text));
        setPrefWidth(140);
        setCellFactory(StatusColumn::tableCell);
    }

    private static TableCell<PluginRow, String> tableCell(TableColumn<PluginRow, String> col) {
        return new TableCell<>() {
            private final Label pill = new Label();

            {
                pill.getStyleClass().add("pill");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                PluginRow row = (PluginRow) getTableRow().getItem();
                pill.setText(row.status().text);
                pill.getStyleClass().removeAll("pill-none", "pill-installed", "pill-update");
                switch (row.status()) {
                    case INSTALLED -> pill.getStyleClass().add("pill-installed");
                    case UPDATE_AVAILABLE -> pill.getStyleClass().add("pill-update");
                    default -> pill.getStyleClass().add("pill-none");
                }
                setGraphic(pill);
                setText(null);
            }
        };
    }
}
