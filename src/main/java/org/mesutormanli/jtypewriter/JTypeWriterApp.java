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
        Application.launch(FxApplication.class, args);
    }
}