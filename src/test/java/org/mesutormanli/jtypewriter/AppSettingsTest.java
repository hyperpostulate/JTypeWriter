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

    /** RW-18: toolbar visibility, fullscreen and the stats bar size survive a restart. */
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

    /** RW-18: a corrupt stats size must degrade to MEDIUM, never break startup. */
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
}