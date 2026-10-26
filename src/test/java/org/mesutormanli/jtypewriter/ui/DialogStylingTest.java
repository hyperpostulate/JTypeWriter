/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless guard for the themed Alert chrome. No JavaFX control,
 * dialog pane or scene is constructed: the tests pin the pure path list and the
 * CSS wiring that makes the dialog tokens resolvable in every theme.
 */
class DialogStylingTest {

    @Test
    void stylesheetPathsForLightStartWithBaseThenLight() {
        assertEquals(List.of("/css/base.css", "/css/light.css"),
                DialogStyling.stylesheetPaths(Theme.LIGHT));
    }

    @Test
    void stylesheetPathsForDarkStartWithBaseThenDark() {
        assertEquals(List.of("/css/base.css", "/css/dark.css"),
                DialogStyling.stylesheetPaths(Theme.DARK));
    }

    @Test
    void stylesheetPathsForSepiaStartWithBaseThenSepia() {
        assertEquals(List.of("/css/base.css", "/css/sepia.css"),
                DialogStyling.stylesheetPaths(Theme.SEPIA));
    }

    @Test
    void everyListedStylesheetResolvesOnTheClasspath() {
        for (var theme : Theme.values()) {
            for (var path : DialogStyling.stylesheetPaths(theme)) {
                assertNotNull(DialogStyling.class.getResource(path),
                        "missing classpath resource " + path);
            }
        }
    }

    @Test
    void baseStylesheetThemesTheDialogPane() {
        assertTrue(read("/css/base.css").contains(".dialog-pane"),
                "base.css must carry the dialog-pane chrome rules");
    }

    @Test
    void eachThemeDefinesItsTokensForDialogPanes() {
        for (var theme : Theme.values()) {
            var path = "/" + theme.getCssPath();
            assertTrue(read(path).contains(".root, .dialog-pane"),
                    path + " must define its tokens for .dialog-pane too");
        }
    }

    private static String read(String path) {
        try (InputStream stream = DialogStyling.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing classpath resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException(path, error);
        }
    }
}
