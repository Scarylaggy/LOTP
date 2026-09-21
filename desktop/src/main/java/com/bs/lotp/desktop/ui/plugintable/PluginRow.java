package com.bs.lotp.desktop.ui.plugintable;

import com.bs.lotp.desktop.feed.PluginInfo;
import com.bs.lotp.desktop.local.InstalledStore;
import com.bs.lotp.desktop.local.LocalLibrary;
import com.bs.lotp.desktop.ui.Status;

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
}
