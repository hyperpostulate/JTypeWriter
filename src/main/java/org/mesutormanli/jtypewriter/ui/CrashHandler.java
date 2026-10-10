package org.mesutormanli.jtypewriter.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppDirs;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.locale.Messages;

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

    private final Messages messages;
    private final Supplier<Stage> owner;
    private final AtomicBoolean dialogShowing = new AtomicBoolean();

    public CrashHandler(Messages messages, Supplier<Stage> owner) {
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
            var alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
            alert.setTitle(messages.errorTitle());
            alert.setHeaderText(context);
            alert.setContentText(error + "\n\n" + messages.errorLogged(AppDirs.logsDir().toString()));
            var stage = owner.get();
            if (stage != null) {
                alert.initOwner(stage);
            }
            alert.showAndWait();
        } finally {
            dialogShowing.set(false);
        }
    }
}