/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

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
        var settings = AppSettings.load();
        var messages = new Messages(new LocaleManager(settings));
        var alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(messages.singleInstanceTitle());
        alert.setHeaderText(messages.singleInstanceHeader());
        alert.setContentText(messages.singleInstanceContent());
        // No main scene exists to copy from, so paint the pane from the
        // classpath with the user's persisted theme.
        DialogStyling.applyTheme(alert.getDialogPane(), settings.getTheme());
        alert.showAndWait();
        Platform.exit();
    }
}
