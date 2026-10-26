/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

/**
 * Pure sizing maths shared by the helper dialogs.
 *
 * <p>The welcome/About windows used to open at their preferred size whatever
 * the display was, so on a 768 px-tall screen (and even more so with a
 * taskbar) their lower edge - including the dismiss button - landed off
 * screen. These methods fit a preferred size into the visual bounds instead.
 *
 * <p>Kept free of JavaFX types on purpose: the tests run headless, so the
 * clamping has to be reachable without a toolkit.
 */
public final class DialogSizing {

    /**
     * Breathing room left around a dialog inside the screen's visual bounds.
     *
     * <p>The visual bounds already exclude the taskbar/panel, but only when the
     * window manager reports them correctly; a decorated, resizable window also
     * needs a few pixels of slack to keep its shadow and resize grip reachable.
     * Clamping to the visual bounds minus this margin keeps the dialog - and
     * its close button - on screen on a 768 px display instead of running
     * under the panel.
     */
    public static final double SCREEN_MARGIN = 48;

    /**
     * Smallest height the clamp will produce. Content still scrolls below this,
     * but a dialog that collapsed to a strip would be unusable.
     */
    public static final double MIN_HEIGHT = 420;

    /** Smallest width the clamp will produce; see {@link #MIN_HEIGHT}. */
    public static final double MIN_WIDTH = 420;

    private DialogSizing() {
    }

    /**
     * Fits a preferred window height into the screen.
     *
     * <p>Never exceeds {@code visualHeight - 2 * SCREEN_MARGIN}, so the dialog
     * cannot hang off the display. On an implausibly short screen, where that
     * bound is below {@link #MIN_HEIGHT}, the floor wins: a 420 px window with
     * a scrollbar is still usable, a zero-height one is not.
     */
    public static double clampHeight(double preferred, double visualHeight) {
        return Math.max(MIN_HEIGHT, Math.min(preferred, visualHeight - 2 * SCREEN_MARGIN));
    }

    /** Width counterpart of {@link #clampHeight}; same margin and floor logic. */
    public static double clampWidth(double preferred, double visualWidth) {
        return Math.max(MIN_WIDTH, Math.min(preferred, visualWidth - 2 * SCREEN_MARGIN));
    }
}
