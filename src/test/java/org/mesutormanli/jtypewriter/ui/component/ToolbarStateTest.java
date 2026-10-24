package org.mesutormanli.jtypewriter.ui.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sound dropdown's single setter: persists through {@link AppSettings}
 * exactly like the old toggle button did, and a repeat of the current state
 * is a no-op. YOLO mode itself is covered by {@link YoloPolicyTest}; the flag
 * itself needs the JavaFX editor control and is therefore not exercised here
 * (tests never construct controls).
 */
class ToolbarStateTest {

    @Test
    void soundStartsFromThePersistedSetting(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var stored = new AppSettings(file);
        stored.setSoundEnabled(false);

        var state = new ToolbarState(new TypewriterSound(), new AppSettings(file));

        assertFalse(state.isSoundEnabled());
    }

    @Test
    void setSoundUpdatesTheSoundAndPersistsIt(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var sound = new TypewriterSound();
        var state = new ToolbarState(sound, new AppSettings(file));

        state.setSound(false);

        assertFalse(state.isSoundEnabled());
        assertFalse(sound.isEnabled(), "the dropdown must reach the audio engine too");
        assertFalse(new AppSettings(file).isSoundEnabled(), "survives a restart");

        state.setSound(true);

        assertTrue(state.isSoundEnabled());
        assertTrue(new AppSettings(file).isSoundEnabled());
    }

    /** A repeat of the current value is a no-op, never a corruption of the state. */
    @Test
    void setSoundIgnoresTheStateAlreadyInForce(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var settings = new AppSettings(file);
        settings.setSoundEnabled(true);
        var sound = new TypewriterSound();
        var state = new ToolbarState(sound, settings);

        state.setSound(true);
        state.setSound(true);

        assertTrue(state.isSoundEnabled());
        assertTrue(sound.isEnabled());
        assertTrue(new AppSettings(file).isSoundEnabled());
    }
}
