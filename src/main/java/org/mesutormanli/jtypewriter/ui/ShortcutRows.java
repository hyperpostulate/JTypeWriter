package org.mesutormanli.jtypewriter.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns the raw {@code welcomeShortcuts} bundle text into key/description
 * rows for the welcome and About shortcut cards.
 *
 * <p>The bundles fake column alignment with runs of spaces
 * ({@code "Ctrl+T          Toggle Toolbar"}), which renders ragged when the
 * whole block is drawn as one label. The parser reads that padding back as a
 * separator so the dialogs can lay the list out as a real two-column grid
 * without the translations gaining a second format to maintain.
 *
 * <p>Deliberately free of JavaFX types, like {@link DialogSizing}: the unit
 * tests parse every shipped bundle without starting a toolkit. The JavaFX
 * side of the card lives in {@link ShortcutTable}.
 */
public final class ShortcutRows {

    /** One line of the shortcut list: the chord(s) and what they do. */
    public record Row(String keys, String description) {
    }

    /**
     * Key and description are separated by two or more consecutive spaces -
     * the column padding the bundles were written with. No key contains a
     * double space and descriptions use single spaces only;
     * {@code ShortcutRowsTest} pins that for all six bundles rather than
     * trusting it, so the first run of spaces is an unambiguous separator.
     */
    private static final Pattern SEPARATOR = Pattern.compile(" {2,}");

    private ShortcutRows() {
    }

    /**
     * Splits the raw bundle text into rows, preserving order. Blank lines are
     * skipped. A line without the separator degrades into a key-only row
     * instead of throwing: one malformed translation must not blank out the
     * whole shortcut card of a dialog.
     */
    public static List<Row> parse(String raw) {
        var rows = new ArrayList<Row>();
        for (var line : raw.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            // Split first, trim after: the separator itself is spaces, and
            // limit 2 keeps any stray run inside a description in its place.
            var parts = SEPARATOR.split(line.trim(), 2);
            var keys = parts[0].trim();
            var description = parts.length > 1 ? parts[1].trim() : "";
            rows.add(new Row(keys, description));
        }
        return List.copyOf(rows);
    }
}
