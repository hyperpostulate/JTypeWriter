package org.mesutormanli.jtypewriter.ui.component;

import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.springframework.stereotype.Component;

@Component
public class ToolbarState {

    private final TypewriterSound typewriterSound;

    private boolean yoloMode;
    private boolean soundEnabled = true;

    public ToolbarState(TypewriterSound typewriterSound) {
        this.typewriterSound = typewriterSound;
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
    }
}
