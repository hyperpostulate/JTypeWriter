package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Centralized logging setup on top of {@code java.util.logging} (zero extra
 * dependencies). Logs go to a rotating file under {@link AppDirs#logsDir()} and
 * warnings/errors additionally to stderr. Everything below the application
 * package is silenced so libraries do not flood the log.
 *
 * <p>Thrown exceptions are rendered as a full, indented stack trace on the
 * lines following the message line (including the {@code Caused by:} chain)
 * instead of only their {@code toString()}. A field error report can therefore
 * be diagnosed remotely from the log file. The message header itself stays a
 * single line.</p>
 */
public final class AppLogging {

    private static final Logger APP_LOGGER = Logger.getLogger("org.mesutormanli.jtypewriter");

    /** Prefix applied to every stack-trace line so it cannot be mistaken for a record. */
    private static final String TRACE_INDENT = "    ";

    /**
     * Single-line log layout for the message header. The date/time conversions
     * ({@code %tF}/{@code %tT}) require a value carrying calendar fields;
     * {@link LogRecord#getInstant()} (a bare {@link java.time.Instant}) has none
     * and makes {@code String.format} throw
     * {@code IllegalFormatConversionException} - which {@code java.util.logging}
     * silently swallows, so every log line would vanish. The millisecond epoch
     * value ({@link LogRecord#getMillis()}) is the supported form.
     *
     * <p>When a throwable is attached, the header (built by the same single
     * {@code String.format} call) is followed by the indented stack trace. If
     * the throwable's own {@code printStackTrace} fails, the formatter degrades
     * to the historic {@code " :: " + toString()} suffix rather than losing the
     * record; it never throws.</p>
     *
     * <p>Package-private so a regression test can format a {@link LogRecord}
     * without installing a handler.</p>
     */
    static final Formatter ONE_LINE = new Formatter() {
        @Override
        public String format(LogRecord record) {
            var message = record.getMessage() == null ? "" : record.getMessage();
            var thrown = record.getThrown();
            if (thrown == null) {
                return header(record, message);
            }
            var trace = traceback(thrown);
            if (trace == null) {
                return header(record, message + " :: " + describe(thrown));
            }
            return header(record, message) + trace;
        }
    };

    private static boolean initialized;

    /**
     * Whether the console handler has been installed. Separate from
     * {@link #initialized} so a setup failure after the console was added does
     * not make the retry stack a second console handler.
     */
    private static boolean consoleInstalled;

    private AppLogging() {
    }

    private static String header(LogRecord record, String message) {
        return String.format("%1$tF %1$tT %2$-7s %3$s %4$s%n",
                record.getMillis(),
                record.getLevel(),
                record.getLoggerName(),
                message);
    }

    /**
     * Renders the throwable's own stack trace, one indented line per frame, and
     * terminating with a single newline. Returns {@code null} when the
     * throwable refuses to render (a hostile {@code printStackTrace}/{@code
     * toString}), so the caller can fall back to the inline form. Iterative - no
     * recursion and no truncation; the output is bounded by the trace itself.
     */
    private static String traceback(Throwable thrown) {
        try {
            var buffer = new StringWriter();
            var writer = new PrintWriter(buffer);
            thrown.printStackTrace(writer);
            writer.flush();
            if (writer.checkError()) {
                return null;
            }
            var rendered = new StringBuilder();
            for (var line : buffer.toString().lines().toList()) {
                if (line.isEmpty()) {
                    continue;
                }
                rendered.append(TRACE_INDENT).append(line).append('\n');
            }
            return rendered.isEmpty() ? null : rendered.toString();
        } catch (Throwable failure) {
            // A misbehaving throwable (or writer) must not make the formatter
            // throw: java.util.logging would swallow the whole record.
            return null;
        }
    }

    /** Safe replacement for {@code String.valueOf(thrown)} in the fallback path. */
    private static String describe(Throwable thrown) {
        try {
            return String.valueOf(thrown);
        } catch (Throwable failure) {
            return thrown.getClass().getName();
        }
    }

    /**
     * Installs the stderr handler and the file handler exactly once. Crash-safe
     * by design (V18): this method is reached from the {@code static final
     * Logger} initialisers of {@code AtomicFiles}, {@code FileService},
     * {@code AppSettings} and {@code RecoveryStore}, so a throw here would
     * surface as {@code ExceptionInInitializerError} and the application would
     * not start at all.
     *
     * <p>The ordering matters: {@link #initialized} is set last, only after the
     * setup completed. If a future change makes setup throw, the flag stays
     * {@code false} and the next {@link #logger(Class)} call retries - instead
     * of silently continuing with no handlers for the rest of the process.</p>
     *
     * <p>{@link #installConsole()} runs first because the console must work even
     * when the log file cannot be opened; it is idempotent through
     * {@link #consoleInstalled}.</p>
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        installConsole(); // the app must log even if the file fails
        // The directory is resolved *inside* openFileHandler: AppDirs.logsDir()
        // reads user.home, which can be unset (NullPointerException) or denied
        // (SecurityException) on a hardened JVM. Evaluating it here would put
        // those failures back outside the guard this whole method is about.
        openFileHandler(AppDirs::logsDir).ifPresent(APP_LOGGER::addHandler);
        initialized = true; // only after setup: a failed attempt retries
    }

    /**
     * Adds the stderr handler on first use. Guarded by {@link #consoleInstalled}
     * so a retried {@link #init()} after a partial failure never attaches a
     * second console handler.
     */
    private static void installConsole() {
        if (consoleInstalled) {
            return;
        }
        APP_LOGGER.setLevel(Level.INFO);
        APP_LOGGER.setUseParentHandlers(false);
        var console = new ConsoleHandler();
        configure(console, Level.WARNING);
        APP_LOGGER.addHandler(console);
        consoleInstalled = true;
    }

    /**
     * Creates {@code logsDir}, opens the rotating file handler
     * ({@code jtypewriter-%g.log}, 1 MB, 3 generations, append) and returns it,
     * or returns an empty optional after a warning when the directory or the
     * file cannot be used. Package-private as the test seam, mirroring
     * {@link #ONE_LINE}.
     *
     * <p>This method may never throw. It runs inside static initialisers
     * (V18), so any escaping exception would prevent the application from
     * starting. The catch therefore covers {@code IOException} and every
     * runtime failure expected here:</p>
     * <ul>
     *   <li>{@link SecurityException} - sandboxed home directory or a hardened
     *       JVM that denies file access;</li>
     *   <li>{@link UnsupportedOperationException} - a read-only NIO
     *       filesystem;</li>
     *   <li>{@link NullPointerException}, {@link IllegalArgumentException} and
     *       {@link java.nio.file.ClosedFileSystemException} - from
     *       {@link AppDirs#logsDir()} / {@link Path#resolve(String)} when
     *       {@code user.home} is unset or the filesystem has already been
     *       closed.</li>
     * </ul>
     *
     * <p>The returned handler is owned by the caller: {@link #init()} adds it
     * to the application logger, tests publish to it directly and must close
     * it.</p>
     */
    static Optional<Handler> openFileHandler(Path logsDir) {
        return openFileHandler(() -> logsDir);
    }

    /**
     * The real entry point, taking a supplier so that resolving the directory
     * happens inside the guarded region. {@link AppDirs#logsDir()} reads the
     * {@code user.home} property, and that read is itself a failure point:
     * a {@code SecurityException} on a hardened JVM, a {@code
     * NullPointerException} when the property is unset. It is also what makes
     * the security failure testable: a supplier that throws is exactly the
     * sandboxed-home scenario (PW-30).
     */
    static Optional<Handler> openFileHandler(Supplier<Path> logsDir) {
        try {
            var dir = logsDir.get();
            Files.createDirectories(dir);
            var file = new FileHandler(
                    dir.resolve("jtypewriter-%g.log").toString(),
                    1_000_000, 3, true);
            configure(file, Level.INFO);
            return Optional.of(file);
        } catch (IOException | RuntimeException e) {
            APP_LOGGER.log(Level.WARNING, "cannot open the log file, console only", e);
            return Optional.empty();
        }
    }

    private static void configure(Handler handler, Level level) {
        handler.setLevel(level);
        handler.setFormatter(ONE_LINE);
    }

    /** Logger for a component; logging is initialized on first use. */
    public static Logger logger(Class<?> type) {
        init();
        return Logger.getLogger(type.getName());
    }
}
