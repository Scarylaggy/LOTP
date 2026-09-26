package com.bs.lotp.desktop.ui.browser;

import com.bs.lotp.desktop.ui.plugintable.PluginRow;
import com.bs.lotp.desktop.ui.plugintable.PluginTableModel;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Passive view of the plugin browser: builds every control and forwards UI
 * events through {@link Handlers}. Holds no service references and makes no
 * decisions — the presenter owns all behavior.
 */
public class PluginBrowserView {

    /**
     * UI events the view forwards to the presenter. Nulls become no-ops.
     */
    public record Handlers(
            Runnable onRefresh,
            Runnable onInstall,
            Runnable onUpdate,
            Runnable onUninstall,
            Runnable onChooseFolder,
            Runnable onThemeToggle,
            Consumer<String> onSearchQuery,
            Consumer<String> onCategory) {

        public Handlers {
            if (onRefresh == null) {
                onRefresh = () -> {};
            }
            if (onInstall == null) {
                onInstall = () -> {};
            }
            if (onUpdate == null) {
                onUpdate = () -> {};
            }
            if (onUninstall == null) {
                onUninstall = () -> {};
            }
            if (onChooseFolder == null) {
                onChooseFolder = () -> {};
            }
            if (onThemeToggle == null) {
                onThemeToggle = () -> {};
            }
            if (onSearchQuery == null) {
                onSearchQuery = query -> {};
            }
            if (onCategory == null) {
                onCategory = category -> {};
            }
        }
    }

    private final Stage owner;
    private final BorderPane root = new BorderPane();

    private final Label detailTitle = new Label("Select a plugin");
    private final TextArea detail = new TextArea();
    private final Button installBtn = new Button("⬇ Install");
    private final Button updateBtn = new Button("⬆ Update");
    private final Button uninstallBtn = new Button("Uninstall");
    private final Button themeBtn = new Button();
    private final ComboBox<String> categoryBox = new ComboBox<>();

    public PluginBrowserView(
            Stage owner, TableView<PluginRow> table, PluginBrowserModel model, Handlers handlers) {
        this.owner = owner;

        // --- Header ---
        Label title = new Label("Plugins");
        title.getStyleClass().add("app-title");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("primary-button");
        refreshBtn.setOnAction(e -> handlers.onRefresh().run());

        TextField searchField = new TextField();
        searchField.setPromptText("Search name, author, description...");
        searchField.setPrefWidth(260);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, o, n) -> handlers.onSearchQuery().accept(n));

        categoryBox.getItems().add(PluginTableModel.ALL_CATEGORIES);
        categoryBox.getSelectionModel().selectFirst();
        categoryBox.setPrefWidth(200);
        categoryBox.valueProperty().addListener((obs, o, n) -> handlers.onCategory().accept(n));

        themeBtn.getStyleClass().add("ghost-button");
        themeBtn.setOnAction(e -> handlers.onThemeToggle().run());

        Button folderBtn = new Button("Plugins folder...");
        folderBtn.setOnAction(e -> handlers.onChooseFolder().run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerBar = new HBox(8, title, searchField, categoryBox, spacer, themeBtn, folderBtn, refreshBtn);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.getStyleClass().add("toolbar");

        Label folderLabel = new Label();
        folderLabel.textProperty().bind(model.folderTextProperty());
        folderLabel.getStyleClass().add("muted");
        HBox folderBar = new HBox(folderLabel);
        folderBar.setAlignment(Pos.CENTER_LEFT);
        folderBar.setPadding(new Insets(4, 12, 6, 14));
        folderBar.setStyle("-fx-background-color: transparent;");

        VBox header = new VBox(headerBar, folderBar);

        // --- Detail + actions ---
        detailTitle.getStyleClass().add("detail-title");

        detail.setEditable(false);
        detail.setWrapText(true);
        detail.setPrefRowCount(6);
        detail.getStyleClass().add("detail-area");
        VBox.setVgrow(detail, Priority.ALWAYS);

        installBtn.getStyleClass().add("primary-button");
        installBtn.setOnAction(e -> handlers.onInstall().run());

        updateBtn.getStyleClass().add("accent-button");
        updateBtn.setOnAction(e -> handlers.onUpdate().run());

        uninstallBtn.getStyleClass().add("danger-button");
        uninstallBtn.setOnAction(e -> handlers.onUninstall().run());

        HBox actions = new HBox(8, installBtn, updateBtn, uninstallBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, detailTitle, detail, actions);
        card.getStyleClass().add("card");

        SplitPane split = new SplitPane(table, card);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.62);

        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(model.progressProperty());
        progressBar.setPrefWidth(150);

        Label status = new Label();
        status.textProperty().bind(model.statusTextProperty());

        HBox statusBar = new HBox(8, progressBar, status);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");

        root.getStyleClass().add("app-root");
        root.setTop(header);
        root.setCenter(split);
        root.setBottom(statusBar);
    }

    public BorderPane root() {
        return root;
    }

    public void showDetail(String title, String description) {
        detailTitle.setText(title);
        detail.setText(description);
    }

    public void setActionsEnabled(boolean canInstall, boolean canUpdate, boolean canUninstall) {
        installBtn.setDisable(!canInstall);
        updateBtn.setDisable(!canUpdate);
        uninstallBtn.setDisable(!canUninstall);
    }

    public void setThemeToggleText(String text) {
        themeBtn.setText(text);
    }

    public String selectedCategory() {
        return categoryBox.getValue();
    }

    public void setCategories(List<String> categories, String selectedToRestore) {
        categoryBox.getItems().setAll(PluginTableModel.ALL_CATEGORIES);
        categoryBox.getItems().addAll(categories);
        if (selectedToRestore != null && categoryBox.getItems().contains(selectedToRestore)) {
            categoryBox.setValue(selectedToRestore);
        }
    }

    /**
     * Shows the folder picker; empty when the user cancels.
     */
    public Optional<Path> chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select LOTRO Plugins folder");
        chooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
        var chosen = chooser.showDialog(owner);
        return chosen == null ? Optional.empty() : Optional.of(chosen.toPath());
    }

    /**
     * Confirmation dialog for uninstall; themed like the main scene.
     */
    public boolean confirmUninstall(String name, List<String> dirs) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Uninstall");
        confirm.setHeaderText("Uninstall " + name + "?");
        confirm.setContentText("Removes from the Plugins directory:\n" + String.join("\n", dirs));
        styleDialog(confirm);
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Error dialog; themed like the main scene.
     */
    public void showError(String action, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("lotp Desktop");
        alert.setHeaderText(action + " failed");
        alert.setContentText(message == null ? "Unknown error" : message);
        styleDialog(alert);
        alert.showAndWait();
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
}
