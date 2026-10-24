package org.mesutormanli.jtypewriter.ui.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dropdown ladder, its off-ladder handling and the persistence rules.
 * {@link FontSizeManager#reset()} is the single reset path (Ctrl+0 must not
 * hard-code the default size) and {@link FontSizeManager#setSize(int)} the
 * single setter (the dropdown must never silently change a stored size that
 * is inside the supported range).
 */
class FontSizeManagerTest {

    @Test
    void theLadderIsTheFixedSequenceAscendingAndUnique() {
        var expected = new ArrayList<Integer>();
        for (int i = 10; i <= 20; i++) {
            expected.add(i);
        }
        for (int i : new int[]{22, 24, 28, 32, 36, 40, 44, 48}) {
            expected.add(i);
        }

        assertEquals(expected, FontSizeManager.options());
        assertEquals(FontSizeManager.options().size(),
                new HashSet<>(FontSizeManager.options()).size(), "no duplicates");
        assertFalse(FontSizeManager.options().isEmpty());
    }

    @Test
    void theDefaultSizeIsOnTheLadder() {
        assertTrue(FontSizeManager.options().contains(FontSizeManager.DEFAULT_SIZE));
    }

    @Test
    void optionsForALadderSizeReturnsTheLadderUnchanged() {
        assertEquals(FontSizeManager.options(), FontSizeManager.optionsFor(16));
        assertEquals(FontSizeManager.options(), FontSizeManager.optionsFor(48));
    }

    /** A 21 persisted by an older build must stay visible in the dropdown, not be changed. */
    @Test
    void optionsForInsertsAnOffLadderSizeInSortedPosition() {
        var items = FontSizeManager.optionsFor(21);

        assertEquals(FontSizeManager.options().size() + 1, items.size());
        assertEquals(List.of(20, 21, 22), items.subList(10, 13));
    }

    @Test
    void optionsForClampsAnOutOfRangeSizeIntoTheLadder() {
        assertEquals(FontSizeManager.options(), FontSizeManager.optionsFor(51)); // clamps to 48
        assertEquals(FontSizeManager.options(), FontSizeManager.optionsFor(3));  // clamps to 10
    }

    @Test
    void setSizePersistsAndIsReadableByAFreshManager(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var manager = new FontSizeManager(new AppSettings(file));

        manager.setSize(24);

        assertEquals(24, manager.getCurrentSize());
        assertEquals(24, new AppSettings(file).getFontSize());
    }

    @Test
    void setSizeClampsToTheSupportedRange(@TempDir Path dir) {
        var manager = new FontSizeManager(new AppSettings(dir.resolve("settings.properties")));

        manager.setSize(100);
        assertEquals(FontSizeManager.MAX_SIZE, manager.getCurrentSize());

        manager.setSize(1);
        assertEquals(FontSizeManager.MIN_SIZE, manager.getCurrentSize());
    }

    @Test
    void setSizeKeepsAnOffLadderSizeThatIsInRange(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var manager = new FontSizeManager(new AppSettings(file));

        manager.setSize(21);

        assertEquals(21, manager.getCurrentSize());
        assertEquals(21, new AppSettings(file).getFontSize());
    }

    @Test
    void stepUpWalksTheLadderAndStopsAtTheTop() {
        assertEquals(17, FontSizeManager.stepUp(16));
        assertEquals(22, FontSizeManager.stepUp(20)); // the ladder gap after 20
        assertEquals(FontSizeManager.MAX_SIZE, FontSizeManager.stepUp(48));
    }

    @Test
    void stepDownWalksTheLadderAndStopsAtTheBottom() {
        assertEquals(16, FontSizeManager.stepDown(17));
        assertEquals(20, FontSizeManager.stepDown(22)); // the ladder gap after 20
        assertEquals(FontSizeManager.MIN_SIZE, FontSizeManager.stepDown(10));
    }

    @Test
    void stepsSnapAnOffLadderSizeOntoTheLadder() {
        assertEquals(22, FontSizeManager.stepUp(21));
        assertEquals(20, FontSizeManager.stepDown(21));
    }

    @Test
    void resetRestoresTheDefaultSizeAndPersistsIt(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var manager = new FontSizeManager(new AppSettings(file));
        manager.setSize(26);

        manager.reset();

        assertEquals(FontSizeManager.DEFAULT_SIZE, manager.getCurrentSize());
        assertEquals(FontSizeManager.DEFAULT_SIZE, new AppSettings(file).getFontSize());
    }

    /** An out-of-range stored size degrades to the clamped value, as before. */
    @Test
    void aStoredSizeOutOfRangeIsClampedOnLoad(@TempDir Path dir) {
        var file = dir.resolve("settings.properties");
        var stored = new AppSettings(file);
        stored.setFontSize(9999);

        var manager = new FontSizeManager(new AppSettings(file));

        assertEquals(FontSizeManager.MAX_SIZE, manager.getCurrentSize());
    }
}
