package org.mesutormanli.jtypewriter.ui.theme;


import org.mesutormanli.jtypewriter.AppSettings;

public class FontSizeManager {

    public static final int MIN_SIZE = 10;
    public static final int MAX_SIZE = 48;
    public static final int DEFAULT_SIZE = 16;

    private final AppSettings settings;
    private int currentSize;

    public FontSizeManager(AppSettings settings) {
        this.settings = settings;
        this.currentSize = clamp(settings.getFontSize());
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public void changeSize(int delta) {
        var next = clamp(currentSize + delta);
        if (next != currentSize) {
            currentSize = next;
            settings.setFontSize(currentSize);
        }
    }

    public void reset() {
        currentSize = DEFAULT_SIZE;
        settings.setFontSize(currentSize);
    }

    private static int clamp(int size) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }
}