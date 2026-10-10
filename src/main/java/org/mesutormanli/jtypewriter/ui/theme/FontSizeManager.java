package org.mesutormanli.jtypewriter.ui.theme;


public class FontSizeManager {

    private int currentSize = 16;

    public int getCurrentSize() {
        return currentSize;
    }

    public void changeSize(int delta) {
        currentSize = Math.max(10, Math.min(48, currentSize + delta));
    }

    public void reset() {
        currentSize = 16;
    }
}
