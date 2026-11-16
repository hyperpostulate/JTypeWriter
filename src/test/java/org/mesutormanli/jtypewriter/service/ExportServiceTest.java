/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.IoExecutor;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The export gate: no path to a PDF may bypass the saved document, and the
 * renderer is a seam so the headless suite never runs the real PDF writer.
 */
class ExportServiceTest {

    @TempDir
    Path dir;

    private FileService fileService;
    private DialogService dialogService;
    private Messages messages;

    @BeforeEach
    void setUp() {
        fileService = mock(FileService.class);
        dialogService = mock(DialogService.class);
        messages = mock(Messages.class);
    }

    @Test
    void aCancelledSaveGateShowsNoChooserAndWritesNothing() {
        when(fileService.savedDocumentAsync(any())).thenReturn(
                CompletableFuture.completedFuture(Optional.empty()));
        var rendered = new AtomicBoolean();
        var service = new ExportService(fileService, dialogService, messages,
                IoExecutor.synchronous(), text -> {
                    rendered.set(true);
                    return new byte[]{1};
                });

        service.exportPdfAsync(null);

        verify(fileService).savedDocumentAsync(any());
        verify(dialogService, never()).showPdfExportDialog(any(), any());
        verify(dialogService, never()).showExportFailure(any(), any(), any());
        assertFalse(rendered.get(), "the renderer must not run without a saved document");
    }

    @Test
    void theChosenTargetReceivesExactlyTheRenderersBytes() throws IOException {
        var textFile = dir.resolve("novel.txt");
        var target = dir.resolve("novel.pdf");
        var bytes = new byte[]{'%', 'P', 'D', 'F', 0, 1, 2};
        when(fileService.savedDocumentAsync(any())).thenReturn(CompletableFuture.completedFuture(
                Optional.of(new FileService.SavedDocument(textFile, "saved text"))));
        when(dialogService.showPdfExportDialog(any(), eq(textFile)))
                .thenReturn(Optional.of(target.toFile()));
        var service = new ExportService(fileService, dialogService, messages,
                IoExecutor.synchronous(), text -> bytes);

        service.exportPdfAsync(null);

        assertArrayEquals(bytes, Files.readAllBytes(target));
        verify(dialogService, never()).showExportFailure(any(), any(), any());
    }

    @Test
    void aRendererFailureIsReportedAndLeavesNoFile() {
        var textFile = dir.resolve("novel.txt");
        var target = dir.resolve("novel.pdf");
        when(fileService.savedDocumentAsync(any())).thenReturn(CompletableFuture.completedFuture(
                Optional.of(new FileService.SavedDocument(textFile, "saved text"))));
        when(dialogService.showPdfExportDialog(any(), eq(textFile)))
                .thenReturn(Optional.of(target.toFile()));
        var service = new ExportService(fileService, dialogService, messages,
                IoExecutor.synchronous(), text -> {
                    throw new IllegalStateException("font exploded");
                });

        service.exportPdfAsync(null);

        assertFalse(Files.exists(target), "a failed render must not leave a partial PDF");
        verify(dialogService).showExportFailure(any(), eq(target), contains("font exploded"));
    }

    @Test
    void noChooserIsShownWhenThereIsNoSavedDocument() {
        when(fileService.savedDocumentAsync(any())).thenReturn(
                CompletableFuture.completedFuture(Optional.empty()));
        var service = new ExportService(fileService, dialogService, messages,
                IoExecutor.synchronous(), text -> new byte[]{1});

        service.exportPdfAsync(null);

        verify(dialogService, never()).showPdfExportDialog(any(), any());
        verifyNoInteractions(messages);
    }
}
