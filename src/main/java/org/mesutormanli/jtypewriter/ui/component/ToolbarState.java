/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.audio.SoundLevel;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;

import java.util.List;

public class ToolbarState {

    /**
     * Option order of the YOLO and Sound dropdowns: the current value is one
     * of these booleans, On is offered before Off.
     */
    public static final List<Boolean> TOGGLE_OPTIONS = List.of(Boolean.TRUE, Boolean.FALSE);

    private final TypewriterSound typewriterSound;
    private final AppSettings settings;

    private boolean yoloMode;
    private double soundVolume;
    /** The last audible level, so On restores what the user chose rather than 50%. */
    private double lastAudibleVolume;

    public ToolbarState(TypewriterSound typewriterSound, AppSettings settings) {
        this.typewriterSound = typewriterSound;
        this.settings = settings;
        this.soundVolume = settings.getSoundVolume();
        this.lastAudibleVolume = soundVolume > 0 ? soundVolume : SoundLevel.MEDIUM.volume();
        typewriterSound.setVolume(soundVolume);
        typewriterSound.setEnabled(soundVolume > 0);
    }

    public boolean isYoloMode() {
        return yoloMode;
    }

    /**
     * The single entry point for YOLO mode, used by the toolbar dropdown and
     * the Ctrl+Shift+Y toggle alike, so the dropdown can never disagree with
     * the editor. A repeat request for the mode already in force is ignored.
     * The {@code [YOLO]} window title badge is refreshed by the caller through
     * {@code onYoloChange} (see {@link ToolbarView}).
     */
    public void setYolo(boolean enabled, EditorArea editorArea) {
        if (yoloMode == enabled || editorArea == null) {
            return;
        }
        yoloMode = enabled;
        editorArea.setYoloMode(enabled);
    }

    public boolean isSoundEnabled() {
        return soundVolume > 0;
    }

    public double getSoundVolume() {
        return soundVolume;
    }

    /**
     * The single entry point for the keystroke level, used by {@code View >
     * Sound}. The level is the setting: 0.0 is complete silence, and the
     * toolbar's On/Off dropdown is a view over the same value rather than a
     * second flag. A repeat request for the level already in force is ignored.
     */
    public void setSoundVolume(double volume) {
        var clamped = Math.max(0.0, Math.min(1.0, volume));
        if (soundVolume == clamped) {
            return;
        }
        soundVolume = clamped;
        if (clamped > 0) {
            lastAudibleVolume = clamped;
        }
        typewriterSound.setVolume(clamped);
        typewriterSound.setEnabled(clamped > 0);
        settings.setSoundVolume(clamped);
    }

    /**
     * The On/Off entry point used by the toolbar dropdown: Off writes 0.0,
     * On restores the last audible level (the default level if there never was
     * one). Delegates to {@link #setSoundVolume} so both entry points persist
     * and reach the engine through one path.
     */
    public void setSound(boolean enabled) {
        setSoundVolume(enabled ? lastAudibleVolume : 0.0);
    }
}
