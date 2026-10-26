/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shortcut list used to describe the themes in declaration order
 * (Light/Dark/Sepia) while {@link Theme#next()} cycles from the persisted
 * default {@link Theme#DARK} in the order Dark/Sepia/Light. The docs and the
 * code must say the same thing, so this test pins the real cycle in every
 * bundle; flipping a translation back to declaration order breaks it.
 */
class ThemeCycleTest {

    @TempDir
    Path dir;

    @Test
    void valuesKeepTheToolbarOrder() {
        assertEquals(List.of(Theme.LIGHT, Theme.DARK, Theme.SEPIA), List.of(Theme.values()));
    }

    @Test
    void defaultThemeIsDark() {
        var settings = new AppSettings(dir.resolve("settings.properties"));

        assertEquals(Theme.DARK, settings.getTheme());
    }

    /** From the default, next() yields SEPIA, LIGHT, DARK - a 3-cycle back to the start. */
    @Test
    void nextCyclesBackToTheStartFromTheDefault() {
        var start = new AppSettings(dir.resolve("settings.properties")).getTheme();
        var visited = new ArrayList<Theme>();
        var theme = start;
        for (var i = 0; i < Theme.values().length; i++) {
            theme = theme.next();
            visited.add(theme);
        }

        assertEquals(List.of(Theme.SEPIA, Theme.LIGHT, Theme.DARK), visited);
        assertEquals(start, theme, "the cycle must return to its starting theme");
    }

    @Test
    void everyBundleDocumentsTheCycleStartingAtTheDefault() {
        for (var language : Language.values()) {
            var messages = messagesFor(language);
            var expected = List.of(
                    Theme.DARK.getDisplayName(messages),
                    Theme.SEPIA.getDisplayName(messages),
                    Theme.LIGHT.getDisplayName(messages));

            assertEquals(expected, themeNamesIn(messages.welcomeShortcuts()),
                    "the shortcut list of " + language + " does not match Theme.next() from the default");
        }
    }

    /**
     * The theme names are the last parenthesised group of the shortcut line,
     * so this does not depend on how many spaces separate the chord from its
     * description (the bundles pad the columns differently per language).
     */
    private static List<String> themeNamesIn(String shortcuts) {
        var line = shortcuts.lines()
                .filter(candidate -> candidate.contains("Ctrl+Shift+T"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no theme shortcut in: " + shortcuts));
        var open = line.lastIndexOf('(');
        var close = line.indexOf(')', open);
        assertTrue(open > 0 && close > open, "no theme list in: " + line);

        var names = new ArrayList<String>();
        for (var name : line.substring(open + 1, close).split("/")) {
            names.add(name.trim());
        }
        return names;
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }
}
