package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shortcut bundles pad their columns with space runs and
 * {@link ShortcutRows#parse(String)} reads those runs back as the separator,
 * so the welcome/About cards can lay the list out as a real grid. These tests
 * pin the assumptions the parser makes - one unambiguous separator per line,
 * single-spaced descriptions - against all six shipped bundles, so a
 * translation cannot silently turn a card row inside out. The raw bundle
 * format itself is untouched; {@code MessagesTest} and {@code ThemeCycleTest}
 * keep reading it as before.
 */
class ShortcutRowsTest {

    @TempDir
    Path dir;

    /** Mirrors the separator in {@link ShortcutRows}: two or more spaces. */
    private static final Pattern SEPARATOR = Pattern.compile(" {2,}");

    /**
     * The parser only drops blank lines, so equal counts also pin that no
     * bundle grew a blank line - and the chords assert order survived. The
     * first and last chords are language-independent (only descriptions are
     * translated), so they can be checked for every bundle.
     */
    @Test
    void everyLineOfEveryBundleBecomesExactlyOneRow() {
        for (var language : Language.values()) {
            var raw = shortcutsFor(language);
            var lines = raw.lines().toList();
            var rows = ShortcutRows.parse(raw);

            assertEquals(lines.size(), rows.size(),
                    language + ": the parse dropped or invented a line");
            assertEquals("Ctrl+T", rows.get(0).keys(),
                    language + ": wrong first row");
            assertEquals("Escape", rows.get(rows.size() - 1).keys(),
                    language + ": wrong last row");
        }
    }

    @Test
    void everyRowOfEveryBundleHasAKeyAndADescription() {
        for (var language : Language.values()) {
            for (var row : ShortcutRows.parse(shortcutsFor(language))) {
                assertFalse(row.keys().isBlank(),
                        language + ": row lost its chord: " + row);
                assertFalse(row.description().isBlank(),
                        language + ": row lost its description: " + row);
            }
        }
    }

    /**
     * The split rule is only unambiguous while no key contains a double space
     * and every description uses single spaces only - splitting the raw line
     * on space runs must therefore yield exactly two parts. A third part is
     * either a padded key or a double-spaced description, and either would
     * have been mis-split before this test existed to catch it.
     */
    @Test
    void everyBundleLineSplitsIntoExactlyTwoParts() {
        for (var language : Language.values()) {
            for (var line : shortcutsFor(language).lines().toList()) {
                var parts = SEPARATOR.split(line.trim());

                assertEquals(2, parts.length,
                        language + ": separator is ambiguous in: " + line);
            }
        }
    }

    /**
     * A PW-18 guard through the parser: the theme cycle text must land in the
     * description of the Ctrl+Shift+T row in cycle order (Dark/Sepia/Light),
     * in every language.
     */
    @Test
    void themeSwitchRowKeepsTheCycleInTheDescription() {
        for (var language : Language.values()) {
            var messages = messagesFor(language);
            var cycle = Theme.DARK.getDisplayName(messages) + "/"
                    + Theme.SEPIA.getDisplayName(messages) + "/"
                    + Theme.LIGHT.getDisplayName(messages);

            var row = ShortcutRows.parse(shortcutsFor(language)).stream()
                    .filter(candidate -> candidate.keys().equals("Ctrl+Shift+T"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no theme shortcut row in " + language));

            assertTrue(row.description().contains("(" + cycle + ")"),
                    language + ": " + row);
        }
    }

    /** Single spaces inside a description are content, not separator. */
    @Test
    void singleSpacesInsideADescriptionSurviveTheSplit() {
        var rows = ShortcutRows.parse("Ctrl+S          Save / Save As");

        assertEquals(1, rows.size());
        assertEquals("Ctrl+S", rows.get(0).keys());
        assertEquals("Save / Save As", rows.get(0).description());
    }

    /**
     * Defensive path: a line without the separator (a malformed translation,
     * or a key whose padding was lost) degrades into a key-only row instead
     * of throwing, so one bad line cannot blank the whole card. Unlike the
     * six real bundles, this text is authored here, so a descriptionless row
     * is exactly what the test demands.
     */
    @Test
    void aLineWithoutTheSeparatorBecomesAKeyWithAnEmptyDescription() {
        var rows = ShortcutRows.parse("Ctrl+B  Bold\nOrphan line\n\n");

        assertEquals(2, rows.size(), "blank lines are skipped");
        assertEquals("Ctrl+B", rows.get(0).keys());
        assertEquals("Bold", rows.get(0).description());
        assertEquals("Orphan line", rows.get(1).keys());
        assertEquals("", rows.get(1).description());
    }

    private String shortcutsFor(Language language) {
        return messagesFor(language).welcomeShortcuts();
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }
}
