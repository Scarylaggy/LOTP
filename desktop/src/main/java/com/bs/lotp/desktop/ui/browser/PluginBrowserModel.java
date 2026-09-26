package com.bs.lotp.desktop.ui.browser;

import java.nio.file.Path;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model of the plugin browser: status/folder display state plus the resolved
 * plugins directory. Plain properties, no controls — safe to use and
 * unit-test without the JavaFX toolkit running.
 */
public class PluginBrowserModel {

    private final StringProperty statusText =
            new SimpleStringProperty("Press Refresh to load the plugin list.");
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty folderText = new SimpleStringProperty("");

    private Path pluginsDir;

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public String getStatusText() {
        return statusText.get();
    }

    public void setStatusText(String text) {
        statusText.set(text);
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double value) {
        progress.set(value);
    }

    public StringProperty folderTextProperty() {
        return folderText;
    }

    public String getFolderText() {
        return folderText.get();
    }

    public void setFolderText(String text) {
        folderText.set(text);
    }

    public Path getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(Path pluginsDir) {
        this.pluginsDir = pluginsDir;
    }
}
