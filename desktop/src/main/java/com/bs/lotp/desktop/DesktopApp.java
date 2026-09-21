package com.bs.lotp.desktop;

import com.bs.lotp.desktop.feed.PluginFeedService;
import com.bs.lotp.desktop.local.InstalledStore;
import com.bs.lotp.desktop.local.PluginInstaller;
import com.bs.lotp.desktop.settings.PluginPaths;
import com.bs.lotp.desktop.ui.PluginsView;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Spring Boot + JavaFX: Boot owns the services, JavaFX owns the UI thread. */
@SpringBootApplication
public class DesktopApp extends Application {

    private ConfigurableApplicationContext context;
    private Stage stage;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(DesktopApp.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        PluginsView view = new PluginsView(
                stage,
                context.getBean(PluginFeedService.class),
                context.getBean(PluginInstaller.class),
                context.getBean(InstalledStore.class),
                context.getBean(PluginPaths.class));
        stage.setTitle("LOTRO Plugin Manager");
        double[] bounds = restoreBounds(stage);
        double width = bounds == null ? 1100 : bounds[2];
        double height = bounds == null ? 680 : bounds[3];
        Scene scene = new Scene(view.build(), width, height);
        scene.getStylesheets().setAll(view.currentTheme().stylesheet());
        stage.setScene(scene);
        stage.show();
        trackBounds(stage);
        view.refreshIfEmpty();
    }

    @Override
    public void stop() {
        saveBounds();
        context.close();
    }

    /** Saves one second after the last move/resize, so abnormal exits keep the size too. */
    private void trackBounds(Stage stage) {
        PauseTransition saver = new PauseTransition(Duration.seconds(1));
        saver.setOnFinished(e -> saveBounds());
        stage.xProperty().addListener((obs, old, val) -> saver.playFromStart());
        stage.yProperty().addListener((obs, old, val) -> saver.playFromStart());
        stage.widthProperty().addListener((obs, old, val) -> saver.playFromStart());
        stage.heightProperty().addListener((obs, old, val) -> saver.playFromStart());
    }

    private void saveBounds() {
        try {
            if (stage != null && stage.getWidth() > 0) {
                context.getBean(PluginPaths.class)
                        .setWindowBounds(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            }
        } catch (Exception e) {
            System.err.println("lotp: could not save window bounds: " + e.getMessage());
        }
    }

    private double[] restoreBounds(Stage stage) {
        try {
            double[] bounds = context.getBean(PluginPaths.class).windowBounds();
            if (bounds != null) {
                stage.setX(bounds[0]);
                stage.setY(bounds[1]);
                stage.setWidth(bounds[2]);
                stage.setHeight(bounds[3]);
                return bounds;
            }
        } catch (Exception e) {
            System.err.println("lotp: could not restore window bounds: " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
