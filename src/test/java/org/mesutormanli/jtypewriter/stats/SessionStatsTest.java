/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

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

    // --- incremental counters: same numbers as a full scan -----------------

    /**
     * The reference every incremental case is compared against: the exact
     * implementations the getters used before the counters existed. Keeping
     * them here means a test can never drift with the production code - it
     * is the old behaviour, written out.
     */
    private static void assertMatchesFullScan(SessionStats stats, String text) {
        var words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
        var noSpaces = text.replace(" ", "").replace("\n", "").replace("\r", "").length();
        var lines = text.isEmpty() ? 0 : text.split("\n", -1).length;
        assertEquals(words, stats.getWordCount(), "word count for: " + preview(text));
        assertEquals(text.length(), stats.getCharCount(), "character count for: " + preview(text));
        assertEquals(noSpaces, stats.getCharCountNoSpaces(), "no-space count for: " + preview(text));
        assertEquals(lines, stats.getLineCount(), "line count for: " + preview(text));
    }

    private static String preview(String text) {
        return text.length() <= 40 ? text : text.substring(0, 40) + "...";
    }

    @Test
    void incrementalAppendMatchesFullScan() {
        var stats = new SessionStats();
        var text = "";
        for (var piece : new String[]{"hello", " ", "world", "\n", "second line", " "}) {
            var next = text + piece;
            stats.applyChange(new TextChange(text.length(), 0, piece), next);
            text = next;
            assertMatchesFullScan(stats, text);
        }
    }

    @Test
    void incrementalDeleteMatchesFullScan() {
        var stats = new SessionStats();
        var text = "one two\nthree four";
        stats.updateText(text);

        var offset = text.indexOf("two");
        var next = text.substring(0, offset) + text.substring(offset + 3);
        stats.applyChange(new TextChange(offset, 3, ""), next);

        assertMatchesFullScan(stats, next);

        // deleting the newline joins two lines and the two words
        var newline = next.indexOf('\n');
        var joined = next.substring(0, newline) + next.substring(newline + 1);
        stats.applyChange(new TextChange(newline, 1, ""), joined);

        assertMatchesFullScan(stats, joined);
    }

    @Test
    void incrementalReplaceMatchesFullScan() {
        var stats = new SessionStats();
        var text = "alpha beta gamma";
        stats.updateText(text);

        var next = text.replace("beta", "BETA delta");
        stats.applyChange(new TextChange(text.indexOf("beta"), 4, "BETA delta"), next);

        assertMatchesFullScan(stats, next);
        assertEquals(4, stats.getWordCount());
    }

    @Test
    void wordBoundaryEditsMatchFullScan() {
        var stats = new SessionStats();
        var text = "ab";
        stats.updateText(text);

        // a space inserted inside a word splits it in two
        var split = "a b";
        stats.applyChange(new TextChange(1, 0, " "), split);
        assertMatchesFullScan(stats, split);
        assertEquals(2, stats.getWordCount());

        // removing it merges them back
        var merged = "ab";
        stats.applyChange(new TextChange(1, 1, ""), merged);
        assertMatchesFullScan(stats, merged);
        assertEquals(1, stats.getWordCount());
    }

    @Test
    void unicodeWhitespaceKeepsTheFullScanDefinitions() {
        var stats = new SessionStats();

        // isBlank() is wider than the regex class: em-spaces are blank but
        // would each start a regex "word".
        var emSpaces = "\u2003\u2003";
        stats.updateText(emSpaces);
        assertMatchesFullScan(stats, emSpaces);
        assertEquals(0, stats.getWordCount());

        // a no-break space is neither trim()-able nor a \s separator
        var noBreak = "alpha\u00A0beta";
        stats.updateText(noBreak);
        assertMatchesFullScan(stats, noBreak);
        assertEquals(1, stats.getWordCount());
    }

    @Test
    void inconsistentChangeRescansInsteadOfTrustingTheHint() {
        var stats = new SessionStats();
        stats.updateText("one two");

        // A stale hint: the new text is not what the change would produce.
        stats.applyChange(new TextChange(0, 0, "x"), "something else entirely");

        assertMatchesFullScan(stats, "something else entirely");
    }

    @Test
    void replacingWholeDocumentMatchesFullScan() {
        var stats = new SessionStats();
        stats.updateText("first document\nwith two lines");

        var loaded = "completely different\ncontent\nhere";
        stats.updateText(loaded);

        assertMatchesFullScan(stats, loaded);
        assertEquals(3, stats.getLineCount());
    }

    @Test
    void deletingEverythingReturnsLineCountToZero() {
        var stats = new SessionStats();
        var text = "a\nb\nc";
        stats.updateText(text);

        stats.applyChange(new TextChange(0, text.length(), ""), "");

        assertMatchesFullScan(stats, "");
        assertEquals(0, stats.getLineCount());
        assertEquals(0, stats.getWordCount());
    }

    /**
     * Property-style: a long random sequence of insertions, deletions,
     * replacements and whole-document swaps, asserting the counters against
     * the full-scan reference after every single step. The alphabet deliberately
     * mixes the whitespace characters whose definitions disagree.
     */
    @Test
    void randomEditsMatchFullScan() {
        var random = new java.util.Random(20261105L);
        var alphabet = new char[]{'a', 'b', ' ', '\t', '\n', '\r', '\u2003', '\u00A0'};
        var stats = new SessionStats();
        var text = "";
        stats.updateText(text);

        for (var step = 0; step < 500; step++) {
            var operation = random.nextInt(10);
            if (operation == 0) {
                // whole-document replacement, as a file load does
                var length = random.nextInt(60);
                var builder = new StringBuilder(length);
                for (var i = 0; i < length; i++) {
                    builder.append(alphabet[random.nextInt(alphabet.length)]);
                }
                text = builder.toString();
                stats.updateText(text);
            } else {
                var offset = random.nextInt(text.length() + 1);
                if (operation <= 4) {
                    var inserted = randomPiece(random, alphabet);
                    text = text.substring(0, offset) + inserted + text.substring(offset);
                    stats.applyChange(new TextChange(offset, 0, inserted), text);
                } else if (operation <= 7) {
                    var removed = random.nextInt(Math.min(8, text.length() - offset) + 1);
                    text = text.substring(0, offset) + text.substring(offset + removed);
                    stats.applyChange(new TextChange(offset, removed, ""), text);
                } else {
                    var removed = random.nextInt(Math.min(8, text.length() - offset) + 1);
                    var inserted = randomPiece(random, alphabet);
                    text = text.substring(0, offset) + inserted
                            + text.substring(offset + removed);
                    stats.applyChange(new TextChange(offset, removed, inserted), text);
                }
            }
            assertMatchesFullScan(stats, text);
        }
    }

    private static String randomPiece(java.util.Random random, char[] alphabet) {
        var length = random.nextInt(5);
        var builder = new StringBuilder(length);
        for (var i = 0; i < length; i++) {
            builder.append(alphabet[random.nextInt(alphabet.length)]);
        }
        return builder.toString();
    }
}