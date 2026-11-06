/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract of the pre-overwrite backup rotation: a corrupted save must always
 * leave at least one earlier generation to roll back to.
 */
class BackupKeeperTest {

    private final BackupKeeper keeper = new BackupKeeper();

    @Test
    void missingTargetIsANoOp(@TempDir Path dir) throws IOException {
        var target = dir.resolve("absent.txt");

        keeper.backup(target);

        assertFalse(Files.exists(BackupKeeper.backupPath(target, 0)));
    }

    @Test
    void keepsPreviousVersionBeforeOverwrite(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v1");

        keeper.backup(target);

        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));
    }

    @Test
    void rotatesGenerationsAndDropsTheOldest(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v1");
        // mirrors FileService: back up what is on disk, then overwrite
        for (int version = 2; version <= 6; version++) {
            keeper.backup(target);
            Files.writeString(target, "v" + version);
        }

        // target holds v6, so the three retained backups are v5..v3
        assertEquals("v5", Files.readString(BackupKeeper.backupPath(target, 0)));
        assertEquals("v4", Files.readString(BackupKeeper.backupPath(target, 1)));
        assertEquals("v3", Files.readString(BackupKeeper.backupPath(target, 2)));
        assertFalse(Files.exists(BackupKeeper.backupPath(target, 3)));
    }

    /**
     * A backup taken through a symbolic link must land next to the real
     * document, not next to the link: the link is just another name for the
     * same content, and the backup belongs with the bytes it protects.
     */
    @Test
    void aBackupOfASymbolicLinkLandsNextToItsRealTarget(@TempDir Path dir) throws IOException {
        var realDir = Files.createDirectory(dir.resolve("real"));
        var linkDir = Files.createDirectory(dir.resolve("link"));
        var real = realDir.resolve("doc.txt");
        Files.writeString(real, "v1");
        var link = linkDir.resolve("doc.txt");
        try {
            Files.createSymbolicLink(link, real);
        } catch (IOException | UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("symbolic links unavailable: " + e.getMessage());
        }

        keeper.backup(link);

        assertEquals("v1", Files.readString(BackupKeeper.backupPath(real, 0)));
        assertFalse(Files.exists(BackupKeeper.backupPath(link, 0)),
                "no backup may appear next to the link");
    }

    @Test
    void successfulBackupLeavesNoStagingFileBehind(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v1");

        keeper.backup(target);

        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));
        assertFalse(Files.exists(dir.resolve("doc.txt.bak.new")));
    }

    @Test
    void prepareAndInstallOnlyRotateOnceTheCopySucceeded(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v2");
        Files.writeString(BackupKeeper.backupPath(target, 0), "v1");

        var prepared = keeper.prepare(target);

        assertTrue(prepared.isPresent());
        // staging must not touch the history yet
        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));
        assertFalse(Files.exists(BackupKeeper.backupPath(target, 1)));

        keeper.install(prepared.get());

        assertEquals("v2", Files.readString(BackupKeeper.backupPath(target, 0)));
        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 1)));
        assertFalse(Files.exists(BackupKeeper.stagingPath(target)));
    }

    @Test
    void discardedPreparationLeavesExistingGenerationsUntouched(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v2");
        Files.writeString(BackupKeeper.backupPath(target, 0), "v1");

        var prepared = keeper.prepare(target).orElseThrow();
        keeper.discard(prepared);

        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));
        assertFalse(Files.exists(BackupKeeper.backupPath(target, 1)));
        assertFalse(Files.exists(BackupKeeper.stagingPath(target)));
    }

    @Test
    void stagingFailureLeavesExistingGenerationsUntouched(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v2");
        var newest = BackupKeeper.backupPath(target, 0);
        var older = BackupKeeper.backupPath(target, 1);
        Files.writeString(newest, "v1");
        Files.writeString(older, "v0");
        // A non-empty directory at the staging path makes the copy fail; an
        // empty one would simply be replaced.
        var staging = BackupKeeper.stagingPath(target);
        Files.createDirectories(staging);
        Files.writeString(staging.resolve("blocker"), "not a file");

        assertThrows(IOException.class, () -> keeper.backup(target));

        assertEquals("v1", Files.readString(newest));
        assertEquals("v0", Files.readString(older));
        assertFalse(Files.exists(BackupKeeper.backupPath(target, 2)));
    }

    /**
     * The disk cost told to the user is derived, not typed. A dialog that says
     * "4 times" while the rotation keeps five generations is a claim the user
     * can check and find false - and this notice is shown once, so the mistake
     * can never correct itself.
     */
    @Test
    void theDiskFootprintFollowsTheRotationCount() {
        assertEquals(1 + BackupKeeper.generations(), StorageFootprint.persistentCopies(),
                "the document plus one copy per rotated generation");
    }

    /**
     * The peak adds the two transient copies a save needs: the staging file
     * that becomes {@code .bak}, and the {@code jtw-*.tmp} that
     * {@code AtomicFiles} fsyncs before renaming it into place. Four persisted
     * plus two transient is the measured six.
     */
    @Test
    void thePeakFootprintAddsTheStagingAndTempCopies() {
        assertEquals(StorageFootprint.persistentCopies() + 2, StorageFootprint.peakCopies());
        assertEquals(4, StorageFootprint.persistentCopies(),
                "3 generations plus the document itself");
        assertEquals(6, StorageFootprint.peakCopies());
    }
}
