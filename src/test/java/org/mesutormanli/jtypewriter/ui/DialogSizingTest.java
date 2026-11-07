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
 * The helper views must fit the display they are opened on instead of spilling
 * past a 768 px screen. The clamping is pure maths so it is verified
 * headlessly; the shared scroll host of the two overlay cards is checked by
 * reading their sources as plain text.
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

    /**
     * An Alert must fit a 768 px-wide screen once the shared margin is taken
     * off both sides.
     */
    @Test
    void alertWidthFitsA768PixelWideScreenInsideTheSharedMargin() {
        var onSmallestSupportedScreen = DialogSizing.alertWidth(768);
        var onDesktop = DialogSizing.alertWidth(1920);

        assertTrue(onSmallestSupportedScreen <= 768 - 2 * DialogSizing.SCREEN_MARGIN,
                "an alert would overflow a 768 px-wide display");
        assertEquals(DialogSizing.ALERT_PREFERRED_WIDTH, onDesktop, DELTA);
    }

    /** The wrapped labels must fit the dialog with its chrome subtracted. */
    @Test
    void alertContentColumnLeavesRoomForTheDialogChrome() {
        var dialogWidth = DialogSizing.alertWidth(768);
        var contentWidth = DialogSizing.alertContentWidth(dialogWidth);

        assertEquals(dialogWidth - DialogSizing.ALERT_CHROME_WIDTH, contentWidth, DELTA);
        assertTrue(contentWidth > 0, "the content column must stay usable");
        assertTrue(contentWidth < dialogWidth, "the content cannot be wider than its dialog");
    }

    /**
     * Both overlay cards are built on {@link OverlayCard}, which hosts their
     * content in the shared {@code dialog-scroll} pane. Pinned by source
     * because the alternative - each card growing its own scroll pane and its
     * own background - is exactly the drift this card base removed.
     */
    @Test
    void bothCardsShareTheOverlayCardScrollHost() {
        for (var source : new String[]{"WelcomeCard.java", "AboutCard.java"}) {
            var text = readSource(source);
            assertTrue(text.contains("extends OverlayCard"),
                    source + " is not an OverlayCard");
            assertFalse(text.contains("ScrollPane"),
                    source + " builds its own scroll host instead of using OverlayCard's");
        }
        var base = readSource("OverlayCard.java");
        assertTrue(base.contains("\"dialog-scroll\""),
                "OverlayCard does not use the shared dialog-scroll style class");
        assertFalse(base.contains("\"about-scroll\""),
                "OverlayCard still uses the old about-scroll style class");
    }

    /**
     * The column the two cards share - mark, title, tagline, shortcut grid - is
     * built once by {@link OverlayCard#cardColumn}. Pinned by source because the
     * cards are otherwise near-identical: a copy of the tagline label or of the
     * shortcut grid in either subclass is how they drifted apart before, and
     * with the welcome card's call-to-action gone nothing else distinguishes
     * them.
     */
    @Test
    void neitherCardRebuildsTheSharedColumn() {
        for (var source : new String[]{"WelcomeCard.java", "AboutCard.java"}) {
            var text = readSource(source);
            assertFalse(text.contains("ShortcutTable.build("),
                    source + " builds its own shortcut grid instead of calling cardColumn");
            assertFalse(text.contains("appTagline()"),
                    source + " builds its own tagline label instead of calling cardColumn");
        }
        var base = readSource("OverlayCard.java");
        assertTrue(base.contains("ShortcutTable.build(") && base.contains("appTagline()"),
                "OverlayCard no longer builds the shared card column");
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
