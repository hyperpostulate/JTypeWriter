package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
 */
public final class AtomicFiles {

    private static final Logger LOG = AppLogging.logger(AtomicFiles.class);

    private AtomicFiles() {
    }

    /** Writes {@code bytes} to {@code target} atomically and durably. */
    public static void writeMoving(Path target, byte[] bytes) throws IOException {
        var absolute = target.toAbsolutePath();
        var dir = absolute.getParent();
        var temp = Files.createTempFile(dir, absolute.getFileName().toString(), ".tmp");
        var moved = false;
        try {
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true); // bytes on disk before the rename points at them
            }
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