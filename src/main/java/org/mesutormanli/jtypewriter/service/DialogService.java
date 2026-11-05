/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.DialogButtons;
import org.mesutormanli.jtypewriter.ui.DialogSizing;
import org.mesutormanli.jtypewriter.ui.DialogStyling;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Every user-facing {@link Alert} of the application, plus the file choosers.
 *
 * <p>The single construction site is deliberate: alerts used to be built in
 * {@code SingleInstanceWarning} and {@code CrashHandler} too, and each copy
 * missed part of the preparation (theme, owner, screen clamp, text wrap), so
 * the same dialog could look and behave differently depending on which code
 * path opened it. {@link #prepare(Alert, Stage)} now runs for all of them, and
 * the source-scan test in {@code DialogServiceTest} keeps any other alert
 * construction from appearing in the sources.
 */
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
            chooser.setInitialFileName("JTypeWriter_" + fileStamp(LocalDateTime.now()) + DEFAULT_EXTENSION);
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

        var overwriteButton = DialogButtons.primary(messages.overwriteConfirm());
        var cancelButton = DialogButtons.cancel(messages.overwriteCancel());
        alert.getButtonTypes().setAll(overwriteButton, cancelButton);

        var result = show(alert, stage);
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

        var saveButton = DialogButtons.primary(messages.unsavedSave());
        var discardButton = DialogButtons.alternative(messages.unsavedDiscard());
        var cancelButton = DialogButtons.cancel(messages.unsavedCancel());

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        var result = show(alert, stage);
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

        var utf8Button = DialogButtons.primary(messages.encodingUpgradeSave());
        var cancelButton = DialogButtons.cancel(messages.encodingUpgradeCancel());
        alert.getButtonTypes().setAll(utf8Button, cancelButton);

        var result = show(alert, stage);
        return result.isPresent() && result.get() == utf8Button;
    }

    /** What to do when the file on disk changed after the editor loaded it. */
    public enum ExternalChangeChoice { OVERWRITE, SAVE_AS, CANCEL }

    /**
     * Asks what to do about a file that another program changed or deleted
     * after it was opened here. Overwriting is the deliberate choice; Save As
     * keeps both versions; closing the window or choosing Cancel writes
     * nothing and leaves the draft dirty.
     */
    public ExternalChangeChoice showExternalChangeDialog(Stage stage, Path file) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.externalChangeTitle());
        alert.setHeaderText(messages.externalChangeHeader()
                + messages.dashSeparator() + file.getFileName());
        alert.setContentText(messages.externalChangeContent());

        var overwriteButton = DialogButtons.primary(messages.externalChangeOverwrite());
        var saveAsButton = DialogButtons.alternative(messages.externalChangeSaveAs());
        // Cancel (and the window X) is the non-destructive answer: nothing is
        // written and the recovery draft keeps the only copy of the editor text.
        var cancelButton = DialogButtons.cancel(messages.externalChangeCancel());
        alert.getButtonTypes().setAll(overwriteButton, saveAsButton, cancelButton);

        var result = show(alert, stage);
        if (result.isEmpty()) return ExternalChangeChoice.CANCEL;
        if (result.get() == overwriteButton) return ExternalChangeChoice.OVERWRITE;
        if (result.get() == saveAsButton) return ExternalChangeChoice.SAVE_AS;
        return ExternalChangeChoice.CANCEL;
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
        alert.setHeaderText(messages.encodingMalformedHeader()
                + messages.dashSeparator() + file.getFileName());
        alert.setContentText(messages.encodingMalformedContent(declared.displayName()));

        var openButton = DialogButtons.primary(messages.encodingMalformedOpen());
        var cancelButton = DialogButtons.cancel(messages.encodingMalformedCancel());
        alert.getButtonTypes().setAll(openButton, cancelButton);

        var result = show(alert, stage);
        return result.isPresent() && result.get() == openButton;
    }

    public EncodingChoice showEncodingDialog(Stage stage, java.nio.file.Path file) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(messages.encodingTitle());
        alert.setHeaderText(messages.encodingHeader()
                + messages.dashSeparator() + file.getFileName());
        alert.setContentText(messages.encodingContent());

        // UTF-8 is the default because it is the lossless choice: it round-trips
        // every character already in the buffer, while Windows-1254 can only
        // represent a subset (and that is why it is merely an alternative).
        var turkishButton = DialogButtons.alternative(messages.encodingTurkish());
        var utf8Button = DialogButtons.primary(messages.encodingUtf8());
        var cancelButton = DialogButtons.cancel(messages.encodingCancel());
        alert.getButtonTypes().setAll(turkishButton, utf8Button, cancelButton);

        var result = show(alert, stage);
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

        var restoreButton = DialogButtons.primary(messages.recoveryYes());
        var discardButton = DialogButtons.alternative(messages.recoveryNo());
        // "Later" deliberately gets the cancel semantics: Escape and the window X
        // already mean "keep the draft, decide later" (see the Javadoc above and
        // choiceFor), so cancel must land on Later - never on the destructive
        // Discard button, which would turn a stray Escape into data loss.
        var laterButton = DialogButtons.cancel(messages.recoveryLater());
        alert.getButtonTypes().setAll(restoreButton, discardButton, laterButton);

        var result = show(alert, stage);
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

    /**
     * Shows the disk-cost notice once, after the first successful save.
     *
     * <p>Placed at the first save rather than at startup on purpose: that is
     * the moment the copies actually appear, so the sentence explains something
     * the user can see in their folder instead of predicting it. It is modal and
     * informational - it has no decision to make, and the caller marks it shown
     * regardless of how the window is closed.</p>
     */
    public void showStorageFootprintNotice(Stage stage, int persistentCopies, int peakCopies) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(messages.storageFootprintTitle());
        alert.setHeaderText(messages.storageFootprintHeader());
        alert.setContentText(messages.storageFootprintContent(persistentCopies, peakCopies));
        // The button text comes from the bundle for the same reason every other
        // dialog builds its buttons by hand: JavaFX's default OK label is
        // localised from the system locale, not from the chosen UI language.
        alert.getButtonTypes().setAll(DialogButtons.primary(messages.storageFootprintOk()));
        show(alert, stage);
    }

    public void showError(Stage owner, String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(messages.errorTitle());
        alert.setHeaderText(header);
        alert.setContentText(content);
        show(alert, owner);
    }

    /**
     * Shows a themed warning. Used by the single-instance guard, which has no
     * main scene to copy stylesheets from; {@link #prepare(Alert, Stage)}
     * therefore paints the pane from the classpath with the persisted theme
     * when the owner (or its scene) is missing.
     */
    public void showWarning(Stage owner, String title, String header, String content) {
        var alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        show(alert, owner);
    }

    /**
     * The one path every Alert takes to the screen.
     *
     * <p>Preparation happens here, never at the call sites: the owner is set
     * (so the dialog cannot hide behind the main window), the pane is themed,
     * the window is made resizable and clamped into the screen's visual bounds,
     * and the header/content labels wrap inside
     * {@link DialogSizing#alertContentWidth(double)}. A long German or French
     * sentence therefore wraps instead of widening the dialog into a
     * horizontal scrollbar on a 768 px display.
     */
    private Optional<ButtonType> show(Alert alert, Stage owner) {
        prepare(alert, owner);
        return alert.showAndWait();
    }

    private void prepare(Alert alert, Stage owner) {
        if (owner != null) {
            alert.initOwner(owner);
        }

        var pane = alert.getDialogPane();
        // Modena already wraps the labels it creates; setting the wrap on the
        // looked-up labels keeps the guarantee explicit and survives a future
        // JavaFX that stops doing it, while the max width keeps a single very
        // long token (a path, a URL in an error message) from forcing the pane
        // wider than the clamp below.
        var visual = Screen.getPrimary().getVisualBounds();
        var width = DialogSizing.alertWidth(visual.getWidth());
        var contentWidth = DialogSizing.alertContentWidth(width);
        pane.setPrefWidth(width);
        pane.setMaxHeight(DialogSizing.clampHeight(Double.MAX_VALUE, visual.getHeight()));
        alert.setResizable(true);

        if (owner != null && owner.getScene() != null) {
            DialogStyling.applyFromScene(pane, owner.getScene());
        } else {
            DialogStyling.applyTheme(pane, settings.getTheme());
        }

        wrap(pane, ".content.label", contentWidth);
        wrap(pane, ".header-panel .label", contentWidth);
    }

    /**
     * Turns the pane's header/content label into a wrapped one capped at
     * {@code maxWidth}. Missing labels are skipped rather than fatal: an alert
     * without header text simply has none.
     */
    private static void wrap(DialogPane pane, String selector, double maxWidth) {
        if (pane.lookup(selector) instanceof Label label) {
            label.setWrapText(true);
            label.setMaxWidth(maxWidth);
        }
    }

    /**
     * A recovery draft's timestamp in the UI language, in the machine's time
     * zone. The locale is the UI language's, not the system default: an
     * English UI on a German machine must not print a German month name.
     */
    private String format(Instant instant) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(messages.locale())
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * The timestamp in the suggested file name of a never-saved document.
     *
     * <p>Deliberately ISO ordering with filesystem-safe punctuation: a file
     * name is not prose, so it must not follow the UI language (a Turkish or
     * German user must be able to sort the drafts the same way, and the digits
     * must not depend on which language the app happens to run in), and it may
     * contain neither a colon nor a space.</p>
     */
    static String fileStamp(LocalDateTime now) {
        var second = now.truncatedTo(ChronoUnit.SECONDS);
        var time = second.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME).replace(':', '-');
        return second.format(DateTimeFormatter.ISO_LOCAL_DATE) + "_" + time;
    }
}