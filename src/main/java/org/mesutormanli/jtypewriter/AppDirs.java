package org.mesutormanli.jtypewriter;

import java.nio.file.Path;

/**
 * Well-known per-user directories for drafts, backups metadata and logs.
 * Kept in one place so tests and features agree on the layout.
 */
public final class AppDirs {

    private AppDirs() {
    }

    public static Path dataDir() {
        return Path.of(System.getProperty("user.home"), ".jtypewriter");
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