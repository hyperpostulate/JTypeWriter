/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PW-14 (analysis U4): the welcome/About dialogs must fit the display instead
 * of opening taller than a 768 px screen. The clamping is pure maths so it is
 * verified headlessly; the shared scroll style class is checked by reading the
 * two dialog sources as plain text.
 */
class DialogSizingTest {

    private static final double DELTA = 0.001;

    @Test
    void preferredHeightThatFitsIsReturnedUnchanged() {
        assertEquals(750, DialogSizing.clampHeight(750, 1080), DELTA);
    }

    @Test
    void heightIsClampedToA768PixelScreen() {
        var clamped = DialogSizing.clampHeight(820, 768);
        assertEquals(768 - 2 * DialogSizing.SCREEN_MARGIN, clamped, DELTA);
        assertTrue(clamped <= 768 - 2 * DialogSizing.SCREEN_MARGIN);
    }

    @Test
    void heightFallsBackToTheFloorOnAnAbsurdlySmallScreen() {
        var clamped = DialogSizing.clampHeight(820, 300);
        assertEquals(DialogSizing.MIN_HEIGHT, clamped, DELTA);
        assertTrue(clamped > 0, "a dialog must never collapse to zero height");
    }

    @Test
    void preferredWidthThatFitsIsReturnedUnchanged() {
        assertEquals(540, DialogSizing.clampWidth(540, 1920), DELTA);
    }

    @Test
    void widthIsClampedToTheVisualBounds() {
        var clamped = DialogSizing.clampWidth(1200, 1024);
        assertEquals(1024 - 2 * DialogSizing.SCREEN_MARGIN, clamped, DELTA);
        assertTrue(clamped <= 1024 - 2 * DialogSizing.SCREEN_MARGIN);
    }

    @Test
    void widthFallsBackToTheFloorOnAnAbsurdlySmallScreen() {
        var clamped = DialogSizing.clampWidth(1200, 300);
        assertEquals(DialogSizing.MIN_WIDTH, clamped, DELTA);
        assertTrue(clamped > 0, "a dialog must never collapse to zero width");
    }

    /** Both axes subtract the very same margin, so one constant tunes them. */
    @Test
    void heightAndWidthUseTheSameScreenMarginSymmetrically() {
        var visual = 768.0;
        var preferred = 2000.0;
        var expected = visual - 2 * DialogSizing.SCREEN_MARGIN;
        assertEquals(expected, DialogSizing.clampHeight(preferred, visual), DELTA);
        assertEquals(expected, DialogSizing.clampWidth(preferred, visual), DELTA);
    }

    @Test
    void bothDialogsUseTheSharedScrollStyleClass() {
        for (var source : new String[]{"WelcomeDialog.java", "AboutDialog.java"}) {
            var text = readSource(source);
            assertTrue(text.contains("\"dialog-scroll\""),
                    source + " does not use the shared dialog-scroll style class");
            assertFalse(text.contains("\"about-scroll\""),
                    source + " still uses the old about-scroll style class");
        }
    }

    private static String readSource(String name) {
        var path = Path.of("src", "main", "java", "org", "mesutormanli", "jtypewriter", "ui", name);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path.toAbsolutePath(), e);
        }
    }
}
