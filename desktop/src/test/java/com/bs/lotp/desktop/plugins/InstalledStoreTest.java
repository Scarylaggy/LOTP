package com.bs.lotp.desktop.plugins;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstalledStoreTest {

    @Test
    void roundTrip(@TempDir Path temp) throws Exception {
        InstalledStore store = new InstalledStore(temp);
        assertTrue(store.load().isEmpty());

        store.put(new InstalledStore.InstalledEntry(
                1308, "Broke Legs", "1.0.2", 1786644762L, Instant.parse("2026-08-12T00:00:00Z"),
                List.of("Nolemir")));

        Map<Long, InstalledStore.InstalledEntry> loaded = new InstalledStore(temp).load();
        assertEquals(1, loaded.size());
        InstalledStore.InstalledEntry entry = loaded.get(1308L);
        assertEquals("Broke Legs", entry.name());
        assertEquals("1.0.2", entry.version());
        assertEquals(List.of("Nolemir"), entry.topLevelDirs());

        store.remove(1308L);
        assertTrue(new InstalledStore(temp).load().isEmpty());
    }

    @Test
    void missingFileLoadsEmpty(@TempDir Path temp) throws Exception {
        assertTrue(new InstalledStore(temp.resolve("nope")).load().isEmpty());
    }

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
    void migratesLegacyProperties(@TempDir Path temp) throws Exception {
        Path config = temp.resolve("config");
        Files.createDirectories(config);
        Path plugins = temp.resolve("Plugins");
        Files.createDirectories(plugins);
        Files.writeString(config.resolve("settings.properties"),
                "plugins.dir=" + plugins + "\nwindow.x=10\nwindow.y=20\nwindow.w=1280\nwindow.h=800\n");

        PluginPaths paths = new PluginPaths(config, temp.resolve("home"));
        assertEquals(plugins, paths.pluginsDir());
        assertArrayEquals(new double[]{10, 20, 1280, 800}, paths.windowBounds());

        // migrated to yaml, legacy file gone
        assertTrue(Files.isRegularFile(config.resolve("settings.yaml")));
        assertTrue(Files.notExists(config.resolve("settings.properties")));
    }

    @Test
    void candidatesCoverWineAndProton(@TempDir Path temp) {
        List<Path> candidates = PluginPaths.candidates(temp);
        assertTrue(candidates.stream().anyMatch(p -> p.toString().contains("compatdata")));
        assertTrue(candidates.stream().anyMatch(p -> p.toString().contains(".wine")));
        assertTrue(candidates.stream().anyMatch(p -> p.endsWith("The Lord of the Rings Online/Plugins")));
    }
}
