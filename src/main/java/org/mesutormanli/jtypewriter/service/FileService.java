package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.locale.Messages;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class FileService {

    private final DialogService dialogService;
    private final Messages messages;
    private Path currentFilePath;
    private String savedContent = "";
    private String currentContent = "";

    public FileService(DialogService dialogService, Messages messages) {
        this.dialogService = dialogService;
        this.messages = messages;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public void setCurrentContent(String content) {
        this.currentContent = content;
    }

    public boolean hasUnsavedChanges() {
        return !currentContent.equals(savedContent);
    }

    public Optional<Path> openFile(javafx.stage.Stage stage) {
        var file = dialogService.showOpenDialog(stage);
        if (file.isEmpty()) return Optional.empty();

        try {
            var content = Files.readString(file.get().toPath());
            currentFilePath = file.get().toPath();
            savedContent = content;
            currentContent = content;
            return Optional.of(currentFilePath);
        } catch (IOException e) {
            dialogService.showError(messages.errorOpen(), e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Path> saveFile(javafx.stage.Stage stage, String content) {
        if (currentFilePath == null) {
            return saveFileAs(stage, content);
        }
        return writeFile(currentFilePath, content);
    }

    public Optional<Path> saveFileAs(javafx.stage.Stage stage, String content) {
        var file = dialogService.showSaveDialog(stage);
        if (file.isEmpty()) return Optional.empty();

        return writeFile(file.get().toPath(), content);
    }

    private Optional<Path> writeFile(Path path, String content) {
        try {
            Files.writeString(path, content);
            currentFilePath = path;
            savedContent = content;
            currentContent = content;
            return Optional.of(path);
        } catch (IOException e) {
            dialogService.showError(messages.errorSave(), e.getMessage());
            return Optional.empty();
        }
    }

    public void promptSaveOnExit(javafx.stage.Stage stage) {
        var choice = dialogService.showUnsavedChangesDialog(stage);
        if (choice == DialogService.SaveChoice.SAVE) {
            saveFile(stage, currentContent).ifPresent(p -> stage.close());
        } else if (choice == DialogService.SaveChoice.DISCARD) {
            stage.close();
        }
    }

    public Path getCurrentFilePath() {
        return currentFilePath;
    }
}
