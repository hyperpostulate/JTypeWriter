package org.mesutormanli.jtypewriter.ui;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;
import org.springframework.stereotype.Component;

@Component
public class KeyboardShortcutHandler {

    private final FileService fileService;
    private final ThemeManager themeManager;

    private EditorArea editorArea;
    private ToolbarView toolbarView;
    private Stage stage;
    private Runnable onToggleToolbar;
    private Runnable onThemeCycle;

    public KeyboardShortcutHandler(FileService fileService, ThemeManager themeManager) {
        this.fileService = fileService;
        this.themeManager = themeManager;
    }

    public void bind(EditorArea editorArea, ToolbarView toolbarView, Stage stage,
                     Runnable onToggleToolbar, Runnable onThemeCycle) {
        this.editorArea = editorArea;
        this.toolbarView = toolbarView;
        this.stage = stage;
        this.onToggleToolbar = onToggleToolbar;
        this.onThemeCycle = onThemeCycle;

        editorArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    /** Decoded meaning of a Ctrl-combination key press. */
    enum ShortcutAction {
        NONE, TOGGLE_TOOLBAR, CYCLE_THEME, UNDO, REDO, TOGGLE_YOLO, CYCLE_TEXT_COLOR,
        OPEN, SAVE, SAVE_AS, FONT_RESET, FONT_UP, FONT_DOWN
    }

    /**
     * Maps a Ctrl-combination to its action. Kept free of UI state so the
     * shift-sensitive combos can be unit tested headlessly: with two separate
     * key filters the Ctrl+Shift+Y combination leaked into the Ctrl+Y branch and
     * ran Redo instead of toggling YOLO mode.
     */
    static ShortcutAction resolve(KeyCode code, boolean shift) {
        return switch (code) {
            case T -> shift ? ShortcutAction.CYCLE_THEME : ShortcutAction.TOGGLE_TOOLBAR;
            case Y -> shift ? ShortcutAction.TOGGLE_YOLO : ShortcutAction.REDO;
            case Z -> shift ? ShortcutAction.REDO : ShortcutAction.UNDO;
            case C -> shift ? ShortcutAction.CYCLE_TEXT_COLOR : ShortcutAction.NONE;
            case O -> ShortcutAction.OPEN;
            case S -> shift ? ShortcutAction.SAVE_AS : ShortcutAction.SAVE;
            case DIGIT0 -> ShortcutAction.FONT_RESET;
            case EQUALS, PLUS -> ShortcutAction.FONT_UP;
            case MINUS -> ShortcutAction.FONT_DOWN;
            default -> ShortcutAction.NONE;
        };
    }

    private void handleKeyPress(KeyEvent event) {
        if (!event.isControlDown()) return;

        switch (resolve(event.getCode(), event.isShiftDown())) {
            case TOGGLE_TOOLBAR -> onToggleToolbar.run();
            case CYCLE_THEME -> onThemeCycle.run();
            case UNDO -> editorArea.undo();
            case REDO -> editorArea.redo();
            case TOGGLE_YOLO -> toolbarView.toggleYolo();
            case CYCLE_TEXT_COLOR -> toolbarView.cycleTextColor();
            case OPEN -> fileService.openFile(stage).ifPresent(path ->
                    editorArea.setText(fileService.getCurrentContent()));
            case SAVE -> fileService.saveFile(stage, editorArea.getText());
            case SAVE_AS -> fileService.saveFileAs(stage, editorArea.getText());
            case FONT_RESET -> toolbarView.changeFontSize(16 - toolbarView.getCurrentFontSize());
            case FONT_UP -> toolbarView.changeFontSize(1);
            case FONT_DOWN -> toolbarView.changeFontSize(-1);
            case NONE -> {
                return; // e.g. plain Ctrl+C (Copy) must never be consumed
            }
        }
        event.consume();
    }
}
