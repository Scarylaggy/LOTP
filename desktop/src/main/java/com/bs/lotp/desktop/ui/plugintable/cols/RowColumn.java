package com.bs.lotp.desktop.ui.plugintable.cols;

import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableCell;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Text column that sorts by the row itself rather than the displayed string,
 * for values where lexicographic order is wrong (versions, dates, sizes).
 */
public class RowColumn extends AbstractPluginColumn<PluginRow> {

    public RowColumn(
            String title,
            double width,
            Function<PluginRow, String> text,
            Comparator<PluginRow> order) {
        super(title);
        this.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue()));
        this.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PluginRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(empty || row == null ? null : text.apply(row));
            }
        });
        this.setComparator(order);
        this.setPrefWidth(width);
    }
}
