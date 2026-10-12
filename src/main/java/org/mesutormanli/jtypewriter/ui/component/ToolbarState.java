package org.mesutormanli.jtypewriter.ui.component;

import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;

public class ToolbarState {

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

    public void toggleYolo(EditorArea editorArea) {
        yoloMode = !yoloMode;
        editorArea.setYoloMode(yoloMode);
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void toggleSound() {
        soundEnabled = !soundEnabled;
        typewriterSound.setEnabled(soundEnabled);
        settings.setSoundEnabled(soundEnabled);
    }
}