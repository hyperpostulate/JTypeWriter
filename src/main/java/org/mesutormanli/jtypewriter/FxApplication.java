/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.ui.AppFonts;
import org.mesutormanli.jtypewriter.ui.AppIcons;
import org.mesutormanli.jtypewriter.ui.MainController;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX lifecycle bridge. Builds the object graph once in {@link #init()},
 * installs the global crash handler and wires the graph into the scene in
 * {@link #start(Stage)}.
 */
public class FxApplication extends Application {

    private static final Logger LOG = AppLogging.logger(FxApplication.class);

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
            registerBundledFonts();
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
            // user is being asked about. It also outranks everything else,
            // command-line arguments included: a draft is the only copy of that
            // text, while the document named on the command line is still on
            // disk and can be reopened.
            var request = StartupArguments.resolve(getParameters().getRaw());
            var openedFromArguments = false;
            if (!app.autoSaveService.offerRecovery(primaryStage, app.editorArea)) {
                openedFromArguments = openRequestedDocument(primaryStage, request);
                if (!openedFromArguments && request.argument() == null) {
                    restoreLastSession(primaryStage); // continue where the last session ended
                }
            }
            app.autoSaveService.attach(primaryStage, app.editorArea);
            // A document named on the command line is what the user asked for;
            // the welcome card would be the answer to a question nobody asked.
            if (greetsAtStartup(openedFromArguments, app.settings.isWelcomeCardEnabled())) {
                // The card takes focus itself (Escape and Enter dismiss it) and
                // hands it back to the editor when it closes, so nothing here
                // has to undo a hand-over.
                app.welcomeCard.show();
            } else {
                // Either the user opted out, or the text is what they asked for:
                // in both cases the first keystroke belongs in the editor.
                app.editorArea.requestFocus();
            }
            // Settings are written before this point (theme, geometry, session
            // restore); a failed write could not be reported then because there
            // was no window to own the alert yet.
            app.announceSettingsWriteFailureOnce();
        } catch (Throwable error) {
            app.crashHandler.report("Startup failed", error);
            throw error;
        }
    }

    /**
     * The startup greeting policy: greet unless the text is what the user asked
     * for, and unless they have turned the card off. Pure so both rules are
     * pinned without a toolkit - the opt-out is a preference, not a hidden
     * branch in the startup sequence, and a document named on the command line
     * outranks a greeting in every case (the card would cover the text the user
     * asked to see).
     */
    static boolean greetsAtStartup(boolean openedFromArguments, boolean welcomeEnabled) {
        return !openedFromArguments && welcomeEnabled;
    }

    /**
     * Registers the bundled UI chrome font with the JavaFX font registry
     * before the first {@code Scene} is built, so {@code -fx-font-family:
     * "DejaVu Sans Condensed"} in {@code base.css} resolves to the bytes the
     * toolbar measures with ({@link AppFonts}). A file that fails to load is a
     * startup failure, not a silent fallback to whichever font the host has:
     * a fallback would make the fit decision depend on the machine again.
     */
    private static void registerBundledFonts() {
        for (var resource : AppFonts.RESOURCES) {
            try (InputStream stream = FxApplication.class.getResourceAsStream(resource)) {
                var font = stream == null ? null : Font.loadFont(stream, 12);
                if (font == null || !AppFonts.FAMILY.equals(font.getFamily())) {
                    throw new IllegalStateException("bundled font " + resource
                            + " did not register as " + AppFonts.FAMILY);
                }
            } catch (IOException error) {
                throw new IllegalStateException("cannot read bundled font " + resource, error);
            }
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

    /**
     * Opens the document named on the command line (or handed over by a file
     * association, which arrives the same way) and reports anything that keeps
     * that from happening.
     *
     * <p>A refused argument is shown in the UI language, not as a platform
     * message: the user typed a name, so the answer has to name that same text
     * back. A file that does exist but cannot be read keeps the load path's own
     * dialog, which already explains the specific failure.</p>
     *
     * @return {@code true} when a document is now in the editor
     */
    private boolean openRequestedDocument(Stage stage, StartupArguments request) {
        if (request.problem().isPresent()) {
            var problem = request.problem().orElseThrow();
            LOG.log(Level.WARNING, "refusing the startup document \"" + request.argument()
                    + "\": " + problem);
            app.dialogService.showError(stage, app.messages.startupOpenFailedHeader(),
                    switch (problem) {
                        case INVALID_PATH -> app.messages.startupInvalidPath(request.argument());
                        case MISSING -> app.messages.startupMissingFile(request.argument());
                        case NOT_A_FILE -> app.messages.startupNotAFile(request.argument());
                        case TOO_MANY_ARGUMENTS ->
                                app.messages.startupTooManyArguments(request.argument());
                    });
            return false;
        }
        if (request.document().isEmpty()) {
            return false; // no argument at all: the caller restores the last session
        }
        var path = request.document().orElseThrow();
        var loaded = app.fileService.loadFile(stage, path);
        loaded.ifPresent(ignored ->
                app.editorArea.setDocument(app.fileService.getCurrentContent()));
        return loaded.isPresent();
    }

    /**
     * End of the JavaFX lifecycle. Reached by {@code Platform.exit()}, by the
     * implicit exit after the last window closed, and by a session log-out -
     * paths that never answer a close request. Without this hook the draft of a
     * dirty document survived only by luck: the hide handler used to delete it.
     * Now the decision is made once, in
     * {@code AutoSaveService.shutdown()}, and an unasked exit parks the draft
     * instead of deleting it.
     *
     * <p>Runs on every exit, including the confirmed one, where the draft was
     * already cleared; clearing twice is harmless and keeps the rule in one
     * place.</p>
     */
    @Override
    public void stop() {
        if (app == null) {
            return; // init() failed before the graph existed
        }
        app.autoSaveService.shutdown();
        app.ioExecutor.close(); // the draft decision above already blocked on this worker
        app.findScanService.close(); // a late scan result must not outlive the window
    }
}
