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
 * <p>{@link #acquire(Path)} reports three distinct states via {@link Result}:</p>
 * <ul>
 *   <li>{@link Status#ACQUIRED} - this process owns the lock;</li>
 *   <li>{@link Status#ALREADY_RUNNING} - another instance owns the lock; only
 *       this state may block startup;</li>
 *   <li>{@link Status#UNAVAILABLE} - the lock cannot be used at all (read-only
 *       data directory, a directory squatting on the lock file name, missing
 *       permission, ...). The application continues without the lock rather
 *       than locking the user out forever, and the reason is reported as a
 *       single line on {@code System.err}.</li>
 * </ul>
 *
 * <p>{@link AppLogging} is deliberately not used here because its logger would
 * initialise the log directory before the lock check has even run.</p>
 */
public final class InstanceLock implements AutoCloseable {

    private static final String LOCK_FILE_NAME = "instance.lock";

    /** Outcome of an attempt to take the instance lock. */
    public enum Status {
        /** This process holds the lock. */
        ACQUIRED,
        /** Another process (or another lock in this JVM) holds the lock. */
        ALREADY_RUNNING,
        /** The lock file cannot be used; the application starts without it. */
        UNAVAILABLE
    }

    /**
     * Result of {@link #acquire(Path)}. {@code held} is present exactly for
     * {@link Status#ACQUIRED}.
     */
    public record Result(Status status, Optional<InstanceLock> held) {

        public Result {
            held = held == null ? Optional.empty() : held;
        }

        static Result acquired(InstanceLock lock) {
            return new Result(Status.ACQUIRED, Optional.of(lock));
        }

        static Result alreadyRunning() {
            return new Result(Status.ALREADY_RUNNING, Optional.empty());
        }

        static Result unavailable() {
            return new Result(Status.UNAVAILABLE, Optional.empty());
        }
    }

    private final FileChannel channel;
    private final FileLock lock;

    private InstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * Tries to take the instance lock for {@code dataDir}, creating the
     * directory if needed. Never throws.
     *
     * @return the three-state outcome; see the class javadoc
     */
    public static Result acquire(Path dataDir) {
        Path lockFile = dataDir.resolve(LOCK_FILE_NAME);
        FileChannel channel = null;
        try {
            Files.createDirectories(dataDir);
            channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock == null) {
                closeQuietly(channel);
                return Result.alreadyRunning();
            }
            return Result.acquired(new InstanceLock(channel, lock));
        } catch (OverlappingFileLockException e) {
            closeQuietly(channel);
            return Result.alreadyRunning();
        } catch (IOException | SecurityException e) {
            closeQuietly(channel);
            reportUnavailable(lockFile, e);
            return Result.unavailable();
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

    private static void reportUnavailable(Path lockFile, Exception reason) {
        System.err.println("JTypeWriter: the instance lock " + lockFile
                + " is unavailable (" + reason + "); continuing without it");
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
