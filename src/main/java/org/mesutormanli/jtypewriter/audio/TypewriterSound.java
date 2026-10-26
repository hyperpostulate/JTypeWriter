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
    private final double volume;

    private AudioClip clip;
    private boolean clipLoadAttempted;
    private boolean enabled = true;

    public TypewriterSound() {
        this(RESOURCE, 0.5);
    }

    /** Visible for tests (fake resource path). */
    TypewriterSound(String resourcePath, double volume) {
        this.resourcePath = resourcePath;
        this.volume = volume;
    }

    public void play() {
        if (!enabled) {
            return;
        }
        var loaded = clip();
        if (loaded == null) {
            return;
        }
        loaded.stop();
        loaded.play();
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