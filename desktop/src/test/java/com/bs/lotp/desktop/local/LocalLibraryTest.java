package com.bs.lotp.desktop.local;

import com.bs.lotp.desktop.feed.PluginInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalLibraryTest {

    private static void pluginFile(Path file, String name, String author, String version) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                <?xml version="1.0"?>
                <Plugin><Information>
                <Name>%s</Name>
                <Author>%s</Author>
                <Version>%s</Version>
                </Information></Plugin>
                """.formatted(name, author, version));
    }

    private static PluginInfo feed(long uid, String name, String author, String version) {
        return new PluginInfo(uid, name, author, version, null, 0, "", "", "", "", 0, "");
    }

    @Test
    void scansAndMatchesByNormalizedName(@TempDir Path temp) throws Exception {
        Path plugins = temp.resolve("Plugins");
        pluginFile(plugins.resolve("HabnaPlugins/TitanBar.plugin"), "TitanBar", "Fanchen", "v1.45");
        pluginFile(plugins.resolve("CubePlugins/DeedTracker.plugin"), "DeedTracker", "Cube", "3.3.0");
        Files.writeString(plugins.resolve("notes.txt"), "not a plugin");

        List<LocalLibrary.LocalPlugin> local = LocalLibrary.scan(plugins);
        assertEquals(2, local.size());

        // feed name "Deed Tracker" matches folder/descriptor "DeedTracker"
        Optional<LocalLibrary.LocalPlugin> deed = LocalLibrary.matchFor(
                feed(1139, "Deed Tracker", "b414213562", "3.3.0"), local);
        assertTrue(deed.isPresent());
        assertEquals("CubePlugins", deed.get().topLevelDir());

        Optional<LocalLibrary.LocalPlugin> titan = LocalLibrary.matchFor(
                feed(1, "TitanBar", "someone", "1.45"), local);
        assertTrue(titan.isPresent());
        assertEquals("v1.45", titan.get().version());
    }

    @Test
    void prefersAuthorMatchOnAmbiguousNames(@TempDir Path temp) throws Exception {
        Path plugins = temp.resolve("Plugins");
        pluginFile(plugins.resolve("AuthorA/Thing.plugin"), "Thing", "AuthorA", "1.0");
        pluginFile(plugins.resolve("AuthorB/Thing.plugin"), "Thing", "AuthorB", "2.0");

        List<LocalLibrary.LocalPlugin> local = LocalLibrary.scan(plugins);
        Optional<LocalLibrary.LocalPlugin> match =
                LocalLibrary.matchFor(feed(9, "Thing", "authorb", "2.0"), local);
        assertTrue(match.isPresent());
        assertEquals("AuthorB", match.get().topLevelDir());
    }

    @Test
    void noMatchForUnknown(@TempDir Path temp) throws Exception {
        Path plugins = temp.resolve("Plugins");
        Files.createDirectories(plugins);
        assertTrue(LocalLibrary.matchFor(
                feed(1, "Something Else", "x", "1.0"), LocalLibrary.scan(plugins)).isEmpty());
        assertTrue(LocalLibrary.scan(temp.resolve("missing")).isEmpty());
    }

    @Test
    void versionsNormalize() {
        assertEquals(
                LocalLibrary.normalizedVersion("1.45"), LocalLibrary.normalizedVersion("v1.45"));
        assertEquals("deedtracker", LocalLibrary.normalized("Deed Tracker"));
    }
}
