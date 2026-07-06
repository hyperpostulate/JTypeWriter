package org.mesutormanli.jtypewriter.ui;

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
        editorArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCtrlShiftCombo);
    }

    private void handleKeyPress(KeyEvent event) {
        if (!event.isControlDown()) return;

        switch (event.getCode()) {
            case T -> {
                if (event.isShiftDown()) {
                    onThemeCycle.run();
                } else {
                    onToggleToolbar.run();
                }
                event.consume();
            }
            case O -> {
                fileService.openFile(stage).ifPresent(path ->
                        editorArea.setText(fileService.getCurrentContent()));
                event.consume();
            }
            case Z -> {
                editorArea.undo();
                event.consume();
            }
            case Y -> {
                editorArea.redo();
                event.consume();
            }
            case S -> {
                if (event.isShiftDown()) {
                    fileService.saveFileAs(stage, editorArea.getText());
                } else {
                    fileService.saveFile(stage, editorArea.getText());
                }
                event.consume();
            }
            case DIGIT0 -> {
                toolbarView.changeFontSize(16 - toolbarView.getCurrentFontSize());
                event.consume();
            }
            case EQUALS, PLUS -> {
                toolbarView.changeFontSize(1);
                event.consume();
            }
            case MINUS -> {
                toolbarView.changeFontSize(-1);
                event.consume();
            }
            default -> {}
        }
    }

    private void handleCtrlShiftCombo(KeyEvent event) {
        if (!event.isControlDown() || !event.isShiftDown()) return;

        switch (event.getCode()) {
            case Y -> {
                editorArea.toggleYoloMode();
                event.consume();
            }
            case C -> {
                toolbarView.cycleTextColor();
                event.consume();
            }
            default -> {}
        }
    }
}
