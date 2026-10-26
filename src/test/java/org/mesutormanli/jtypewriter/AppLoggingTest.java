/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression for the logging layout: building the line from
 * {@link LogRecord#getInstant()} with the {@code %tF}/{@code %tT} conversions
 * threw {@code IllegalFormatConversionException} (an {@code Instant} carries no
 * calendar fields), and {@code java.util.logging} swallows formatter failures -
 * so every log line was silently lost.
 *
 * <p>Also pins the throwable rendering: the message header must stay a single
 * line and the attached exception must be emitted as an indented stack trace
 * (cause chain included), not just {@code toString()}.</p>
 */
class AppLoggingTest {

    @Test
    void formatsALogRecordWithoutThrowing() {
        var record = new LogRecord(Level.INFO, "hello world");
        record.setInstant(Instant.parse("2026-10-13T12:34:56Z"));
        record.setLoggerName("test.logger");

        var line = assertDoesNotThrow(() -> AppLogging.ONE_LINE.format(record));

        assertTrue(line.contains("hello world"), line);
        assertTrue(line.contains("INFO"), line);
        assertTrue(line.contains("test.logger"), line);
    }

    @Test
    void formatsExceptionAsIndentedStackTrace() {
        var failure = new IllegalStateException("boom");
        var record = record(Level.SEVERE, "save failed", failure);

        var output = assertDoesNotThrow(() -> AppLogging.ONE_LINE.format(record));
        var lines = output.lines().toList();

        assertTrue(lines.size() > 1, output);
        assertTrue(lines.get(0).contains("SEVERE"), output);
        assertTrue(lines.get(0).contains("save failed"), output);
        assertTrue(lines.get(0).contains("test.logger"), output);
        assertFalse(lines.get(0).contains(" :: "), output);

        assertTrue(output.contains(failure.toString()), output);
        assertTrue(output.contains("formatsExceptionAsIndentedStackTrace"), output);
        assertTrue(output.endsWith("\n"), output);
    }

    @Test
    void rendersCauseChain() {
        var failure = new RuntimeException("outer", new IllegalArgumentException("inner"));
        var output = AppLogging.ONE_LINE.format(record(Level.SEVERE, "wrapped", failure));

        assertTrue(output.contains("Caused by:"), output);
        assertTrue(output.contains("inner"), output);
    }

    @Test
    void indentsEveryTraceLine() {
        var failure = new IllegalStateException("boom");
        var output = AppLogging.ONE_LINE.format(record(Level.WARNING, "indent", failure));
        var lines = output.lines().toList();

        assertTrue(lines.size() > 1, output);
        for (var line : lines.subList(1, lines.size())) {
            if (!line.isEmpty()) {
                assertTrue(line.startsWith("    "), line);
            }
        }
    }

    @Test
    void formatsRecordWithoutThrowableToExactlyOneLine() {
        var output = AppLogging.ONE_LINE.format(record(Level.INFO, "plain", null));

        assertEquals(1, output.lines().count(), output);
        assertTrue(output.endsWith("\n"), output);
        assertFalse(output.endsWith("\n\n"), output);
    }

    @Test
    void keepsNonAsciiThrowableMessage() {
        var failure = new IllegalStateException("bozuk çöğü");
        var output = AppLogging.ONE_LINE.format(record(Level.SEVERE, "kodlama", failure));

        assertTrue(output.contains("çöğü"), output);
        assertFalse(output.contains("?"), output);
    }

    @Test
    void fallsBackWhenThrowableCannotRenderItself() {
        var hostile = new IllegalStateException("hostile") {
            @Override
            public void printStackTrace(java.io.PrintWriter writer) {
                throw new IllegalStateException("no trace for you");
            }
        };
        var output = assertDoesNotThrow(
                () -> AppLogging.ONE_LINE.format(record(Level.SEVERE, "hostile", hostile)));

        assertEquals(1, output.lines().count(), output);
        assertTrue(output.contains(" :: "), output);
        assertTrue(output.contains("hostile"), output);
    }

    @Test
    void writesToTheLogFileInTheRequestedDirectory(@TempDir Path dir) throws Exception {
        var logsDir = dir.resolve("logs");
        var handler = AppLogging.openFileHandler(logsDir).orElseThrow();
        try {
            var record = new LogRecord(Level.INFO, "written to the log file");
            record.setLoggerName("org.mesutormanli.jtypewriter");
            handler.publish(record);
            handler.flush();
        } finally {
            handler.close();
        }

        var logFile = logsDir.resolve("jtypewriter-0.log");
        assertTrue(Files.isRegularFile(logFile), "the first generation must be created: " + logFile);
        assertTrue(Files.readString(logFile).contains("written to the log file"),
                "the record must reach the requested directory");
    }

    @Test
    void anUnusableLogDirectoryFallsBackToConsoleOnly(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("logs"), "a regular file, not a directory");

        var handler = assertDoesNotThrow(() -> AppLogging.openFileHandler(dir.resolve("logs")),
                "an uncreatable log directory must not throw (FileAlreadyExistsException)");

        assertTrue(handler.isEmpty(), "there is no file handler to return");
    }

    /**
     * A closed {@code zip:} filesystem makes every path operation throw
     * {@link java.nio.file.ClosedFileSystemException} (an
     * {@link IllegalStateException}, i.e. the {@code RuntimeException} branch).
     * The genuine sandbox case is covered by
     * {@link #aDeniedHomeDirectoryFallsBackToConsoleOnly}; this one adds a
     * failure raised by the filesystem itself rather than by the path lookup.
     */
    @Test
    void aClosedFileSystemDoesNotBreakLogging(@TempDir Path dir) throws Exception {
        FileSystem fs = FileSystems.newFileSystem(
                URI.create("jar:" + dir.resolve("closed.zip").toUri()), Map.of("create", "true"));
        Path closedPath;
        try {
            closedPath = fs.getPath("/logs");
        } finally {
            fs.close();
        }

        var handler = assertDoesNotThrow(() -> AppLogging.openFileHandler(closedPath),
                "a closed filesystem must degrade to console-only logging");

        assertTrue(handler.isEmpty(), "there is no file handler to return");
    }

    /**
     * The exact defect. {@code SecurityException} used to escape
     * {@code init()} and surface as {@code ExceptionInInitializerError} from the
     * {@code static final Logger} fields, so the application never started on a
     * sandboxed home directory. The directory is now resolved inside the guard,
     * which this test drives with a supplier that is denied.
     */
    @Test
    void aDeniedHomeDirectoryFallsBackToConsoleOnly() {
        Supplier<Path> deniedHome = () -> {
            throw new SecurityException("access denied to the user home");
        };

        var handler = assertDoesNotThrow(() -> AppLogging.openFileHandler(deniedHome),
                "a denied home directory must degrade to console-only logging");

        assertTrue(handler.isEmpty(), "there is no file handler to return");
    }

    @Test
    void repeatedInitDoesNotStackHandlers() {
        var logger = Logger.getLogger("org.mesutormanli.jtypewriter");
        AppLogging.init(); // the first, legitimate installation
        var before = logger.getHandlers().length;

        AppLogging.init();
        AppLogging.init();

        assertEquals(before, logger.getHandlers().length,
                "init() must be idempotent even when called twice");
    }

    @Test
    void enablingLoggingFromAStaticInitialiserNeverThrows() {
        var logger = assertDoesNotThrow(() -> AppLogging.logger(AppLoggingTest.class),
                "logger() runs in static initialisers and must never throw");

        assertNotNull(logger);
        assertEquals(AppLoggingTest.class.getName(), logger.getName());
    }

    private static LogRecord record(Level level, String message, Throwable thrown) {
        var record = new LogRecord(level, message);
        record.setInstant(Instant.parse("2026-10-13T12:34:56Z"));
        record.setLoggerName("test.logger");
        if (thrown != null) {
            record.setThrown(thrown);
        }
        return record;
    }
}
