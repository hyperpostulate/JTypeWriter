/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.audio;

/**
 * The volume ladder offered by {@code View > Sound}.
 *
 * <p><strong>Why the level is the setting, not a second flag.</strong> Sound
 * used to be one boolean, so "off" and "quiet" were the same state: there was
 * no way to keep the typewriter audible without it dominating. A level that
 * includes {@link #OFF} at 0.0 expresses both, and it makes "0 is completely
 * silent" a property of the value instead of a rule the player has to
 * remember. The toolbar's On/Off dropdown stays a shortcut over the same
 * value: Off writes 0.0 and On restores the last audible level.</p>
 *
 * <p>Quarter steps are deliberately coarse. The sample is a single short
 * click, so finer steps would not be audible as different, and every extra
 * menu row costs width in a toolbar whose collapsed row is already measured to
 * the pixel.</p>
 */
public enum SoundLevel {

    /** Fully silent: the persisted value that switches the sound off. */
    OFF(0.0),
    LOW(0.25),
    MEDIUM(0.5),
    HIGH(0.75),
    FULL(1.0);

    private final double volume;

    SoundLevel(double volume) {
        this.volume = volume;
    }

    /** The linear {@link javafx.scene.media.AudioClip#setVolume(double)} value. */
    public double volume() {
        return volume;
    }

    /** Whether this level silences the player. */
    public boolean isOff() {
        return volume == 0.0;
    }

    /**
     * The nearest rung for a persisted volume. Used when the settings file
     * holds a value the ladder does not name (a hand-edited file, or a level
     * from an older build): the menu still shows one checked row instead of
     * none. Any positive volume maps to an audible rung - showing "Off" for a
     * sound that is actually playing would make the menu lie - while zero is
     * exactly {@link #OFF}.
     */
    public static SoundLevel nearest(double volume) {
        if (volume <= 0) {
            return OFF;
        }
        var nearest = LOW;
        var best = Double.MAX_VALUE;
        for (var candidate : values()) {
            if (candidate == OFF) {
                continue;
            }
            var distance = Math.abs(candidate.volume - volume);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }
}
