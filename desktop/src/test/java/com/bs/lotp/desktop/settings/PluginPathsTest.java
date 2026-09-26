package com.bs.lotp.desktop.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PluginPathsTest {

    @Test
    void pathsRememberOverride(@TempDir Path temp) throws Exception {
        Path fakeHome = temp.resolve("home");
        Files.createDirectories(fakeHome);
        PluginPaths paths = new PluginPaths(temp.resolve("config"), fakeHome);
        assertNull(paths.pluginsDir());

        Path plugins = fakeHome.resolve("Documents/The Lord of the Rings Online/Plugins");
        Files.createDirectories(plugins);
        // auto-detect finds the fresh dir and remembers it
        assertEquals(plugins, paths.pluginsDir());
        assertEquals(plugins, new PluginPaths(temp.resolve("config"), fakeHome).pluginsDir());

        // explicit override wins
        Path other = temp.resolve("other");
        Files.createDirectories(other);
        paths.setPluginsDir(other);
        assertEquals(other, new PluginPaths(temp.resolve("config"), fakeHome).pluginsDir());
    }

    @Test
    void windowBoundsRoundTrip(@TempDir Path temp) throws Exception {
        PluginPaths paths = new PluginPaths(temp.resolve("config"), temp.resolve("home"));
        assertNull(paths.windowBounds());

        paths.setWindowBounds(10, 20, 1280, 800);
        assertArrayEquals(
                new double[]{10, 20, 1280, 800},
                new PluginPaths(temp.resolve("config"), temp.resolve("home")).windowBounds());

        paths.setWindowBounds(0, 0, 100, 100); // absurdly small: rejected
        assertNull(new PluginPaths(temp.resolve("config"), temp.resolve("home")).windowBounds());
    }

    @Test
    void themeRoundTrip(@TempDir Path temp) throws Exception {
        PluginPaths paths = new PluginPaths(temp.resolve("config"), temp.resolve("home"));
        assertNull(paths.theme());

        paths.setTheme("dark");
        assertEquals("dark", new PluginPaths(temp.resolve("config"), temp.resolve("home")).theme());
    }

    @Test
    void migratesLegacyProperties(@TempDir Path temp) throws Exception {
        Path config = temp.resolve("config");
        Files.createDirectories(config);
        Path plugins = temp.resolve("Plugins");
        Files.createDirectories(plugins);
        // Write via Properties.store so backslashes are escaped the way a real
        // legacy file would be (raw "C:\..." would be misparsed on Windows).
        Properties legacy = new Properties();
        legacy.setProperty("plugins.dir", plugins.toString());
        legacy.setProperty("window.x", "10");
        legacy.setProperty("window.y", "20");
        legacy.setProperty("window.w", "1280");
        legacy.setProperty("window.h", "800");
        try (var out = Files.newOutputStream(config.resolve("settings.properties"))) {
            legacy.store(out, null);
        }

        PluginPaths paths = new PluginPaths(config, temp.resolve("home"));
        assertEquals(plugins, paths.pluginsDir());
        assertArrayEquals(new double[]{10, 20, 1280, 800}, paths.windowBounds());

        // migrated to yaml, legacy file gone
        assertTrue(Files.isRegularFile(config.resolve("settings.yaml")));
        assertTrue(Files.notExists(config.resolve("settings.properties")));
    }

    @Test
    void migratesPreviousConfigDir(@TempDir Path temp) throws Exception {
        Path previous = temp.resolve(".lotp-desktop");
        Files.createDirectories(previous);
        Files.writeString(previous.resolve("settings.yaml"), "theme: dark\n");

        PluginPaths paths = new PluginPaths(temp.resolve(".lotp"), temp);
        assertEquals("dark", paths.theme());
        assertTrue(Files.isRegularFile(temp.resolve(".lotp/settings.yaml")));
    }

    @Test
    void candidatesCoverWineAndProton(@TempDir Path temp) {
        List<Path> candidates = PluginPaths.candidates(temp);
        assertTrue(candidates.stream().anyMatch(p -> p.toString().contains("compatdata")));
        assertTrue(candidates.stream().anyMatch(p -> p.toString().contains(".wine")));
        assertTrue(candidates.stream().anyMatch(p -> p.endsWith("The Lord of the Rings Online/Plugins")));
    }
}
