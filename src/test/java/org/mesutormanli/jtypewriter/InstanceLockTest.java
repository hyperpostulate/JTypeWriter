package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
