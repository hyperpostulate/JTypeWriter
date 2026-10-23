package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
