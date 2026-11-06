/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.Scene;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.DialogService;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns a dropped file into the same document switch the Open command performs.
 *
 * <p>The decision which paths are acceptable lives in {@link DroppedFiles} and
 * is pure; this class is only the toolkit glue: accept the gesture, resolve
 * what it carried, then either open the document or say why not.</p>
 *
 * <p><strong>Why the whole scene, as a filter.</strong> A user drops onto
 * whatever is under the pointer - the text, the status bar, an empty area below
 * the last line. Registering the handlers on the scene catches all of them, and
 * registering them as <em>filters</em> makes them run before
 * {@code TextInputControl}'s own drag handling, which would otherwise try to
 * insert the gesture as text. A drag that carries no file at all (a selection
 * from another editor) is left untouched, so ordinary text dragging still
 * works.</p>
 *
 * <p><strong>Why accept a drop that will be refused.</strong> Refusing at
 * drag-over time is silent: the platform simply does not let go, and the user
 * cannot tell a wrong file type from a frozen window. The gesture is therefore
 * accepted and the refusal is reported in the UI language, naming the path or
 * the count.</p>
 */
public class DropHandler {

    private static final Logger LOG = AppLogging.logger(DropHandler.class);

    private final FileService fileService;
    private final DialogService dialogService;
    private final Messages messages;
    private final EditorArea editorArea;

    private Stage stage;

    public DropHandler(FileService fileService, DialogService dialogService, Messages messages,
                       EditorArea editorArea) {
        this.fileService = fileService;
        this.dialogService = dialogService;
        this.messages = messages;
        this.editorArea = editorArea;
    }

    /** Installs the drop gesture on the whole window. Called once, on the built scene. */
    public void install(Stage stage, Scene scene) {
        this.stage = stage;
        scene.addEventFilter(DragEvent.DRAG_OVER, this::handleDragOver);
        scene.addEventFilter(DragEvent.DRAG_DROPPED, this::handleDropped);
    }

    private void handleDragOver(DragEvent event) {
        if (!event.getDragboard().hasFiles()) {
            return; // a text drag belongs to the editor, not to this handler
        }
        event.acceptTransferModes(TransferMode.COPY);
        // Keeps the TextArea from treating the same gesture as a text drop.
        event.consume();
    }

    private void handleDropped(DragEvent event) {
        var dragboard = event.getDragboard();
        if (!dragboard.hasFiles()) {
            return;
        }
        event.consume();
        var request = DroppedFiles.resolve(dragboard.getFiles().stream()
                .map(File::toPath)
                .toList());
        if (request.problem().isPresent()) {
            event.setDropCompleted(false);
            report(request.problem().orElseThrow(), request.description());
            return;
        }
        event.setDropCompleted(true);
        open(request.document().orElseThrow());
    }

    /**
     * The document switch itself. {@code openPathAsync} runs the same
     * unsaved-changes gate as Open and New, so a dirty document is asked about
     * before anything is replaced and a cancelled or failed save leaves the
     * editor exactly as it was.
     */
    private void open(java.nio.file.Path path) {
        fileService.openPathAsync(stage, path).thenAccept(opened -> opened.ifPresent(ignored ->
                editorArea.setDocument(fileService.getCurrentContent())));
    }

    private void report(DroppedFiles.Problem problem, String description) {
        LOG.log(Level.WARNING, "refusing the dropped file: " + problem + " (" + description + ")");
        dialogService.showError(stage, messages.dropOpenFailedHeader(), switch (problem) {
            case NOTHING_TO_OPEN -> messages.dropNothingToOpen();
            case MULTIPLE_FILES -> messages.dropMultipleFiles(description);
            case UNSUPPORTED_TYPE -> messages.dropUnsupportedType(description);
            case MISSING -> messages.dropMissingFile(description);
            case NOT_A_FILE -> messages.dropNotAFile(description);
        });
    }
}
