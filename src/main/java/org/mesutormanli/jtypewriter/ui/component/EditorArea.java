/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.ContextMenuEvent;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.stats.TextChange;

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
 *
 * <p>Cut is shown greyed out so the menu stays predictable instead of silently
 * losing an entry while YOLO is on.</p>
 */
public class EditorArea extends TextArea {

    private final Messages messages;

    private boolean yoloMode;

    /**
     * The replacement most recently applied through {@link #replaceText},
     * waiting for the text listener to consume it. {@code null} means the
     * change did not pass through the override (undo/redo and document loads
     * rewrite the content directly), so the listener must rescan instead.
     */
    private TextChange pendingChange;

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

    /**
     * Enforcement point of YOLO mode:
     * <ul>
     *   <li>pure insertion ({@code start == end}) is always allowed;</li>
     *   <li>pure deletion (empty {@code text}) is blocked;</li>
     *   <li>a replacement inserts the new text without removing the old one, so
     *       typing or pasting over a selection never loses characters.</li>
     * </ul>
     *
     * <p>This is also the one place where the applied edit is recorded, so the
     * incremental statistics can adjust a window instead of rescanning the
     * document. The record is the <em>applied</em> change, after the YOLO
     * rewrite: a blocked deletion records nothing and an
     * {@code INSERT_WITHOUT_DELETION} records an insertion at {@code start},
     * which is what the content actually did.</p>
     */
    @Override
    public void replaceText(int start, int end, String text) {
        pendingChange = null;
        var describable = text != null && start >= 0 && start <= end && end <= getLength();
        switch (YoloPolicy.decide(yoloMode, start, end, text, getLength())) {
            case APPLY -> {
                if (describable) {
                    pendingChange = new TextChange(start, end - start, text);
                }
                super.replaceText(start, end, text);
            }
            case BLOCK -> {
                // Backspace / Delete / Cut / deleting a selection: ignored while YOLO is on
            }
            case INSERT_WITHOUT_DELETION -> {
                pendingChange = new TextChange(start, 0, text);
                super.replaceText(start, start, text);
            }
        }
    }

    /**
     * Applies a replacement <em>without</em> the YOLO funnel of
     * {@link #replaceText(int, int, String)}.
     *
     * <p>This is the only sanctioned bypass, and only the find panel calls it.
     * The panel asks {@link org.mesutormanli.jtypewriter.ui.FindReplacePolicy}
     * first: while YOLO mode is on that returns {@code REQUIRED}, and nothing
     * is written until {@link org.mesutormanli.jtypewriter.service.DialogService#confirmReplaceInYolo}
     * has been accepted. With YOLO off every replacement is an ordinary edit,
     * so the funnel would already apply it unchanged.</p>
     *
     * <p>Without this bypass YOLO's {@code INSERT_WITHOUT_DELETION} branch would
     * keep the matched characters and insert the replacement beside them,
     * duplicating text instead of replacing it - the opposite of what the user
     * asked for and of what {@code FindModel} computed. The find panel's whole
     * job is to remove exactly the range it matched, so the deletion it applies
     * is the one deletion YOLO has explicitly consented to.</p>
     */
    public void replaceConfirmed(int start, int end, String text) {
        super.replaceText(start, end, text);
    }

    /**
     * The edit applied by the most recent {@link #replaceText}, or {@code null}
     * when the change came from a path that bypasses it. Consumed exactly once
     * by the text listener, so a hint can never describe a later edit.
     */
    public TextChange takeChange() {
        var change = pendingChange;
        pendingChange = null;
        return change;
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
     * Replaces the editor content with a loaded document and resets the
     * undo/redo history.
     *
     * <p>Every document load - opening a file, session restore, a recovered
     * draft and a new document - must go through this method, so the undo stack
     * can never cross a document boundary (Ctrl+Z must not restore the previous
     * document's text). {@code setText} is still what does both jobs: it
     * bypasses the YOLO-mode funnel, so loading is never blocked while drafting,
     * and {@code TextInputControl.TextProperty.doSet} clears the undo manager
     * itself, because a programmatic text set means the control may be reused
     * for another document.</p>
     *
     * <p>That reset used to be done here by reflection, by setting the private
     * {@code undoChange} field to {@code null}. The field is not a record but a
     * cursor into a linked list, initialised to the sentinel
     * {@code undoChangeHead} and dereferenced - never null-checked - by
     * {@code updateContent} on the next edit. Nulling it therefore turned the
     * first keystroke after every document load into an
     * {@code NullPointerException} inside JavaFX, and the next {@code setText}
     * into another one in {@code resetUndoRedoState}. The framework already
     * performed this exact reset, so the reflection was removed rather than
     * corrected: there is no API to reach for, and nothing left to reach for.</p>
     */
    public void setDocument(String text) {
        setText(text);
        deselect();
        positionCaret(0);
        setScrollTop(0);
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
        var cut = item(messages.menuCut(), this::cut);
        cut.setDisable(true); // Cut is a deletion: visible but not usable in YOLO mode
        var menu = new ContextMenu(
                cut,
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
