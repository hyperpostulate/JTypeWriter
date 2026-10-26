/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.locale;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The language dropdown shows each language's own name for itself. These are
 * deliberately code constants, not bundle keys (see {@link Language#nativeName()}):
 * a native name is identical in every UI language, and anything that resolved
 * them through the <em>system</em> locale ({@code Locale.getDisplayLanguage})
 * would reintroduce the exact fallback trap {@link Messages} exists to avoid.
 * The tests below pin the trap shut under the Turkish and German system
 * locales the CI runs under.
 */
class LanguageTest {

    @Test
    void everyLanguageNamesItselfInItsOwnOrthography() {
        assertEquals("English", Language.EN.nativeName());
        assertEquals("Türkçe", Language.TR.nativeName());
        assertEquals("Deutsch", Language.DE.nativeName());
        assertEquals("Français", Language.FR.nativeName());
        assertEquals("Italiano", Language.IT.nativeName());
        assertEquals("Español", Language.ES.nativeName());
    }

    /**
     * The dropdown text must not wobble with the system locale: "Türkçe" must
     * not become "Tuerkce"/"Türk" the way {@code getDisplayLanguage} would
     * render it, and none of the six may degrade to ASCII.
     */
    @Test
    void nativeNamesAreIdenticalUnderEverySystemLocale() {
        var previous = Locale.getDefault();
        try {
            for (var system : new Locale[]{
                    Locale.forLanguageTag("tr-TR"),
                    Locale.forLanguageTag("de-DE"),
                    Locale.forLanguageTag("en-US")}) {
                Locale.setDefault(system);
                assertEquals("Türkçe", Language.TR.nativeName(), "under " + system);
                assertEquals("Deutsch", Language.DE.nativeName(), "under " + system);
                assertEquals("Français", Language.FR.nativeName(), "under " + system);
                assertEquals("Español", Language.ES.nativeName(), "under " + system);
                assertEquals("English", Language.EN.nativeName(), "under " + system);
                assertEquals("Italiano", Language.IT.nativeName(), "under " + system);
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void everyLanguageHasABlankFreeAndUniqueNativeName() {
        var names = new HashSet<String>();
        for (var language : Language.values()) {
            var name = language.nativeName();
            assertFalse(name.isBlank(), language + " has no native name");
            assertEquals(name, name.trim(), language + " native name has padding");
            assertFalse(names.contains(name), "duplicate native name: " + name);
            names.add(name);
        }
        assertEquals(Language.values().length, names.size());
    }
}
