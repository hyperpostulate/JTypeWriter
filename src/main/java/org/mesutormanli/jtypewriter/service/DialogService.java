/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.DialogButtons;
import org.mesutormanli.jtypewriter.ui.DialogStyling;

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
        if (chosen == null) {
            return Optional.empty();
        }
        var target = withDefaultExtension(chosen);
        // The chooser asked about overwriting the name as typed; when the .txt
        // extension turns it into a different, existing file it never saw that
        // target, so the overwrite needs its own confirmation.
        if (!target.equals(chosen) && target.exists() && !confirmOverwrite(stage, target)) {
            return Optional.empty(); // keep the current document, save cancelled
        }
        rememberDirectory(target);
        return Optional.of(target);
    }

    /** @return {@code true} when the user accepts replacing an existing file */
    private boolean confirmOverwrite(Stage stage, File target) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.overwriteTitle());
        alert.setHeaderText(messages.overwriteHeader());
        alert.setContentText(messages.overwriteContent(target.getName()));
        initOwner(alert, stage);

        var overwriteButton = DialogButtons.primary(messages.overwriteConfirm());
        var cancelButton = DialogButtons.cancel(messages.overwriteCancel());
        alert.getButtonTypes().setAll(overwriteButton, cancelButton);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == overwriteButton;
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

        var saveButton = DialogButtons.primary(messages.unsavedSave());
        var discardButton = DialogButtons.alternative(messages.unsavedDiscard());
        var cancelButton = DialogButtons.cancel(messages.unsavedCancel());

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        var result = alert.showAndWait();
        if (result.isEmpty()) return SaveChoice.CANCEL;

        if (result.get() == saveButton) return SaveChoice.SAVE;
        if (result.get() == discardButton) return SaveChoice.DISCARD;
        return SaveChoice.CANCEL;
    }

    /** How to interpret a document that is neither BOM-marked nor valid UTF-8. */
    public enum EncodingChoice { WINDOWS1254, UTF8, CANCEL }

    /**
     * What to do when the document text no longer fits the encoding it is
     * stored in: switching the document to UTF-8 keeps every character, and
     * cancelling leaves the file (and the dirty draft) untouched.
     *
     * @return {@code true} when the document must be saved as UTF-8 instead
     */
    public boolean showEncodingUpgradeDialog(Stage stage, java.nio.charset.Charset charset) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.encodingUpgradeTitle());
        alert.setHeaderText(messages.encodingUpgradeHeader());
        alert.setContentText(messages.encodingUpgradeContent(charset.displayName()));
        initOwner(alert, stage);

        var utf8Button = DialogButtons.primary(messages.encodingUpgradeSave());
        var cancelButton = DialogButtons.cancel(messages.encodingUpgradeCancel());
        alert.getButtonTypes().setAll(utf8Button, cancelButton);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == utf8Button;
    }

    /**
     * Asks whether to open a file whose byte-order mark declares an encoding
     * its bytes do not satisfy. Accepting opens it with replacement characters
     * (lossy); cancelling aborts the open so the current document is untouched.
     *
     * @return {@code true} when the user accepts the lossy decode
     */
    public boolean showMalformedEncodingDialog(Stage stage, java.nio.file.Path file,
            java.nio.charset.Charset declared) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.encodingMalformedTitle());
        alert.setHeaderText(messages.encodingMalformedHeader() + " - " + file.getFileName());
        alert.setContentText(messages.encodingMalformedContent(declared.displayName()));
        initOwner(alert, stage);

        var openButton = DialogButtons.primary(messages.encodingMalformedOpen());
        var cancelButton = DialogButtons.cancel(messages.encodingMalformedCancel());
        alert.getButtonTypes().setAll(openButton, cancelButton);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == openButton;
    }

    public EncodingChoice showEncodingDialog(Stage stage, java.nio.file.Path file) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.encodingTitle());
        alert.setHeaderText(messages.encodingHeader() + " - " + file.getFileName());
        alert.setContentText(messages.encodingContent());
        initOwner(alert, stage);

        // UTF-8 is the default because it is the lossless choice: it round-trips
        // every character already in the buffer, while Windows-1254 can only
        // represent a subset (and that is why it is merely an alternative).
        var turkishButton = DialogButtons.alternative(messages.encodingTurkish());
        var utf8Button = DialogButtons.primary(messages.encodingUtf8());
        var cancelButton = DialogButtons.cancel(messages.encodingCancel());
        alert.getButtonTypes().setAll(turkishButton, utf8Button, cancelButton);

        var result = alert.showAndWait();
        if (result.isEmpty()) return EncodingChoice.CANCEL;

        if (result.get() == turkishButton) return EncodingChoice.WINDOWS1254;
        if (result.get() == utf8Button) return EncodingChoice.UTF8;
        return EncodingChoice.CANCEL;
    }

    /**
     * Asks what to do with a surviving draft. Buttons are restore, discard and
     * later; closing the prompt (window X, Escape) is treated as "later", never
     * as "discard", so a stray click cannot delete the only copy of the text.
     */
    public RecoveryChoice showRecoveryDialog(Stage stage, Instant savedAt) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.recoveryTitle());
        alert.setHeaderText(messages.recoveryHeader());
        alert.setContentText(messages.recoveryContent(format(savedAt)));
        initOwner(alert, stage);

        var restoreButton = DialogButtons.primary(messages.recoveryYes());
        var discardButton = DialogButtons.alternative(messages.recoveryNo());
        // "Later" deliberately gets the cancel semantics: Escape and the window X
        // already mean "keep the draft, decide later" (see the Javadoc above and
        // choiceFor), so cancel must land on Later - never on the destructive
        // Discard button, which would turn a stray Escape into data loss.
        var laterButton = DialogButtons.cancel(messages.recoveryLater());
        alert.getButtonTypes().setAll(restoreButton, discardButton, laterButton);

        var result = alert.showAndWait();
        int selected = result.map(alert.getButtonTypes()::indexOf).orElse(-1);
        return choiceFor(selected);
    }

    /**
     * Pure answer mapping for {@link #showRecoveryDialog}: {@code -1} means the
     * prompt was closed without a choice and must keep the draft.
     */
    static RecoveryChoice choiceFor(int selectedIndex) {
        return switch (selectedIndex) {
            case 0 -> RecoveryChoice.RESTORE;
            case 1 -> RecoveryChoice.DISCARD;
            default -> RecoveryChoice.LATER;
        };
    }

    public void showError(Stage owner, String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(messages.errorTitle());
        alert.setHeaderText(header);
        alert.setContentText(content);
        initOwner(alert, owner);
        alert.showAndWait();
    }

    /** Owns the dialog to its stage so it can never hide behind the main window. */
    private static void initOwner(Alert alert, Stage stage) {
        if (stage != null) {
            alert.initOwner(stage);
        }
        // One call covers every alert above: the pane only gets themed when the
        // scene stylesheets reach it, never on its own (analysis U3).
        DialogStyling.applyFromScene(
                alert.getDialogPane(), stage == null ? null : stage.getScene());
    }

    private static String format(Instant instant) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}