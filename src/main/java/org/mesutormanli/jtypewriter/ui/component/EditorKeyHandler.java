package org.mesutormanli.jtypewriter.ui.component;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.stats.SessionStats;
import org.springframework.stereotype.Component;

@Component
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

        editorArea.textProperty().addListener((obs, oldVal, newVal) ->
                sessionStats.updateText(newVal));
    }

    private void handleKeyPressed(KeyEvent event) {
        if (!event.getCode().isModifierKey()) {
            typewriterSound.play();
        }
    }

    private void handleKeyTyped(KeyEvent event) {
        if (event.getCharacter() != null && !event.getCharacter().isEmpty()) {
            sessionStats.registerKeystroke();
        }
    }
}
