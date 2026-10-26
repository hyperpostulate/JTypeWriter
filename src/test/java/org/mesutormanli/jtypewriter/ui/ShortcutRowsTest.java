package org.mesutormanli.jtypewriter.ui;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.KeyboardShortcutHandler.ShortcutAction;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * PW-22/U23: the card and the handler must tell the same story. The old
     * list claimed {@code Ctrl+S  Save / Save As} - a shortcut the handler does
     * not have, since Save As is {@code Ctrl+Shift+S} - and never mentioned
     * {@code Ctrl+Shift+Z}, which the handler does map to Redo. Chords are
     * language-independent (only descriptions are translated), so one table
     * driven by the package-private {@link KeyboardShortcutHandler#resolve}
     * covers every bundle.
     *
     * <p>The row order is pinned too: dropping a chord and duplicating another
     * would keep a set equal, but the card would still list the wrong thing.
     * {@code Escape} resolves to {@code NONE} on purpose - it is handled by the
     * fullscreen exit combination, not by {@code resolve}, and the card must
     * not promise it any other action.
     */
    @Test
    void theCardListsExactlyTheChordsTheHandlerImplements() {
        for (var language : Language.values()) {
            var rows = ShortcutRows.parse(shortcutsFor(language));
            var chords = rows.stream().map(ShortcutRows.Row::keys).toList();

            assertEquals(List.copyOf(HANDLER_CHORDS.keySet()), chords,
                    language + ": the card and the handler disagree on the chords");

            for (var row : rows) {
                var chord = HANDLER_CHORDS.get(row.keys());
                assertEquals(chord.action(),
                        KeyboardShortcutHandler.resolve(chord.code(), chord.shift()),
                        language + " " + row.keys() + ": the card promises an action the handler does not perform");
            }
        }
    }

    /** A card chord and how the handler decodes it. */
    private record Chord(KeyCode code, boolean shift, ShortcutAction action) {
    }

    /** The full card, in the order every bundle must list it. */
    private static final Map<String, Chord> HANDLER_CHORDS = handlerChords();

    private static Map<String, Chord> handlerChords() {
        var chords = new LinkedHashMap<String, Chord>();
        chords.put("Ctrl+T", new Chord(KeyCode.T, false, ShortcutAction.TOGGLE_TOOLBAR));
        chords.put("Ctrl+Shift+T", new Chord(KeyCode.T, true, ShortcutAction.CYCLE_THEME));
        chords.put("Ctrl+Shift+Y", new Chord(KeyCode.Y, true, ShortcutAction.TOGGLE_YOLO));
        chords.put("Ctrl+Shift+C", new Chord(KeyCode.C, true, ShortcutAction.CYCLE_TEXT_COLOR));
        chords.put("Ctrl++", new Chord(KeyCode.EQUALS, false, ShortcutAction.FONT_UP));
        chords.put("Ctrl+-", new Chord(KeyCode.MINUS, false, ShortcutAction.FONT_DOWN));
        chords.put("Ctrl+0", new Chord(KeyCode.DIGIT0, false, ShortcutAction.FONT_RESET));
        chords.put("Ctrl+Z", new Chord(KeyCode.Z, false, ShortcutAction.UNDO));
        chords.put("Ctrl+Shift+Z", new Chord(KeyCode.Z, true, ShortcutAction.REDO));
        chords.put("Ctrl+Y", new Chord(KeyCode.Y, false, ShortcutAction.REDO));
        chords.put("Ctrl+N", new Chord(KeyCode.N, false, ShortcutAction.NEW_DOCUMENT));
        chords.put("Ctrl+O", new Chord(KeyCode.O, false, ShortcutAction.OPEN));
        chords.put("Ctrl+S", new Chord(KeyCode.S, false, ShortcutAction.SAVE));
        chords.put("Ctrl+Shift+S", new Chord(KeyCode.S, true, ShortcutAction.SAVE_AS));
        chords.put("F11", new Chord(KeyCode.F11, false, ShortcutAction.TOGGLE_FULLSCREEN));
        // Fullscreen exit is not resolved here; see the test javadoc.
        chords.put("Escape", new Chord(KeyCode.ESCAPE, false, ShortcutAction.NONE));
        return Collections.unmodifiableMap(chords);
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
