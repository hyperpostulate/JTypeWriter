package org.mesutormanli.jtypewriter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.ui.MainController;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

/**
 * JavaFX lifecycle bridge. Builds the object graph once in {@link #init()} and
 * wires it into the scene in {@link #start(Stage)}.
 */
public class FxApplication extends Application {

    private AppGraph app;

    @Override
    public void init() {
        app = new AppGraph();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
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

        app.themeManager.applyTheme(scene, Theme.DARK);
        app.mainController.setStage(primaryStage);
        app.mainStage.configure(primaryStage, scene);

        primaryStage.show();

        app.welcomeDialog.show(primaryStage);
    }
}