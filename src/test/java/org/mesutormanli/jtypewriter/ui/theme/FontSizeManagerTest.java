package org.mesutormanli.jtypewriter.ui.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Font size is clamped to the supported range and persisted. {@link FontSizeManager#reset()}
 * is the single reset path (Ctrl+0 must not hard-code the default size).
 */
class FontSizeManagerTest {

    @Test
    void resetRestoresTheDefaultSizeAndPersistsIt(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var manager = new FontSizeManager(new AppSettings(file));
        manager.changeSize(10); // 16 -> 26

        manager.reset();

        assertEquals(FontSizeManager.DEFAULT_SIZE, manager.getCurrentSize());
        assertEquals(FontSizeManager.DEFAULT_SIZE, new AppSettings(file).getFontSize());
    }

    @Test
    void changeSizeClampsToTheSupportedRange(@TempDir Path dir) {
        var manager = new FontSizeManager(new AppSettings(dir.resolve("settings.properties")));

        manager.changeSize(100);

        assertEquals(FontSizeManager.MAX_SIZE, manager.getCurrentSize());
    }
}
