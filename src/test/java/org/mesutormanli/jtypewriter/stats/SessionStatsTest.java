package org.mesutormanli.jtypewriter.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
