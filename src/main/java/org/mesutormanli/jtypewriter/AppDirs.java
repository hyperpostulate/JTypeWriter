/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import java.nio.file.Path;

/**
 * Well-known per-user directories for drafts, backups metadata and logs.
 * Kept in one place so tests and features agree on the layout.
 *
 * <p>The home directory is read through {@link #homeDir()} because
 * {@code user.home} is not guaranteed: a sandboxed launcher can leave it unset
 * or blank, and {@code Path.of(null, ...)} would then throw a
 * {@link NullPointerException} during startup - failing the instance lock,
 * settings, drafts and logs all at once. Falling back to the system temp
 * directory keeps the application usable; the data there is not persistent,
 * which is visible to the user from the path itself. {@code AppLogging} and
 * {@code RecoveryStore} already degrade on the same kind of failure rather
 * than crashing, so this is the same policy applied to the layout.</p>
 */
public final class AppDirs {

    private AppDirs() {
    }

    public static Path dataDir() {
        return homeDir().resolve(".jtypewriter");
    }

    /**
     * @return the user's home, or the temp directory when {@code user.home} is
     *         unset or blank; an absolute path either way so the layout stays
     *         well-defined even for a relative {@code java.io.tmpdir}
     */
    private static Path homeDir() {
        var home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return Path.of(System.getProperty("java.io.tmpdir", ".")).toAbsolutePath();
        }
        return Path.of(home);
    }

    /** Crash-recovery drafts (see {@code RecoveryStore}). */
    public static Path recoveryDir() {
        return dataDir().resolve("recovery");
    }

    /** Rotating application logs (see {@code AppLogging}). */
    public static Path logsDir() {
        return dataDir().resolve("logs");
    }
}