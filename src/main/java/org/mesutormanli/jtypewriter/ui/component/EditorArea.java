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

    private boolean lineFocusMode;
    private boolean paragraphFocusMode;
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

        textProperty().addListener((obs, oldVal, newVal) -> {
            sessionStats.updateText(newVal);
            updateFocusHighlight();
        });

        caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (lineFocusMode || paragraphFocusMode) {
                updateFocusHighlight();
            }
        });
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

    public void setLineFocusMode(boolean enabled) {
        this.lineFocusMode = enabled;
        if (enabled) paragraphFocusMode = false;
        updateFocusHighlight();
    }

    public void setParagraphFocusMode(boolean enabled) {
        this.paragraphFocusMode = enabled;
        if (enabled) lineFocusMode = false;
        updateFocusHighlight();
    }

    public void setYoloMode(boolean enabled) {
        this.yoloMode = enabled;
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("yolo"), enabled);
    }

    public boolean isLineFocusMode() {
        return lineFocusMode;
    }

    public boolean isParagraphFocusMode() {
        return paragraphFocusMode;
    }

    public boolean isYoloMode() {
        return yoloMode;
    }

    public void toggleLineFocus() {
        setLineFocusMode(!lineFocusMode);
    }

    public void toggleParagraphFocus() {
        setParagraphFocusMode(!paragraphFocusMode);
    }

    public void toggleYoloMode() {
        setYoloMode(!yoloMode);
    }

    private void updateFocusHighlight() {
        if (lineFocusMode) {
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("line-focus"), true);
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("paragraph-focus"), false);
        } else if (paragraphFocusMode) {
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("paragraph-focus"), true);
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("line-focus"), false);
        } else {
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("line-focus"), false);
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("paragraph-focus"), false);
        }
    }
}
