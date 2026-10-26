/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * The two-column shortcut card shared by the welcome and About dialogs.
 *
 * <p>Both dialogs used to drop the whole bundle string into one monospace
 * {@code Label} and let hand-counted space runs align the columns, which read
 * as noise because nothing actually lined up. This factory lays the parsed
 * {@link ShortcutRows.Row}s out as a real grid: chords in the left column,
 * descriptions right-aligned against the inner edge of the card, the gap
 * between the columns absorbing any leftover width.
 *
 * <p>One factory on purpose - the card is the one piece of the two dialogs
 * that must stay pixel-identical, and copy-pasting the grid let them drift.
 */
public final class ShortcutTable {

    /**
     * Vertical gap between rows. Matches the {@code -fx-line-spacing: 7}
     * the single-label card used, so the list keeps its old top-down rhythm.
     */
    private static final double ROW_SPACING = 7;

    /** Minimum horizontal gap between the two columns. */
    private static final double COLUMN_GAP = 18;

    private ShortcutTable() {
    }

    /**
     * Builds the card for {@code rows}, sized to {@code contentWidth} - the
     * full card width a dialog can give it (its scene width minus the root
     * padding). The card surface itself (background, border, padding, radius)
     * comes from {@code .welcome-shortcuts} in base.css; JavaFX subtracts that
     * padding from the column area, so giving the grid the content box is what
     * makes the descriptions flush with the inner edge of the card.
     */
    public static GridPane build(List<ShortcutRows.Row> rows, double contentWidth) {
        var table = new GridPane();
        table.getStyleClass().add("welcome-shortcuts");
        // Preferred width equals the maximum so the card occupies the whole
        // content box even when the columns' own preferred widths are
        // narrower - otherwise the right-aligned descriptions would hug an
        // invisible seam instead of the card edge.
        table.setPrefWidth(contentWidth);
        table.setMaxWidth(contentWidth);
        table.setHgap(COLUMN_GAP);
        table.setVgap(ROW_SPACING);

        // Column 0 is left alone on purpose: GridPane measures it as the
        // widest key across all rows of the current locale, so every row
        // shares one left column without per-row jitter and without the
        // code having to measure fonts itself.
        var keyColumn = new ColumnConstraints();
        var descriptionColumn = new ColumnConstraints();
        descriptionColumn.setHgrow(Priority.ALWAYS);
        descriptionColumn.setHalignment(HPos.RIGHT);
        table.getColumnConstraints().addAll(keyColumn, descriptionColumn);

        var row = 0;
        for (var shortcut : rows) {
            var keys = new Label(shortcut.keys());
            keys.getStyleClass().add("shortcut-key");

            // The description keeps the card's monospace font: the block is
            // read as a table, and a proportional second font would break the
            // fixed-width rhythm the right edge relies on, making wrapped
            // lines end at uneven pixels even when the column is aligned.
            var description = new Label(shortcut.description());
            description.getStyleClass().add("shortcut-description");
            // The longer German/French/Italian/Spanish descriptions
            // are wider than the card, so they must wrap inside the clamped
            // column - never clip and never push the card wider.
            description.setWrapText(true);
            description.setTextAlignment(TextAlignment.RIGHT);
            description.setAlignment(Pos.CENTER_RIGHT);
            // Without an unbounded max the label refuses to be stretched to
            // the column width, and the right alignment has nothing to
            // measure against.
            description.setMaxWidth(Double.MAX_VALUE);

            table.add(keys, 0, row);
            table.add(description, 1, row);
            row++;
        }
        return table;
    }
}
