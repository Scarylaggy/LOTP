package com.bs.lotp.desktop.local;

import com.bs.lotp.desktop.settings.PluginPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Persists which feed plugins are installed locally: feed version + update
 * timestamp at install time plus the top-level folders the zip created, so
 * uninstall can remove exactly what was installed.
 */
@Service
public class InstalledStore {

    public record InstalledEntry(
            long uid,
            String name,
            String version,
            long feedUpdated,
            Instant installedAt,
            List<String> topLevelDirs) {
    }

    private static final TypeReference<Map<Long, InstalledEntry>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper json = new ObjectMapper();
    private final Path stateFile;

    @Autowired
    public InstalledStore(PluginPaths paths) {
        this(paths.configDir());
    }

    public InstalledStore(Path configDir) {
        this.stateFile = configDir.resolve("installed.json");
    }

    public synchronized Map<Long, InstalledEntry> load() throws IOException {
        if (!Files.isRegularFile(stateFile)) {
            return new LinkedHashMap<>();
        }
        Map<Long, InstalledEntry> map = json.readValue(stateFile.toFile(), MAP_TYPE);
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    public synchronized void save(Map<Long, InstalledEntry> entries) throws IOException {
        Files.createDirectories(stateFile.getParent());
        json.writerWithDefaultPrettyPrinter().writeValue(stateFile.toFile(), entries);
    }

    public synchronized void put(InstalledEntry entry) throws IOException {
        Map<Long, InstalledEntry> entries = load();
        entries.put(entry.uid(), entry);
        save(entries);
    }

    public synchronized void remove(long uid) throws IOException {
        Map<Long, InstalledEntry> entries = load();
        entries.remove(uid);
        save(entries);
    }

    public static List<String> copyOf(List<String> dirs) {
        return new ArrayList<>(dirs);
    }
}
