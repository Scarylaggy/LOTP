package com.bs.lotp.desktop.ui.plugintable.cols;

import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import javafx.beans.property.SimpleStringProperty;

import java.util.function.Function;

public class TextColumn extends AbstractPluginColumn<String> {

    public TextColumn(String title, double width, Function<PluginRow, String> value) {
        super(title);
        this.setCellValueFactory(cell ->
                new SimpleStringProperty(value.apply(cell.getValue())));
        this.setPrefWidth(width);
    }
}
