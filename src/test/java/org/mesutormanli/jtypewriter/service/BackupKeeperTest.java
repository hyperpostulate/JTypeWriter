package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}