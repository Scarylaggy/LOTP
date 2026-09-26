package com.bs.lotp.desktop.ui;

import com.bs.lotp.desktop.feed.PluginFeedService;
import com.bs.lotp.desktop.local.InstalledStore;
import com.bs.lotp.desktop.local.PluginInstaller;
import com.bs.lotp.desktop.settings.PluginPaths;
import com.bs.lotp.desktop.ui.browser.PluginBrowserPresenter;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Browse/search the lotrointerface feed, install/update/uninstall into the LOTRO Plugins folder.
 *
 * <p>Thin facade over the browser MVP triad ({@code ui.browser}): the
 * presenter owns all behavior, the view builds the controls, the model holds
 * the display state. Kept so {@code DesktopApp} wiring stays unchanged.
 */
public class PluginsView {

    private final PluginBrowserPresenter presenter;

    public PluginsView(
            Stage owner,
            PluginFeedService feed,
            PluginInstaller installer,
            InstalledStore store,
            PluginPaths paths) {
        this.presenter = new PluginBrowserPresenter(owner, feed, installer, store, paths);
    }

    public UiTheme currentTheme() {
        return presenter.currentTheme();
    }

    public BorderPane build() {
        return presenter.build();
    }

    /** First load; safe to call once when the tab is first shown. */
    public void refreshIfEmpty() {
        presenter.refreshIfEmpty();
    }
}
