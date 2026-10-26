/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.stats;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Writing session statistics.
 *
 * <p>WPM is computed from <em>active typing time</em>, not wall-clock session
 * time: only the gaps between consecutive keystrokes up to
 * {@value #IDLE_GAP_MS} ms count towards it, so taking a break no longer drags
 * the number down (the classic "WPM decays while I read" bug). Speed is the
 * standard {@code characters / 5} per active minute. The clock is injectable so
 * tests can drive it deterministically.</p>
 */
public class SessionStats {

    /** Pauses longer than this are thinking time, not typing time. */
    static final long IDLE_GAP_MS = 5_000;
    /** Below this much accumulated typing the speed is meaningless and shown as 0. */
    static final long MIN_ACTIVE_MS = 1_000;

    private final LongSupplier clock;

    private long sessionStartMs;
    private long activeTypingMs;
    private long lastKeystrokeMs;
    private long totalKeystrokes;
    private String currentText = "";

    public SessionStats() {
        this(System::currentTimeMillis);
    }

    public SessionStats(LongSupplier clock) {
        this.clock = clock;
        reset();
    }

    public void reset() {
        sessionStartMs = now();
        activeTypingMs = 0;
        lastKeystrokeMs = -1;
        totalKeystrokes = 0;
        currentText = "";
    }

    public void registerKeystroke() {
        var now = now();
        if (lastKeystrokeMs >= 0) {
            var gap = now - lastKeystrokeMs;
            if (gap > 0 && gap <= IDLE_GAP_MS) {
                activeTypingMs += gap;
            }
        }
        lastKeystrokeMs = now;
        totalKeystrokes++;
    }

    public void updateText(String text) {
        currentText = text;
    }

    public int getWordCount() {
        if (currentText.isBlank()) return 0;
        return currentText.trim().split("\\s+").length;
    }

    public int getCharCount() {
        return currentText.length();
    }

    public int getCharCountNoSpaces() {
        return currentText.replace(" ", "").replace("\n", "").replace("\r", "").length();
    }

    public int getLineCount() {
        if (currentText.isEmpty()) return 0;
        return currentText.split("\n", -1).length;
    }

    public long getTotalKeystrokes() {
        return totalKeystrokes;
    }

    /** Words per minute, sustained typing speed (characters divided by five). */
    public int getWpm() {
        if (activeTypingMs < MIN_ACTIVE_MS) return 0;
        var minutes = activeTypingMs / 60_000.0;
        return (int) Math.round((totalKeystrokes / 5.0) / minutes);
    }

    public Duration getSessionDuration() {
        return Duration.ofMillis(Math.max(0, now() - sessionStartMs));
    }

    public String getFormattedDuration() {
        var d = getSessionDuration();
        var hours = d.toHours();
        var minutes = d.toMinutes() % 60;
        var seconds = d.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private long now() {
        return clock.getAsLong();
    }
}