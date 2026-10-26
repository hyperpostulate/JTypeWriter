/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Headless coverage for the per-user instance lock. None of these tests start
 * the JavaFX toolkit, and every directory comes from {@link TempDir}.
 */
class InstanceLockTest {

    @TempDir
    Path dir;

    @Test
    void acquireSucceedsInFreshDirectoryAndCreatesLockFile() throws Exception {
        Path dataDir = dir.resolve("data");

        var result = InstanceLock.acquire(dataDir);
        try {
            assertEquals(InstanceLock.Status.ACQUIRED, result.status(),
                    "a fresh directory must be lockable");
            assertTrue(result.held().isPresent(), "the acquired lock must be returned");
            assertTrue(Files.isDirectory(dataDir), "the data directory must be created");
            assertTrue(Files.isRegularFile(dataDir.resolve("instance.lock")),
                    "the lock file must live exactly in the passed directory");
        } finally {
            result.held().ifPresent(InstanceLock::close);
        }
    }

    @Test
    void secondAcquireInSameJvmIsAlreadyRunning() {
        var first = InstanceLock.acquire(dir);
        try {
            assertEquals(InstanceLock.Status.ACQUIRED, first.status());
            var second = InstanceLock.acquire(dir);
            assertEquals(InstanceLock.Status.ALREADY_RUNNING, second.status(),
                    "a second acquire in the same JVM must be reported as already running");
            assertTrue(second.held().isEmpty(), "already-running must not return a lock");
        } finally {
            first.held().ifPresent(InstanceLock::close);
        }
    }

    @Test
    void closeReleasesTheLockAndIsIdempotent() {
        var first = InstanceLock.acquire(dir);
        assertEquals(InstanceLock.Status.ACQUIRED, first.status());
        var lock = first.held().orElseThrow();
        lock.close();
        lock.close();

        var second = InstanceLock.acquire(dir);
        try {
            assertEquals(InstanceLock.Status.ACQUIRED, second.status(),
                    "the lock must be acquirable again after close");
        } finally {
            second.held().ifPresent(InstanceLock::close);
        }
    }

    @Test
    void directoryInPlaceOfLockFileIsUnavailableWithoutThrowing() throws Exception {
        Files.createDirectories(dir.resolve("instance.lock"));

        var result = InstanceLock.acquire(dir);
        assertEquals(InstanceLock.Status.UNAVAILABLE, result.status(),
                "an unusable lock file must report unavailable instead of throwing");
        assertTrue(result.held().isEmpty(), "unavailable must not return a lock");
    }

    /**
     * Read-only-directory case: the lock file cannot be created, so the
     * lock is reported unavailable and startup continues. A genuine
     * {@link SecurityException} cannot be injected any more (no
     * {@code SecurityManager} in JDK 25), so the same single catch is exercised
     * through the closest reachable I/O failure, {@code AccessDeniedException}.
     */
    @Test
    void aReadOnlyDataDirectoryIsUnavailableInsteadOfBlockingStartup(@TempDir Path dir)
            throws Exception {
        assumeTrue(Files.getFileStore(dir).supportsFileAttributeView("posix"),
                "POSIX permissions are required for a read-only directory");
        assumeFalse("root".equals(System.getProperty("user.name")),
                "root bypasses file permissions");

        Path dataDir = dir.resolve("data");
        Files.createDirectories(dataDir);
        try {
            Files.setPosixFilePermissions(dataDir, PosixFilePermissions.fromString("r-xr-xr-x"));
            var result = assertDoesNotThrow(() -> InstanceLock.acquire(dataDir),
                    "an unusable lock must never block startup by throwing");
            assertEquals(InstanceLock.Status.UNAVAILABLE, result.status(),
                    "a read-only data directory must report unavailable");
            assertTrue(result.held().isEmpty(), "unavailable must not return a lock");
        } finally {
            Files.setPosixFilePermissions(dataDir, PosixFilePermissions.fromString("rwx------"));
        }
    }
}
