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

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The window title is the only place a user can see which document is open and
 * whether it is saved (the toolbar can be hidden), and the stats bar carries
 * the "Saved 12:04" confirmation. Both compositions are pure so they
 * can be pinned down here without a JavaFX toolkit.
 */
class MainControllerTest {

    @Test
    void titleShowsTheApplicationAndDocumentName() {
        assertEquals("JTypeWriter - novel.txt",
                MainController.titleFor("JTypeWriter", "novel.txt", false, false));
    }

    @Test
    void unsavedChangesAreMarkedWithAStar() {
        assertEquals("JTypeWriter - novel.txt *",
                MainController.titleFor("JTypeWriter", "novel.txt", true, false));
    }

    @Test
    void yoloBadgeIsAppendedAfterTheDirtyMarker() {
        assertEquals("JTypeWriter - novel.txt *   [YOLO]",
                MainController.titleFor("JTypeWriter", "novel.txt", true, true));
        assertEquals("JTypeWriter - novel.txt   [YOLO]",
                MainController.titleFor("JTypeWriter", "novel.txt", false, true));
    }

    /**
     * The text listener refreshes the title on every keystroke, so
     * an unchanged composition must never reach the native window again.
     */
    @Test
    void titleIsOnlyRewrittenWhenTheDirtyStateChanges() {
        var clean = MainController.titleFor("JTypeWriter", "novel.txt", false, false);
        var dirty = MainController.titleFor("JTypeWriter", "novel.txt", true, false);

        assertFalse(MainController.titleChanged(clean, clean));
        assertTrue(MainController.titleChanged(clean, dirty));
        assertTrue(MainController.titleChanged(null, clean), "the first paint has no previous title");
    }

    /**
     * The save status is composed from the bundle pattern and an
     * injected time (SessionStats' clock idiom), so it tests headlessly with
     * a fixed time - and the reference example "Saved 12:04" is pinned.
     */
    @Test
    void savedStatusUsesTheInjectedTime() {
        assertEquals("Saved 12:04",
                MainController.savedStatusFor("Saved {0}", LocalTime.of(12, 4), Locale.ENGLISH));
        assertEquals("Saved 09:07",
                MainController.savedStatusFor("Saved {0}", LocalTime.of(9, 7), Locale.ENGLISH),
                "single-digit hours and minutes keep their leading zero");
    }

    /**
     * The toolbar toggle is a focus boundary too: the control
     * that had focus may be gone after it, so hiding the toolbar has to hand
     * the caret back to the editor.
     */
    @Test
    void hidingTheToolbarHandsFocusBackToTheEditor() {
        assertTrue(MainController.handOffFocusToEditor(false));
        assertFalse(MainController.handOffFocusToEditor(true));
    }

    /** The shipped English and Turkish texts, through the real bundle lookup. */
    @Test
    void savedStatusIsLocalisedThroughTheBundles(@TempDir Path dir) {
        assertEquals("Saved 12:04", savedStatusFromBundle(Language.EN, LocalTime.of(12, 4), dir));
        assertEquals("Kaydedildi 12:04", savedStatusFromBundle(Language.TR, LocalTime.of(12, 4), dir));
    }

    private static String savedStatusFromBundle(Language language, LocalTime savedAt, Path dir) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        var messages = new Messages(localeManager);
        return MainController.savedStatusFor(messages.savedStatusTemplate(), savedAt, messages.locale());
    }
}
