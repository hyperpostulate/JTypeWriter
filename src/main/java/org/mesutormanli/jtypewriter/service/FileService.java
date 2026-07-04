package org.mesutormanli.jtypewriter.service;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class FileService {

    private final Messages messages;
    private Path currentFilePath;
    private String savedContent = "";
    private String currentContent = "";

    public FileService(Messages messages) {
        this.messages = messages;
    }

    public void setCurrentContent(String content) {
        this.currentContent = content;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public boolean hasUnsavedChanges() {
        return !currentContent.equals(savedContent);
    }

    public Optional<Path> openFile(Stage stage) {
        var chooser = new FileChooser();
        chooser.setTitle(messages.fileOpenTitle());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileTextFiles(), "*.txt")
        );
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileAllFiles(), "*.*")
        );

        var file = chooser.showOpenDialog(stage);
        if (file == null) return Optional.empty();

        try {
            var content = Files.readString(file.toPath());
            currentFilePath = file.toPath();
            savedContent = content;
            currentContent = content;
            return Optional.of(currentFilePath);
        } catch (IOException e) {
            showError(messages.errorOpen(), e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Path> saveFile(Stage stage, String content) {
        if (currentFilePath == null) {
            return saveFileAs(stage, content);
        }
        return writeFile(currentFilePath, content);
    }

    public Optional<Path> saveFileAs(Stage stage, String content) {
        var chooser = new FileChooser();
        chooser.setTitle(messages.fileSaveTitle());
        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        chooser.setInitialFileName("JTypeWriter_" + timestamp + ".txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileTextFile(), "*.txt")
        );

        var file = chooser.showSaveDialog(stage);
        if (file == null) return Optional.empty();

        return writeFile(file.toPath(), content);
    }

    private Optional<Path> writeFile(Path path, String content) {
        try {
            Files.writeString(path, content);
            currentFilePath = path;
            savedContent = content;
            currentContent = content;
            return Optional.of(path);
        } catch (IOException e) {
            showError(messages.errorSave(), e.getMessage());
            return Optional.empty();
        }
    }

    public void promptSaveOnExit(Stage stage) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.unsavedTitle());
        alert.setHeaderText(messages.unsavedHeader());
        alert.setContentText(messages.unsavedContent());

        var saveButton = new ButtonType(messages.unsavedSave());
        var discardButton = new ButtonType(messages.unsavedDiscard());
        var cancelButton = new ButtonType(messages.unsavedCancel());

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        var result = alert.showAndWait();
        if (result.isEmpty()) return;

        if (result.get() == saveButton) {
            saveFile(stage, currentContent).ifPresent(p -> stage.close());
        } else if (result.get() == discardButton) {
            stage.close();
        }
    }

    private void showError(String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(messages.errorTitle());
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public Path getCurrentFilePath() {
        return currentFilePath;
    }
}
