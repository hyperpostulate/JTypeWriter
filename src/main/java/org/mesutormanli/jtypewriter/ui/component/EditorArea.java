package org.mesutormanli.jtypewriter.ui.component;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.stats.SessionStats;
import org.springframework.stereotype.Component;

@Component
public class EditorArea extends TextArea {

    private final TypewriterSound typewriterSound;
    private final SessionStats sessionStats;

    private boolean yoloMode;

    public EditorArea(TypewriterSound typewriterSound, SessionStats sessionStats) {
        this.typewriterSound = typewriterSound;
        this.sessionStats = sessionStats;

        setWrapText(true);
        setPrefRowCount(40);
        getStyleClass().add("editor-area");

        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);

        textProperty().addListener((obs, oldVal, newVal) ->
                sessionStats.updateText(newVal));
    }

    private void handleKeyPressed(KeyEvent event) {
        if (yoloMode && isDeletionKey(event.getCode())) {
            event.consume();
            return;
        }
        if (!event.getCode().isModifierKey()) {
            typewriterSound.play();
        }
    }

    private void handleKeyTyped(KeyEvent event) {
        if (event.getCharacter() != null && !event.getCharacter().isEmpty()) {
            sessionStats.registerKeystroke();
        }
    }

    private boolean isDeletionKey(KeyCode code) {
        return code == KeyCode.BACK_SPACE || code == KeyCode.DELETE
                || code == KeyCode.CUT;
    }

    public boolean isYoloMode() {
        return yoloMode;
    }

    public void setYoloMode(boolean enabled) {
        this.yoloMode = enabled;
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("yolo"), enabled);
    }

    public void toggleYoloMode() {
        setYoloMode(!yoloMode);
    }
}
