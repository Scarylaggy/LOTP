package com.bs.lotp.desktop.ui.plugintable.cols;

import javafx.beans.binding.Bindings;

public class DownloadColumn extends AbstractPluginColumn<Number> {
    public DownloadColumn() {
        super("Downloads");
        this.setCellValueFactory(cell -> Bindings.createIntegerBinding(
                () -> (int) Math.min(cell.getValue().plugin().downloads(), Integer.MAX_VALUE)));
        this.setPrefWidth(100);
    }
}
