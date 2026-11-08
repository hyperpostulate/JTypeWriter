/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression for the off-screen restore guard: a window position remembered on
 * a monitor that is no longer attached must not be treated as visible.
 * {@code intersectsAny} is deliberately pure so it tests headlessly.
 */
class MainStageTest {

    private static final Rectangle2D PRIMARY = new Rectangle2D(0, 0, 1920, 1080);

    @Test
    void windowWithinAScreenIsVisible() {
        assertTrue(MainStage.intersectsAny(new Rectangle2D(100, 100, 800, 600), List.of(PRIMARY)));
    }

    @Test
    void windowOnlyOnARemovedMonitorIsNotVisible() {
        // geometry remembered on a second monitor to the right that is now gone
        assertFalse(MainStage.intersectsAny(new Rectangle2D(3000, 100, 800, 600), List.of(PRIMARY)));
    }

    @Test
    void partiallyOffScreenWindowStillCounts() {
        assertTrue(MainStage.intersectsAny(new Rectangle2D(-400, 100, 800, 600), List.of(PRIMARY)));
    }

    @Test
    void windowOnASecondScreenIsVisible() {
        var secondary = new Rectangle2D(1920, 0, 1920, 1080);
        assertTrue(MainStage.intersectsAny(new Rectangle2D(2000, 200, 800, 600), List.of(PRIMARY, secondary)));
    }

    @Test
    void noScreensMeansNothingIntersects() {
        assertFalse(MainStage.intersectsAny(new Rectangle2D(0, 0, 800, 600), List.of()));
    }

    /**
     * A remembered width narrower than the minimum is widened on the
     * way in, not accepted. JavaFX may or may not clamp a width below
     * {@code minWidth} depending on the platform's window manager, so the
     * floor is applied here where it is deterministic and testable - and
     * because {@code setOnHidden} stores the width the stage actually has, the
     * too-narrow setting repairs itself at the end of that session instead of
     * being reapplied forever.
     */
    @Test
    void aRememberedWidthNarrowerThanTheMinimumIsWidened() {
        assertEquals(MainStage.MINIMUM_WIDTH_PX,
                MainStage.restoredWidth(600, MainStage.MINIMUM_WIDTH_PX));
    }

    @Test
    void aRememberedWidthWiderThanTheMinimumIsKept() {
        assertEquals(1400, MainStage.restoredWidth(1400, MainStage.MINIMUM_WIDTH_PX));
    }

    /** Exactly at the minimum is inside the rule, not below it. */
    @Test
    void aRememberedWidthAtTheMinimumIsKeptAsItIs() {
        assertEquals(900, MainStage.restoredWidth(900, 900));
    }

    /**
     * Fullscreen and maximized are mutually exclusive at restore, and
     * fullscreen wins, so the two persisted window states can never be applied
     * in a contradictory order.
     */
    @Test
    void fullscreenRestoreSuppressesMaximized() {
        assertFalse(MainStage.maximizeOnRestore(true, true));
        assertTrue(MainStage.maximizeOnRestore(true, false));
        assertFalse(MainStage.maximizeOnRestore(false, false));
        assertFalse(MainStage.maximizeOnRestore(false, true));
    }
}
