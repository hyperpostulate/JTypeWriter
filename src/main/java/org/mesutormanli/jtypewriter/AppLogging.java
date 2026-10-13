package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.nio.file.Files;
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
 */
public final class AppLogging {

    private static final Logger APP_LOGGER = Logger.getLogger("org.mesutormanli.jtypewriter");

    /**
     * Single-line log layout. The date/time conversions ({@code %tF}/{@code %tT})
     * require a value carrying calendar fields; {@link LogRecord#getInstant()}
     * (a bare {@link java.time.Instant}) has none and makes {@code String.format}
     * throw {@code IllegalFormatConversionException} - which {@code java.util.logging}
     * silently swallows, so every log line would vanish. The millisecond epoch
     * value ({@link LogRecord#getMillis()}) is the supported form.
     *
     * <p>Package-private so a regression test can format a {@link LogRecord}
     * without installing a handler.</p>
     */
    static final Formatter ONE_LINE = new Formatter() {
        @Override
        public String format(LogRecord record) {
            var message = record.getMessage() == null ? "" : record.getMessage();
            return String.format("%1$tF %1$tT %2$-7s %3$s %4$s%n",
                    record.getMillis(),
                    record.getLevel(),
                    record.getLoggerName(),
                    message + (record.getThrown() == null ? ""
                            : " :: " + record.getThrown()));
        }
    };

    private static boolean initialized;

    private AppLogging() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        APP_LOGGER.setLevel(Level.INFO);
        APP_LOGGER.setUseParentHandlers(false);

        var console = new ConsoleHandler();
        configure(console, Level.WARNING);
        APP_LOGGER.addHandler(console);

        try {
            Files.createDirectories(AppDirs.logsDir());
            var file = new FileHandler(
                    AppDirs.logsDir().resolve("jtypewriter-%g.log").toString(),
                    1_000_000, 3, true);
            configure(file, Level.INFO);
            APP_LOGGER.addHandler(file);
        } catch (IOException e) {
            APP_LOGGER.log(Level.WARNING, "cannot open log file, console only", e);
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