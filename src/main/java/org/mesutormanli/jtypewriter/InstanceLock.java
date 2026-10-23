package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Prevents two JTypeWriter instances from running for the same OS user at once.
 *
 * <p>The lock is an advisory file lock on {@code <dataDir>/instance.lock}. It is
 * therefore <b>per OS user account</b> (the data directory lives in the user's
 * home), the operating system releases it automatically when the owning process
 * dies, and the lock file itself is left behind harmlessly - it carries no
 * content and is reused on the next start.</p>
 *
 * <p><b>Fail open:</b> if the lock cannot be created at all (read-only data
 * directory, a directory squatting on the lock file name, no permission), the
 * application starts anyway rather than locking the user out forever. The
 * failure is reported as a single line on {@code System.err}; {@link AppLogging}
 * is deliberately not used here because its logger would initialise the log
 * directory before the lock check has even run.</p>
 */
public final class InstanceLock implements AutoCloseable {

    private static final String LOCK_FILE_NAME = "instance.lock";

    private final FileChannel channel;
    private final FileLock lock;

    private InstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * Tries to take the instance lock for {@code dataDir}, creating the
     * directory if needed.
     *
     * @return the held lock, or empty when another instance already owns it or
     *         the lock file cannot be used (fail open, see class javadoc)
     */
    public static Optional<InstanceLock> acquire(Path dataDir) {
        Path lockFile = dataDir.resolve(LOCK_FILE_NAME);
        FileChannel channel = null;
        try {
            Files.createDirectories(dataDir);
            channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock == null) {
                closeQuietly(channel);
                reportFailure(lockFile, "already held by another instance");
                return Optional.empty();
            }
            return Optional.of(new InstanceLock(channel, lock));
        } catch (OverlappingFileLockException e) {
            closeQuietly(channel);
            reportFailure(lockFile, "already held in this JVM");
            return Optional.empty();
        } catch (IOException | SecurityException e) {
            closeQuietly(channel);
            reportFailure(lockFile, e.toString());
            return Optional.empty();
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
        } catch (IOException ignored) {
            // nothing useful to do; the handle is gone with the process anyway
        }
    }

    private static void reportFailure(Path lockFile, String reason) {
        System.err.println("JTypeWriter: could not acquire the instance lock "
                + lockFile + " (" + reason + ")");
    }

    /** Releases the lock and closes the channel; safe to call more than once. */
    @Override
    public void close() {
        try {
            if (lock.isValid()) {
                lock.release();
            }
        } catch (IOException ignored) {
            // closing the channel below releases it as well
        }
        closeQuietly(channel);
    }
}
