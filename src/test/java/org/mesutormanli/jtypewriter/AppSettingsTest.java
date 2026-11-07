/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.ui.theme.FontSizeManager;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Preferences must survive a restart, and nothing the user (or an older
 * version) writes into the settings file may break startup: settings are a
 * convenience, never a data channel.
 */
class AppSettingsTest {

    @Test
    void defaultsApplyWhenNoFileExists(@TempDir Path dir) {
        var settings = new AppSettings(dir.resolve("settings.properties"));

        assertEquals(Optional.empty(), settings.getLanguage());
        assertEquals(Theme.DARK, settings.getTheme());
        assertEquals(TextColor.DEFAULT, settings.getTextColor());
        assertEquals(FontSizeManager.DEFAULT_SIZE, settings.getFontSize());
        assertTrue(settings.isSoundEnabled());
        assertTrue(settings.isToolbarVisible());
        assertEquals(AppSettings.StatsBarSize.MEDIUM, settings.getStatsBarSize());
        assertFalse(settings.isFullScreen());
        assertEquals(Optional.empty(), settings.getLastFile());
        assertEquals(Optional.empty(), settings.getLastDirectory());
        assertEquals(Optional.empty(), settings.getWindowState());
    }

    @Test
    void preferencesSurviveARestart(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        settings.setLanguage(Language.TR);
        settings.setTheme(Theme.SEPIA);
        settings.setTextColor(TextColor.SAGE);
        settings.setFontSize(22);
        settings.setSoundEnabled(false);

        var reloaded = new AppSettings(file);

        assertEquals(Optional.of(Language.TR), reloaded.getLanguage());
        assertEquals(Theme.SEPIA, reloaded.getTheme());
        assertEquals(TextColor.SAGE, reloaded.getTextColor());
        assertEquals(22, reloaded.getFontSize());
        assertFalse(reloaded.isSoundEnabled());
    }

    /**
     * The disk-cost notice is shown once and then never again, so the flag has
     * to survive a restart: losing it would turn a one-time explanation into a
     * nag on every launch, and keeping it in memory only would do exactly that.
     */
    @Test
    void theStorageNoticeIsShownOnlyOnce(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        assertFalse(settings.isStorageNoticeShown(), "a fresh install has not been told yet");

        settings.setStorageNoticeShown(true);

        assertTrue(new AppSettings(file).isStorageNoticeShown());
    }

    @Test
    void remembersTheLastDocumentAndItsDirectory(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        var document = dir.resolve("novel.txt");
        Files.writeString(document, "chapter one");

        settings.rememberFile(document);

        var reloaded = new AppSettings(file);
        assertEquals(document.toAbsolutePath(), reloaded.getLastFile().orElseThrow());
        assertEquals(dir.toAbsolutePath(), reloaded.getLastDirectory().orElseThrow());
    }

    @Test
    void windowGeometrySurvivesARestart(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        var state = new AppSettings.WindowState(false, 12.5, 30, 1024, 768);

        settings.setWindowState(state);

        assertEquals(state, new AppSettings(file).getWindowState().orElseThrow());
    }

    /**
     * {@code Double.parseDouble} accepts {@code NaN} and {@code Infinity},
     * and {@code Infinity > 0} passes the width/height guard in {@code MainStage}
     * — so a hand-edited settings file used to reach {@code Stage.setWidth(
     * Infinity)} and break startup. A non-finite size is now indistinguishable
     * from a missing one, and the window opens with its default geometry.
     */
    @Test
    void nonFiniteWindowSizeIsRejectedInsteadOfBreakingStartup(@TempDir Path dir) throws IOException {
        for (var broken : new String[]{"Infinity", "-Infinity", "NaN"}) {
            var file = dir.resolve("settings-" + broken.replace("-", "negative") + ".properties");
            Files.writeString(file,
                    "window.width=" + broken + "\nwindow.height=768\n");
            assertEquals(Optional.empty(), new AppSettings(file).getWindowState(),
                    "window.width=" + broken + " must not produce a window state");

            Files.writeString(file,
                    "window.width=1024\nwindow.height=" + broken + "\n");
            assertEquals(Optional.empty(), new AppSettings(file).getWindowState(),
                    "window.height=" + broken + " must not produce a window state");
        }
    }

    /** A non-finite position is only as dangerous as a non-finite size; it
     * degrades to the origin while the finite size is still restored. */
    @Test
    void nonFiniteWindowPositionFallsBackToTheOrigin(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file,
                "window.width=1024\nwindow.height=768\nwindow.x=Infinity\nwindow.y=-Infinity\n");

        var state = new AppSettings(file).getWindowState().orElseThrow();

        assertEquals(0d, state.x());
        assertEquals(0d, state.y());
        assertEquals(1024d, state.width());
        assertEquals(768d, state.height());
    }

    /** Toolbar visibility, fullscreen and the stats bar size survive a restart. */
    @Test
    void appearanceStateSurvivesARestart(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);

        settings.setToolbarVisible(false);
        settings.setFullScreen(true);
        settings.setStatsBarSize(AppSettings.StatsBarSize.LARGE);

        var reloaded = new AppSettings(file);
        assertFalse(reloaded.isToolbarVisible());
        assertTrue(reloaded.isFullScreen());
        assertEquals(AppSettings.StatsBarSize.LARGE, reloaded.getStatsBarSize());
    }

    /** A corrupt stats size must degrade to MEDIUM, never break startup. */
    @Test
    void corruptAppearanceValuesFallBackToDefaults(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file,
                "toolbar.visible=perhaps\nwindow.fullscreen=maybe\nstats.size=HUGE\n");

        var settings = new AppSettings(file);

        assertTrue(settings.isToolbarVisible());
        assertFalse(settings.isFullScreen());
        assertEquals(AppSettings.StatsBarSize.MEDIUM, settings.getStatsBarSize());
    }

    @Test
    void unknownValuesFallBackToDefaults(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "theme=NOPE\nfont.size=big\nsound.enabled=perhaps\nlanguage=klingon\n");

        var settings = new AppSettings(file);

        assertEquals(Theme.DARK, settings.getTheme());
        assertEquals(FontSizeManager.DEFAULT_SIZE, settings.getFontSize());
        assertTrue(settings.isSoundEnabled());
        assertEquals(Optional.empty(), settings.getLanguage());
    }

    @Test
    void aCorruptFileNeverBreaksStartup(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        // a malformed unicode escape: Properties.load throws IllegalArgumentException
        Files.writeString(file, "bad=\\u00\ntheme=LIGHT\n");

        var settings = new AppSettings(file);

        assertEquals(Theme.DARK, settings.getTheme());
    }

    @Test
    void aCorruptFileIsQuarantinedInsteadOfOverwritten(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        var corrupt = "bad=\\u00\ntheme=LIGHT\n";
        Files.writeString(file, corrupt);

        var settings = new AppSettings(file);

        assertEquals(Theme.DARK, settings.getTheme());
        assertFalse(Files.exists(file), "the corrupt file must be moved aside");
        var copies = quarantined(dir);
        assertEquals(1, copies.size());
        assertTrue(Files.readString(copies.get(0)).contains("bad=\\u00"),
                "the quarantined copy must still contain the original corrupt bytes");
    }

    @Test
    void savingAfterQuarantineCreatesAFreshValidFile(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "bad=\\u00\ntheme=LIGHT\n");
        var settings = new AppSettings(file);
        var copy = quarantined(dir).get(0);
        var quarantinedBytes = Files.readAllBytes(copy);

        settings.setTheme(Theme.SEPIA);

        assertTrue(Files.isRegularFile(file));
        assertEquals(Theme.SEPIA, new AppSettings(file).getTheme());
        assertArrayEquals(quarantinedBytes, Files.readAllBytes(copy),
                "the quarantined copy must not be touched by later saves");
    }

    @Test
    void aSecondCorruptionDoesNotOverwriteTheFirstCopy(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "bad=\\u00\nmarker=first\n");
        new AppSettings(file);

        Files.writeString(file, "bad=\\u00\nmarker=second\n");
        new AppSettings(file);

        var copies = quarantined(dir);
        assertEquals(2, copies.size());
        var contents = new ArrayList<String>();
        for (var copy : copies) {
            contents.add(Files.readString(copy));
        }
        assertTrue(contents.stream().anyMatch(c -> c.contains("marker=first")));
        assertTrue(contents.stream().anyMatch(c -> c.contains("marker=second")));
    }

    @Test
    void anExistingQuarantineCopyIsPreserved(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        var existing = file.resolveSibling("settings.properties.corrupt");
        Files.writeString(existing, "previous run evidence");
        Files.writeString(file, "bad=\\u00\n");

        new AppSettings(file);

        assertEquals("previous run evidence", Files.readString(existing));
        var copies = quarantined(dir);
        assertEquals(2, copies.size());
        var moved = new ArrayList<String>();
        for (var copy : copies) {
            if (!copy.equals(existing)) {
                moved.add(Files.readString(copy));
            }
        }
        assertEquals(1, moved.size());
        assertTrue(moved.get(0).contains("bad=\\u00"));
    }

    @Test
    void anUnpreservableCorruptFileStillFallsBackToDefaults(@TempDir Path dir) throws IOException {
        Assumptions.assumeTrue(dir.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "requires a POSIX file system");
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "bad=\\u00\ntheme=LIGHT\n");
        var original = Files.getPosixFilePermissions(dir);
        try {
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("r-xr-xr-x"));
            Assumptions.assumeFalse(Files.isWritable(dir),
                    "privileges ignore directory permissions, cannot simulate a failed quarantine");

            var settings = assertDoesNotThrow(() -> new AppSettings(file));

            assertEquals(Theme.DARK, settings.getTheme());
            assertEquals(Optional.empty(), settings.getLanguage());
            assertTrue(Files.exists(file), "the corrupt file must be left in place when it cannot be moved");
        } finally {
            Files.setPosixFilePermissions(dir, original);
        }
    }

    @Test
    void validSettingsAreNeverQuarantined(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "theme=SEPIA\n");

        new AppSettings(file);
        new AppSettings(file);

        assertTrue(quarantined(dir).isEmpty());
    }

    private static List<Path> quarantined(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries
                    .filter(path -> path.getFileName().toString().startsWith("settings.properties.corrupt"))
                    .sorted()
                    .toList();
        }
    }

    @Test
    void unusablePathSettingsAreIgnoredInsteadOfBreakingStartup(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        // the file system rejects this path everywhere (NUL character)
        var unusable = "bad" + (char) 0 + "name";
        Files.writeString(file, "last.file=" + unusable + "\nlast.directory=" + unusable + "\n");

        var settings = new AppSettings(file);

        assertEquals(Optional.empty(), settings.getLastFile());
        assertEquals(Optional.empty(), settings.getLastDirectory());
    }

    /**
     * W4-01 / D13: settings files written before {@code schema.version} existed
     * have exactly the shape version 1 reads (no key was ever renamed or
     * removed), so opening one must keep every value - and must not rewrite the
     * file behind the user's back: a read is not a migration.
     */
    @Test
    void aFileWithoutASchemaVersionKeepsEveryValueAndIsNotRewrittenOnRead(@TempDir Path dir)
            throws IOException {
        var file = dir.resolve("settings.properties");
        var onDisk = "theme=LIGHT\nfont.size=19\nsound.enabled=false\nlanguage=TR\n";
        Files.writeString(file, onDisk);

        var settings = new AppSettings(file);

        assertEquals(Theme.LIGHT, settings.getTheme());
        assertEquals(19, settings.getFontSize());
        assertFalse(settings.isSoundEnabled());
        assertEquals(Optional.of(Language.TR), settings.getLanguage());
        assertEquals(onDisk, Files.readString(file),
                "reading a settings file must not rewrite it");
    }

    /**
     * The migration is in memory only, so the version marker has to appear on
     * the first real write - otherwise a first-run file would stay unversioned
     * until the user happened to change a setting. The marker is the product
     * version, the same number About shows.
     */
    @Test
    void theFirstSaveStampsTheCurrentSchemaVersionAndKeepsTheMigratedValues(@TempDir Path dir)
            throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "theme=LIGHT\nfont.size=19\n");
        var settings = new AppSettings(file);

        settings.setSoundEnabled(false);

        var rewritten = Files.readString(file);
        assertTrue(rewritten.contains("schema.version=" + AppSettings.CURRENT_SCHEMA_VERSION),
                rewritten);
        assertTrue(rewritten.contains("theme=LIGHT"), rewritten);
        assertTrue(rewritten.contains("font.size=19"), rewritten);
    }

    /**
     * A file stamped by an older <em>release</em> is migrated forward the same
     * way an unversioned one is, and keeps every value: the only migration step
     * that exists is the stamp, because no persisted key has ever been renamed.
     */
    @Test
    void aFileFromAnOlderReleaseIsStampedForwardOnTheNextSave(@TempDir Path dir)
            throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "schema.version=0.9.9\ntheme=SEPIA\nfont.size=17\n");
        var settings = new AppSettings(file);

        assertEquals(Theme.SEPIA, settings.getTheme());
        assertEquals(17, settings.getFontSize());
        assertEquals("schema.version=0.9.9", Files.readString(file).lines()
                        .filter(line -> line.startsWith("schema.version"))
                        .findFirst()
                        .orElseThrow(),
                "reading must not touch the file");

        settings.setTextColor(TextColor.SAGE);

        var rewritten = Files.readString(file);
        assertTrue(rewritten.contains("schema.version=" + AppSettings.CURRENT_SCHEMA_VERSION),
                rewritten);
        assertTrue(rewritten.contains("theme=SEPIA"), rewritten);
        assertTrue(rewritten.contains("font.size=17"), rewritten);
    }

    /**
     * Versions are compared by their numeric segments, so the question the
     * migration asks - older, equal or newer than this build - answers the same
     * way for {@code 1.0}, {@code 1.0.0} and {@code 1.0.0.0}, and a two-digit
     * segment is not read as a string.
     */
    @Test
    void versionsCompareByNumericSegmentsWithMissingSegmentsTreatedAsZero() {
        assertEquals(0, AppSettings.compareVersions("1.0", "1.0.0"));
        assertEquals(0, AppSettings.compareVersions("1.0.3", "1.0.3"));
        assertTrue(AppSettings.compareVersions("1.0.3", "1.1.0") < 0);
        assertTrue(AppSettings.compareVersions("1.1.0", "1.0.3") > 0);
        assertTrue(AppSettings.compareVersions("1.0.10", "1.0.9") > 0,
                "a segment is a number, not text");
        assertTrue(AppSettings.compareVersions("99", "1.0.3") > 0);
    }

    /**
     * A file from a newer build must neither break this one nor be downgraded
     * by a save: the version number and the keys this build does not know are
     * preserved verbatim, so the newer build still recognises its own file after
     * an older build has written to it.
     */
    @Test
    void aNewerSchemaIsToleratedAndNeverDowngradedByASave(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "schema.version=99\ntheme=SEPIA\nfuture.setting=kept\n");
        var settings = new AppSettings(file);

        assertEquals(Theme.SEPIA, settings.getTheme(), "a newer schema must still be read");

        settings.setFontSize(20);

        var rewritten = Files.readString(file);
        assertTrue(rewritten.contains("schema.version=99"),
                "the newer version must survive: " + rewritten);
        assertTrue(rewritten.contains("future.setting=kept"),
                "a key this build does not know must survive: " + rewritten);
        assertTrue(rewritten.contains("theme=SEPIA"), rewritten);
        assertTrue(rewritten.contains("font.size=20"), rewritten);
    }

    /** A garbage version is the same shape as a missing one: unversioned. */
    @Test
    void aMalformedSchemaVersionIsTreatedAsUnversioned(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "schema.version=not-a-number\ntheme=LIGHT\n");

        var settings = new AppSettings(file);

        assertEquals(Theme.LIGHT, settings.getTheme());
        assertFalse(settings.isSettingsWriteFailurePending());
        settings.setFontSize(21);
        assertTrue(Files.readString(file).contains(
                "schema.version=" + AppSettings.CURRENT_SCHEMA_VERSION));
    }

    /**
     * W4-01 / D12: preferences live in a folder the user (or a policy) may make
     * read-only. The setter has already changed the in-memory value, so a failed
     * write must not throw on the caller's thread - it must raise a flag the UI
     * can turn into one notice. The session keeps the change either way; only
     * persistence is lost.
     */
    @Test
    void anUnwritableSettingsFolderRaisesTheReportableFlagInsteadOfThrowing(@TempDir Path dir)
            throws IOException {
        Assumptions.assumeTrue(dir.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "requires a POSIX file system");
        var folder = Files.createDirectory(dir.resolve("settings"));
        var file = folder.resolve("settings.properties");
        var settings = new AppSettings(file);
        assertFalse(settings.isSettingsWriteFailurePending(), "a fresh instance has nothing to report");

        var original = Files.getPosixFilePermissions(folder);
        try {
            Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("r-xr-xr-x"));
            Assumptions.assumeFalse(Files.isWritable(folder),
                    "privileges ignore directory permissions, cannot simulate a failed write");

            assertDoesNotThrow(() -> settings.setTheme(Theme.SEPIA));

            assertEquals(Theme.SEPIA, settings.getTheme(), "the session must keep the change");
            assertTrue(settings.isSettingsWriteFailurePending(), "the failed write must be reportable");

            settings.clearSettingsWriteFailure();
            assertFalse(settings.isSettingsWriteFailurePending(),
                    "a reported failure must not be reported again");
        } finally {
            Files.setPosixFilePermissions(folder, original);
        }
    }

    /**
     * W4-02: the welcome card offers recently opened documents, so opening one
     * has to be remembered in the order it happened - the card's whole value is
     * that "the one I was just writing" is at the top.
     */
    @Test
    void recentDocumentsSurviveARestartNewestFirst(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        var first = Files.writeString(dir.resolve("first.txt"), "one");
        var second = Files.writeString(dir.resolve("second.txt"), "two");

        settings.rememberFile(first);
        settings.rememberFile(second);

        var reloaded = new AppSettings(file).getRecentFiles();
        assertEquals(List.of(second.toAbsolutePath(), first.toAbsolutePath()), reloaded);
    }

    /**
     * Reopening a document moves it back to the top instead of listing it
     * twice, and the list stops at {@link RecentFiles#MAX} entries: the card is
     * a greeting, not a file manager.
     */
    @Test
    void theRecentListIsCappedAndDeduplicated(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        var documents = new ArrayList<Path>();
        for (var index = 0; index < RecentFiles.MAX + 3; index++) {
            documents.add(Files.writeString(dir.resolve("doc-" + index + ".txt"), "text " + index));
        }

        for (var document : documents) {
            settings.rememberFile(document);
        }
        settings.rememberFile(documents.get(0)); // reopened: back to the front, still one row

        var recent = new AppSettings(file).getRecentFiles();
        assertEquals(RecentFiles.MAX, recent.size(), "the list must not grow past the cap");
        assertEquals(documents.get(0).toAbsolutePath(), recent.get(0),
                "the reopened document must be first");
        assertEquals(1,
                recent.stream().filter(path -> path.equals(documents.get(0).toAbsolutePath())).count(),
                "a reopened document must appear exactly once");
        assertFalse(recent.contains(documents.get(1).toAbsolutePath()),
                "the entries that fell off the cap are the oldest, not the reopened one");
    }

    /**
     * A document deleted outside the app must silently leave the list. The card
     * cannot offer a row that fails when clicked: that would be a permanent
     * error the user has no way to clear, on a surface shown at every launch.
     */
    @Test
    void aDeletedDocumentSilentlyLeavesTheRecentList(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        var kept = Files.writeString(dir.resolve("kept.txt"), "keep me");
        var removed = Files.writeString(dir.resolve("removed.txt"), "delete me");
        settings.rememberFile(kept);
        settings.rememberFile(removed);

        Files.delete(removed);

        var recent = assertDoesNotThrow(() -> new AppSettings(file).getRecentFiles());
        assertEquals(List.of(kept.toAbsolutePath()), recent);
    }

    /** A hand-edited or half-written entry is skipped, not fatal. */
    @Test
    void anUnusableRecentEntryIsIgnored(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        var kept = Files.writeString(dir.resolve("kept.txt"), "text");
        Files.writeString(file, "recent.file.0=" + kept + "\nrecent.file.1=bad" + (char) 0 + "name\n");

        var recent = new AppSettings(file).getRecentFiles();

        assertEquals(List.of(kept), recent);
    }

    /**
     * W4-02: the greeting is on until the user says otherwise, and the opt-out
     * is a stored preference - an in-memory flag would bring the card back at
     * the next launch, which is exactly the nag the checkbox exists to stop.
     */
    @Test
    void theWelcomeCardIsShownUntilTheUserOptsOut(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        assertTrue(settings.isWelcomeCardEnabled(), "a fresh install greets the user");

        settings.setWelcomeCardEnabled(false);

        assertFalse(new AppSettings(file).isWelcomeCardEnabled());
    }

    /** Asking for the card again re-enables it, so the checkbox is not a one-way door. */
    @Test
    void theWelcomeCardCanBeTurnedBackOn(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        settings.setWelcomeCardEnabled(false);

        settings.setWelcomeCardEnabled(true);

        assertTrue(new AppSettings(file).isWelcomeCardEnabled());
    }
}
