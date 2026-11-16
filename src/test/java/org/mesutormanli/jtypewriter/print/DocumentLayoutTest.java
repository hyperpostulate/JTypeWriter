/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared pagination engine must be pure and unit-testable: every rule that
 * decides where a line breaks, where a baseline sits or when a page overflows
 * lives here rather than in a {@code Text} node, so these tests need no
 * toolkit and no display.
 */
class DocumentLayoutTest {

    private static final DocumentLayout.Metrics METRICS = DocumentLayout.monoMetrics();

    @Test
    void normalisesCarriageReturnLineFeedAndLoneCarriageReturn() {
        var pages = paginate("a\r\nb\rc", 1000, 1000, cells(10));

        assertEquals(List.of("a", "b", "c"), texts(pages));
    }

    @Test
    void keepsExplicitNewlinesAsSeparateLines() {
        var pages = paginate("one\ntwo\nthree", 1000, 1000, cells(10));

        assertEquals(List.of("one", "two", "three"), texts(pages));
    }

    @Test
    void trailingNewlineLeavesAFinalEmptyLine() {
        var pages = paginate("a\n", 1000, 1000, cells(10));

        assertEquals(List.of("a", ""), texts(pages));
    }

    @Test
    void expandsTabsToTheNextFourCellStop() {
        var pages = paginate("a\tb\nab\tc\n\tX", 1000, 1000, cells(10));

        assertEquals(List.of("a   b", "ab  c", "    X"), texts(pages));
    }

    @Test
    void wrapsGreedilyAtSpacesAtTheMeasuredWidth() {
        double width = METRICS.width("M") * 5;
        var pages = DocumentLayout.paginate("aaa bbb ccc",
                new PageGeometry(width, 1000, 0, 0, 0, 0), METRICS);

        assertEquals(List.of("aaa", "bbb", "ccc"), texts(pages));
    }

    @Test
    void aLineExactlyAsWideAsTheBlockIsNotBroken() {
        double width = METRICS.width("abcde");

        var pages = DocumentLayout.paginate("abcde",
                new PageGeometry(width, 1000, 0, 0, 0, 0), METRICS);

        assertEquals(List.of("abcde"), texts(pages));
    }

    @Test
    void aLongUnbreakableRunBreaksAtTheLatestCodePointBoundary() {
        var pages = paginate("abcdefghij", 30, 1000, cells(10));

        assertEquals(List.of("abc", "def", "ghi", "j"), texts(pages));
    }

    @Test
    void breakingALongRunNeverSplitsASurrogatePair() {
        var pages = paginate("ab\uD83D\uDE00cd", 30, 1000, cells(10));

        assertEquals(List.of("ab\uD83D\uDE00", "cd"), texts(pages));
        for (DocumentLayout.Page page : pages) {
            for (DocumentLayout.Line line : page.lines()) {
                assertTrue(line.text().indexOf('\uFFFD') < 0);
            }
        }
    }

    @Test
    void theFirstBaselineIsPageTopPlusAscentAndEachLineAddsTheLineHeight() {
        var pages = paginate("a\nb", 1000, 1000, cells(10, 8));

        List<DocumentLayout.Line> lines = pages.get(0).lines();
        assertEquals(10 + 8, lines.get(0).baseline(), 1e-9);
        assertEquals(10 + 8 + DocumentLayout.LINE_HEIGHT, lines.get(1).baseline(), 1e-9);
        assertEquals(10, lines.get(0).x(), 1e-9);
        assertEquals(10, lines.get(1).x(), 1e-9);
    }

    @Test
    void overflowStartsANewPageAndRecomputesTheBaselineFromItsTop() {
        double height = DocumentLayout.LINE_HEIGHT * 2 + 1;
        var pages = paginate("a\nb\nc\nd\ne", 1000, height, cells(10, 8));

        assertEquals(3, pages.size());
        assertEquals(List.of(2, 2, 1), pages.stream().map(page -> page.lines().size()).toList());
        assertEquals(List.of(0, 1, 2), pages.stream().map(DocumentLayout.Page::index).toList());
        for (DocumentLayout.Page page : pages) {
            assertEquals(10 + 8, page.lines().get(0).baseline(), 1e-9);
        }
        assertEquals(10 + 8 + DocumentLayout.LINE_HEIGHT,
                pages.get(1).lines().get(1).baseline(), 1e-9);
    }

    @Test
    void aPageAlwaysHoldsAtLeastOneLineEvenIfTheBlockIsShorterThanOneLine() {
        var pages = paginate("a\nb", 1000, 1, cells(10));

        assertEquals(2, pages.size());
        assertEquals(1, pages.get(0).lines().size());
        assertEquals(1, pages.get(1).lines().size());
    }

    @Test
    void anEmptyDocumentIsOneEmptyPage() {
        var pages = paginate("", 1000, 1000, cells(10));

        assertEquals(1, pages.size());
        assertTrue(pages.get(0).lines().isEmpty());
        assertEquals(0, pages.get(0).index());
    }

    @Test
    void paginationIsDeterministic() {
        String text = "first line with spaces\nsecond\tline\n\n" + "x".repeat(200);

        assertEquals(paginate(text, 120, 100, cells(10)),
                paginate(text, 120, 100, cells(10)));
    }

    @Test
    void a4HasTheDocumentedMarginsAndContentBox() {
        PageGeometry a4 = PageGeometry.a4();

        assertEquals(595.276, a4.width(), 1e-9);
        assertEquals(841.890, a4.height(), 1e-9);
        assertEquals(56.693, a4.left(), 1e-9);
        assertEquals(56.693, a4.top(), 1e-9);
        assertEquals(481.890, a4.contentWidth(), 1e-9);
        assertEquals(728.504, a4.contentHeight(), 1e-9);
    }

    @Test
    void printableAreaHasNoMarginsOfItsOwn() {
        PageGeometry printable = PageGeometry.printable(500, 400);

        assertEquals(500, printable.width());
        assertEquals(400, printable.height());
        assertEquals(0, printable.left());
        assertEquals(0, printable.top());
        assertEquals(500, printable.contentWidth());
        assertEquals(400, printable.contentHeight());
    }

    @Test
    void impossibleGeometryIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PageGeometry(0, 100, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PageGeometry(100, -1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PageGeometry(100, 100, -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PageGeometry(100, 100, 60, 0, 40, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PageGeometry(100, 100, 0, 60, 0, 40));
    }

    private static List<DocumentLayout.Page> paginate(String text, double width, double height,
                                                      DocumentLayout.Metrics metrics) {
        return DocumentLayout.paginate(text,
                new PageGeometry(width + 20, height + 20, 10, 10, 10, 10), metrics);
    }

    private static List<String> texts(List<DocumentLayout.Page> pages) {
        return pages.stream()
                .flatMap(page -> page.lines().stream())
                .map(DocumentLayout.Line::text)
                .toList();
    }

    private static DocumentLayout.Metrics cells(double cellWidth) {
        return cells(cellWidth, 10);
    }

    /** A deterministic monospace metre: one cell per code point. */
    private static DocumentLayout.Metrics cells(double cellWidth, double ascent) {
        return new DocumentLayout.Metrics() {
            @Override
            public double width(String text) {
                return text.codePointCount(0, text.length()) * cellWidth;
            }

            @Override
            public double ascent() {
                return ascent;
            }
        };
    }
}
