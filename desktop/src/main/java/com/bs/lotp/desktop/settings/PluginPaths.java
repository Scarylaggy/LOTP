package com.bs.lotp.desktop.settings;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Locates the LOTRO {@code Plugins} folder: explicit override from settings,
 * otherwise well-known locations (native docs dir, Steam/Proton prefixes, Wine).
 */
@Service
public class PluginPaths {

    private static final String SETTINGS_FILE = "settings.yaml";
    private static final String LEGACY_SETTINGS_FILE = "settings.properties";
    private static final String SETTINGS_DIR = ".lotp";

    /**
     * Steam AppID of LOTRO (for Proton prefix lookup on Linux).
     */
    private static final String LOTRO_STEAM_APP_ID = "212500";

    private final Path configDir;
    private final Path home;
    private final ObjectMapper yaml = new YAMLMapper();

    public PluginPaths() {
        this(Path.of(System.getProperty("user.home"), SETTINGS_DIR),
                Path.of(System.getProperty("user.home")));
    }

    public PluginPaths(Path configDir) {
        this(configDir, Path.of(System.getProperty("user.home")));
    }

    /**
     * Test-friendly constructor (inject config dir and fake home).
     */
    public PluginPaths(Path configDir, Path home) {
        this.configDir = configDir;
        this.home = home;
    }

    /**
     * Ordered candidates for {@code <home>/...}. Public for tests.
     */
    public static List<Path> candidates(Path home) {
        String user = home.getFileName() == null ? "" : home.getFileName().toString();
        List<Path> result = new ArrayList<>();
        // Native client docs folder (Windows layout, also valid for native installs).
        result.add(home.resolve("Documents/The Lord of the Rings Online/Plugins"));
        // Steam / Proton.
        result.add(home.resolve(".steam/steam/steamapps/compatdata/" + LOTRO_STEAM_APP_ID
                + "/pfx/drive_c/users/steamuser/Documents/The Lord of the Rings Online/Plugins"));
        result.add(home.resolve(".local/share/Steam/steamapps/compatdata/" + LOTRO_STEAM_APP_ID
                + "/pfx/drive_c/users/steamuser/Documents/The Lord of the Rings Online/Plugins"));
        // Plain Wine prefixes.
        result.add(home.resolve(".wine/drive_c/users/" + user + "/Documents/The Lord of the Rings Online/Plugins"));
        result.add(home.resolve(".wine/drive_c/users/" + user + "/My Documents/The Lord of the Rings Online/Plugins"));
        return result;
    }

    /**
     * Returns the configured folder, or auto-detects and remembers the first hit.
     */
    public Path pluginsDir() throws IOException {
        Path configured = readConfigured();
        if (configured != null) {
            return configured;
        }
        List<Path> candidates = candidates(home);
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                setPluginsDir(candidate);
                return candidate;
            }
        }
        return null;
    }

    public void setPluginsDir(Path dir) throws IOException {
        AppSettings settings = loadSettings();
        saveSettings(settings.withPluginsDir(dir.toString()));
    }

    /**
     * Saved UI theme id ("light"/"dark"), or null if never chosen.
     */
    public String theme() throws IOException {
        return loadSettings().theme();
    }

    public void setTheme(String theme) throws IOException {
        AppSettings settings = loadSettings();
        saveSettings(settings.withTheme(theme));
    }

    public Path configDir() {
        return configDir;
    }

    /**
     * Last window bounds as {x, y, width, height}, or null if never saved or invalid.
     */
    public double[] windowBounds() throws IOException {
        AppSettings.WindowBounds window = loadSettings().window();
        if (window == null || window.w() < 400 || window.h() < 300) {
            return null;
        }
        return new double[]{window.x(), window.y(), window.w(), window.h()};
    }

    public void setWindowBounds(double x, double y, double w, double h) throws IOException {
        AppSettings settings = loadSettings();
        saveSettings(settings.withWindow(x, y, w, h));
    }

    private AppSettings loadSettings() throws IOException {
        Path file = configDir.resolve(SETTINGS_FILE);
        if (Files.isRegularFile(file)) {
            AppSettings settings = yaml.readValue(file.toFile(), AppSettings.class);
            return settings == null ? new AppSettings() : settings;
        }
        AppSettings migrated = migrateLegacy();
        if (migrated != null) {
            return migrated;
        }
        return new AppSettings();
    }

    private void saveSettings(AppSettings settings) throws IOException {
        Files.createDirectories(configDir);
        yaml.writerWithDefaultPrettyPrinter().writeValue(configDir.resolve(SETTINGS_FILE).toFile(), settings);
    }

    /**
     * One-time import from the old properties file; removes it afterwards.
     */
    private AppSettings migrateLegacy() throws IOException {
        Path legacy = configDir.resolve(LEGACY_SETTINGS_FILE);
        if (!Files.isRegularFile(legacy)) {
            return null;
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(legacy)) {
            props.load(in);
        }
        AppSettings settings = new AppSettings();
        String dir = props.getProperty("plugins.dir");
        if (dir != null && !dir.isBlank()) {
            settings = settings.withPluginsDir(dir.trim());
        }
        try {
            settings = settings.withWindow(
                    Double.parseDouble(props.getProperty("window.x", "")),
                    Double.parseDouble(props.getProperty("window.y", "")),
                    Double.parseDouble(props.getProperty("window.w", "")),
                    Double.parseDouble(props.getProperty("window.h", "")));
        } catch (NumberFormatException e) {
            // no usable bounds in the legacy file
        }
        saveSettings(settings);
        Files.deleteIfExists(legacy);
        return settings;
    }

    private Path readConfigured() throws IOException {
        String value = loadSettings().pluginsDir();
        if (value == null || value.isBlank()) {
            return null;
        }
        Path dir = Path.of(value.trim());
        return Files.isDirectory(dir) ? dir : null;
    }
}
