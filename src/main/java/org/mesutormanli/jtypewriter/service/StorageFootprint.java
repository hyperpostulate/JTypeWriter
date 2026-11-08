/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

/**
 * What one saved document actually costs on disk.
 *
 * <p>Saving keeps the previous content in {@code .bak}, {@code .bak.1} and
 * {@code .bak.2} (see {@link BackupKeeper}), and the save itself needs a
 * staging copy plus the temp file {@code AtomicFiles} renames into place. So a
 * document occupies four times its size while it sits on disk and up to six
 * times while it is being written - measured, not estimated.
 * For a 50 MB document that is 300 MB of
 * transient free space, and until now nothing in the product said so: the user
 * learned it when a save failed on a full disk.</p>
 *
 * <p>The numbers are derived from {@link BackupKeeper#generations()} rather
 * than written as "4" and "6", because the pair that must never drift apart is
 * "how many rotations exist" and "what the dialog claims". Adding a fourth
 * generation would otherwise turn the notice into a lie, and the notice is only
 * shown once - so it would be a lie with no chance of being corrected.</p>
 */
public final class StorageFootprint {

    /**
     * Copies the save path holds besides the document itself: the staging copy
     * that becomes {@code .bak} and the {@code jtw-*.tmp} file that
     * {@link AtomicFiles} fsyncs before the rename.
     */
    private static final int TRANSIENT_COPIES = 2;

    private StorageFootprint() {
    }

    /** Copies of a document that stay on disk: the document plus its backups. */
    public static int persistentCopies() {
        return 1 + BackupKeeper.generations();
    }

    /** Copies present at the worst moment of a save. */
    public static int peakCopies() {
        return persistentCopies() + TRANSIENT_COPIES;
    }
}
