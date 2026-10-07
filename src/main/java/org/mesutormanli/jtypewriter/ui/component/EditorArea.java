package org.mesutormanli.jtypewriter.ui.component;

import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

/**
 * YOLO mode: while it is enabled no user edit may remove characters that are
 * already in the document - Backspace, Delete, Cut and deleting a selection are
 * all disabled - while insertions keep working so drafting can push forward.
 *
 * <p>Every destructive user edit funnels through {@link #replaceText(int, int, String)}:
 * {@code insertText}, {@code deleteText}, {@code replaceSelection}, {@code cut},
 * {@code paste} and the Backspace/Delete key bindings of {@code TextInputControl}
 * all delegate to it, so that override is the single enforcement point.
 * {@code setText}, {@code undo()} and {@code redo()} intentionally bypass that
 * funnel and therefore keep working in YOLO mode: loading a file must never be
 * blocked by it.</p>
 */
@Component
public class EditorArea extends TextArea {

    private boolean yoloMode;

    public EditorArea() {
        setWrapText(true);
        setPrefRowCount(40);
        getStyleClass().add("editor-area");
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

    /**
     * Enforcement point of YOLO mode:
     * <ul>
     *   <li>pure insertion ({@code start == end}) is always allowed;</li>
     *   <li>pure deletion (empty {@code text}) is blocked;</li>
     *   <li>a replacement inserts the new text without removing the old one, so
     *       typing or pasting over a selection never loses characters.</li>
     * </ul>
     */
    @Override
    public void replaceText(int start, int end, String text) {
        switch (YoloPolicy.decide(yoloMode, start, end, text, getLength())) {
            case APPLY -> super.replaceText(start, end, text);
            case BLOCK -> {
                // Backspace / Delete / Cut / deleting a selection: ignored while YOLO is on
            }
            case INSERT_WITHOUT_DELETION -> super.replaceText(start, start, text);
        }
    }

    /** {@code clear()} goes through {@code setText} and would bypass {@link #replaceText}. */
    @Override
    public void clear() {
        if (yoloMode) {
            return;
        }
        super.clear();
    }
}
