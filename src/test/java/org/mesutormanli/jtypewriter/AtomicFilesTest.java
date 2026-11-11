/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

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
     * Disk-full simulation without filling a disk: the pre-check must reject
     * the write before a temp file exists, and the target must keep its old
     * content so the caller can report a failed save instead of a success.
     */
    @Test
    void aFullDiskFailsBeforeAnyTempFileIsCreated(@TempDir Path dir) throws IOException {
        var target = dir.resolve("note.txt");
        Files.writeString(target, "keep me");

        var failure = assertThrows(IOException.class,
                () -> AtomicFiles.writeMoving(target,
                        "new text".getBytes(StandardCharsets.UTF_8), 0));

        assertTrue(failure.getMessage().contains("free disk space"));
        assertEquals("keep me", Files.readString(target));
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "no temp file may be created when the pre-check fails");
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
     * A document opened through a symbolic link must still be a symbolic link
     * after saving, and the file it points at must receive the new content
     * Replacing the link with a regular file would break
     * every other name and sync tool pointing at the real document.
     */
    @Test
    void savingThroughASymbolicLinkKeepsTheLinkAndUpdatesItsTarget(@TempDir Path dir) throws IOException {
        var realDir = Files.createDirectory(dir.resolve("real"));
        var linkDir = Files.createDirectory(dir.resolve("link"));
        var real = realDir.resolve("notes.txt");
        Files.writeString(real, "v1");
        var link = linkDir.resolve("notes.txt");
        try {
            Files.createSymbolicLink(link, real);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.abort("symbolic links unavailable: " + e.getMessage());
        }

        AtomicFiles.writeMoving(link, "v2".getBytes(StandardCharsets.UTF_8));

        assertTrue(Files.isSymbolicLink(link), "the link must survive the save");
        assertEquals("v2", Files.readString(real), "the real target must get the new content");
        try (Stream<Path> files = Files.list(linkDir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "the temp file belongs next to the real target, not next to the link");
        }
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

    /**
     * Regression: resolving a dangling symlink used to fall back to "write as
     * named", which replaced the link with a regular file. The save must be
     * refused so the link - and the document it was meant to reach - survives.
     */
    @Test
    void aDanglingSymbolicLinkIsNeverReplacedByARegularFile(@TempDir Path dir) throws IOException {
        var link = dir.resolve("draft.txt");
        try {
            Files.createSymbolicLink(link, dir.resolve("missing.txt"));
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.abort("symbolic links unavailable: " + e.getMessage());
        }

        assertThrows(IOException.class,
                () -> AtomicFiles.writeMoving(link, "text".getBytes(StandardCharsets.UTF_8)));

        assertTrue(Files.isSymbolicLink(link), "the dangling link must survive a refused save");
        assertTrue(Files.notExists(link), "no regular file may be created at the link path");
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "a refused save must not leave a temp file behind");
        }
    }

    /**
     * Regression: a brand-new document inherited the JVM temp file's owner-only
     * {@code 0600} instead of the process umask. A probe created with
     * {@link Files#createFile} in the same test answers what the umask allows, so
     * the saved file must match it rather than stay owner-only.
     */
    @Test
    void aNewDocumentGetsTheUmaskPermissionsNotOwnerOnly(@TempDir Path dir) throws IOException {
        Assumptions.assumeTrue(
                FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permissions unavailable on this platform");
        var probe = Files.createFile(dir.resolve("probe"));
        var expected = Files.getPosixFilePermissions(probe);

        var target = dir.resolve("new.txt");
        AtomicFiles.writeMoving(target, "hello".getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, Files.getPosixFilePermissions(target));
    }

    /**
     * The save must not change the document's owner. An actual chown to a
     * <em>different</em> user cannot be exercised unprivileged, so this compares
     * against a probe owned by the caller: that is enough to prove the save does
     * not hand the file to somebody else or fail when it cannot chown.
     */
    @Test
    void aNewDocumentKeepsTheCreatingUsersOwnership(@TempDir Path dir) throws IOException {
        Assumptions.assumeTrue(
                FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permissions unavailable on this platform");
        var probe = Files.createFile(dir.resolve("probe"));

        var target = dir.resolve("new.txt");
        AtomicFiles.writeMoving(target, "hello".getBytes(StandardCharsets.UTF_8));

        assertEquals(Files.getOwner(probe), Files.getOwner(target));
    }

    /**
     * The umask probe and the temp file are both implementation details; a
     * successful save must leave the user's directory exactly as it found it.
     */
    @Test
    void aNewDocumentLeavesNeitherProbeNorTempFileBehind(@TempDir Path dir) throws IOException {
        Assumptions.assumeTrue(
                FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permissions unavailable on this platform");

        AtomicFiles.writeMoving(dir.resolve("new.txt"), "hello".getBytes(StandardCharsets.UTF_8));

        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().startsWith(".jtw-umask-")
                            || p.getFileName().toString().endsWith(".tmp")),
                    "no probe or temp file may survive a successful save");
        }
    }

    /**
     * Regression: the temp prefix used to embed the whole document name, so a
     * 250-character name overflowed {@code NAME_MAX} and the save failed before
     * the rename. The target itself is under 255 bytes and must save intact.
     */
    @Test
    void a250CharacterAsciiNameSavesAndReadsBack(@TempDir Path dir) throws IOException {
        var name = "a".repeat(250);
        var target = dir.resolve(name);
        var content = "uzun isimli belge";

        AtomicFiles.writeMoving(target, content.getBytes(StandardCharsets.UTF_8));

        assertEquals(content, Files.readString(target));
    }

    /**
     * A long name with 2- and 3-byte UTF-8 characters exceeds the prefix budget
     * in bytes long before it exceeds it in characters; the save must still
     * work. The name here is capped at 249 bytes so the target itself is a
     * legal file name, while the prefix is truncated and hashed.
     */
    @Test
    void aLongMultiByteNameSavesAndReadsBack(@TempDir Path dir) throws IOException {
        var name = "ğ中".repeat(49) + "ğ"; // 99 characters, 247 UTF-8 bytes
        var target = dir.resolve(name);
        var content = "çok baytlı isim";

        AtomicFiles.writeMoving(target, content.getBytes(StandardCharsets.UTF_8));

        assertEquals(content, Files.readString(target));
    }

    /** The temp prefix is bounded in UTF-8 bytes and keeps the readable stem. */
    @Test
    void theTempPrefixIsBoundedInUtf8Bytes(@TempDir Path dir) {
        var prefix = AtomicFiles.tempPrefix(dir.resolve("ğ中".repeat(60) + "-belge.txt"));

        assertTrue(prefix.startsWith("jtw-"), "the format is kept: " + prefix);
        assertTrue(prefix.getBytes(StandardCharsets.UTF_8).length
                        <= AtomicFiles.MAX_TEMP_PREFIX_BYTES,
                "the prefix must leave room for the random part and .tmp");
    }

    /** A long name never ends in half of a surrogate pair. */
    @Test
    void truncationNeverCutsASurrogatePair(@TempDir Path dir) {
        var prefix = AtomicFiles.tempPrefix(dir.resolve("😀".repeat(100)));

        var bytes = prefix.getBytes(StandardCharsets.UTF_8);
        assertTrue(bytes.length <= AtomicFiles.MAX_TEMP_PREFIX_BYTES);
        assertEquals(prefix, new String(bytes, StandardCharsets.UTF_8),
                "the prefix is valid UTF-8, so the surrogate pair was not split");
    }

    /** Ordinary names stay readable: the document name is visible in the prefix. */
    @Test
    void aShortNameStaysVisibleInTheTempPrefix(@TempDir Path dir) {
        var prefix = AtomicFiles.tempPrefix(dir.resolve("note.txt"));

        assertTrue(prefix.startsWith("jtw-"));
        assertTrue(prefix.contains("note.txt"), "the readable name is kept: " + prefix);
    }
}