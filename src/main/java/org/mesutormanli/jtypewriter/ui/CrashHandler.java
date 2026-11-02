/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppDirs;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.DialogService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global crash net: every uncaught exception is logged to
 * {@link AppDirs#logsDir()} and surfaced to the user in an error dialog instead
 * of dying silently in a console nobody sees. Guards against dialog stacking so
 * a repeating error cannot flood the UI.
 */
public class CrashHandler {

    private static final Logger LOG = AppLogging.logger(CrashHandler.class);

    private final DialogService dialogService;
    private final Messages messages;
    private final Supplier<Stage> owner;
    private final AtomicBoolean dialogShowing = new AtomicBoolean();

    public CrashHandler(DialogService dialogService, Messages messages, Supplier<Stage> owner) {
        this.dialogService = dialogService;
        this.messages = messages;
        this.owner = owner;
    }

    /** Installs the handler covering every application thread. */
    public void install() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                report("Uncaught exception in " + thread.getName(), error));
    }

    /** Logs a failure and shows it to the user (once, on the FX thread). */
    public void report(String context, Throwable error) {
        LOG.log(Level.SEVERE, context, error);
        Runnable showDialog = () -> showDialog(context, error);
        try {
            if (Platform.isFxApplicationThread()) {
                showDialog.run();
            } else {
                Platform.runLater(showDialog);
            }
        } catch (Throwable toolkitUnavailable) {
            // toolkit not up yet: keep the console trail as the only channel
            System.err.println(context + ": " + error);
            error.printStackTrace();
        }
    }

    private void showDialog(String context, Throwable error) {
        if (!dialogShowing.compareAndSet(false, true)) {
            return;
        }
        try {
            // DialogService owns the alert: it themes the pane, clamps it to the
            // screen and wraps the text. The owner supplier can hand back a
            // stage before its scene exists early in startup, and prepare()
            // then falls back to the persisted theme.
            dialogService.showError(owner.get(), context,
                    error + "\n\n" + messages.errorLogged(AppDirs.logsDir().toString()));
        } finally {
            dialogShowing.set(false);
        }
    }
}