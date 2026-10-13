package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression for the logging layout: building the line from
 * {@link LogRecord#getInstant()} with the {@code %tF}/{@code %tT} conversions
 * threw {@code IllegalFormatConversionException} (an {@code Instant} carries no
 * calendar fields), and {@code java.util.logging} swallows formatter failures -
 * so every log line was silently lost.
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
}
