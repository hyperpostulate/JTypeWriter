/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The welcome and About cards are generated from {@link EditorAction}, so the
 * old two-way drift between the hand-written bundle list and the key handler
 * is structurally impossible: there is no second chord table left to fall out
 * of step. These tests pin the generation itself (order, two-way coverage,
 * descriptions in all six languages) rather than a copied table.
 */
class ShortcutRowsTest {

    @TempDir
    Path dir;

    @Test
    void everyRowOfEveryLanguageHasAChordAndADescription() {
        for (var language : Language.values()) {
            for (var row : rowsFor(language)) {
                assertFalse(row.keys().isBlank(),
                        language + ": row lost its chord: " + row);
                assertFalse(row.description().isBlank(),
                        language + ": row lost its description: " + row);
            }
        }
    }

    /**
     * Two-way coverage: absolutely every chord the inventory declares appears
     * on the card exactly once, and the card invents nothing. A duplicate
     * would also be a routing ambiguity, since {@code resolve()} returns the
     * first match.
     */
    @Test
    void theCardAndTheInventoryCoverEachOtherExactly() {
        var inventory = new LinkedHashSet<String>();
        for (var action : EditorAction.values()) {
            for (var accelerator : action.accelerators()) {
                assertTrue(inventory.add(accelerator.chord()),
                        accelerator.chord() + " is declared twice in EditorAction");
            }
        }

        for (var language : Language.values()) {
            var chords = rowsFor(language).stream().map(ShortcutRows.Row::keys).toList();
            assertEquals(List.copyOf(inventory), chords,
                    language + ": the card and EditorAction disagree on the chords");
        }
    }

    /** The description of each row is exactly the action's localized label. */
    @Test
    void eachRowUsesTheActionsLocalizedLabel() {
        for (var language : Language.values()) {
            var messages = messagesFor(language);
            var rows = rowsFor(language);

            var index = 0;
            for (var action : EditorAction.values()) {
                for (var accelerator : action.accelerators()) {
                    var row = rows.get(index++);
                    assertEquals(accelerator.chord(), row.keys());
                    assertEquals(action.label(messages), row.description(),
                            language + " " + accelerator.chord());
                }
            }
            assertEquals(rows.size(), index);
        }
    }

    /** Ordering is part of the card: it follows the enum, and it is stable. */
    @Test
    void theCardKeepsItsPublishedOrder() {
        for (var language : Language.values()) {
            assertEquals(List.of("Ctrl+T", "Ctrl+Shift+T", "Ctrl+Shift+Y", "Ctrl+Shift+C",
                            "Ctrl++", "Ctrl+-", "Ctrl+0", "Ctrl+Z", "Ctrl+Shift+Z", "Ctrl+Y",
                            "Ctrl+N", "Ctrl+O", "Ctrl+S", "Ctrl+Shift+S", "F11", "Escape"),
                    rowsFor(language).stream().map(ShortcutRows.Row::keys).toList(),
                    language + ": the row order changed");
        }
    }

    /**
     * The theme row must describe the real cycle ({@link Theme#next()} from the
     * persisted default), not the enum declaration order - the bug
     * {@code ThemeCycleTest} still guards at the bundle level.
     */
    @Test
    void themeRowKeepsTheCycleInTheDescription() {
        for (var language : Language.values()) {
            var messages = messagesFor(language);
            var cycle = Theme.DARK.getDisplayName(messages) + "/"
                    + Theme.SEPIA.getDisplayName(messages) + "/"
                    + Theme.LIGHT.getDisplayName(messages);

            var row = rowsFor(language).stream()
                    .filter(candidate -> candidate.keys().equals("Ctrl+Shift+T"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no theme shortcut row in " + language));

            assertTrue(row.description().contains("(" + cycle + ")"),
                    language + ": " + row);
        }
    }

    /** The card is rebuilt for the current language on every call, not frozen. */
    @Test
    void switchingTheLanguageRebuildsTheDescriptions() {
        var messages = messagesFor(Language.EN);
        assertTrue(rowsFor(messages).stream()
                .anyMatch(row -> row.description().equals("Toggle Toolbar")));

        var settings = new AppSettings(dir.resolve("switch.properties"));
        var localeManager = new LocaleManager(settings);
        var live = new Messages(localeManager);
        localeManager.setLanguage(Language.TR);

        assertTrue(ShortcutRows.fromActions(live).stream()
                        .anyMatch(row -> row.description().equals("Araç çubuğu aç/kapa")),
                "the card still shows the previous language");
    }

    /** A chord listed twice would also make routing ambiguous; the card must not. */
    @Test
    void noChordAppearsTwiceOnTheCard() {
        for (var language : Language.values()) {
            var chords = new ArrayList<String>();
            for (var row : rowsFor(language)) {
                chords.add(row.keys());
            }
            assertEquals(Set.copyOf(chords).size(), chords.size(),
                    language + ": a chord is listed twice");
        }
    }

    private List<ShortcutRows.Row> rowsFor(Language language) {
        return ShortcutRows.fromActions(messagesFor(language));
    }

    private List<ShortcutRows.Row> rowsFor(Messages messages) {
        return ShortcutRows.fromActions(messages);
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }
}
