package org.mesutormanli.jtypewriter.ui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.service.FileService;
import org.springframework.stereotype.Component;

@Component
public class MainStage {

    private final FileService fileService;

    public MainStage(FileService fileService) {
        this.fileService = fileService;
    }

    public void configure(Stage stage, Scene scene) {
        stage.setMaximized(true);
        stage.setFullScreen(false);
        stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("Escape"));
        stage.setFullScreenExitHint("");

        stage.setOnCloseRequest(event -> {
            if (fileService.hasUnsavedChanges()) {
                event.consume();
                fileService.promptSaveOnExit(stage);
            }
        });
    }
}
