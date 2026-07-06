package org.mesutormanli.jtypewriter.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.EditorKeyHandler;
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
    private final FileService fileService;
    private final KeyboardShortcutHandler shortcutHandler;
    private final EditorKeyHandler editorKeyHandler;

    private boolean toolbarVisible;

    @FXML
    private BorderPane rootPane;

    public MainController(EditorArea editorArea, ToolbarView toolbarView, StatsBar statsBar,
                          ThemeManager themeManager, FileService fileService,
                          KeyboardShortcutHandler shortcutHandler,
                          EditorKeyHandler editorKeyHandler) {
        this.editorArea = editorArea;
        this.toolbarView = toolbarView;
        this.statsBar = statsBar;
        this.themeManager = themeManager;
        this.fileService = fileService;
        this.shortcutHandler = shortcutHandler;
        this.editorKeyHandler = editorKeyHandler;
    }

    public void setStage(Stage stage) {
        toolbarView.setStage(stage);
        shortcutHandler.bind(editorArea, toolbarView, stage, this::toggleToolbar, this::cycleTheme);
        editorKeyHandler.bind(editorArea);
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

        editorArea.textProperty().addListener((obs, oldVal, newVal) ->
                fileService.setCurrentContent(newVal));
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
}
