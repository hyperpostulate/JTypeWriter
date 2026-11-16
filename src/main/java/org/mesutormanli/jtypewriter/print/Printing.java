/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.ui.AppFonts;

import javafx.geometry.Insets;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Sends a document to a printer: the system print dialog, then one page per
 * page of {@link DocumentLayout}, black 12 pt text on white paper.
 *
 * <p>Nothing from the editor's theme or font-size setting reaches this class.
 * The pages are built outside the application's scene graph, so no stylesheet
 * can paint them: the text is filled {@code Color.BLACK}, the sheet is painted
 * white and the font is the bundled monospace family at
 * {@link DocumentLayout#FONT_SIZE}.</p>
 *
 * <p>JavaFX-thread only ({@code PrinterJob} is). The caller decides what to
 * tell the user about the outcome - see {@link Outcome}.</p>
 */
public final class Printing {

    /** What became of one print attempt. */
    public enum Outcome {
        /** A job was sent to the printer. */
        PRINTED,
        /** The user closed the print dialog; nothing was sent. */
        CANCELLED,
        /** The platform reported no usable printer. */
        NO_PRINTER,
        /** A printer exists but the job could not be printed. */
        FAILED
    }

    /** The outcome, plus the framework's message when it failed. */
    public record Result(Outcome outcome, String detail) {

        public static Result printed() {
            return new Result(Outcome.PRINTED, null);
        }

        public static Result cancelled() {
            return new Result(Outcome.CANCELLED, null);
        }

        public static Result noPrinter() {
            return new Result(Outcome.NO_PRINTER, null);
        }

        public static Result failed(String detail) {
            return new Result(Outcome.FAILED, detail);
        }
    }

    private Printing() {
    }

    /**
     * Shows the print dialog and prints {@code text} if the user accepts.
     *
     * <p>The page geometry comes from the chosen printer's own
     * {@code PageLayout}: its printable area is what the pagination gets, so a
     * narrow printer breaks lines where the paper forces it to.</p>
     */
    public static Result print(Stage owner, String text) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return Result.noPrinter();
        }
        try {
            if (!job.showPrintDialog(owner)) {
                job.endJob();
                return Result.cancelled();
            }

            PageLayout layout = job.getJobSettings().getPageLayout();
            PageGeometry geometry = PageGeometry.printable(
                    layout.getPrintableWidth(), layout.getPrintableHeight());
            Font font = loadMonoFont();
            if (font == null) {
                job.endJob();
                return Result.failed("the bundled print font could not be loaded");
            }
            if (!AppFonts.MONO_FAMILY.equals(font.getFamily())) {
                job.endJob();
                return Result.failed("the bundled print font has the wrong family");
            }

            List<DocumentLayout.Page> pages =
                    DocumentLayout.paginate(text, geometry, DocumentLayout.monoMetrics());
            for (DocumentLayout.Page page : pages) {
                Node node = pageNode(page, geometry, font);
                if (!job.printPage(layout, node)) {
                    job.endJob();
                    return Result.failed("the printer rejected a page");
                }
            }
            if (!job.endJob()) {
                return Result.failed("the print job could not be finished");
            }
            return Result.printed();
        } catch (RuntimeException error) {
            try {
                job.endJob();
            } catch (RuntimeException ignored) {
                // The job is already unusable; the report below is what matters.
            }
            String message = error.getMessage();
            return Result.failed(message == null || message.isBlank()
                    ? "the print job failed" : message);
        }
    }

    /**
     * Builds one page as a free-standing {@link Pane}. A {@code Pane} has no
     * user-agent stylesheet and is not attached to a {@code Scene}, so nothing
     * in the application's CSS can change the paper or the glyphs. The white
     * background is a {@link BackgroundFill}, not {@code -fx-background-color}:
     * a printer should not have to interpret a stylesheet that was never
     * loaded. The text is positioned from the same points pagination produced,
     * so print and PDF use identical geometry.
     */
    private static Node pageNode(DocumentLayout.Page page, PageGeometry geometry, Font font) {
        Pane pane = new Pane();
        double width = geometry.width();
        double height = geometry.height();
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        pane.setBackground(new Background(
                new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        for (DocumentLayout.Line line : page.lines()) {
            Text text = new Text(line.text());
            text.setFont(font);
            text.setFill(Color.BLACK);
            text.setX(line.x());
            text.setY(line.baseline());
            pane.getChildren().add(text);
        }
        return pane;
    }

    /**
     * Loads the bundled face through this class rather than assuming
     * {@code FxApplication} registered it. Printing can run before or without
     * that registration (tests and headless callers), and silently picking a
     * system font would make the printed page differ from the PDF.
     */
    private static Font loadMonoFont() {
        try (InputStream stream = Printing.class.getResourceAsStream(AppFonts.MONO_RESOURCE)) {
            if (stream == null) {
                return null;
            }
            return Font.loadFont(stream, DocumentLayout.FONT_SIZE);
        } catch (IOException error) {
            return null;
        }
    }
}
