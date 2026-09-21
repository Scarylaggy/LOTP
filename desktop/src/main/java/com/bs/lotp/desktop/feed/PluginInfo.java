package com.bs.lotp.desktop.feed;

import java.time.Instant;

/**
 * One entry from the lotrointerface plugin compendium feed.
 */
public record PluginInfo(
        long uid,
        String name,
        String author,
        String version,
        Instant updated,
        long downloads,
        String category,
        String description,
        String fileName,
        String md5,
        long size,
        String fileUrl) {

    public String updatedText() {
        return updated == null ? "?" : updated.toString().substring(0, 10);
    }

    public String sizeText() {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        }
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
}
