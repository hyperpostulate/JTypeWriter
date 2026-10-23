package org.mesutormanli.jtypewriter.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

/**
 * Minimal JavaFX application that shows the "already running" warning and then
 * exits. It is launched by {@code JTypeWriterApp} instead of the real
 * application when another instance owns the instance lock, so it deliberately
 * creates no other window, scene graph or application graph.
 */
public final class SingleInstanceWarning extends Application {

    @Override
    public void start(Stage primaryStage) {
        var messages = new Messages(new LocaleManager(AppSettings.load()));
        var alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(messages.singleInstanceTitle());
        alert.setHeaderText(messages.singleInstanceHeader());
        alert.setContentText(messages.singleInstanceContent());
        alert.showAndWait();
        Platform.exit();
    }
}
