/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.mesutormanli.jtypewriter.audio.SoundPolicy;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.stats.SessionStats;

public class EditorKeyHandler {

    private final TypewriterSound typewriterSound;
    private final SessionStats sessionStats;

    public EditorKeyHandler(TypewriterSound typewriterSound, SessionStats sessionStats) {
        this.typewriterSound = typewriterSound;
        this.sessionStats = sessionStats;
    }

    public void bind(EditorArea editorArea) {
        editorArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        editorArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);

        // The editor records the applied edit at its single enforcement point;
        // consuming it keeps the statistics O(edit size). A change that did not
        // pass through that point (undo/redo, a document load) has no hint and
        // is rescanned once, which is correct and rare.
        editorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            var change = editorArea.takeChange();
            if (change != null) {
                sessionStats.applyChange(change, newVal);
            } else {
                sessionStats.updateText(newVal);
            }
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        if (SoundPolicy.playsFor(event.getCode(), event.isShortcutDown())) {
            typewriterSound.play();
        }
    }

    private void handleKeyTyped(KeyEvent event) {
        if (SoundPolicy.playsOnTyped(event.getCharacter())) {
            typewriterSound.play();
            sessionStats.registerKeystroke();
        }
    }
}
