package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.ui.theme.FontSizeManager;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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