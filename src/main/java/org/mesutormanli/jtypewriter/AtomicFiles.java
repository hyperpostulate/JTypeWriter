/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Durable "write a small file" primitive shared by document saves, recovery
 * drafts and settings. The contract is stronger than a plain write:
 * <ol>
 *   <li>content goes to a temp file in the target directory;</li>
 *   <li>the temp file is <em>fsync</em>ed, so the bytes reach the disk before
 *       anything points at them (a power cut cannot leave a renamed but empty
 *       file - the classic rename-is-atomic-but-not-durable trap);</li>
 *   <li>it is moved into place with an atomic rename, so a crash mid-write can
 *       never leave a truncated target;</li>
 *   <li>the directory entry is flushed best-effort afterwards.</li>
 * </ol>
 * Failures of the directory fsync are logged and swallowed: it is unavailable
 * on some file systems (notably on Windows), and the data fsync plus the atomic
 * rename already cover the important cases.
 *
 * <p>The temp file is created by the JVM (owner-only on POSIX) and then renamed
 * over the target, so without care the move would silently tighten the
 * permissions of an existing document to {@code 0600}. Before the rename the
 * target's permission model is therefore copied onto the temp file (POSIX
 * permissions, DOS attributes or an ACL, whichever the file system supports),
 * so saving a file never changes who may read it.</p>
 *
 * <p>A target reached through a symbolic link is resolved with
 * {@link Path#toRealPath()} first: the temp file and the atomic rename then act
 * on the real document, so the link survives the save instead of being replaced
 * by a regular file. A path that does not exist yet - a
 * brand-new document, or a dangling link - cannot be resolved and is written as
 * named.</p>
 */
public final class AtomicFiles {

    private static final Logger LOG = AppLogging.logger(AtomicFiles.class);

    private AtomicFiles() {
    }

    /** Writes {@code bytes} to {@code target} atomically and durably. */
    public static void writeMoving(Path target, byte[] bytes) throws IOException {
        writeMoving(target, bytes, -1);
    }

    /**
     * The implementation behind {@link #writeMoving(Path, byte[])}.
     *
     * @param usableSpace pre-measured free space on the target's volume, or a
     *        negative value to measure it from the file store. The overload is
     *        package-private so a test can simulate a full disk without filling
     *        one: the pre-check must fail <em>before</em> any temp file exists.
     */
    static void writeMoving(Path target, byte[] bytes, long usableSpace) throws IOException {
        var absolute = resolveRealTarget(target);
        var dir = absolute.getParent();
        requireFreeSpace(dir, bytes.length, usableSpace);
        // createTempFile rejects prefixes shorter than three characters, so a
        // document called "a" could not be saved at all without the fixed prefix
        var temp = Files.createTempFile(dir, "jtw-" + absolute.getFileName(), ".tmp");
        var moved = false;
        try {
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                // FileChannel.write may perform a partial write; loop until every byte is out
                var buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true); // bytes on disk before the rename points at them
            }
            preserveAttributes(absolute, temp);
            try {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
            syncDirectory(dir);
        } finally {
            if (!moved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    /**
     * Follows symbolic links to the document they point at, so the atomic rename
     * replaces the real file and the link stays a link. A missing target has no
     * real path; the named path is used so a new document is still created.
     */
    private static Path resolveRealTarget(Path target) {
        try {
            return target.toRealPath();
        } catch (IOException missingOrUnresolvable) {
            LOG.log(Level.FINE, "no real path for " + target + "; writing as named",
                    missingOrUnresolvable);
            return target.toAbsolutePath();
        }
    }

    /**
     * Fails the save before the new document's temp file is created when the
     * volume obviously cannot hold it. Without this check a full disk is only
     * discovered halfway through the write, after the user has already waited
     * for the backup staging copy.
     *
     * <p>A negative {@code usableSpace} means "measure it"; an unmeasurable
     * file store (network shares, exotic providers) must never block a save, so
     * the check is skipped instead of guessed.</p>
     */
    private static void requireFreeSpace(Path dir, long requiredBytes, long usableSpace)
            throws IOException {
        long usable = usableSpace >= 0 ? usableSpace : usableSpace(dir);
        if (usable < 0) {
            return; // unknown: let the write itself decide
        }
        if (requiredBytes > usable) {
            throw new IOException("not enough free disk space: " + requiredBytes
                    + " bytes needed, " + usable + " available");
        }
    }

    /**
     * @return usable bytes, or {@code -1} when the file store cannot report
     *         them. A total size of zero means the provider does not know the
     *         volume at all; a zero <em>usable</em> size on a known volume is a
     *         genuinely full disk and must stay distinguishable from "unknown".
     */
    private static long usableSpace(Path dir) {
        if (dir == null) {
            return -1;
        }
        try {
            var store = Files.getFileStore(dir);
            return store.getTotalSpace() > 0 ? store.getUsableSpace() : -1;
        } catch (IOException e) {
            LOG.log(Level.FINE, "cannot read free space of " + dir, e);
            return -1;
        }
    }

    /**
     * Copies the target's permission model onto the freshly written temp file so
     * the atomic rename cannot change the document's access rights. Best-effort:
     * an unsupported or unreadable attribute view is logged and skipped - a
     * missing permission copy must never cost the user the save itself.
     */
    private static void preserveAttributes(Path target, Path temp) {
        if (!Files.exists(target)) {
            return; // a brand new file keeps the temp file's defaults (owner-only)
        }
        try {
            var source = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            var destination = Files.getFileAttributeView(temp, PosixFileAttributeView.class);
            if (source != null && destination != null) {
                destination.setPermissions(source.readAttributes().permissions());
                return; // a POSIX file system owns the permission model
            }
        } catch (IOException | UnsupportedOperationException e) {
            LOG.log(Level.FINE, "could not copy POSIX permissions of " + target, e);
        }
        try {
            var source = Files.getFileAttributeView(target, DosFileAttributeView.class);
            var destination = Files.getFileAttributeView(temp, DosFileAttributeView.class);
            if (source != null && destination != null) {
                var attributes = source.readAttributes();
                destination.setArchive(attributes.isArchive());
                destination.setHidden(attributes.isHidden());
                destination.setReadOnly(attributes.isReadOnly());
                destination.setSystem(attributes.isSystem());
            }
        } catch (IOException | UnsupportedOperationException e) {
            LOG.log(Level.FINE, "could not copy DOS attributes of " + target, e);
        }
        try {
            var source = Files.getFileAttributeView(target, AclFileAttributeView.class);
            var destination = Files.getFileAttributeView(temp, AclFileAttributeView.class);
            if (source != null && destination != null) {
                destination.setAcl(source.getAcl());
            }
        } catch (IOException | UnsupportedOperationException e) {
            LOG.log(Level.FINE, "could not copy ACL of " + target, e);
        }
    }

    /** Best-effort flush of the rename itself (unsupported on some platforms). */
    private static void syncDirectory(Path dir) {
        if (dir == null) {
            return;
        }
        try (var channel = FileChannel.open(dir, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException e) {
            LOG.log(Level.FINE, "directory fsync unavailable for " + dir, e);
        }
    }
}