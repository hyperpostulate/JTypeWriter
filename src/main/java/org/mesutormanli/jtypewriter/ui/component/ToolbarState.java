/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import org.mesutormanli.jtypewriter.AppSettings;
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
    private boolean soundEnabled;

    public ToolbarState(TypewriterSound typewriterSound, AppSettings settings) {
        this.typewriterSound = typewriterSound;
        this.settings = settings;
        this.soundEnabled = settings.isSoundEnabled();
        typewriterSound.setEnabled(soundEnabled);
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
        return soundEnabled;
    }

    /**
     * The single entry point for the keystroke sound, used by the toolbar
     * dropdown; persists exactly like the old toggle button path did. A repeat
     * request for the state already in force is ignored.
     */
    public void setSound(boolean enabled) {
        if (soundEnabled == enabled) {
            return;
        }
        soundEnabled = enabled;
        typewriterSound.setEnabled(enabled);
        settings.setSoundEnabled(enabled);
    }
}
