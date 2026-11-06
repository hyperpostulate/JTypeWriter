/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.event.EventTarget;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;

/**
 * Routes key presses and toolbar buttons to {@link EditorAction}: the chord
 * table lives in the action inventory, and the effect is this class's
 * {@link EditorAction.Context} implementation. Both entry points call
 * {@link #run(EditorAction)}, so a toolbar button and its shortcut cannot end
 * up doing different things.
 */
public class KeyboardShortcutHandler implements EditorAction.Context {

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

    /** Runs a command; the one entry point shared by the key filter and the toolbar. */
    public void run(EditorAction action) {
        action.run(this);
    }

    /**
     * The guard accepts the platform shortcut modifier (Ctrl on Windows/Linux)
     * and plain Ctrl as well, so a shortcut is not lost when the JavaFX
     * {@code shortcutKey} differs from the physical Ctrl key. Only the chords
     * that do not require Ctrl (F11) pass without a modifier.
     */
    private void handleKeyPress(KeyEvent event) {
        var action = EditorAction.resolve(event.getCode(), event.isShiftDown());
        if (action.isEmpty()) {
            return; // e.g. plain Ctrl+C (Copy) must never be consumed
        }
        if (action.get().accelerators().stream().noneMatch(accelerator -> !accelerator.requiresCtrl())
                && !event.isShortcutDown() && !event.isControlDown()) {
            return;
        }
        run(action.get());
        event.consume();
    }

    // --- EditorAction.Context ------------------------------------------------

    @Override
    public void toggleToolbar() {
        onToggleToolbar.run();
    }

    @Override
    public void cycleTheme() {
        onThemeCycle.run();
    }

    @Override
    public void undo() {
        if (editorArea != null) {
            editorArea.requestUndo(); // blocked while YOLO mode is on
        }
    }

    @Override
    public void redo() {
        if (editorArea != null) {
            editorArea.requestRedo();
        }
    }

    @Override
    public void toggleYolo() {
        if (toolbarView != null) {
            toolbarView.toggleYolo();
        }
    }

    @Override
    public void cycleTextColor() {
        if (toolbarView != null) {
            toolbarView.cycleTextColor();
        }
    }

    @Override
    public void openDocument() {
        fileService.openFileAsync(stage).thenAccept(opened -> opened.ifPresent(path ->
                editorArea.setDocument(fileService.getCurrentContent())));
    }

    @Override
    public void newDocument() {
        fileService.newDocumentAsync(stage).thenAccept(created -> {
            if (created) {
                editorArea.setDocument("");
            }
        });
    }

    @Override
    public void save() {
        fileService.saveFileAsync(stage, editorArea.getText());
    }

    @Override
    public void saveAs() {
        fileService.saveFileAsAsync(stage, editorArea.getText());
    }

    @Override
    public void increaseFontSize() {
        if (toolbarView != null) {
            toolbarView.changeFontSize(1);
        }
    }

    @Override
    public void decreaseFontSize() {
        if (toolbarView != null) {
            toolbarView.changeFontSize(-1);
        }
    }

    @Override
    public void resetFontSize() {
        if (toolbarView != null) {
            toolbarView.resetFontSize();
        }
    }

    @Override
    public void toggleFullScreen() {
        if (stage != null) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    @Override
    public void exitFullScreen() {
        // JavaFX performs this through the stage's fullScreenExitKeyCombination;
        // the action exists so the shortcut card and the inventory agree.
        if (stage != null) {
            stage.setFullScreen(false);
        }
    }
}
