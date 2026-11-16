/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

/**
 * A page and the margins text must stay inside, in PDF points (1/72 inch).
 *
 * <p>Points are the unit of both outputs on purpose: PDF's user space is
 * already measured in points, and JavaFX's print layout reports its paper and
 * margins in the same unit, so one pagination model serves the printed page and
 * the exported file without a conversion that could differ between them.</p>
 *
 * <p>For printing the margins are zero: {@code PrinterJob} already hands back
 * the <em>printable</em> area, and subtracting margins a second time would
 * shift the text. For the PDF the margins are ours, because we choose the
 * sheet ({@link #a4()}).</p>
 */
public record PageGeometry(double width, double height,
                           double marginLeft, double marginTop,
                           double marginRight, double marginBottom) {

    /** A4 in points, with 20 mm margins on every side. */
    private static final double A4_WIDTH = 595.276;
    private static final double A4_HEIGHT = 841.890;
    private static final double TWENTY_MILLIMETRES = 56.693;

    public PageGeometry {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("page must have a positive size: "
                    + width + " x " + height);
        }
        if (marginLeft < 0 || marginTop < 0 || marginRight < 0 || marginBottom < 0) {
            throw new IllegalArgumentException("margins cannot be negative");
        }
        if (width - marginLeft - marginRight <= 0 || height - marginTop - marginBottom <= 0) {
            throw new IllegalArgumentException("margins leave no room for text");
        }
    }

    /** The exported document's sheet: A4 portrait with 20 mm margins. */
    public static PageGeometry a4() {
        return new PageGeometry(A4_WIDTH, A4_HEIGHT,
                TWENTY_MILLIMETRES, TWENTY_MILLIMETRES,
                TWENTY_MILLIMETRES, TWENTY_MILLIMETRES);
    }

    /** The printable area of a printer's page layout, whose margins it already applied. */
    public static PageGeometry printable(double printableWidth, double printableHeight) {
        return new PageGeometry(printableWidth, printableHeight, 0, 0, 0, 0);
    }

    /** Width available to a line of text. */
    public double contentWidth() {
        return width - marginLeft - marginRight;
    }

    /** Height available to the lines of text. */
    public double contentHeight() {
        return height - marginTop - marginBottom;
    }

    /** The left edge of the text block. */
    public double left() {
        return marginLeft;
    }

    /** The top edge of the text block. */
    public double top() {
        return marginTop;
    }
}
