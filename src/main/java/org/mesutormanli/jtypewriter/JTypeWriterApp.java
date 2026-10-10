package org.mesutormanli.jtypewriter;

import javafx.application.Application;

/**
 * Entry point. Deliberately not a subclass of {@link Application}: launching a
 * non-Application main class keeps the JDK launcher happy when JavaFX is on the
 * classpath instead of the module path.
 */
public final class JTypeWriterApp {

    private JTypeWriterApp() {
    }

    public static void main(String[] args) {
        try {
            Application.launch(FxApplication.class, args);
        } catch (Throwable error) {
            // last-resort trail when the toolkit dies before the crash dialog can show
            System.err.println("JTypeWriter terminated unexpectedly: " + error);
            error.printStackTrace();
            System.exit(1);
        }
    }
}