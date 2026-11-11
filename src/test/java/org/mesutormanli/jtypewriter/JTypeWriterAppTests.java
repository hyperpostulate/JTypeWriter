/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JTypeWriterAppTests {

    @TempDir
    Path dir;

    private static final Path APP_SOURCE =
            Path.of("src", "main", "java", "org", "mesutormanli", "jtypewriter",
                    "JTypeWriterApp.java");
    private static final Path FX_SOURCE =
            Path.of("src", "main", "java", "org", "mesutormanli", "jtypewriter",
                    "FxApplication.java");

    @Test
    void hasMainMethod() {
        var hasMain = Arrays.stream(JTypeWriterApp.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("main"));
        assertTrue(hasMain);
    }

    /**
     * The launcher and a file association both hand the document to {@code main}
     * as a {@code String[]}; without that parameter the path never reaches the
     * application at all, and no amount of resolver code could help.
     */
    @Test
    void mainDeclaresTheStringArrayParameterTheLauncherPassesArgumentsThrough() {
        assertDoesNotThrow(() -> JTypeWriterApp.class.getDeclaredMethod("main", String[].class));
    }

    /**
     * {@code main} must hand its arguments to JavaFX, because that is what makes
     * them visible as {@code getParameters()} later. Headless by necessity, so
     * this is pinned by reading the source - the {@code EditorUndoContractTest}
     * precedent for contracts no toolkit-free test can observe.
     */
    @Test
    void mainForwardsItsArgumentsToTheJavaFxLauncher() {
        var source = read(APP_SOURCE);

        assertTrue(source.contains("Application.launch(FxApplication.class, args)"),
                APP_SOURCE + " no longer forwards its arguments to Application.launch, so"
                        + " JTypeWriter <file> would start with an empty parameter list");
    }

    /**
     * The startup wiring itself: the resolved process arguments must be what
     * decides whether a document opens. A future refactor could keep every pure
     * piece and still forget to call it at startup.
     */
    @Test
    void theStartupPathResolvesTheApplicationParameters() {
        var source = read(FX_SOURCE);

        assertTrue(source.contains("StartupArguments.resolve(getParameters().getRaw())"),
                FX_SOURCE + " never resolves getParameters().getRaw(), so a document named on"
                        + " the command line would be ignored");
        assertTrue(source.contains("openRequestedDocument"),
                FX_SOURCE + " resolves the startup arguments but never opens the document");
    }

    /**
     * The title bar is chrome like every other label, so it must come from the
     * bundle rather than a literal: a renamed or translated application name
     * has to reach the window without touching this class.
     */
    @Test
    void theWindowTitleIsTheBundlesAppNameInEverySupportedLanguage() {
        for (var language : Language.values()) {
            var settings = new AppSettings(
                    dir.resolve("title-" + language.code() + ".properties"));
            var localeManager = new LocaleManager(settings);
            localeManager.setLanguage(language);
            var messages = new Messages(localeManager);

            var title = FxApplication.windowTitle(messages);

            assertEquals(messages.appName(), title,
                    "window title of " + language + " must be the bundle's appName");
            assertFalse(title.isBlank(),
                    "window title of " + language + " must not be blank");
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
