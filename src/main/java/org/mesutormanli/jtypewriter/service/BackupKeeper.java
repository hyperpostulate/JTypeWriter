/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Rotating file backups protecting against corrupt overwrites: before a target
 * file is replaced, its previous content is copied to {@code <file>.bak}, the
 * older {@code <file>.bak.<n>} generations shift up and the oldest is dropped.
 * Free of UI types so it can be unit tested headlessly.
 *
 * <p>Copying and rotating are split into two phases so a caller that writes the
 * document itself can order the operations safely:</p>
 * <ol>
 *   <li>{@link #prepare(Path)} copies the current content to a staging file
 *       next to the target, without touching the backup history;</li>
 *   <li>{@link #install(PreparedBackup)} rotates the generations and promotes
 *       the staging file to {@code .bak} - only possible once the copy is
 *       known to have succeeded.</li>
 * </ol>
 * <p>A failure while staging therefore cannot delete the oldest generation,
 * unlike the previous copy-after-rotate order. {@link #discard(PreparedBackup)}
 * cleans up a prepared-but-unused copy.</p>
 */
public final class BackupKeeper {

    private static final int GENERATIONS = 3;

    /** A staged copy of a document's previous content, ready to become {@code .bak}. */
    public record PreparedBackup(Path target, Path staged) {
    }

    /**
     * Stages a copy of {@code target}'s current content without rotating any
     * generation. Empty when {@code target} is not a regular file (nothing to
     * back up yet).
     *
     * @throws IOException when the copy fails; the partial staging file is
     *         removed first and the existing generations stay untouched
     */
    public Optional<PreparedBackup> prepare(Path target) throws IOException {
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        var staged = stagingPath(target);
        try {
            Files.copy(target, staged,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return Optional.of(new PreparedBackup(target, staged));
        } catch (IOException e) {
            deleteQuietly(staged);
            throw e;
        }
    }

    /**
     * Rotates the generations and promotes {@code backup.staged()} to
     * {@code .bak}. Must only be called after {@link #prepare(Path)} succeeded,
     * so the oldest generation is dropped only when a fresh copy exists.
     */
    public void install(PreparedBackup backup) throws IOException {
        for (int i = GENERATIONS - 1; i >= 1; i--) {
            var older = backupPath(backup.target(), i);
            var newer = backupPath(backup.target(), i - 1);
            if (Files.isRegularFile(newer)) {
                Files.move(newer, older, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.move(backup.staged(), backupPath(backup.target(), 0),
                StandardCopyOption.REPLACE_EXISTING);
    }

    /** Deletes a staged copy that will not become the new {@code .bak}. */
    public void discard(PreparedBackup backup) {
        deleteQuietly(backup.staged());
    }

    /**
     * Copies {@code target} to {@code <target>.bak} and rotates the older
     * generations; no-op when the target does not exist.
     *
     * <p>Equivalent to {@link #prepare(Path)} followed by
     * {@link #install(PreparedBackup)}: the history is only rotated once the
     * copy has been made.</p>
     */
    public void backup(Path target) throws IOException {
        var prepared = prepare(target);
        if (prepared.isEmpty()) {
            return;
        }
        try {
            install(prepared.get());
        } catch (IOException e) {
            discard(prepared.get());
            throw e;
        }
    }

    /** @param generation 0 is the newest backup ({@code .bak}), 1 the previous one, ... */
    public static Path backupPath(Path target, int generation) {
        var name = target.getFileName().toString();
        return target.resolveSibling(generation == 0 ? name + ".bak" : name + ".bak." + generation);
    }

    /** Sibling staging file used while the new backup is being copied. */
    static Path stagingPath(Path target) {
        return target.resolveSibling(target.getFileName().toString() + ".bak.new");
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Only a leftover staging artefact: never let cleanup mask the real failure.
        }
    }
}
