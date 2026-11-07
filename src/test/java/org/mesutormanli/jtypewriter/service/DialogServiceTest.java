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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Stream;

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

    /**
     * The default name of a never-saved document carries an ISO-ordered,
     * filesystem-safe stamp: sortable, identical in every UI language
     * (a file name is not prose), and free of the colon Windows forbids.
     */
    @Test
    void theSuggestedFileNameStampIsIsoOrderedAndFilesystemSafe() {
        assertEquals("2026-11-03_14-05-30",
                DialogService.fileStamp(LocalDateTime.of(2026, 11, 3, 14, 5, 30, 123_000_000)));
        assertFalse(DialogService.fileStamp(LocalDateTime.now()).contains(":"),
                "a colon is illegal in a Windows file name");
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
     * The tagline is drawn as text inside both overlay cards (welcome and
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

    /**
     * The single-alert-home invariant of W0-01: no other class may construct an
     * {@code Alert}, because a second construction site is a second chance to
     * forget the theme, the owner, the screen clamp or the text wrap. Verified
     * by scanning the sources, the same way {@code DialogSizingTest} checks the
     * shared scroll class; a live Alert needs a toolkit, which this suite must
     * never start.
     */
    @Test
    void noClassOutsideDialogServiceConstructsAnAlert() {
        var offenders = new ArrayList<String>();
        try (Stream<Path> sources = Files.walk(Path.of("src", "main", "java"))) {
            for (var path : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (!path.getFileName().toString().equals("DialogService.java")
                        && Files.readString(path, StandardCharsets.UTF_8).contains("new Alert(")) {
                    offenders.add(path.toString());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot scan the main sources", e);
        }

        assertTrue(offenders.isEmpty(),
                "these files build their own Alert instead of going through DialogService: " + offenders);
    }

    /**
     * Every Alert built here must take the shared preparation path (theme,
     * clamp, wrap) instead of calling {@code showAndWait} itself. Counting the
     * construction sites against the calls into {@code show(...)} catches a new
     * dialog added without the clamp.
     */
    @Test
    void everyAlertInDialogServiceIsShownThroughTheSharedPath() {
        var source = readSource("DialogService.java");

        assertEquals(count(source, "new Alert("), count(source, "show(alert,"),
                "an Alert is constructed but shown without prepare()");
        assertTrue(source.contains("DialogSizing.alertWidth("),
                "the alert width is not clamped to the screen");
        assertTrue(source.contains("DialogSizing.alertContentWidth("),
                "the wrapped labels are not bounded");
        assertTrue(source.contains("setWrapText(true)"),
                "alert text is not wrapped");
        assertTrue(source.contains("setResizable(true)"),
                "a clamped alert must stay resizable");
    }

    private static String readSource(String name) {
        var path = Path.of("src", "main", "java", "org", "mesutormanli", "jtypewriter",
                "service", name);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path.toAbsolutePath(), e);
        }
    }

    private static int count(String haystack, String needle) {
        int found = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            found++;
        }
        return found;
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }
}