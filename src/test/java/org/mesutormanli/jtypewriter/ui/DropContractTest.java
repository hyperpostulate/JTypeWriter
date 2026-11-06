/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two things a drag-and-drop shortcut could quietly regress, in the
 * only way a headless suite can: by reading the sources.
 *
 * <p><strong>The gesture must be installed.</strong> Resolving and reporting a
 * drop is worthless if no scene ever listens for it.</p>
 *
 * <p><strong>The unsaved-changes gate must stay single.</strong> The card's
 * requirement is that a drop on a dirty document goes through the existing
 * matrix rather than inventing a second save path, and there is exactly one
 * such matrix in this codebase: {@code FileService.confirmDiscardOrSave*}.
 * {@code DropHandler} therefore may only reach it through
 * {@code FileService.openPathAsync}; a direct
 * {@code showUnsavedChangesDialog} call - or a direct save before the load -
 * would be the second path. The check is source-level because the honest
 * behavioural version needs a stage and a toolkit, and no test here may start
 * one.</p>
 */
class DropContractTest {

    private static final Path SOURCES = Path.of("src", "main", "java");

    private static final Path DROP_HANDLER = SOURCES.resolve(
            Path.of("org", "mesutormanli", "jtypewriter", "ui", "DropHandler.java"));

    private static final Path MAIN_STAGE = SOURCES.resolve(
            Path.of("org", "mesutormanli", "jtypewriter", "ui", "MainStage.java"));

    @Test
    void theStageInstallsTheDropGestureOnTheScene() throws IOException {
        var source = Files.readString(MAIN_STAGE, StandardCharsets.UTF_8);

        assertTrue(source.contains("dropHandler.install(stage, scene)"),
                MAIN_STAGE + " never installs the drop handler, so a dropped document opens nothing");
    }

    /**
     * A scene filter, not a handler on the text control: the drop must work over
     * the status bar and the empty area too, and it has to run before
     * {@code TextInputControl} treats the gesture as text.
     */
    @Test
    void theDropGestureListensOnTheWholeSceneAsAFilter() throws IOException {
        var source = codeOnly(Files.readString(DROP_HANDLER, StandardCharsets.UTF_8));

        assertTrue(source.contains("scene.addEventFilter(DragEvent.DRAG_OVER"),
                DROP_HANDLER + " does not filter DRAG_OVER on the scene");
        assertTrue(source.contains("scene.addEventFilter(DragEvent.DRAG_DROPPED"),
                DROP_HANDLER + " does not filter DRAG_DROPPED on the scene");
    }

    @Test
    void theDropReusesTheDocumentSwitchGateInsteadOfAskingAgain() throws IOException {
        var source = codeOnly(Files.readString(DROP_HANDLER, StandardCharsets.UTF_8));

        assertTrue(source.contains("fileService.openPathAsync(stage, path)"),
                DROP_HANDLER + " does not open through FileService.openPathAsync, so a drop would"
                        + " bypass the same SAVE/DISCARD/CANCEL gate as Open");
        assertFalse(source.contains("showUnsavedChangesDialog"),
                DROP_HANDLER + " asks its own unsaved-changes question; the gate lives in FileService");
        assertFalse(source.contains("saveFile"),
                DROP_HANDLER + " saves by itself before opening; a drop must not invent a save path");
    }

    /**
     * The loaded text has to reach the editor. {@code FileService} only reports
     * the path, so a handler that forgets the document would leave the old text
     * (or an empty editor) on screen with the new title.
     */
    @Test
    void theDropHandsTheLoadedTextToTheEditor() throws IOException {
        var source = codeOnly(Files.readString(DROP_HANDLER, StandardCharsets.UTF_8));

        assertTrue(source.contains("editorArea.setDocument(fileService.getCurrentContent())"),
                DROP_HANDLER + " opens the file but never puts its text into the editor");
    }

    /** The unsaved-changes question is asked in one service, by one pair of methods. */
    @Test
    void onlyTheFileServiceAsksTheUnsavedChangesQuestion() throws IOException {
        var offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (var file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                var name = file.getFileName().toString();
                if (name.equals("DialogService.java") || name.equals("FileService.java")) {
                    continue; // the dialog itself and the one gate that shows it
                }
                if (codeOnly(Files.readString(file, StandardCharsets.UTF_8))
                        .contains("showUnsavedChangesDialog")) {
                    offenders.add(SOURCES.relativize(file).toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "a second unsaved-changes gate exists outside FileService: " + List.copyOf(offenders));
    }

    /** Live code only: prose in a Javadoc may name the methods it explains. */
    private static String codeOnly(String source) {
        var withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)//.*$", "");
    }
}
