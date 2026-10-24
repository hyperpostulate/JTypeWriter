package org.mesutormanli.jtypewriter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.ui.AppIcons;
import org.mesutormanli.jtypewriter.ui.MainController;

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
            app.themeManager.applyTheme(scene); // the theme persisted from the last session
            app.mainController.setStage(primaryStage);
            app.mainStage.configure(primaryStage, scene);

            AppIcons.applyTo(primaryStage);
            primaryStage.show();

            // The recovery offer must be resolved before autosave starts: the
            // periodic snapshot sees an empty, clean editor while the (modal)
            // recovery dialog is still open and would clear the very draft the
            // user is being asked about. Attach afterwards, on the settled
            // document (restored draft or last session file).
            if (!app.autoSaveService.offerRecovery(primaryStage, app.editorArea)) {
                restoreLastSession(primaryStage); // continue where the last session ended
            }
            app.autoSaveService.attach(primaryStage, app.editorArea);
            app.welcomeDialog.show(primaryStage);
        } catch (Throwable error) {
            app.crashHandler.report("Startup failed", error);
            throw error;
        }
    }

    /**
     * Reopens the document from the last session. A document that has vanished
     * is silently skipped: a stale settings entry must never turn startup into
     * an error parade.
     */
    private void restoreLastSession(Stage stage) {
        app.settings.getLastFile()
                .filter(java.nio.file.Files::isRegularFile)
                .flatMap(path -> app.fileService.loadFile(stage, path))
                .ifPresent(path -> app.editorArea.setDocument(app.fileService.getCurrentContent()));
    }
}