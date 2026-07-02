package org.mesutormanli.jtypewriter.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.StatsBar;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class MainController implements Initializable {

    private final EditorArea editorArea;
    private final ToolbarView toolbarView;
    private final StatsBar statsBar;
    private final ThemeManager themeManager;
    private final TypewriterSound typewriterSound;
    private final FileService fileService;

    private Stage stage;
    private boolean toolbarVisible;

    @FXML
    private BorderPane rootPane;

    public MainController(EditorArea editorArea, ToolbarView toolbarView, StatsBar statsBar,
                          ThemeManager themeManager, TypewriterSound typewriterSound,
                          FileService fileService) {
        this.editorArea = editorArea;
        this.toolbarView = toolbarView;
        this.statsBar = statsBar;
        this.themeManager = themeManager;
        this.typewriterSound = typewriterSound;
        this.fileService = fileService;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        toolbarView.setStage(stage);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.setCenter(editorArea);
        rootPane.setTop(toolbarView);
        rootPane.setBottom(statsBar);

        toolbarView.setVisible(false);
        toolbarView.setManaged(false);
        toolbarView.setEditorArea(editorArea);

        toolbarView.setOnStyleChange(this::applyEditorStyle);
        applyEditorStyle();

        editorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            fileService.setCurrentContent(newVal);
        });

        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        editorArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case T -> {
                        if (event.isShiftDown()) {
                            cycleTheme();
                        } else {
                            toggleToolbar();
                        }
                        event.consume();
                    }
                    case O -> {
                        fileService.openFile(stage).ifPresent(path -> {
                            try {
                                editorArea.setText(java.nio.file.Files.readString(path));
                            } catch (java.io.IOException e) {
                                // handled in service
                            }
                        });
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
                    case DIGIT0 -> resetFontSize();
                    case EQUALS, PLUS -> changeFontSize(1);
                    case MINUS -> changeFontSize(-1);
                }
            }
        });

        editorArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.isShiftDown()) {
                switch (event.getCode()) {
                    case F -> {
                        editorArea.toggleLineFocus();
                        event.consume();
                    }
                    case P -> {
                        editorArea.toggleParagraphFocus();
                        event.consume();
                    }
                    case Y -> {
                        editorArea.toggleYoloMode();
                        event.consume();
                    }
                    case C -> {
                        toolbarView.cycleTextColor();
                        event.consume();
                    }
                }
            }
        });
    }

    private void toggleToolbar() {
        toolbarVisible = !toolbarVisible;
        toolbarView.setVisible(toolbarVisible);
        toolbarView.setManaged(toolbarVisible);
    }

    private void applyEditorStyle() {
        var style = "-fx-font-size: " + toolbarView.getCurrentFontSize() + "px;";
        var cssColor = toolbarView.getCurrentTextCssColor();
        if (cssColor != null) {
            style += " -fx-text-fill: " + cssColor + ";";
        }
        editorArea.setStyle(style);
    }

    private void cycleTheme() {
        var scene = editorArea.getScene();
        if (scene != null) {
            themeManager.cycleTheme(scene);
        }
    }

    private void changeFontSize(int delta) {
        toolbarView.changeFontSize(delta);
    }

    private void resetFontSize() {
        toolbarView.changeFontSize(16 - toolbarView.getCurrentFontSize());
    }
}
