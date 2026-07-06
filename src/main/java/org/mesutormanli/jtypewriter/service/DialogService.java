package org.mesutormanli.jtypewriter.service;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DialogService {

    private final Messages messages;

    public DialogService(Messages messages) {
        this.messages = messages;
    }

    public Optional<java.io.File> showOpenDialog(Stage stage) {
        var chooser = new FileChooser();
        chooser.setTitle(messages.fileOpenTitle());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileTextFiles(), "*.txt")
        );
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileAllFiles(), "*.*")
        );
        return Optional.ofNullable(chooser.showOpenDialog(stage));
    }

    public Optional<java.io.File> showSaveDialog(Stage stage) {
        var chooser = new FileChooser();
        chooser.setTitle(messages.fileSaveTitle());
        var timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        chooser.setInitialFileName("JTypeWriter_" + timestamp + ".txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileTextFile(), "*.txt")
        );
        return Optional.ofNullable(chooser.showSaveDialog(stage));
    }

    public enum SaveChoice { SAVE, DISCARD, CANCEL }

    public SaveChoice showUnsavedChangesDialog(Stage stage) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.unsavedTitle());
        alert.setHeaderText(messages.unsavedHeader());
        alert.setContentText(messages.unsavedContent());

        var saveButton = new ButtonType(messages.unsavedSave());
        var discardButton = new ButtonType(messages.unsavedDiscard());
        var cancelButton = new ButtonType(messages.unsavedCancel());

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        var result = alert.showAndWait();
        if (result.isEmpty()) return SaveChoice.CANCEL;

        if (result.get() == saveButton) return SaveChoice.SAVE;
        if (result.get() == discardButton) return SaveChoice.DISCARD;
        return SaveChoice.CANCEL;
    }

    public void showError(String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(messages.errorTitle());
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
