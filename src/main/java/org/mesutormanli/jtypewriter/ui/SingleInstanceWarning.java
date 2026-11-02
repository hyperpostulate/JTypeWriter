/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.DialogService;

/**
 * Minimal JavaFX application that shows the "already running" warning and then
 * exits. It is launched by {@code JTypeWriterApp} instead of the real
 * application when another instance owns the instance lock, so it deliberately
 * creates no other window, scene graph or application graph.
 *
 * <p>The warning itself comes from {@link DialogService} like every other alert:
 * there is no main scene to copy stylesheets from, so that service paints the
 * pane from the classpath with the user's persisted theme.
 */
public final class SingleInstanceWarning extends Application {

    @Override
    public void start(Stage primaryStage) {
        var settings = AppSettings.load();
        var messages = new Messages(new LocaleManager(settings));
        new DialogService(messages, settings).showWarning(null,
                messages.singleInstanceTitle(),
                messages.singleInstanceHeader(),
                messages.singleInstanceContent());
        Platform.exit();
    }
}
