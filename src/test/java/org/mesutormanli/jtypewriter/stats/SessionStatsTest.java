package org.mesutormanli.jtypewriter.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionStatsTest {

    private SessionStats stats;

    @BeforeEach
    void setUp() {
        stats = new SessionStats();
    }

    @Test
    void emptyTextHasZeroCounts() {
        assertEquals(0, stats.getWordCount());
        assertEquals(0, stats.getCharCount());
        assertEquals(0, stats.getCharCountNoSpaces());
        assertEquals(0, stats.getLineCount());
    }

    @Test
    void wordCountIsCorrect() {
        stats.updateText("hello world");
        assertEquals(2, stats.getWordCount());
    }

    @Test
    void charCountIsCorrect() {
        stats.updateText("hello");
        assertEquals(5, stats.getCharCount());
    }

    @Test
    void charCountNoSpaces() {
        stats.updateText("hello world");
        assertEquals(10, stats.getCharCountNoSpaces());
    }

    @Test
    void lineCountIsCorrect() {
        stats.updateText("line1\nline2\nline3");
        assertEquals(3, stats.getLineCount());
    }

    @Test
    void keystrokeCounterWorks() {
        stats.registerKeystroke();
        stats.registerKeystroke();
        stats.registerKeystroke();
        assertEquals(3, stats.getTotalKeystrokes());
    }

    @Test
    void resetClearsCounts() {
        stats.registerKeystroke();
        stats.updateText("some text");
        stats.reset();
        assertEquals(0, stats.getTotalKeystrokes());
        assertEquals(0, stats.getWordCount());
    }

    // --- WPM: active typing time, not wall-clock time ---------------------

    /** Deterministic millisecond clock driven by the test. */
    private static final class FakeClock {
        long now = 1_000_000L;

        long read() {
            return now;
        }
    }

    private SessionStats statsWith(FakeClock clock) {
        return new SessionStats(clock::read);
    }

    private void typeBurst(SessionStats stats, FakeClock clock, int keystrokes, long gapMs) {
        for (int i = 0; i < keystrokes; i++) {
            stats.registerKeystroke();
            clock.now += gapMs;
        }
    }

    @Test
    void wpmIsCharactersPerFivePerActiveMinute() {
        var clock = new FakeClock();
        var stats = statsWith(clock);

        typeBurst(stats, clock, 12, 100); // 12 keys across 1.1s of active typing

        // (12 / 5) chars-per-word over 1.1s => ~131 wpm
        assertEquals(131, stats.getWpm());
    }

    @Test
    void wpmDoesNotDecayWhileIdle() {
        var clock = new FakeClock();
        var stats = statsWith(clock);
        typeBurst(stats, clock, 12, 100);
        var settled = stats.getWpm();

        clock.now += 3_600_000; // reading break: one hour, no typing

        assertEquals(settled, stats.getWpm());
    }

    @Test
    void thinkingPausesAreNotCountedAsTypingTime() {
        var clock = new FakeClock();
        var stats = statsWith(clock);

        stats.registerKeystroke();
        clock.now += 60_000; // a minute of thinking between two keys
        stats.registerKeystroke();

        assertEquals(0, stats.getWpm()); // no measurable typing time yet
    }

    @Test
    void wpmIsZeroWithoutTypingTime() {
        var clock = new FakeClock();
        var stats = statsWith(clock);

        assertEquals(0, stats.getWpm());
    }

    @Test
    void resetRestartsSessionMetrics() {
        var clock = new FakeClock();
        var stats = statsWith(clock);
        typeBurst(stats, clock, 12, 100);

        stats.reset();

        assertEquals(0, stats.getTotalKeystrokes());
        assertEquals(0, stats.getWpm());
        assertEquals(0, stats.getSessionDuration().toMillis());
    }
}