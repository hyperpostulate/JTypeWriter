/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import org.mesutormanli.jtypewriter.ui.AppFonts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Turns a document into pages of positioned lines, independently of JavaFX and
 * of any file format.
 *
 * <p>One pagination model serves both outputs: {@link Printing} paints these
 * lines onto the printer's page, {@link PdfExport} writes the same lines into a
 * PDF, so the printed sheet and the exported file cannot drift apart. The
 * measurement comes from the bundled monospace face at a fixed
 * 12 pt - the editor's font size manager and the theme are
 * deliberately not consulted: a printed page is black on white at 12 pt
 * whatever the window looks like.</p>
 */
public final class DocumentLayout {

    /** The one size print and export use, in points. Fixed on purpose. */
    public static final double FONT_SIZE = 12.0;

    /** Baseline-to-baseline distance in points (1.4 x the font size). */
    public static final double LINE_HEIGHT = FONT_SIZE * 1.4;

    /** A tab advances to the next multiple of this many monospace cells. */
    public static final int TAB_CELLS = 4;

    private DocumentLayout() {
    }

    /** One line placed on a page: its text, its left edge and its baseline, in points. */
    public record Line(String text, double x, double baseline) {
    }

    /** One page: its zero-based index and the lines on it, top to bottom. */
    public record Page(int index, List<Line> lines) {

        public Page {
            lines = List.copyOf(lines);
        }
    }

    /** What pagination needs to know about the face it is measuring. */
    public interface Metrics {

        /** The advance width of {@code text} at 12 points. */
        double width(String text);

        /** The distance from the baseline to the top of the font, in points. */
        double ascent();
    }

    /**
     * Metrics of the bundled monospace face at 12 points.
     *
     * <p>Measured through a {@link java.awt.font.FontRenderContext} rather than
     * a font metrics object: no component, no toolkit and no display is
     * involved, so pagination is testable headlessly and returns fractional
     * advances instead of rounded pixels. AWT's unit for a font of this size is
     * one seventy-second of an inch - the same as a PDF point - so a line
     * measured here fits a page written there. The face is the bundled file,
     * never a host font: see {@link AppFonts#monoAwt(double)}.</p>
     */
    public static Metrics monoMetrics() {
        var font = AppFonts.monoAwt(FONT_SIZE);
        var context = new java.awt.font.FontRenderContext(null, true, true);
        var line = font.getLineMetrics("Hxg", context);
        return new Metrics() {
            @Override
            public double width(String text) {
                return font.getStringBounds(text, context).getWidth();
            }

            @Override
            public double ascent() {
                return line.getAscent();
            }
        };
    }

    /**
     * Lays {@code text} out onto pages of {@code geometry}.
     *
     * <p>Always returns at least one page: an empty document is an empty sheet,
     * not a zero-page file. Lines are broken at spaces, a word wider than the
     * text block is broken at code-point boundaries, existing line breaks are
     * kept, and a tab advances to the next four-cell stop.</p>
     *
     * <p>Carriage returns are normalised first, so {@code \r\n} and a lone
     * {@code \r} both become an ordinary line break. Split with a negative
     * limit, so a trailing newline leaves a final empty line: pressing Enter at
     * the end of the document is a line the caret can sit on, and print and
     * export agree with the editor about that.</p>
     *
     * <p>Pure and deterministic by construction: it reads no clock, uses no
     * locale-sensitive formatting, keeps no mutable static state and returns
     * immutable pages. Two calls with the same arguments are equal.</p>
     */
    public static List<Page> paginate(String text, PageGeometry geometry, Metrics metrics) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(geometry, "geometry");
        Objects.requireNonNull(metrics, "metrics");

        if (text.isEmpty()) {
            return List.of(new Page(0, List.of()));
        }

        String normalised = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        for (String sourceLine : normalised.split("\n", -1)) {
            lines.addAll(wrap(expandTabs(sourceLine), geometry.contentWidth(), metrics));
        }

        int linesPerPage = Math.max(1,
                (int) Math.floor(geometry.contentHeight() / LINE_HEIGHT));
        List<Page> pages = new ArrayList<>();
        for (int start = 0, index = 0; start < lines.size(); start += linesPerPage, index++) {
            int end = Math.min(start + linesPerPage, lines.size());
            List<Line> pageLines = new ArrayList<>(end - start);
            double baseline = geometry.top() + metrics.ascent();
            for (int i = start; i < end; i++) {
                pageLines.add(new Line(lines.get(i), geometry.left(), baseline));
                baseline += LINE_HEIGHT;
            }
            pages.add(new Page(index, pageLines));
        }
        return List.copyOf(pages);
    }

    /**
     * Expands a tab to the spaces up to the next four-cell stop. The face is
     * monospace, so a cell is one code point and the stop depends only on the
     * column, never on the surrounding glyphs. Counting code points rather than
     * {@code char}s keeps a supplementary character one cell wide.
     */
    private static String expandTabs(String line) {
        StringBuilder expanded = new StringBuilder(line.length());
        int column = 0;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            int width = Character.charCount(codePoint);
            if (codePoint == '\t') {
                int spaces = TAB_CELLS - (column % TAB_CELLS);
                expanded.append(" ".repeat(spaces));
                column += spaces;
            } else {
                expanded.appendCodePoint(codePoint);
                column++;
            }
            i += width;
        }
        return expanded.toString();
    }

    /**
     * Greedily wraps one logical line. The scan stops at the first code point
     * that no longer fits, remembering the last space that did; the line then
     * breaks there, dropping that space. With no usable space it breaks at the
     * last code point that fit, which is the latest code-point boundary that
     * keeps the text block intact. A surrogate pair is two {@code char}s, so
     * the break is always taken at a code-point boundary. The fit check is
     * {@code <=}, so a line exactly as wide as the block stays whole.
     */
    private static List<String> wrap(String line, double maxWidth, Metrics metrics) {
        List<String> wrapped = new ArrayList<>();
        if (line.isEmpty()) {
            wrapped.add("");
            return wrapped;
        }
        int start = 0;
        while (start < line.length()) {
            int scan = start;
            int lastSpace = -1;
            while (scan < line.length()) {
                int codePoint = line.codePointAt(scan);
                int next = scan + Character.charCount(codePoint);
                if (metrics.width(line.substring(start, next)) <= maxWidth) {
                    scan = next;
                    if (codePoint == ' ') {
                        lastSpace = next;
                    }
                } else {
                    break;
                }
            }
            if (scan == line.length()) {
                wrapped.add(line.substring(start));
                break;
            }
            if (lastSpace > start && lastSpace - 1 > start) {
                wrapped.add(line.substring(start, lastSpace - 1));
                start = lastSpace;
            } else {
                int breakAt = scan;
                if (breakAt == start) {
                    breakAt = start + Character.charCount(line.codePointAt(start));
                }
                wrapped.add(line.substring(start, breakAt));
                start = breakAt;
            }
        }
        return wrapped;
    }
}
