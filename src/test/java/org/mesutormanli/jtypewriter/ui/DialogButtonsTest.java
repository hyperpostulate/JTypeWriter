/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless guard for the Alert keyboard contract. {@code ButtonType}
 * and {@code ButtonBar.ButtonData} are plain data classes, so no JavaFX toolkit,
 * window or control is started here.
 */
class DialogButtonsTest {

    @Test
    void primaryCarriesOkDone() {
        assertEquals(ButtonBar.ButtonData.OK_DONE,
                DialogButtons.primary("Save").getButtonData());
    }

    @Test
    void cancelCarriesCancelClose() {
        assertEquals(ButtonBar.ButtonData.CANCEL_CLOSE,
                DialogButtons.cancel("Cancel").getButtonData());
    }

    @Test
    void alternativeCarriesOther() {
        assertEquals(ButtonBar.ButtonData.OTHER,
                DialogButtons.alternative("Discard").getButtonData());
    }

    @Test
    void buttonTextIsPreservedVerbatim() {
        assertEquals("Übernehmen", DialogButtons.primary("Übernehmen").getText());
        assertEquals("Abbrechen", DialogButtons.cancel("Abbrechen").getText());
        assertEquals("Verwerfen", DialogButtons.alternative("Verwerfen").getText());
    }

    /** Enter must reach exactly one action, Escape exactly one. */
    @Test
    void exactlyOneDefaultAndOneCancelAmongTheThree() {
        List<ButtonType> buttons = List.of(
                DialogButtons.primary("Save"),
                DialogButtons.alternative("Discard"),
                DialogButtons.cancel("Cancel"));

        assertEquals(1, buttons.stream()
                .filter(button -> button.getButtonData().isDefaultButton())
                .count());
        assertEquals(1, buttons.stream()
                .filter(button -> button.getButtonData().isCancelButton())
                .count());
    }

    /**
     * The one non-obvious mapping: in the recovery prompt Escape and the window
     * close mean "keep the draft, decide later", so cancel semantics belong to
     * "Later" and must never sit on the destructive "Discard".
     */
    @Test
    void recoveryDialogPutsCancelOnLaterNotOnDiscard() {
        List<ButtonType> buttons = List.of(
                DialogButtons.primary("Restore"),
                DialogButtons.alternative("Discard"),
                DialogButtons.cancel("Later"));

        for (var button : buttons) {
            if (button.getText().equals("Later")) {
                assertTrue(button.getButtonData().isCancelButton(),
                        "Later must be the cancel button");
            } else {
                assertFalse(button.getButtonData().isCancelButton(),
                        button.getText() + " must not be the cancel button");
            }
        }
        assertTrue(buttons.get(0).getButtonData().isDefaultButton());
    }
}
