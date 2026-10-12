package org.mesutormanli.jtypewriter.service;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

public class DialogService {

    /** Extension added when the user types an extension-less file name. */
    static final String DEFAULT_EXTENSION = ".txt";

    private final Messages messages;
    private final AppSettings settings;

    public DialogService(Messages messages, AppSettings settings) {
        this.messages = messages;
        this.settings = settings;
    }

    public Optional<File> showOpenDialog(Stage stage) {
        var chooser = new FileChooser();
        chooser.setTitle(messages.fileOpenTitle());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileTextFiles(), "*.txt")
        );
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileAllFiles(), "*.*")
        );
        applyInitialDirectory(chooser, settings.getLastDirectory());
        var chosen = chooser.showOpenDialog(stage);
        if (chosen != null) {
            rememberDirectory(chosen);
        }
        return Optional.ofNullable(chosen);
    }

    /**
     * Save-as dialog. For a document that already lives on disk the current
     * name and directory are offered (so "Save As" on {@code novel.txt} does
     * not suggest a new, timestamped file and silently fork the document);
     * only a never-saved document gets the timestamped default name.
     */
    public Optional<File> showSaveDialog(Stage stage, Path currentFile) {
        var chooser = new FileChooser();
        chooser.setTitle(messages.fileSaveTitle());
        if (currentFile != null) {
            var parent = currentFile.toAbsolutePath().getParent();
            if (parent != null) {
                applyInitialDirectory(chooser, Optional.of(parent));
            }
            chooser.setInitialFileName(currentFile.getFileName().toString());
        } else {
            applyInitialDirectory(chooser, settings.getLastDirectory());
            var timestamp = java.time.LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            chooser.setInitialFileName("JTypeWriter_" + timestamp + DEFAULT_EXTENSION);
        }
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(messages.fileTextFile(), "*.txt")
        );
        var chosen = chooser.showSaveDialog(stage);
        if (chosen != null) {
            var target = withDefaultExtension(chosen);
            rememberDirectory(target);
            return Optional.of(target);
        }
        return Optional.empty();
    }

    /** {@code notes} becomes {@code notes.txt}; {@code notes.md} stays as typed. */
    static File withDefaultExtension(File file) {
        var name = file.getName();
        if (name.contains(".")) {
            return file;
        }
        return file.toPath().resolveSibling(name + DEFAULT_EXTENSION).toFile();
    }

    private void rememberDirectory(File file) {
        var parent = file.getParentFile();
        if (parent != null) {
            settings.setLastDirectory(parent.toPath());
        }
    }

    private static void applyInitialDirectory(FileChooser chooser, Optional<Path> directory) {
        directory.filter(Files::isDirectory).map(Path::toFile).ifPresent(chooser::setInitialDirectory);
    }

    public enum SaveChoice { SAVE, DISCARD, CANCEL }

    public SaveChoice showUnsavedChangesDialog(Stage stage) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.unsavedTitle());
        alert.setHeaderText(messages.unsavedHeader());
        alert.setContentText(messages.unsavedContent());
        initOwner(alert, stage);

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

    /** How to interpret a document that is neither BOM-marked nor valid UTF-8. */
    public enum EncodingChoice { WINDOWS1254, UTF8, CANCEL }

    public EncodingChoice showEncodingDialog(Stage stage, java.nio.file.Path file) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.encodingTitle());
        alert.setHeaderText(messages.encodingHeader() + " - " + file.getFileName());
        alert.setContentText(messages.encodingContent());
        initOwner(alert, stage);

        var turkishButton = new ButtonType(messages.encodingTurkish());
        var utf8Button = new ButtonType(messages.encodingUtf8());
        var cancelButton = new ButtonType(messages.encodingCancel());
        alert.getButtonTypes().setAll(turkishButton, utf8Button, cancelButton);

        var result = alert.showAndWait();
        if (result.isEmpty()) return EncodingChoice.CANCEL;

        if (result.get() == turkishButton) return EncodingChoice.WINDOWS1254;
        if (result.get() == utf8Button) return EncodingChoice.UTF8;
        return EncodingChoice.CANCEL;
    }

    /** @return {@code true} when the user wants the autosaved draft restored */
    public boolean showRecoveryDialog(Stage stage, Instant savedAt) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.recoveryTitle());
        alert.setHeaderText(messages.recoveryHeader());
        alert.setContentText(messages.recoveryContent(format(savedAt)));
        initOwner(alert, stage);

        var restoreButton = new ButtonType(messages.recoveryYes());
        var discardButton = new ButtonType(messages.recoveryNo());
        alert.getButtonTypes().setAll(restoreButton, discardButton);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == restoreButton;
    }

    public void showError(String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(messages.errorTitle());
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /** Owns the dialog to its stage so it can never hide behind the main window. */
    private static void initOwner(Alert alert, Stage stage) {
        if (stage != null) {
            alert.initOwner(stage);
        }
    }

    private static String format(Instant instant) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}