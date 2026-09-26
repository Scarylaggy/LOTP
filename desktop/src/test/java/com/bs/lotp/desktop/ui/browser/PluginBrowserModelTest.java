package com.bs.lotp.desktop.ui.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PluginBrowserModelTest {

    @Test
    void defaults() {
        PluginBrowserModel model = new PluginBrowserModel();
        assertEquals("Press Refresh to load the plugin list.", model.getStatusText());
        assertEquals(0, model.getProgress());
        assertEquals("", model.getFolderText());
        assertNull(model.getPluginsDir());
    }

    @Test
    void storesDisplayState() {
        PluginBrowserModel model = new PluginBrowserModel();
        model.setStatusText("Downloading plugin list...");
        model.setProgress(-1);
        model.setFolderText("Plugins: /tmp/Plugins");
        model.setPluginsDir(Path.of("/tmp/Plugins"));

        assertEquals("Downloading plugin list...", model.statusTextProperty().get());
        assertEquals(-1, model.progressProperty().get());
        assertEquals("Plugins: /tmp/Plugins", model.folderTextProperty().get());
        assertEquals(Path.of("/tmp/Plugins"), model.getPluginsDir());
    }
}
