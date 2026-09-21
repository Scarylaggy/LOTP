package com.bs.lotp.desktop.plugins;

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
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/** Browse/search the lotrointerface feed, install/update/uninstall into the LOTRO Plugins folder. */
public class PluginsView {

    enum Status {
        NOT_INSTALLED("Not installed"),
        INSTALLED("Installed"),
        UPDATE_AVAILABLE("Update available");

        final String text;

        Status(String text) {
            this.text = text;
        }
    }

    record Row(PluginInfo plugin, InstalledStore.InstalledEntry installed, Status status) {
    }

    @FunctionalInterface
    interface Job<T> {
        T run() throws Exception;
    }

    private final Stage owner;
    private final PluginFeedService feed;
    private final PluginInstaller installer;
    private final InstalledStore store;
    private final PluginPaths paths;

    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final StringProperty statusText = new SimpleStringProperty("Press Refresh to load the plugin list.");
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty folderText = new SimpleStringProperty("");

    private Path pluginsDir;
    private TableView<Row> table;
    private TextArea detail;
    private Label detailTitle;
    private Button installBtn;
    private Button updateBtn;
    private Button uninstallBtn;
    private Button themeBtn;
    private ComboBox<String> categoryBox;
    private UiTheme theme;

    public PluginsView(
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
        if (themeBtn != null) {
            themeBtn.setText(theme.toggleText());
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

    private UiTheme loadTheme() {
        try {
            return UiTheme.from(paths.theme());
        } catch (Exception e) {
            return UiTheme.LIGHT;
        }
    }

    public BorderPane build() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("plugin-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        FilteredList<Row> filtered = new FilteredList<>(rows, r -> true);
        table.setItems(filtered);

        TableColumn<Row, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().plugin().name()));
        nameCol.setPrefWidth(220);

        TableColumn<Row, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().plugin().author()));
        authorCol.setPrefWidth(130);

        TableColumn<Row, String> versionCol = new TableColumn<>("Version");
        versionCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().plugin().version()
                        + (c.getValue().installed() == null ? "" : " / " + c.getValue().installed().version())));
        versionCol.setPrefWidth(130);

        TableColumn<Row, String> updatedCol = new TableColumn<>("Updated");
        updatedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().plugin().updatedText()));
        updatedCol.setPrefWidth(100);

        TableColumn<Row, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().plugin().category()));
        categoryCol.setPrefWidth(170);

        TableColumn<Row, Number> downloadsCol = new TableColumn<>("Downloads");
        downloadsCol.setCellValueFactory(c -> Bindings.createIntegerBinding(
                () -> (int) Math.min(c.getValue().plugin().downloads(), Integer.MAX_VALUE)));
        downloadsCol.setPrefWidth(100);

        TableColumn<Row, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().plugin().sizeText()));
        sizeCol.setPrefWidth(90);

        TableColumn<Row, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().text));
        statusCol.setPrefWidth(140);
        statusCol.setCellFactory(col -> new TableCell<Row, String>() {
            private final Label pill = new Label();

            {
                pill.getStyleClass().add("pill");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Row row = (Row) getTableRow().getItem();
                pill.setText(row.status().text);
                pill.getStyleClass().removeAll("pill-none", "pill-installed", "pill-update");
                switch (row.status()) {
                    case INSTALLED -> pill.getStyleClass().add("pill-installed");
                    case UPDATE_AVAILABLE -> pill.getStyleClass().add("pill-update");
                    default -> pill.getStyleClass().add("pill-none");
                }
                setGraphic(pill);
                setText(null);
            }
        });

        table.getColumns().add(nameCol);
        table.getColumns().add(authorCol);
        table.getColumns().add(versionCol);
        table.getColumns().add(updatedCol);
        table.getColumns().add(categoryCol);
        table.getColumns().add(downloadsCol);
        table.getColumns().add(sizeCol);
        table.getColumns().add(statusCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row == null) {
                detailTitle.setText("Select a plugin");
                detail.setText("");
            } else {
                detailTitle.setText(row.plugin().name() + " " + row.plugin().version()
                        + "  ·  " + row.plugin().author());
                detail.setText(row.plugin().description());
            }
            updateButtonStates();
        });

        // --- Header ---
        Label title = new Label("Plugins");
        title.getStyleClass().add("app-title");

        Button refreshBtn = new Button("⟳ Refresh");
        refreshBtn.getStyleClass().add("primary-button");
        refreshBtn.setOnAction(e -> refresh());

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search name, author, description...");
        searchField.setPrefWidth(260);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter(filtered, searchField, categoryBox));

        categoryBox = new ComboBox<>();
        categoryBox.getItems().add("All categories");
        categoryBox.getSelectionModel().selectFirst();
        categoryBox.setPrefWidth(200);
        categoryBox.valueProperty().addListener((obs, o, n) -> applyFilter(filtered, searchField, categoryBox));

        themeBtn = new Button(theme.toggleText());
        themeBtn.getStyleClass().add("ghost-button");
        themeBtn.setOnAction(e -> applyTheme(theme == UiTheme.LIGHT ? UiTheme.DARK : UiTheme.LIGHT));

        Button folderBtn = new Button("Plugins folder...");
        folderBtn.setOnAction(e -> chooseFolder());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerBar = new HBox(8, title, searchField, categoryBox, spacer, themeBtn, folderBtn, refreshBtn);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.getStyleClass().add("toolbar");

        Label folderLabel = new Label();
        folderLabel.textProperty().bind(folderText);
        folderLabel.getStyleClass().add("muted");
        HBox folderBar = new HBox(folderLabel);
        folderBar.setAlignment(Pos.CENTER_LEFT);
        folderBar.setPadding(new Insets(4, 12, 6, 14));
        folderBar.setStyle("-fx-background-color: transparent;");

        VBox header = new VBox(headerBar, folderBar);

        // --- Detail + actions ---
        detailTitle = new Label("Select a plugin");
        detailTitle.getStyleClass().add("detail-title");

        detail = new TextArea();
        detail.setEditable(false);
        detail.setWrapText(true);
        detail.setPrefRowCount(6);
        detail.getStyleClass().add("detail-area");
        VBox.setVgrow(detail, Priority.ALWAYS);

        installBtn = new Button("⬇ Install");
        installBtn.getStyleClass().add("primary-button");
        installBtn.setOnAction(e -> selected().ifPresent(this::install));

        updateBtn = new Button("⬆ Update");
        updateBtn.getStyleClass().add("accent-button");
        updateBtn.setOnAction(e -> selected().ifPresent(this::update));

        uninstallBtn = new Button("Uninstall");
        uninstallBtn.getStyleClass().add("danger-button");
        uninstallBtn.setOnAction(e -> selected().ifPresent(row -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Uninstall");
            confirm.setHeaderText("Uninstall " + row.plugin().name() + "?");
            confirm.setContentText("Removes from the Plugins directory:\n"
                    + String.join("\n", row.installed().topLevelDirs()));
            styleDialog(confirm);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                uninstall(row);
            }
        }));

        HBox actions = new HBox(8, installBtn, updateBtn, uninstallBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, detailTitle, detail, actions);
        card.getStyleClass().add("card");

        SplitPane split = new SplitPane(table, card);
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.62);

        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(progress);
        progressBar.setPrefWidth(150);

        Label status = new Label();
        status.textProperty().bind(statusText);

        HBox statusBar = new HBox(8, progressBar, status);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(header);
        root.setCenter(split);
        root.setBottom(statusBar);

        resolveFolder();
        updateButtonStates();
        return root;
    }

    /** First load; safe to call once when the tab is first shown. */
    public void refreshIfEmpty() {
        if (rows.isEmpty()) {
            refresh();
        }
    }

    // --- Actions (all network/disk work off the FX thread) ---

    private void refresh() {
        setBusy(true, "Downloading plugin list...");
        runAsync(feed::fetch, plugins -> {
            try {
                Map<Long, InstalledStore.InstalledEntry> installed = reconcile(plugins);
                List<Row> fresh = plugins.stream()
                        .sorted(Comparator.comparing(PluginInfo::name, String.CASE_INSENSITIVE_ORDER))
                        .map(p -> new Row(p, installed.get(p.uid()), statusOf(p, installed.get(p.uid()))))
                        .toList();
                rows.setAll(fresh);
                TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                plugins.forEach(p -> {
                    if (!p.category().isBlank()) {
                        categories.add(p.category());
                    }
                });
                String selectedCategory = categoryBox.getValue();
                categoryBox.getItems().setAll("All categories");
                categoryBox.getItems().addAll(categories);
                if (selectedCategory != null && categoryBox.getItems().contains(selectedCategory)) {
                    categoryBox.setValue(selectedCategory);
                }
                statusText.set(plugins.size() + " plugins, "
                        + installed.size() + " installed.");
                System.out.println("lotp: loaded " + plugins.size() + " plugins from feed");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            setBusy(false, null);
            updateButtonStates();
        }, "Refreshing plugin list");
    }

    private void install(Row row) {
        if (!ensureFolder()) {
            return;
        }
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
            replaceRow(new Row(row.plugin(), entry, Status.INSTALLED));
            setBusy(false, null);
            statusText.set("Installed " + row.plugin().name() + " " + row.plugin().version());
            updateButtonStates();
        }, "Installing " + row.plugin().name());
    }

    private void update(Row row) {
        if (!ensureFolder() || row.installed() == null) {
            return;
        }
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
            replaceRow(new Row(row.plugin(), entry, Status.INSTALLED));
            setBusy(false, null);
            statusText.set("Updated " + row.plugin().name() + " to " + row.plugin().version());
            updateButtonStates();
        }, "Updating " + row.plugin().name());
    }

    private void uninstall(Row row) {
        if (row.installed() == null) {
            return;
        }
        setBusy(true, "Uninstalling " + row.plugin().name() + "...");
        runAsync(() -> {
            if (pluginsDir != null) {
                installer.uninstall(pluginsDir, row.installed());
            }
            store.remove(row.plugin().uid());
            return row.plugin();
        }, plugin -> {
            replaceRow(new Row(plugin, null, Status.NOT_INSTALLED));
            setBusy(false, null);
            statusText.set("Uninstalled " + plugin.name());
            updateButtonStates();
        }, "Uninstalling " + row.plugin().name());
    }

    // --- Helpers ---

    /**
     * Trusts the disk: drops store entries whose folders are gone, adopts
     * plugins found locally but installed outside the app, and syncs the
     * stored version when files were changed by hand.
     */
    private Map<Long, InstalledStore.InstalledEntry> reconcile(List<PluginInfo> plugins) throws IOException {
        Map<Long, InstalledStore.InstalledEntry> installed = store.load();
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

    private static Status statusOf(PluginInfo plugin, InstalledStore.InstalledEntry installed) {
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

    private void replaceRow(Row replacement) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).plugin().uid() == replacement.plugin().uid()) {
                rows.set(i, replacement);
                table.getSelectionModel().select(i);
                return;
            }
        }
    }

    private Optional<Row> selected() {
        return Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    private void updateButtonStates() {
        Row row = table == null ? null : table.getSelectionModel().getSelectedItem();
        boolean hasFolder = pluginsDir != null;
        installBtn.setDisable(row == null || row.status() != Status.NOT_INSTALLED || !hasFolder);
        updateBtn.setDisable(row == null || row.status() != Status.UPDATE_AVAILABLE || !hasFolder);
        uninstallBtn.setDisable(row == null || row.status() == Status.NOT_INSTALLED);
    }

    private boolean ensureFolder() {
        if (pluginsDir == null) {
            chooseFolder();
        }
        return pluginsDir != null;
    }

    private void resolveFolder() {
        runAsync(paths::pluginsDir, dir -> {
            pluginsDir = dir;
            folderText.set(dir == null ? "Plugins folder: not found" : "Plugins: " + dir);
            updateButtonStates();
        }, "Locating Plugins folder");
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select LOTRO Plugins folder");
        chooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
        var chosen = chooser.showDialog(owner);
        if (chosen != null) {
            runAsync(() -> {
                Path dir = chosen.toPath();
                paths.setPluginsDir(dir);
                return dir;
            }, dir -> {
                pluginsDir = dir;
                folderText.set("Plugins: " + dir);
                statusText.set("Plugins folder set to " + dir);
                updateButtonStates();
            }, "Saving Plugins folder");
        }
    }

    private void applyFilter(FilteredList<Row> filtered, TextField search, ComboBox<String> category) {
        String q = search.getText() == null ? "" : search.getText().toLowerCase();
        String cat = category.getValue();
        filtered.setPredicate(row -> {
            boolean matchCategory = cat == null || cat.equals("All categories")
                    || row.plugin().category().equals(cat);
            if (!matchCategory) {
                return false;
            }
            if (q.isBlank()) {
                return true;
            }
            PluginInfo p = row.plugin();
            return p.name().toLowerCase().contains(q)
                    || p.author().toLowerCase().contains(q)
                    || p.description().toLowerCase().contains(q);
        });
    }

    private void styleDialog(Alert alert) {
        Scene scene = owner == null ? null : owner.getScene();
        if (alert.getDialogPane() != null && scene != null && !scene.getStylesheets().isEmpty()) {
            alert.getDialogPane().getStylesheets().setAll(scene.getStylesheets());
        }
        Node graphic = alert.getDialogPane() != null ? alert.getDialogPane().getGraphic() : null;
        if (graphic != null) {
            graphic.setStyle("-fx-background-color: transparent;");
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
            statusText.set(action + " failed: " + cause.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("lotp Desktop");
            alert.setHeaderText(action + " failed");
            alert.setContentText(cause == null ? "Unknown error" : String.valueOf(cause.getMessage()));
            styleDialog(alert);
            alert.showAndWait();
            return null;
        }, Platform::runLater);
    }

    private void setBusy(boolean busy, String message) {
        if (Platform.isFxApplicationThread()) {
            progress.set(busy ? -1 : 0);
            if (message != null) {
                statusText.set(message);
            }
        } else {
            Platform.runLater(() -> setBusy(busy, message));
        }
    }
}
