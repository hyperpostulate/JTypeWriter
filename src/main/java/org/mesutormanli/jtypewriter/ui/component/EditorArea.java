package org.mesutormanli.jtypewriter.ui.component;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.ContextMenuEvent;
import org.mesutormanli.jtypewriter.locale.Messages;

/**
 * YOLO mode: while it is enabled no user edit may remove characters that are
 * already in the document - Backspace, Delete, Cut, deleting a selection and
 * Undo/Redo are all disabled - while insertions keep working so drafting can
 * push forward.
 *
 * <p>Every destructive user edit funnels through {@link #replaceText(int, int, String)}:
 * {@code insertText}, {@code deleteText}, {@code replaceSelection}, {@code cut},
 * {@code paste} and the Backspace/Delete key bindings of {@code TextInputControl}
 * all delegate to it, so that override is the single enforcement point.
 * {@code setText} intentionally bypasses that funnel and therefore keeps working
 * in YOLO mode: loading a file must never be blocked by it.</p>
 *
 * <p>Undo and Redo are the one tunnel that does <em>not</em> pass through
 * {@link #replaceText} (they rewrite the content directly and their
 * {@code TextInputControl.undo()}/{@code redo()} entry points are final, so they
 * cannot be overridden). They are therefore blocked at every path that reaches
 * them: the toolbar buttons and the keyboard shortcuts call
 * {@link #requestUndo()}/{@link #requestRedo()}, and the native right-click menu
 * is replaced by one without Undo/Redo while YOLO is on. Undoing an insertion
 * deletes characters, so leaving any of these open would break the promise.</p>
 */
public class EditorArea extends TextArea {

    private final Messages messages;

    private boolean yoloMode;

    public EditorArea(Messages messages) {
        this.messages = messages;
        setWrapText(true);
        setPrefRowCount(40);
        getStyleClass().add("editor-area");
        // The behaviour's own right-click menu offers Undo/Redo; while YOLO is
        // on it is replaced by a menu of the non-destructive actions only.
        addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::handleContextMenuRequested);
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

    /**
     * Undo only when YOLO mode permits it. Undoing an insertion removes
     * characters, so it is a deletion and must be blocked while drafting.
     */
    public void requestUndo() {
        if (!yoloMode) {
            undo();
        }
    }

    /** Redo counterpart of {@link #requestUndo()}; blocked in YOLO mode. */
    public void requestRedo() {
        if (!yoloMode) {
            redo();
        }
    }

    private void handleContextMenuRequested(ContextMenuEvent event) {
        if (!yoloMode) {
            return; // when YOLO is off the native menu (with Undo/Redo) is correct
        }
        event.consume(); // suppress the behaviour's menu, which has Undo/Redo
        var menu = new ContextMenu(
                item(messages.menuCut(), this::cut),
                item(messages.menuCopy(), this::copy),
                item(messages.menuPaste(), this::paste),
                new SeparatorMenuItem(),
                item(messages.menuSelectAll(), this::selectAll));
        menu.show(this, event.getScreenX(), event.getScreenY());
    }

    private static MenuItem item(String text, Runnable action) {
        var menuItem = new MenuItem(text);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }
}
