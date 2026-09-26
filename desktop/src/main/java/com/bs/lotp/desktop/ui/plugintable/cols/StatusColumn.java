package com.bs.lotp.desktop.ui.plugintable.cols;

import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

import java.util.Comparator;

/**
 * Status pill column. Carries the row as its value so sorting follows status
 * order (not installed → installed → update available) instead of pill text.
 */
public class StatusColumn extends AbstractPluginColumn<PluginRow> {

    public StatusColumn() {
        super("Status");
        this.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue()));
        this.setComparator(Comparator.comparing(row -> row.status().ordinal()));
        setPrefWidth(140);
        setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();

            {
                pill.getStyleClass().add("pill");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(PluginRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
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
        });
    }
}
