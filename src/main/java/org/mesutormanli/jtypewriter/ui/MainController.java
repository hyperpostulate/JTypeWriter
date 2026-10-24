package org.mesutormanli.jtypewriter.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.EditorKeyHandler;
import org.mesutormanli.jtypewriter.ui.component.StatsBar;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private final EditorArea editorArea;
    private final ToolbarView toolbarView;
    private final StatsBar statsBar;
    private final FileService fileService;
    private final Messages messages;
    private final LocaleManager localeManager;
    private final KeyboardShortcutHandler shortcutHandler;
    private final EditorKeyHandler editorKeyHandler;

    private boolean toolbarVisible = true;
    private Stage stage;

    @FXML
    private BorderPane rootPane;

    public MainController(EditorArea editorArea, ToolbarView toolbarView, StatsBar statsBar,
                          FileService fileService, Messages messages, LocaleManager localeManager,
                          KeyboardShortcutHandler shortcutHandler,
                          EditorKeyHandler editorKeyHandler) {
        this.editorArea = editorArea;
        this.toolbarView = toolbarView;
        this.statsBar = statsBar;
        this.fileService = fileService;
        this.messages = messages;
        this.localeManager = localeManager;
        this.shortcutHandler = shortcutHandler;
        this.editorKeyHandler = editorKeyHandler;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        toolbarView.setStage(stage);
        toolbarView.setOnYoloChange(this::refreshTitle);
        shortcutHandler.bind(editorArea, toolbarView, stage, this::toggleToolbar, toolbarView::cycleTheme);
        editorKeyHandler.bind(editorArea);
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onLoaded(java.nio.file.Path file) {
                refreshTitle();
            }

            @Override
            public void onSaved(java.nio.file.Path file) {
                refreshTitle();
            }

            @Override
            public void onNewDocument() {
                refreshTitle();
            }
        });
        // The title is composed from bundle texts, so a language switch has to
        // rebuild it; the toolbar updates its own labels through the same event.
        localeManager.addListener(language -> refreshTitle());
        refreshTitle();
    }

    /**
     * The title carries the document name, a {@code *} while there are unsaved
     * changes and the persistent YOLO badge (the toolbar may be hidden), so the
     * user can always tell what is open and whether it is safe on disk.
     */
    private void refreshTitle() {
        if (stage == null) {
            return;
        }
        var path = fileService.getCurrentFilePath();
        var documentName = path != null ? path.getFileName().toString() : messages.windowUntitled();
        stage.setTitle(titleFor(messages.appName(), documentName,
                fileService.hasUnsavedChanges(), editorArea.isYoloMode()));
    }

    /** Pure title composition, kept out of the stage so it can be unit tested. */
    static String titleFor(String appName, String documentName, boolean dirty, boolean yolo) {
        return appName + " - " + documentName + (dirty ? " *" : "") + (yolo ? "   [YOLO]" : "");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.setCenter(editorArea);
        rootPane.setTop(toolbarView);
        rootPane.setBottom(statsBar);

        toolbarView.setVisible(toolbarVisible);
        toolbarView.setManaged(toolbarVisible);
        toolbarView.setEditorArea(editorArea);

        toolbarView.setOnStyleChange(this::applyEditorStyle);
        applyEditorStyle();

        editorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            fileService.setCurrentContent(newVal);
            refreshTitle(); // keep the dirty marker in the title in step with the text
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
}
