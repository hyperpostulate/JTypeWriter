/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The level ladder behind {@code View > Sound}: the rungs ascend, only the
 * first is silent, and an off-ladder persisted value still resolves to one
 * checked row.
 */
class SoundLevelTest {

    @Test
    void theRungsAscendFromSilenceToFull() {
        double previous = -1;
        for (var level : SoundLevel.values()) {
            assertTrue(level.volume() > previous, level + " is out of order");
            previous = level.volume();
        }
        assertEquals(0.0, SoundLevel.OFF.volume(), 0.0001);
        assertEquals(1.0, SoundLevel.FULL.volume(), 0.0001);
    }

    @Test
    void onlyTheOffRungIsSilent() {
        for (var level : SoundLevel.values()) {
            assertEquals(level == SoundLevel.OFF, level.isOff(), level.toString());
        }
    }

    @Test
    void anOffLadderVolumeResolvesToItsNearestRung() {
        assertEquals(SoundLevel.OFF, SoundLevel.nearest(0.0));
        assertEquals(SoundLevel.LOW, SoundLevel.nearest(0.25));
        assertEquals(SoundLevel.MEDIUM, SoundLevel.nearest(0.5));
        assertEquals(SoundLevel.MEDIUM, SoundLevel.nearest(0.6));
        assertEquals(SoundLevel.HIGH, SoundLevel.nearest(0.75));
        assertEquals(SoundLevel.FULL, SoundLevel.nearest(0.9));
        assertEquals(SoundLevel.LOW, SoundLevel.nearest(0.1),
                "an audible value below the first rung must not show Off");
        assertTrue(SoundLevel.nearest(-1).isOff());
    }
}
