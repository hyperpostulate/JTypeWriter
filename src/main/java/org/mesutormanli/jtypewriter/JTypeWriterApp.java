package org.mesutormanli.jtypewriter;

import javafx.application.Application;
import org.mesutormanli.jtypewriter.ui.SingleInstanceWarning;

/**
 * Entry point. Deliberately not a subclass of {@link Application}: launching a
 * non-Application main class keeps the JDK launcher happy when JavaFX is on the
 * classpath instead of the module path.
 */
public final class JTypeWriterApp {

    private JTypeWriterApp() {
    }

    public static void main(String[] args) {
        // The lock is taken before the object graph is built, so a rejected
        // second start never touches settings, drafts or recovery data (the
        // warning dialog only reads the settings file to pick the UI language).
        var result = InstanceLock.acquire(AppDirs.dataDir());
        if (result.status() == InstanceLock.Status.ALREADY_RUNNING) {
            showAlreadyRunning();
            return;
        }
        // UNAVAILABLE already logged a warning inside acquire(); start without the lock.
        try {
            Application.launch(FxApplication.class, args);
        } catch (Throwable error) {
            // last-resort trail when the toolkit dies before the crash dialog can show
            System.err.println("JTypeWriter terminated unexpectedly: " + error);
            error.printStackTrace();
            System.exit(1);
        } finally {
            result.held().ifPresent(InstanceLock::close);
        }
    }

    /** Shows the localized warning in a throwaway JavaFX app, or falls back to the console. */
    private static void showAlreadyRunning() {
        try {
            Application.launch(SingleInstanceWarning.class);
        } catch (Throwable error) {
            // no display / no toolkit: at least leave a trail on the console
            System.err.println("JTypeWriter is already running.");
        }
    }
}