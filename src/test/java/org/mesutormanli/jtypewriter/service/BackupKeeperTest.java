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
}
