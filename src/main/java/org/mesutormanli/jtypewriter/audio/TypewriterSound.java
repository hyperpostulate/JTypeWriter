/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.audio;

import javafx.scene.media.AudioClip;
import org.mesutormanli.jtypewriter.AppLogging;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Typewriter keystroke sound.
 *
 * <p>Loading is deliberately defensive, for two reasons:</p>
 * <ul>
 *   <li>the sample is extracted to a temp file first - {@code javafx.media}
 *       needs a seekable file source and chokes on classpath URLs such as
 *       {@code jar:file:...!/...} or {@code jar:nested:...};</li>
 *   <li>any failure (missing resource, no audio device, broken media toolkit)
 *       degrades to silence instead of breaking application startup.</li>
 * </ul>
 * The clip is loaded lazily on first keystroke so construction never touches
 * the media toolkit.
 */
public class TypewriterSound {

    private static final Logger LOG = AppLogging.logger(TypewriterSound.class);
    private static final String RESOURCE = "/audio/keystroke.wav";

    private final String resourcePath;

    private double volume;
    private AudioClip clip;
    private boolean clipLoadAttempted;
    private boolean enabled = true;

    public TypewriterSound() {
        this(RESOURCE, SoundLevel.MEDIUM.volume());
    }

    /** Visible for tests (fake resource path). */
    TypewriterSound(String resourcePath, double volume) {
        this.resourcePath = resourcePath;
        this.volume = volume;
    }

    /**
     * Plays one click.
     *
     * <p><strong>No {@code stop()} before {@code play()}.</strong>
     * {@link AudioClip} is explicitly designed to be played several times
     * simultaneously ("once one of the play methods is called the only
     * operable control is {@link AudioClip#stop()}"), and each call starts its
     * own voice. The old {@code stop(); play();} cut the previous click
     * mid-sample, which at typing speed turned the sound into a stutter
     * instead of a clack. Letting the voices overlap makes fast typing sound
     * like fast typing.</p>
     */
    public void play() {
        if (!enabled || volume <= 0) {
            return;
        }
        var loaded = clip();
        if (loaded == null) {
            return;
        }
        loaded.play();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** The volume used by the next (and, on a loaded clip, the current) playback. */
    public double getVolume() {
        return volume;
    }

    /**
     * Applies a new level to a clip that is already loaded; the value is also
     * remembered for the lazy load, so setting it before the first keystroke
     * costs no media-toolkit work.
     */
    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        if (clip != null) {
            clip.setVolume(this.volume);
        }
    }

    public void toggle() {
        enabled = !enabled;
    }

    private synchronized AudioClip clip() {
        if (!clipLoadAttempted) {
            clip = loadClip();
            clipLoadAttempted = true;
        }
        return clip;
    }

    private AudioClip loadClip() {
        try {
            var file = extractResource();
            if (file == null) {
                return null;
            }
            var loaded = new AudioClip(file.toUri().toString());
            loaded.setVolume(volume);
            return loaded;
        } catch (Throwable error) { // audio must never keep anyone from writing
            LOG.log(Level.WARNING, "typewriter sound disabled", error);
            return null;
        }
    }

    private Path extractResource() throws IOException {
        try (InputStream in = TypewriterSound.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                LOG.log(Level.INFO, "{0} missing; typewriter sound disabled", resourcePath);
                return null;
            }
            var temp = Files.createTempFile("jtypewriter-keystroke", ".wav");
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }
}