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
 *
 * <p><strong>Counters are incremental.</strong> Word, line and non-space
 * character counts used to be recomputed from the whole document by every
 * getter: on a 10 MB draft one stats refresh cost ~164 ms and the bar runs one
 * every second on the FX thread, which made the counter - not autosave - the
 * largest recurring cost of the product. The getters are now O(1) reads of
 * counters maintained in two places: {@link #updateText(String)} does a single
 * full scan when a document is loaded or replaced, and
 * {@link #applyChange(TextChange, String)} adjusts only the window a keystroke
 * touched, so typing costs O(edited characters). The public getters keep their
 * exact signatures, which is what lets {@code StatsBarModel} stay untouched,
 * and the definitions are byte-for-byte the previous ones:</p>
 * <ul>
 *   <li>words: {@code isBlank() ? 0 : trim().split("\\s+").length}, i.e. the
 *       number of runs of non-{@code \s} characters, where {@code \s} is the
 *       ASCII class {@code [ \t\n\x0B\f\r]} - {@code \u00A0} and other Unicode
 *       spaces keep a word together;</li>
 *   <li>lines: {@code 0} for the empty document, otherwise newlines plus
 *       one (a trailing newline opens an empty last line);</li>
 *   <li>characters without spaces: the length after removing exactly space,
 *       {@code \n} and {@code \r}.</li>
 * </ul>
 *
 * <p><strong>Why the blank test still needs its own counter:</strong>
 * {@code isBlank()} uses {@link Character#isWhitespace(char)}, which is a wider
 * set than the regex class above. A document of only em-spaces is blank (count
 * {@code 0}) yet its characters would each start a "word" under the regex, so
 * the two notions cannot be collapsed into one.</p>
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

    /**
     * The last known document. Only a reference is kept - the string is
     * already allocated by whoever reports the change - so this is not a copy.
     */
    private String currentText = "";
    private int wordStarts;
    private int nonWhitespaceChars;
    private int newlineCount;
    private int nonSpaceChars;

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
        recompute("");
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

    /**
     * Replaces the known document and rescans it once.
     *
     * <p>Used for document loads, new documents and any change that cannot be
     * described as a {@link TextChange} (undo/redo rewrite the content
     * directly). The scan is O(n) but happens once per load, not once per
     * refresh.</p>
     */
    public void updateText(String text) {
        recompute(text);
    }

    /**
     * Applies a single replacement and updates the counters from the changed
     * window only.
     *
     * <p>{@code newText} must be the document after the change. It is also the
     * fallback path: when the described change does not match
     * {@code newText}'s length (a stale or missing hint, for instance after
     * undo/redo), the whole document is rescanned instead of leaving the
     * counters quietly wrong. Correctness never depends on the hint being
     * accurate - only speed does.</p>
     */
    public void applyChange(TextChange change, String newText) {
        if (change == null || newText == null) {
            return;
        }
        var previous = currentText;
        var insertedLength = change.insertedText().length();
        if (change.offset() < 0 || change.removedLength() < 0
                || change.offset() > previous.length()
                || change.offset() + change.removedLength() > previous.length()
                || newText.length() != change.resultingLength(previous.length())) {
            recompute(newText);
            return;
        }
        // A word start at index i depends on the characters on both sides of
        // it, so the affected range reaches one character past the edit. The
        // extra unchanged character appears in both windows and cancels out.
        var before = scan(previous, change.offset(),
                change.offset() + change.removedLength() + 1);
        var after = scan(newText, change.offset(), change.offset() + insertedLength + 1);
        wordStarts += after.wordStarts - before.wordStarts;
        nonWhitespaceChars += after.nonWhitespaceChars - before.nonWhitespaceChars;
        newlineCount += after.newlines - before.newlines;
        nonSpaceChars += after.nonSpaceChars - before.nonSpaceChars;
        currentText = newText;
    }

    public int getWordCount() {
        // isBlank() semantics: a document of only whitespace has no words even
        // when the regex would treat some of those characters as word letters.
        return nonWhitespaceChars == 0 ? 0 : wordStarts;
    }

    public int getCharCount() {
        return currentText.length();
    }

    public int getCharCountNoSpaces() {
        return nonSpaceChars;
    }

    public int getLineCount() {
        return currentText.isEmpty() ? 0 : newlineCount + 1;
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

    private void recompute(String text) {
        var counts = scan(text, 0, text.length());
        wordStarts = counts.wordStarts;
        nonWhitespaceChars = counts.nonWhitespaceChars;
        newlineCount = counts.newlines;
        nonSpaceChars = counts.nonSpaceChars;
        currentText = text;
    }

    /**
     * Counts {@code [from, toExclusive)} of {@code text}; {@code from} may
     * index past the end and the range is clipped. The character before
     * {@code from} is read from the full string so a word boundary straddling
     * the window edge is still decided correctly.
     */
    private static Counts scan(String text, int from, int toExclusive) {
        var start = Math.min(text.length(), Math.max(0, from));
        var end = Math.min(text.length(), toExclusive);
        var previousIsSeparator = start == 0 || isWordSeparator(text.charAt(start - 1));
        var counts = new Counts();
        for (var i = start; i < end; i++) {
            var c = text.charAt(i);
            if (!isWordSeparator(c) && previousIsSeparator) {
                counts.wordStarts++;
            }
            previousIsSeparator = isWordSeparator(c);
            if (!Character.isWhitespace(c)) {
                counts.nonWhitespaceChars++;
            }
            if (c == '\n') {
                counts.newlines++;
            }
            if (c != ' ' && c != '\n' && c != '\r') {
                counts.nonSpaceChars++;
            }
        }
        return counts;
    }

    /** The ASCII whitespace class of Java's default {@code \s}. */
    private static boolean isWordSeparator(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    private long now() {
        return clock.getAsLong();
    }

    /** Mutable tally of one scan window; private because it never escapes. */
    private static final class Counts {
        int wordStarts;
        int nonWhitespaceChars;
        int newlines;
        int nonSpaceChars;
    }
}
