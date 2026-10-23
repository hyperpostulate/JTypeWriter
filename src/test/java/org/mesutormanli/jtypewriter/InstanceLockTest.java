package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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

        var lock = InstanceLock.acquire(dataDir);
        try {
            assertTrue(lock.isPresent(), "a fresh directory must be lockable");
            assertTrue(Files.isDirectory(dataDir), "the data directory must be created");
            assertTrue(Files.isRegularFile(dataDir.resolve("instance.lock")),
                    "the lock file must live exactly in the passed directory");
        } finally {
            lock.ifPresent(InstanceLock::close);
        }
    }

    @Test
    void secondAcquireInSameJvmReturnsEmpty() throws Exception {
        var first = InstanceLock.acquire(dir);
        try {
            assertTrue(first.isPresent());
            assertTrue(InstanceLock.acquire(dir).isEmpty(),
                    "a second acquire in the same JVM must be rejected");
        } finally {
            first.ifPresent(InstanceLock::close);
        }
    }

    @Test
    void closeReleasesTheLockAndIsIdempotent() throws Exception {
        var first = InstanceLock.acquire(dir);
        assertTrue(first.isPresent());
        first.get().close();
        first.get().close();

        var second = InstanceLock.acquire(dir);
        try {
            assertTrue(second.isPresent(), "the lock must be acquirable again after close");
        } finally {
            second.ifPresent(InstanceLock::close);
        }
    }

    @Test
    void directoryInPlaceOfLockFileFailsWithoutThrowing() throws Exception {
        Files.createDirectories(dir.resolve("instance.lock"));

        assertTrue(InstanceLock.acquire(dir).isEmpty(),
                "an unusable lock file must fail open instead of throwing");
    }
}
