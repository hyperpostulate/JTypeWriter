package org.mesutormanli.jtypewriter.ui.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RW-18: the stats bar size is persisted through {@link AppSettings} like the
 * sound flag in {@link ToolbarStateTest}, so it survives a restart. Only the
 * state object is exercised - {@code StatsBar.Size} is a plain nested enum,
 * so this stays headless like every other test (no control is constructed).
 */
class StatsBarStateTest {

    @Test
    void sizeStartsFromThePersistedSetting(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        new AppSettings(file).setStatsBarSize(AppSettings.StatsBarSize.LARGE);

        var state = new StatsBarState(new AppSettings(file));

        assertEquals(StatsBar.Size.LARGE, state.getSize());
    }

    @Test
    void aFreshInstallStartsAtMedium(@TempDir Path dir) {
        var state = new StatsBarState(new AppSettings(dir.resolve("settings.properties")));

        assertEquals(StatsBar.Size.MEDIUM, state.getSize(),
                "RW-17: the factory default must stay MEDIUM (>= 13 px)");
    }

    /** The double-click cycle SMALL → MEDIUM → LARGE → SMALL, each step written through. */
    @Test
    void nextSizeCyclesAndPersistsEachStep(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var state = new StatsBarState(new AppSettings(file));

        assertEquals(StatsBar.Size.LARGE, state.nextSize());
        assertEquals(AppSettings.StatsBarSize.LARGE, new AppSettings(file).getStatsBarSize());

        assertEquals(StatsBar.Size.SMALL, state.nextSize());
        assertEquals(AppSettings.StatsBarSize.SMALL, new AppSettings(file).getStatsBarSize());

        assertEquals(StatsBar.Size.MEDIUM, state.nextSize());
        var reloaded = new StatsBarState(new AppSettings(file));
        assertEquals(StatsBar.Size.MEDIUM, reloaded.getSize(), "survives a restart");
    }

    @Test
    void aCorruptStoredSizeDegradesToMediumWithoutFailing(@TempDir Path dir) throws IOException {
        var file = dir.resolve("settings.properties");
        Files.writeString(file, "stats.size=HUGE\n");

        var state = new StatsBarState(new AppSettings(file));

        assertEquals(StatsBar.Size.MEDIUM, state.getSize());
    }

    /** The two enums are mapped exhaustively both ways, not by string round-tripping. */
    @Test
    void theMappingCoversBothEnumsInBothDirections() {
        for (var persisted : AppSettings.StatsBarSize.values()) {
            assertEquals(persisted, StatsBarState.toPersisted(StatsBarState.toSize(persisted)));
        }
        for (var size : StatsBar.Size.values()) {
            assertEquals(size, StatsBarState.toSize(StatsBarState.toPersisted(size)));
        }
    }
}
