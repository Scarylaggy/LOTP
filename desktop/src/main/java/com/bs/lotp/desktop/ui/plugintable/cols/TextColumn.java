package com.bs.lotp.desktop.ui.plugintable.cols;

import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import javafx.beans.property.SimpleStringProperty;

import java.util.Comparator;
import java.util.function.Function;

public class TextColumn extends AbstractPluginColumn<String> {

    public TextColumn(String title, double width, Function<PluginRow, String> value) {
        this(title, width, value, String.CASE_INSENSITIVE_ORDER);
    }

    public TextColumn(
            String title, double width, Function<PluginRow, String> value, Comparator<String> order) {
        super(title);
        this.setCellValueFactory(cell ->
                new SimpleStringProperty(value.apply(cell.getValue())));
        this.setComparator(order);
        this.setPrefWidth(width);
    }
}
