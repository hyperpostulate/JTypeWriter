package org.mesutormanli.jtypewriter.stats;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class SessionStats {

    private Instant sessionStart;
    private long totalKeystrokes;
    private String currentText = "";

    public SessionStats() {
        reset();
    }

    public void reset() {
        sessionStart = Instant.now();
        totalKeystrokes = 0;
        currentText = "";
    }

    public void registerKeystroke() {
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

    public Duration getSessionDuration() {
        return Duration.between(sessionStart, Instant.now());
    }

    public String getFormattedDuration() {
        var d = getSessionDuration();
        var hours = d.toHours();
        var minutes = d.toMinutes() % 60;
        var seconds = d.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
