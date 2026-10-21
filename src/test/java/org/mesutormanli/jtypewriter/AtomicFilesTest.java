package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The durable write primitive behind every save path: content must land intact,
 * overwrites must not leave temp files behind, and a failed write must clean up
 * after itself instead of littering the user's document directory.
 */
class AtomicFilesTest {

    @Test
    void writesTheContentInOnePiece(@TempDir Path dir) throws IOException {
        var target = dir.resolve("note.txt");

        AtomicFiles.writeMoving(target, "merhaba dünya".getBytes(StandardCharsets.UTF_8));

        assertEquals("merhaba dünya", Files.readString(target));
    }

    @Test
    void overwriteLeavesNoTempFilesBehind(@TempDir Path dir) throws IOException {
        var target = dir.resolve("note.txt");
        AtomicFiles.writeMoving(target, "v1".getBytes(StandardCharsets.UTF_8));

        AtomicFiles.writeMoving(target, "v2".getBytes(StandardCharsets.UTF_8));

        assertEquals("v2", Files.readString(target));
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void failedWritesCleanUpTheirTempFile(@TempDir Path dir) throws IOException {
        // The target is an existing directory: the temp file is created and
        // written successfully, and only the move fails - which is the path
        // that exercises the finally cleanup branch.
        var target = dir.resolve("occupied");
        Files.createDirectory(target);

        assertThrows(IOException.class,
                () -> AtomicFiles.writeMoving(target, new byte[]{1, 2, 3}));

        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "no temp files after a failed write");
        }
    }

    /**
     * Regression: {@code createTempFile} rejects a prefix shorter than three
     * characters, so a document named "a" could not be saved.
     */
    @Test
    void shortFileNamesSaveCorrectly(@TempDir Path dir) throws IOException {
        var target = dir.resolve("a");

        AtomicFiles.writeMoving(target, "short".getBytes(StandardCharsets.UTF_8));

        assertEquals("short", Files.readString(target));
    }

    @Test
    void writesEmptyDocumentsToo(@TempDir Path dir) throws IOException {
        var target = dir.resolve("empty.txt");

        AtomicFiles.writeMoving(target, new byte[0]);

        assertEquals(0, Files.size(target));
    }

    /**
     * Regression: the temp file the JVM creates is owner-only, and the rename
     * used to make the document inherit those tighter permissions. Saving an
     * existing file must never change who may read it.
     */
    @Test
    void overwritePreservesTheExistingFilePermissions(@TempDir Path dir) throws IOException {
        Assumptions.assumeTrue(
                FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permissions unavailable on this platform");
        var target = dir.resolve("shared.txt");
        Files.writeString(target, "v1");
        var permissions = PosixFilePermissions.fromString("rw-r--r--");
        Files.setPosixFilePermissions(target, permissions);

        AtomicFiles.writeMoving(target, "v2".getBytes(StandardCharsets.UTF_8));

        assertEquals(permissions, Files.getPosixFilePermissions(target));
        assertEquals("v2", Files.readString(target));
    }
}