package com.bs.lotp.desktop.local;

import com.bs.lotp.desktop.feed.PluginInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads the plugins actually present in the LOTRO Plugins folder by parsing
 * their {@code .plugin} descriptors, and matches them against feed entries.
 * Feed names rarely equal folder names ("Deed Tracker" vs "DeedTracker"),
 * so matching is done on normalized descriptor names, not paths.
 */
public final class LocalLibrary {

    /** One {@code .plugin} descriptor found on disk. */
    public record LocalPlugin(
            String name,
            String author,
            String version,
            /** Top-level entry (dir or file) relative to the Plugins folder. */
            String topLevelDir) {
    }

    private LocalLibrary() {
    }

    /** Walks {@code pluginsDir} for {@code *.plugin} files. Never throws for a missing dir. */
    public static List<LocalPlugin> scan(Path pluginsDir) {
        List<LocalPlugin> result = new ArrayList<>();
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return result;
        }
        try (var stream = Files.walk(pluginsDir)) {
            for (Path file : stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".plugin"))
                    .toList()) {
                parse(file, pluginsDir.normalize()).ifPresent(result::add);
            }
        } catch (IOException e) {
            System.err.println("lotp: plugin folder scan failed: " + e.getMessage());
        }
        return result;
    }

    /** Best local match for a feed entry, preferring an author match on ties. */
    public static Optional<LocalPlugin> matchFor(PluginInfo plugin, List<LocalPlugin> local) {
        String wanted = normalized(plugin.name());
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        List<LocalPlugin> hits = local.stream()
                .filter(l -> normalized(l.name()).equals(wanted))
                .toList();
        if (hits.isEmpty()) {
            return Optional.empty();
        }
        if (hits.size() == 1) {
            return Optional.of(hits.get(0));
        }
        String author = normalized(plugin.author());
        return hits.stream()
                .filter(l -> !author.isEmpty() && normalized(l.author()).contains(author)
                        || !normalized(l.author()).isEmpty() && author.contains(normalized(l.author())))
                .findFirst()
                .or(() -> Optional.of(hits.get(0)));
    }

    /** Normalizes names for comparison: lowercase, alphanumeric only ("Deed Tracker" = "DeedTracker"). */
    static String normalized(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Normalizes versions ("v1.45" = "1.45"). */
    public static String normalizedVersion(String value) {
        String n = normalized(value);
        return n.startsWith("v") ? n.substring(1) : n;
    }

    private static Optional<LocalPlugin> parse(Path file, Path pluginsDir) {
        String name = null, author = "", version = "";
        try (InputStream in = Files.newInputStream(file)) {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            StringBuilder text = new StringBuilder();
            String element = null;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    element = reader.getLocalName();
                    text.setLength(0);
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    text.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals(element)) {
                    String value = text.toString().trim();
                    switch (reader.getLocalName()) {
                        case "Name" -> name = value;
                        case "Author" -> author = value;
                        case "Version" -> version = value;
                        default -> {
                        }
                    }
                    element = null;
                    text.setLength(0);
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("lotp: skipping unreadable descriptor " + file.getFileName() + ": " + e.getMessage());
            return Optional.empty();
        }
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Path relative = pluginsDir.relativize(file.toAbsolutePath().normalize());
        String top = relative.getNameCount() == 1
                ? relative.getFileName().toString()
                : relative.getName(0).toString();
        return Optional.of(new LocalPlugin(name.trim(), author.trim(), version.trim(), top));
    }
}
