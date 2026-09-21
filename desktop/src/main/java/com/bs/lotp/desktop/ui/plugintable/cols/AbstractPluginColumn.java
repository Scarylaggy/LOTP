package com.bs.lotp.desktop.ui.plugintable.cols;

import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import javafx.scene.control.TableColumn;

public abstract class AbstractPluginColumn<V> extends TableColumn<PluginRow, V> {
    protected AbstractPluginColumn(String title) {
        super(title);
    }
}
