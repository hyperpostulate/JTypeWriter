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
import java.util.UUID;
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
 * by a regular file. A path that does not exist yet - a brand-new document -
 * cannot be resolved and is written as named. A <em>dangling</em> link is
 * different: writing "as named" would replace the link with a regular file and
 * orphan the document the user meant to save, so such a save is refused.</p>
 */
public final class AtomicFiles {

    private static final Logger LOG = AppLogging.logger(AtomicFiles.class);

    /**
     * Upper bound on the temp-file prefix in UTF-8 bytes. {@code createTempFile}
     * adds a random suffix and the caller's {@code .tmp}, and the platform's
     * {@code NAME_MAX} is 255 bytes on both ext4 and NTFS; 180 leaves ample
     * headroom for the random part while keeping ordinary names readable.
     */
    static final int MAX_TEMP_PREFIX_BYTES = 180;

    private AtomicFiles() {
    }

    /**
     * Builds the temp-file prefix for {@code file}.
     *
     * <p>Ordinary names keep the readable {@code jtw-<name>-} form. A long name
     * is truncated to fit {@link #MAX_TEMP_PREFIX_BYTES} and a short hash of the
     * full name is appended, so two long names in the same directory cannot
     * collide on the same prefix. Truncation walks code points, never UTF-16
     * units, so a surrogate pair is never cut in half.</p>
     */
    static String tempPrefix(Path file) {
        var name = file.getFileName().toString();
        var readable = "jtw-" + name + "-";
        if (utf8Length(readable) <= MAX_TEMP_PREFIX_BYTES) {
            return readable;
        }
        var hashSuffix = "-" + Integer.toHexString(name.hashCode()) + "-";
        var budget = MAX_TEMP_PREFIX_BYTES - utf8Length("jtw-") - utf8Length(hashSuffix);
        return "jtw-" + truncateToUtf8Bytes(name, budget) + hashSuffix;
    }

    private static String truncateToUtf8Bytes(String value, int maxBytes) {
        var truncated = new StringBuilder();
        int used = 0;
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            int bytes = utf8Length(codePoint);
            if (used + bytes > maxBytes) {
                break;
            }
            truncated.appendCodePoint(codePoint);
            used += bytes;
            index += Character.charCount(codePoint);
        }
        return truncated.toString();
    }

    private static int utf8Length(String value) {
        int bytes = 0;
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            bytes += utf8Length(codePoint);
            index += Character.charCount(codePoint);
        }
        return bytes;
    }

    private static int utf8Length(int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        }
        if (codePoint <= 0x7FF) {
            return 2;
        }
        if (codePoint <= 0xFFFF) {
            return 3;
        }
        return 4;
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
        // document called "a" could not be saved at all without the fixed prefix.
        // tempPrefix also bounds the name so the full prefix stays under NAME_MAX.
        var temp = Files.createTempFile(dir, tempPrefix(absolute), ".tmp");
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
     *
     * <p>A dangling symlink is not a new document: returning its absolute path
     * would let the rename replace the link with a regular file, so the save is
     * refused with an explanatory {@link IOException}. The caller reports a
     * failed save and keeps the document dirty, which is far better than
     * silently orphaning the file the link pointed at.</p>
     */
    private static Path resolveRealTarget(Path target) throws IOException {
        try {
            return target.toRealPath();
        } catch (IOException missingOrUnresolvable) {
            if (Files.isSymbolicLink(target)) {
                throw new IOException("symbolic link " + target
                        + " points at a missing file; refusing to replace the link",
                        missingOrUnresolvable);
            }
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
     *
     * <p>A brand-new document has no target to copy from, but the JVM's temp
     * file is owner-only ({@code 0600}), which silently ignores the process
     * umask. It is therefore replaced by the permissions a normally created file
     * would get: a probe file created with {@link Files#createFile} in the same
     * directory (no explicit attributes, so the umask applies) supplies them.
     * Guessing {@code 0666 & ~umask} from {@code PosixFilePermissions} constants
     * is deliberately not done - the umask is a process property, not a
     * platform constant.</p>
     */
    private static void preserveAttributes(Path target, Path temp) {
        if (!Files.exists(target)) {
            applyCreatePermissions(temp);
            return;
        }
        try {
            var source = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            var destination = Files.getFileAttributeView(temp, PosixFileAttributeView.class);
            if (source != null && destination != null) {
                var attributes = source.readAttributes();
                try {
                    destination.setPermissions(attributes.permissions());
                } catch (IOException | UnsupportedOperationException e) {
                    LOG.log(Level.FINE, "could not copy POSIX permissions of " + target, e);
                }
                // Ownership is copied separately and best-effort: an unprivileged
                // process cannot chown, and that failure must not cost the save.
                // In the common case temp and target already share an owner, so
                // this is a no-op; it matters for a file owned by another user in
                // a directory the caller may write to.
                try {
                    destination.setOwner(attributes.owner());
                    destination.setGroup(attributes.group());
                } catch (IOException | UnsupportedOperationException | SecurityException e) {
                    LOG.log(Level.FINE, "could not copy POSIX ownership of " + target, e);
                }
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

    /**
     * Gives a brand-new document the permissions a normally created file would
     * get under the process umask. {@link Files#createFile(Path, java.nio.file.attribute.FileAttribute...)}
     * with no attributes asks the operating system for the default mode
     * ({@code 0666 & ~umask}), which is exactly the question being answered.
     *
     * <p>The probe is a sibling of the temp file so it lives on the same file
     * system, and it is removed in a {@code finally}. Every failure is logged at
     * {@code FINE} and swallowed: an unavailable POSIX view or a probe that
     * cannot be created must never fail the save, it only leaves the temp file's
     * owner-only default in place.</p>
     */
    private static void applyCreatePermissions(Path temp) {
        var dir = temp.getParent();
        if (dir == null) {
            return;
        }
        Path probe = null;
        try {
            var destination = Files.getFileAttributeView(temp, PosixFileAttributeView.class);
            if (destination == null) {
                LOG.log(Level.FINE, "POSIX view unavailable for " + temp
                        + "; keeping the temp file's default permissions");
                return;
            }
            probe = Files.createFile(dir.resolve(".jtw-umask-" + UUID.randomUUID()));
            destination.setPermissions(Files.getPosixFilePermissions(probe));
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LOG.log(Level.FINE, "could not apply the umask to " + temp, e);
        } finally {
            if (probe != null) {
                try {
                    Files.deleteIfExists(probe);
                } catch (IOException e) {
                    LOG.log(Level.FINE, "could not delete umask probe " + probe, e);
                }
            }
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