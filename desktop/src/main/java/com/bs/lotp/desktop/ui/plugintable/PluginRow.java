package com.bs.lotp.desktop.ui.plugintable;

import com.bs.lotp.desktop.feed.PluginInfo;
import com.bs.lotp.desktop.local.InstalledStore;
import com.bs.lotp.desktop.local.LocalLibrary;
import com.bs.lotp.desktop.ui.Status;
import java.util.ArrayList;
import java.util.List;

/**
 * One row of the plugin table: feed entry + local install state + derived status.
 */
public record PluginRow(PluginInfo plugin, InstalledStore.InstalledEntry installed, Status status) {

    /**
     * Builds a row, deriving the status from feed timestamps/versions vs. the install record.
     */
    public static PluginRow of(PluginInfo plugin, InstalledStore.InstalledEntry installed) {
        return new PluginRow(plugin, installed, statusOf(plugin, installed));
    }

    static Status statusOf(PluginInfo plugin, InstalledStore.InstalledEntry installed) {
        if (installed == null) {
            return Status.NOT_INSTALLED;
        }
        long feedUpdated = plugin.updated() == null ? 0 : plugin.updated().getEpochSecond();
        if (feedUpdated > installed.feedUpdated()
                || !LocalLibrary.normalizedVersion(installed.version())
                .equals(LocalLibrary.normalizedVersion(plugin.version()))) {
            return Status.UPDATE_AVAILABLE;
        }
        return Status.INSTALLED;
    }

    /**
     * Version-aware string comparison for sorting ("1.9" &lt; "1.10", "v1.45" == "1.45").
     * Splits into digit/non-digit runs: numbers compare numerically, the rest
     * case-insensitively. Toolkit-free.
     */
    public static int compareVersions(String a, String b) {
        List<String> left = segments(a);
        List<String> right = segments(b);
        int n = Math.min(left.size(), right.size());
        for (int i = 0; i < n; i++) {
            int cmp = compareSegment(left.get(i), right.get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private static List<String> segments(String version) {
        String v = version == null ? "" : version.trim();
        while (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean digits = false;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            boolean digit = Character.isDigit(c);
            if (current.length() > 0 && digit != digits) {
                parts.add(current.toString());
                current.setLength(0);
            }
            digits = digit;
            current.append(digit ? c : Character.toLowerCase(c));
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    private static int compareSegment(String a, String b) {
        boolean aDigits = !a.isEmpty() && Character.isDigit(a.charAt(0));
        boolean bDigits = !b.isEmpty() && Character.isDigit(b.charAt(0));
        if (aDigits && bDigits) {
            // Length first: avoids overflow and ignores leading zeros.
            int cmp = Integer.compare(a.length(), b.length());
            return cmp != 0 ? cmp : a.compareTo(b);
        }
        if (aDigits != bDigits) {
            return aDigits ? -1 : 1;
        }
        int cmp = String.CASE_INSENSITIVE_ORDER.compare(a, b);
        return cmp != 0 ? cmp : a.compareTo(b);
    }
}
