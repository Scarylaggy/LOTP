package com.bs.lotp.desktop.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.RestClient;

class PluginInstallerTest {

    private final PluginInstaller installer = new PluginInstaller(RestClient.builder());

    private static void addEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes());
        zip.closeEntry();
    }

    private static Path makeZip(Path dir, String... entries) throws Exception {
        Path zip = dir.resolve("plugin.zip");
        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (String entry : entries) {
                if (entry.endsWith("/")) {
                    zipOut.putNextEntry(new ZipEntry(entry));
                    zipOut.closeEntry();
                } else {
                    addEntry(zipOut, entry, "data:" + entry);
                }
            }
        }
        return zip;
    }

    @Test
    void installsAndUninstalls(@TempDir Path temp) throws Exception {
        Path pluginsDir = temp.resolve("Plugins");
        Path zip = makeZip(temp, "Nolemir/", "Nolemir/BrokeLegs/", "Nolemir/BrokeLegs/Main.lua");

        List<String> top = installer.installZip(zip, pluginsDir);

        assertEquals(List.of("Nolemir"), top);
        assertTrue(Files.isRegularFile(pluginsDir.resolve("Nolemir/BrokeLegs/Main.lua")));

        InstalledStore.InstalledEntry entry = new InstalledStore.InstalledEntry(
                1, "Broke Legs", "1.0.2", 0, java.time.Instant.now(), top);
        installer.uninstall(pluginsDir, entry);
        assertFalse(Files.exists(pluginsDir.resolve("Nolemir")));
    }

    @Test
    void rejectsZipSlip(@TempDir Path temp) throws Exception {
        Path pluginsDir = temp.resolve("Plugins");
        Path zip = makeZip(temp, "../evil.txt", "ok/");
        assertThrows(java.io.IOException.class, () -> installer.installZip(zip, pluginsDir));
        assertFalse(Files.exists(temp.resolve("evil.txt")));
    }

    @Test
    void looseFilesTrackedByName(@TempDir Path temp) throws Exception {
        Path pluginsDir = temp.resolve("Plugins");
        Path zip = makeZip(temp, "readme.txt");
        List<String> top = installer.installZip(zip, pluginsDir);
        assertEquals(List.of("readme.txt"), top);
    }
}
