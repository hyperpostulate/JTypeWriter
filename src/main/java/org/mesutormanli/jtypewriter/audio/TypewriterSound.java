package org.mesutormanli.jtypewriter.audio;

import javafx.scene.media.AudioClip;
import org.springframework.stereotype.Component;

@Component
public class TypewriterSound {

    private AudioClip clip;
    private boolean enabled = true;

    public TypewriterSound() {
        var resource = getClass().getResource("/audio/keystroke.wav");
        if (resource != null) {
            clip = new AudioClip(resource.toExternalForm());
            clip.setVolume(0.5);
        }
    }

    public void play() {
        if (enabled && clip != null) {
            clip.stop();
            clip.play();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
