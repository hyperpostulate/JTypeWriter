/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.event.EventTarget;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;

public class KeyboardShortcutHandler {

    private final FileService fileService;

    private EditorArea editorArea;
    private ToolbarView toolbarView;
    private Stage stage;
    private Runnable onToggleToolbar;
    private Runnable onThemeCycle;

    public KeyboardShortcutHandler(FileService fileService) {
        this.fileService = fileService;
    }

    public void bind(EditorArea editorArea, ToolbarView toolbarView, Stage stage,
                     Runnable onToggleToolbar, Runnable onThemeCycle) {
        this.editorArea = editorArea;
        this.toolbarView = toolbarView;
        this.stage = stage;
        this.onToggleToolbar = onToggleToolbar;
        this.onThemeCycle = onThemeCycle;

        // Scene-level, not editor-level: shortcuts must keep working when the
        // focus sits on a toolbar button or anywhere else in the window.
        var scene = stage != null ? stage.getScene() : null;
        EventTarget target = scene != null ? scene : editorArea;
        target.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    /** Decoded meaning of a shortcut key press. */
    enum ShortcutAction {
        NONE, TOGGLE_TOOLBAR, CYCLE_THEME, UNDO, REDO, TOGGLE_YOLO, CYCLE_TEXT_COLOR,
        OPEN, NEW_DOCUMENT, SAVE, SAVE_AS, FONT_RESET, FONT_UP, FONT_DOWN, TOGGLE_FULLSCREEN
    }

    /**
     * Maps a shortcut key to its action. Kept free of UI state so the
     * shift-sensitive combos can be unit tested headlessly: with two separate
     * key filters the Ctrl+Shift+Y combination leaked into the Ctrl+Y branch and
     * ran Redo instead of toggling YOLO mode. Numpad keys are accepted as
     * equivalents of their main-keyboard neighbours (Ctrl+Num0 resets the font
     * just like Ctrl+0). Ctrl+N and Ctrl+Shift+N both start a new document.
     */
    static ShortcutAction resolve(KeyCode code, boolean shift) {
        return switch (code) {
            case T -> shift ? ShortcutAction.CYCLE_THEME : ShortcutAction.TOGGLE_TOOLBAR;
            case Y -> shift ? ShortcutAction.TOGGLE_YOLO : ShortcutAction.REDO;
            case Z -> shift ? ShortcutAction.REDO : ShortcutAction.UNDO;
            case C -> shift ? ShortcutAction.CYCLE_TEXT_COLOR : ShortcutAction.NONE;
            case O -> ShortcutAction.OPEN;
            case N -> ShortcutAction.NEW_DOCUMENT;
            case S -> shift ? ShortcutAction.SAVE_AS : ShortcutAction.SAVE;
            case DIGIT0, NUMPAD0 -> ShortcutAction.FONT_RESET;
            case EQUALS, PLUS, ADD -> ShortcutAction.FONT_UP;
            case MINUS, SUBTRACT -> ShortcutAction.FONT_DOWN;
            case F11 -> ShortcutAction.TOGGLE_FULLSCREEN;
            default -> ShortcutAction.NONE;
        };
    }

    /**
     * {@code isShortcutDown()} is Ctrl on Windows/Linux and Cmd on macOS, so
     * the shortcuts match each platform's conventions; plain Ctrl on macOS is
     * accepted too, for users who switch between platforms.
     */
    private void handleKeyPress(KeyEvent event) {
        // F11 is the only modifier-less shortcut, so it passes the guard.
        if (event.getCode() != KeyCode.F11 && !event.isShortcutDown() && !event.isControlDown()) return;

        switch (resolve(event.getCode(), event.isShiftDown())) {
            case TOGGLE_TOOLBAR -> onToggleToolbar.run();
            case CYCLE_THEME -> onThemeCycle.run();
            case UNDO -> editorArea.requestUndo(); // blocked while YOLO mode is on
            case REDO -> editorArea.requestRedo();
            case TOGGLE_YOLO -> toolbarView.toggleYolo();
            case CYCLE_TEXT_COLOR -> toolbarView.cycleTextColor();
            case OPEN -> fileService.openFile(stage).ifPresent(path ->
                    editorArea.setDocument(fileService.getCurrentContent()));
            case NEW_DOCUMENT -> {
                if (fileService.newDocument(stage)) {
                    editorArea.setDocument("");
                }
            }
            case SAVE -> fileService.saveFile(stage, editorArea.getText());
            case SAVE_AS -> fileService.saveFileAs(stage, editorArea.getText());
            case FONT_RESET -> toolbarView.resetFontSize();
            case FONT_UP -> toolbarView.changeFontSize(1);
            case FONT_DOWN -> toolbarView.changeFontSize(-1);
            case TOGGLE_FULLSCREEN -> toggleFullScreen();
            case NONE -> {
                return; // e.g. plain Ctrl+C (Copy) must never be consumed
            }
        }
        event.consume();
    }

    private void toggleFullScreen() {
        if (stage != null) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }
}
