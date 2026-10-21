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
 */
public final class AtomicFiles {

    private static final Logger LOG = AppLogging.logger(AtomicFiles.class);

    private AtomicFiles() {
    }

    /** Writes {@code bytes} to {@code target} atomically and durably. */
    public static void writeMoving(Path target, byte[] bytes) throws IOException {
        var absolute = target.toAbsolutePath();
        var dir = absolute.getParent();
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