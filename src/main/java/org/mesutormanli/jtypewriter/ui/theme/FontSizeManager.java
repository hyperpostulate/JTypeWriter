/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.theme;


import org.mesutormanli.jtypewriter.AppSettings;

import java.util.ArrayList;
import java.util.List;

public class FontSizeManager {

    public static final int MIN_SIZE = 10;
    public static final int MAX_SIZE = 48;
    public static final int DEFAULT_SIZE = 16;

    /**
     * The fixed ladder the font-size dropdown offers: 10..20 in single steps,
     * then the coarser jumps a toolbar picker needs. Deliberately a constant
     * list rather than a computed range so the available sizes are stable and
     * the keyboard steps (Ctrl+- / Ctrl++) land on visible dropdown entries.
     */
    private static final List<Integer> LADDER =
            List.of(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 22, 24, 28, 32, 36, 40, 44, 48);

    private final AppSettings settings;
    private int currentSize;

    public FontSizeManager(AppSettings settings) {
        this.settings = settings;
        this.currentSize = clamp(settings.getFontSize());
    }

    /** The fixed dropdown ladder, ascending, without duplicates. */
    public static List<Integer> options() {
        return LADDER;
    }

    /**
     * The dropdown list for a given persisted size. A size that is <em>not</em>
     * on the ladder (e.g. 21 persisted by an older build that stepped in single
     * pixels) is inserted in sorted position instead of being silently changed:
     * the dropdown must be able to show the real current value.
     */
    public static List<Integer> optionsFor(int current) {
        var size = clamp(current);
        if (LADDER.contains(size)) {
            return LADDER;
        }
        var withCurrent = new ArrayList<>(LADDER);
        var index = 0;
        while (index < withCurrent.size() && withCurrent.get(index) < size) {
            index++;
        }
        withCurrent.add(index, size);
        return List.copyOf(withCurrent);
    }

    /** The next ladder step above {@code current}; 48 stays 48. */
    public static int stepUp(int current) {
        var size = clamp(current);
        for (var step : LADDER) {
            if (step > size) {
                return step;
            }
        }
        return MAX_SIZE;
    }

    /** The previous ladder step below {@code current}; 10 stays 10. */
    public static int stepDown(int current) {
        var size = clamp(current);
        for (var i = LADDER.size() - 1; i >= 0; i--) {
            if (LADDER.get(i) < size) {
                return LADDER.get(i);
            }
        }
        return MIN_SIZE;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    /**
     * The single setter behind the dropdown: clamps into [{@value MIN_SIZE},
     * {@value MAX_SIZE}] and persists, exactly like the old A+/A- path did. A
     * repeat request for the size already in force is ignored.
     */
    public void setSize(int size) {
        var next = clamp(size);
        if (next != currentSize) {
            currentSize = next;
            settings.setFontSize(currentSize);
        }
    }

    /** Ctrl+0: the default size (single source: {@value #DEFAULT_SIZE}). */
    public void reset() {
        currentSize = DEFAULT_SIZE;
        settings.setFontSize(currentSize);
    }

    private static int clamp(int size) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }
}
