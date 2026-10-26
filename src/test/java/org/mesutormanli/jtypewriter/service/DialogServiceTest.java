/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The save-as name rules are pure logic kept away from the (headless-hostile)
 * FileChooser so they can be pinned down here; the dialog window-title keys
 * are likewise checked as pure resource lookups.
 */
class DialogServiceTest {

    @TempDir
    Path dir;

    @Test
    void extensionLessNamesGetTheTxtExtension() {
        assertEquals("notes.txt", DialogService.withDefaultExtension(new File("notes")).getName());
    }

    @Test
    void explicitExtensionsAreKeptAsTyped() {
        assertEquals("notes.md", DialogService.withDefaultExtension(new File("notes.md")).getName());
        assertEquals("archive.tar.gz", DialogService.withDefaultExtension(new File("archive.tar.gz")).getName());
    }

    @Test
    void theSiblingStaysInPlace() {
        var saved = DialogService.withDefaultExtension(new File("novels/notes"));

        assertEquals(new File("novels/notes.txt"), saved);
    }

    @Test
    void closingTheRecoveryPromptMeansLaterNeverDiscard() {
        // -1 = closed without a choice: the draft must survive, so this is not DISCARD
        assertEquals(RecoveryChoice.LATER, DialogService.choiceFor(-1));
        assertEquals(RecoveryChoice.RESTORE, DialogService.choiceFor(0));
        assertEquals(RecoveryChoice.DISCARD, DialogService.choiceFor(1));
        assertEquals(RecoveryChoice.LATER, DialogService.choiceFor(2));
    }

    /**
     * The window titles of the welcome and about windows must exist, be
     * non-blank and name the application in every bundled locale (the task
     * switcher falls back to a blank/"Stage" entry otherwise). This covers the
     * resource lookup only: applying the title needs a live {@code Stage}, and
     * the error dialog's owner cannot be observed without an {@code Alert} -
     * both need a JavaFX toolkit, so this headless suite deliberately does not
     * test them.
     */
    @Test
    void windowTitlesResolveInEveryBundledLocale() {
        for (var language : Language.values()) {
            var messages = messagesFor(language);
            var welcomeTitle = messages.welcomeWindowTitle();
            var aboutTitle = messages.aboutWindowTitle();

            assertFalse(welcomeTitle.isBlank(), "blank welcome window title in " + language);
            assertFalse(aboutTitle.isBlank(), "blank about window title in " + language);
            assertTrue(welcomeTitle.contains("JTypeWriter"), "welcome title names the app in " + language);
            assertTrue(aboutTitle.contains("JTypeWriter"), "about title names the app in " + language);
        }
    }

    /**
     * The tagline is drawn as text inside both helper windows (welcome and
     * about), so it must resolve non-blank in every bundled locale. This
     * covers the resource lookup only; wrapping it needs a live {@code Label},
     * so this headless suite deliberately does not test the layout.
     */
    @Test
    void appTaglineResolvesInEveryBundledLocale() {
        for (var language : Language.values()) {
            var tagline = messagesFor(language).appTagline();

            assertFalse(tagline.isBlank(), "blank app tagline in " + language);
            assertTrue(tagline.length() > 40, "tagline should be a sentence in " + language);
        }
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }
}