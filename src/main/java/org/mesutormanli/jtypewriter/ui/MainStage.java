package org.mesutormanli.jtypewriter.ui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.service.FileService;

public class MainStage {

    private final FileService fileService;
    private final AppSettings settings;

    public MainStage(FileService fileService, AppSettings settings) {
        this.fileService = fileService;
        this.settings = settings;
    }

    public void configure(Stage stage, Scene scene) {
        // Restore the geometry of the last session; a maximized window keeps
        // its unmaximized bounds so leaving maximized lands where it used to.
        var saved = settings.getWindowState();
        if (saved.isPresent()) {
            var state = saved.get();
            if (!state.maximized() && state.width() > 0 && state.height() > 0) {
                stage.setX(state.x());
                stage.setY(state.y());
                stage.setWidth(state.width());
                stage.setHeight(state.height());
            }
            stage.setMaximized(state.maximized());
        } else {
            stage.setMaximized(true);
        }
        stage.setFullScreen(false);
        stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("Escape"));
        stage.setFullScreenExitHint("");

        stage.setOnCloseRequest(event -> {
            if (fileService.hasUnsavedChanges()) {
                event.consume();
                fileService.promptSaveOnExit(stage);
            }
        });

        // Geometry is remembered on hide, so it survives both a clean close
        // and a close that goes through the unsaved-changes prompt.
        stage.setOnHidden(event -> settings.setWindowState(new AppSettings.WindowState(
                stage.isMaximized(), stage.getX(), stage.getY(),
                stage.getWidth(), stage.getHeight())));
    }
}