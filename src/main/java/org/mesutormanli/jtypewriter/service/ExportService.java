/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AtomicFiles;
import org.mesutormanli.jtypewriter.IoExecutor;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.print.Printing;

import java.nio.file.Path;
import java.util.concurrent.CompletionException;

/**
 * The user-facing half of printing and PDF export: a save gate in front of the
 * pure rendering seam.
 *
 * <p><strong>Only the saved file is output.</strong> Both commands ask
 * {@link FileService#savedDocumentAsync(Stage)} for the document, and that
 * method refuses to proceed until the editor's unsaved text has actually been
 * written. The text handed to the renderer is then read back <em>from disk</em>
 * in the document's own encoding, never from {@link FileService#getCurrentContent()}
 * or the editor. This is a data-integrity promise, not a preference: what the
 * user prints or exports must be the file they saved, so a stale buffer, a
 * cancelled save or a disk that changed underneath cannot end up on paper or in
 * a PDF. There is deliberately no "print the unsaved text anyway" path.</p>
 *
 * <p><strong>Threading.</strong> {@link #printAsync(Stage)} runs on the FX
 * thread because {@link Printing} needs the toolkit. {@link #exportPdfAsync(Stage)}
 * also starts there (dialogs are FX-only), but the CPU-heavy render - font
 * parsing plus deflate - and the atomic write run on the shared
 * {@link IoExecutor}; only the failure dialog returns to the FX thread. Success
 * is silent in both paths: the print dialog has already told the user, and a
 * written PDF needs no announcement.</p>
 */
public class ExportService {

    /**
     * The rendering seam. Production is {@code PdfExport::render}; tests inject
     * a stub so the headless suite never needs the real PDF writer.
     */
    public interface PdfRenderer {

        byte[] render(String text);
    }

    private final FileService fileService;
    private final DialogService dialogService;
    private final Messages messages;
    private final IoExecutor ioExecutor;
    private final PdfRenderer renderer;

    public ExportService(FileService fileService, DialogService dialogService,
                         Messages messages, IoExecutor ioExecutor, PdfRenderer renderer) {
        this.fileService = fileService;
        this.dialogService = dialogService;
        this.messages = messages;
        this.ioExecutor = ioExecutor;
        this.renderer = renderer;
    }

    /**
     * Asks for the saved document, then prints it. FX thread.
     *
     * <p>The gate completes empty when the user cancels the save prompt, the
     * save fails, or the file cannot be read; this method then does nothing at
     * all, because the alternative - falling back to the editor buffer - would
     * print text the file does not contain.</p>
     */
    public void printAsync(Stage stage) {
        fileService.savedDocumentAsync(stage).thenAccept(saved -> saved.ifPresent(document -> {
            var result = Printing.print(stage, document.text());
            switch (result.outcome()) {
                case NO_PRINTER -> dialogService.showPrintFailure(
                        stage, messages.printFailedNoPrinter());
                case FAILED -> dialogService.showPrintFailure(stage, result.detail());
                // PRINTED and CANCELLED are both ordinary outcomes: the print
                // dialog itself has already reported them to the user.
                case PRINTED, CANCELLED -> {
                }
            }
        }));
    }

    /**
     * Asks for the saved document, asks for a target, renders and writes it.
     * FX thread.
     *
     * <p>The chooser only appears after the save gate has produced a real
     * file, so a never-saved or still-dirty document gets the save prompt first
     * and a cancelled save shows no export dialog at all. The target is written
     * with {@link AtomicFiles#writeMoving(Path, byte[])} - the same durable
     * temp-file-and-rename path as document saves - so a failed export never
     * leaves a truncated PDF behind.</p>
     */
    public void exportPdfAsync(Stage stage) {
        fileService.savedDocumentAsync(stage).thenAccept(saved -> saved.ifPresent(document ->
                dialogService.showPdfExportDialog(stage, document.path())
                        .ifPresent(target -> renderAndWrite(stage, target.toPath(), document.text()))));
    }

    private void renderAndWrite(Stage stage, Path target, String text) {
        ioExecutor.submit("export pdf " + target, () -> {
            var bytes = renderer.render(text);
            AtomicFiles.writeMoving(target, bytes);
            return null;
        }).whenComplete((ignored, failure) -> {
            if (failure != null) {
                var reason = describe(failure);
                ioExecutor.completeOnFx(() -> {
                    dialogService.showExportFailure(stage, target, reason);
                    return null;
                });
            }
        });
    }

    /** The human-readable part of a failed export, unwrapping the future's wrapper. */
    private static String describe(Throwable failure) {
        var cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
        var message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }
}
