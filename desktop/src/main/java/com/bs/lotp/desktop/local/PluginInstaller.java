package com.bs.lotp.desktop.local;

import com.bs.lotp.desktop.feed.PluginInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads plugin zips and extracts them into the LOTRO Plugins folder. Call off the FX thread. */
@Service
public class PluginInstaller {

    private final RestClient client;

    public PluginInstaller(RestClient.Builder builder) {
        this.client = builder.build();
    }

    public Path download(PluginInfo plugin, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        String fileName = plugin.fileName().isBlank() ? "plugin-" + plugin.uid() + ".zip" : plugin.fileName();
        Path dest = destDir.resolve(sanitize(fileName));
        byte[] data;
        try {
            data = client.get().uri(plugin.fileUrl()).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw new IOException("Download failed: " + e.getMessage(), e);
        }
        if (data == null || data.length == 0) {
            throw new IOException("Download returned an empty body");
        }
        Files.write(dest, data);
        if (plugin.size() > 0 && Files.size(dest) != plugin.size()) {
            Files.deleteIfExists(dest);
            throw new IOException("Size mismatch: expected " + plugin.size() + " bytes");
        }
        if (!plugin.md5().isBlank()) {
            verifyMd5(dest, plugin.md5());
        }
        return dest;
    }

    /**
     * Extracts {@code zip} into {@code pluginsDir}. Returns the top-level
     * directory names created (for later uninstall). Zip-Slip entries are rejected.
     */
    public List<String> installZip(Path zip, Path pluginsDir) throws IOException {
        Files.createDirectories(pluginsDir);
        List<String> topLevel = new ArrayList<>();
        try (InputStream in = Files.newInputStream(zip);
                ZipInputStream zipIn = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                Path target = pluginsDir.resolve(name).normalize();
                if (!target.startsWith(pluginsDir.normalize())) {
                    throw new IOException("Refusing to extract outside plugins dir: " + name);
                }
                String top = topSegment(name);
                if (top != null && !topLevel.contains(top)) {
                    topLevel.add(top);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (OutputStream out = Files.newOutputStream(target)) {
                        zipIn.transferTo(out);
                    }
                }
                zipIn.closeEntry();
            }
        }
        return topLevel;
    }

    /** Deletes previously installed top-level dirs (deepest first), then drops the state entry. */
    public void uninstall(Path pluginsDir, InstalledStore.InstalledEntry installed) throws IOException {
        List<String> dirs = new ArrayList<>(installed.topLevelDirs());
        dirs.sort(Comparator.comparingInt(String::length).reversed());
        for (String dir : dirs) {
            Path target = pluginsDir.resolve(dir).normalize();
            if (!target.startsWith(pluginsDir.normalize()) || !Files.exists(target)) {
                continue;
            }
            if (Files.isDirectory(target)) {
                deleteTree(target);
            } else {
                Files.deleteIfExists(target);
            }
        }
    }

    public InstalledStore.InstalledEntry recordInstall(
            PluginInfo plugin, List<String> topLevelDirs) {
        return new InstalledStore.InstalledEntry(
                plugin.uid(), plugin.name(), plugin.version(),
                plugin.updated() == null ? 0 : plugin.updated().getEpochSecond(),
                Instant.now(), topLevelDirs);
    }

    private static void verifyMd5(Path file, String expected) throws IOException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    md5.update(buf, 0, read);
                }
            }
            String actual = HexFormat.of().formatHex(md5.digest());
            if (!actual.equalsIgnoreCase(expected.trim())) {
                throw new IOException("MD5 mismatch for " + file.getFileName());
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("MD5 check failed: " + e.getMessage(), e);
        }
    }

    private static String topSegment(String name) {
        String trimmed = name.startsWith("/") ? name.substring(1) : name;
        int slash = trimmed.indexOf('/');
        if (slash == -1) {
            return trimmed.isEmpty() ? null : trimmed;
        }
        String top = trimmed.substring(0, slash);
        return top.isEmpty() ? null : top;
    }

    private static String sanitize(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static void deleteTree(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
