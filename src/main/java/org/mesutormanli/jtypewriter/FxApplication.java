package org.mesutormanli.jtypewriter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.ui.MainController;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

/**
 * JavaFX lifecycle bridge. Builds the object graph once in {@link #init()},
 * installs the global crash handler and wires the graph into the scene in
 * {@link #start(Stage)}.
 */
public class FxApplication extends Application {

    private AppGraph app;

    @Override
    public void init() {
        AppLogging.init();
        app = new AppGraph();
        app.crashHandler.install();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            var resource = getClass().getResource("/fxml/main.fxml");
            if (resource == null) {
                throw new IllegalStateException("main.fxml not found");
            }
            var loader = new FXMLLoader(resource);
            loader.setControllerFactory(type -> {
                if (type == MainController.class) {
                    return app.mainController;
                }
                throw new IllegalStateException("No wired controller for " + type.getName());
            });

            Parent root = loader.load();

            var scene = new Scene(root);
            primaryStage.setTitle("JTypeWriter");
            primaryStage.setScene(scene);

            app.setStage(primaryStage);
            app.themeManager.applyTheme(scene, Theme.DARK);
            app.mainController.setStage(primaryStage);
            app.mainStage.configure(primaryStage, scene);

            primaryStage.show();

            app.autoSaveService.attach(primaryStage, app.editorArea);
            app.autoSaveService.offerRecovery(primaryStage, app.editorArea);
            app.welcomeDialog.show(primaryStage);
        } catch (Throwable error) {
            app.crashHandler.report("Startup failed", error);
            throw error;
        }
    }
}