package com.bs.lotp.desktop.ui.browser;

import com.bs.lotp.desktop.feed.PluginFeedService;
import com.bs.lotp.desktop.feed.PluginInfo;
import com.bs.lotp.desktop.local.InstalledStore;
import com.bs.lotp.desktop.local.LocalLibrary;
import com.bs.lotp.desktop.local.PluginInstaller;
import com.bs.lotp.desktop.settings.PluginPaths;
import com.bs.lotp.desktop.ui.Status;
import com.bs.lotp.desktop.ui.UiTheme;
import com.bs.lotp.desktop.ui.browser.PluginBrowserView.Handlers;
import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import com.bs.lotp.desktop.ui.plugintable.PluginTablePresenter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Presenter of the plugin browser: owns the {@link PluginBrowserModel}, builds
 * the {@link PluginBrowserView} (plus the embedded plugin-table MVP triad),
 * and implements every workflow — feed refresh, install/update/uninstall,
 * folder resolution, and theming. All network/disk work runs off the FX
 * thread. Construct freely; {@link #build()} must run on the FX thread since
 * it builds controls.
 */
public class PluginBrowserPresenter {

    @FunctionalInterface
    interface Job<T> {
        T run() throws Exception;
    }

    private final Stage owner;
    private final PluginFeedService feed;
    private final PluginInstaller installer;
    private final InstalledStore store;
    private final PluginPaths paths;
    private final PluginBrowserModel model = new PluginBrowserModel();

    private PluginBrowserView view;
    private PluginTablePresenter table;
    private UiTheme theme;

    public PluginBrowserPresenter(
            Stage owner,
            PluginFeedService feed,
            PluginInstaller installer,
            InstalledStore store,
            PluginPaths paths) {
        this.owner = owner;
        this.feed = feed;
        this.installer = installer;
        this.store = store;
        this.paths = paths;
        this.theme = loadTheme();
    }

    public UiTheme currentTheme() {
        return theme;
    }

    /** Switches the active theme, updates the live scene, and persists the choice. */
    public void applyTheme(UiTheme next) {
        if (next == null) {
            return;
        }
        theme = next;
        if (view != null) {
            view.setThemeToggleText(theme.toggleText());
        }
        Scene scene = owner == null ? null : owner.getScene();
        if (scene != null) {
            scene.getStylesheets().setAll(theme.stylesheet());
        }
        CompletableFuture.runAsync(() -> {
            try {
                paths.setTheme(theme.id());
            } catch (IOException e) {
                System.err.println("lotp: could not save theme: " + e.getMessage());
            }
        });
    }

    public BorderPane build() {
        table = new PluginTablePresenter();
        view = new PluginBrowserView(owner, table.node(), model, new Handlers(
                this::refresh,
                () -> selected().ifPresent(this::install),
                () -> selected().ifPresent(this::update),
                () -> selected().ifPresent(row -> {
                    if (view.confirmUninstall(
                            row.plugin().name(), row.installed().topLevelDirs())) {
                        uninstall(row);
                    }
                }),
                this::chooseFolder,
                () -> applyTheme(theme == UiTheme.LIGHT ? UiTheme.DARK : UiTheme.LIGHT),
                table::setSearchQuery,
                table::setCategoryFilter));
        view.setThemeToggleText(theme.toggleText());
        table.onSelectionChanged(this::onSelection);
        view.showDetail("Select a plugin", "");
        updateButtonStates();
        resolveFolder();
        return view.root();
    }

    /** First load; safe to call once when the view is first shown. */
    public void refreshIfEmpty() {
        if (table != null && table.isEmpty()) {
            refresh();
        }
    }

    // --- Workflows ---

    private void onSelection(PluginRow row) {
        if (view == null) {
            return;
        }
        if (row == null) {
            view.showDetail("Select a plugin", "");
        } else {
            view.showDetail(row.plugin().name() + " " + row.plugin().version()
                    + "  ·  " + row.plugin().author(), row.plugin().description());
        }
        updateButtonStates();
    }

    private void refresh() {
        setBusy(true, "Downloading plugin list...");
        runAsync(feed::fetch, plugins -> {
            try {
                Map<Long, InstalledStore.InstalledEntry> installed = reconcile(plugins);
                List<PluginRow> fresh = plugins.stream()
                        .sorted(Comparator.comparing(PluginInfo::name, String.CASE_INSENSITIVE_ORDER))
                        .map(p -> PluginRow.of(p, installed.get(p.uid())))
                        .toList();
                table.setRows(fresh);
                TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                plugins.forEach(p -> {
                    if (!p.category().isBlank()) {
                        categories.add(p.category());
                    }
                });
                view.setCategories(List.copyOf(categories), view.selectedCategory());
                model.setStatusText(plugins.size() + " plugins, " + installed.size() + " installed.");
                System.out.println("lotp: loaded " + plugins.size() + " plugins from feed");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            setBusy(false, null);
            updateButtonStates();
        }, "Refreshing plugin list");
    }

    private void install(PluginRow row) {
        if (!ensureFolder()) {
            return;
        }
        Path pluginsDir = model.getPluginsDir();
        setBusy(true, "Installing " + row.plugin().name() + "...");
        runAsync(() -> {
            Path tmp = paths.configDir().resolve("downloads");
            Path zip = installer.download(row.plugin(), tmp);
            try {
                List<String> top = installer.installZip(zip, pluginsDir);
                InstalledStore.InstalledEntry entry = installer.recordInstall(row.plugin(), top);
                store.put(entry);
                return entry;
            } finally {
                Files.deleteIfExists(zip);
            }
        }, entry -> {
            table.replaceRow(new PluginRow(row.plugin(), entry, Status.INSTALLED));
            setBusy(false, null);
            model.setStatusText("Installed " + row.plugin().name() + " " + row.plugin().version());
            updateButtonStates();
        }, "Installing " + row.plugin().name());
    }

    private void update(PluginRow row) {
        if (!ensureFolder() || row.installed() == null) {
            return;
        }
        Path pluginsDir = model.getPluginsDir();
        setBusy(true, "Updating " + row.plugin().name() + "...");
        runAsync(() -> {
            installer.uninstall(pluginsDir, row.installed());
            Path tmp = paths.configDir().resolve("downloads");
            Path zip = installer.download(row.plugin(), tmp);
            try {
                List<String> top = installer.installZip(zip, pluginsDir);
                InstalledStore.InstalledEntry entry = installer.recordInstall(row.plugin(), top);
                store.put(entry);
                return entry;
            } finally {
                Files.deleteIfExists(zip);
            }
        }, entry -> {
            table.replaceRow(new PluginRow(row.plugin(), entry, Status.INSTALLED));
            setBusy(false, null);
            model.setStatusText("Updated " + row.plugin().name() + " to " + row.plugin().version());
            updateButtonStates();
        }, "Updating " + row.plugin().name());
    }

    private void uninstall(PluginRow row) {
        if (row.installed() == null) {
            return;
        }
        Path pluginsDir = model.getPluginsDir();
        setBusy(true, "Uninstalling " + row.plugin().name() + "...");
        runAsync(() -> {
            if (pluginsDir != null) {
                installer.uninstall(pluginsDir, row.installed());
            }
            store.remove(row.plugin().uid());
            return row.plugin();
        }, plugin -> {
            table.replaceRow(new PluginRow(plugin, null, Status.NOT_INSTALLED));
            setBusy(false, null);
            model.setStatusText("Uninstalled " + plugin.name());
            updateButtonStates();
        }, "Uninstalling " + row.plugin().name());
    }

    /**
     * Trusts the disk: drops store entries whose folders are gone, adopts
     * plugins found locally but installed outside the app, and syncs the
     * stored version when files were changed by hand.
     */
    private Map<Long, InstalledStore.InstalledEntry> reconcile(List<PluginInfo> plugins) throws IOException {
        Map<Long, InstalledStore.InstalledEntry> installed = store.load();
        Path pluginsDir = model.getPluginsDir();
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return installed;
        }
        boolean changed = false;
        List<LocalLibrary.LocalPlugin> local = LocalLibrary.scan(pluginsDir);

        var stale = installed.values().stream()
                .filter(e -> e.topLevelDirs().stream()
                        .noneMatch(d -> Files.exists(pluginsDir.resolve(d))))
                .map(InstalledStore.InstalledEntry::uid)
                .toList();
        for (long uid : stale) {
            installed.remove(uid);
            changed = true;
        }

        for (PluginInfo plugin : plugins) {
            var match = LocalLibrary.matchFor(plugin, local);
            if (match.isEmpty()) {
                continue;
            }
            LocalLibrary.LocalPlugin found = match.get();
            InstalledStore.InstalledEntry existing = installed.get(plugin.uid());
            if (existing == null) {
                long feedEpoch = plugin.updated() == null ? 0 : plugin.updated().getEpochSecond();
                installed.put(plugin.uid(), new InstalledStore.InstalledEntry(
                        plugin.uid(), plugin.name(), found.version(), feedEpoch,
                        java.time.Instant.now(), List.of(found.topLevelDir())));
                changed = true;
            } else if (!found.version().equals(existing.version())) {
                installed.put(plugin.uid(), new InstalledStore.InstalledEntry(
                        existing.uid(), existing.name(), found.version(), existing.feedUpdated(),
                        existing.installedAt(), existing.topLevelDirs()));
                changed = true;
            }
        }
        if (changed) {
            store.save(installed);
        }
        return installed;
    }

    // --- Helpers ---

    private Optional<PluginRow> selected() {
        return Optional.ofNullable(table == null ? null : table.selected());
    }

    private void updateButtonStates() {
        if (view == null) {
            return;
        }
        PluginRow row = table == null ? null : table.selected();
        boolean hasFolder = model.getPluginsDir() != null;
        view.setActionsEnabled(
                row != null && row.status() == Status.NOT_INSTALLED && hasFolder,
                row != null && row.status() == Status.UPDATE_AVAILABLE && hasFolder,
                row != null && row.status() != Status.NOT_INSTALLED);
    }

    private boolean ensureFolder() {
        if (model.getPluginsDir() == null) {
            chooseFolder();
        }
        return model.getPluginsDir() != null;
    }

    private void resolveFolder() {
        runAsync(paths::pluginsDir, dir -> {
            model.setPluginsDir(dir);
            model.setFolderText(dir == null ? "Plugins folder: not found" : "Plugins: " + dir);
            updateButtonStates();
        }, "Locating Plugins folder");
    }

    private void chooseFolder() {
        if (view == null) {
            return;
        }
        view.chooseFolder().ifPresent(chosen -> runAsync(() -> {
            paths.setPluginsDir(chosen);
            return chosen;
        }, dir -> {
            model.setPluginsDir(dir);
            model.setFolderText("Plugins: " + dir);
            model.setStatusText("Plugins folder set to " + dir);
            updateButtonStates();
        }, "Saving Plugins folder"));
    }

    private UiTheme loadTheme() {
        try {
            return UiTheme.from(paths.theme());
        } catch (Exception e) {
            return UiTheme.LIGHT;
        }
    }

    private <T> void runAsync(Job<T> job, Consumer<T> onOk, String action) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return job.run();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenAcceptAsync(onOk, Platform::runLater).exceptionallyAsync(ex -> {
            Throwable cause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
            System.err.println("lotp: " + action + " failed: " + cause);
            setBusy(false, null);
            model.setStatusText(action + " failed: " + cause.getMessage());
            if (view != null) {
                view.showError(action, cause == null ? null : String.valueOf(cause.getMessage()));
            }
            return null;
        }, Platform::runLater);
    }

    private void setBusy(boolean busy, String message) {
        if (Platform.isFxApplicationThread()) {
            model.setProgress(busy ? -1 : 0);
            if (message != null) {
                model.setStatusText(message);
            }
        } else {
            Platform.runLater(() -> setBusy(busy, message));
        }
    }
}
