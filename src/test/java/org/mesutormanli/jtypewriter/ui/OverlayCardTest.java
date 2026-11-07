/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The keyboard half of the floating cards. Escape and Enter are what the old
 * modal windows handled on their own scene; as a card they must stay scoped to
 * the card, which is why the dismissal rule is a pure predicate rather than
 * anything installed on the scene.
 */
class OverlayCardTest {

    @Test
    void escapeAndEnterDismissACard() {
        assertTrue(OverlayCard.dismisses(KeyCode.ESCAPE));
        assertTrue(OverlayCard.dismisses(KeyCode.ENTER));
    }

    /** Every other key belongs to the focused control (the card's own button). */
    @Test
    void otherKeysAreLeftToTheCard() {
        assertFalse(OverlayCard.dismisses(KeyCode.TAB));
        assertFalse(OverlayCard.dismisses(KeyCode.SPACE));
        assertFalse(OverlayCard.dismisses(KeyCode.F3));
    }
}
