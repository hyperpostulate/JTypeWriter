/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import org.mesutormanli.jtypewriter.AppSettings;

/**
 * The stats bar text size, persisted through {@link AppSettings} so it
 * survives a restart. Non-Node UI state, following the {@link ToolbarState}
 * precedent: {@link StatsBar} owns the pixels, this class owns the choice -
 * which means the double-click cycle writes through to disk and the restored
 * size is known before the control exists.
 *
 * <p>The persisted form is {@link AppSettings.StatsBarSize}, a neutral mirror
 * enum: {@code AppSettings} must never import {@code StatsBar}, which extends
 * a JavaFX control. The two enums are mapped exhaustively below, so renaming a
 * size is a compile-time error in both directions, and a corrupt stored value
 * never reaches this class ({@code AppSettings} already fell back to
 * {@code MEDIUM}).</p>
 */
public final class StatsBarState {

    private final AppSettings settings;
    private StatsBar.Size currentSize;

    public StatsBarState(AppSettings settings) {
        this.settings = settings;
        this.currentSize = toSize(settings.getStatsBarSize());
    }

    /** The size to paint with; read before the first {@code applySize()}. */
    public StatsBar.Size getSize() {
        return currentSize;
    }

    /** Advances the double-click cycle (SMALL → MEDIUM → LARGE → SMALL) and persists it. */
    public StatsBar.Size nextSize() {
        return setSize(currentSize.next());
    }

    /** Makes {@code size} current and persists it; a repeat of the size in force is a no-op. */
    public StatsBar.Size setSize(StatsBar.Size size) {
        if (size != currentSize) {
            currentSize = size;
            settings.setStatsBarSize(toPersisted(size));
        }
        return currentSize;
    }

    /** Exhaustive persisted → view mapping; a new constant on either side fails compilation. */
    static StatsBar.Size toSize(AppSettings.StatsBarSize persisted) {
        return switch (persisted) {
            case SMALL -> StatsBar.Size.SMALL;
            case MEDIUM -> StatsBar.Size.MEDIUM;
            case LARGE -> StatsBar.Size.LARGE;
        };
    }

    /** Exhaustive view → persisted mapping, the inverse of {@link #toSize}. */
    static AppSettings.StatsBarSize toPersisted(StatsBar.Size size) {
        return switch (size) {
            case SMALL -> AppSettings.StatsBarSize.SMALL;
            case MEDIUM -> AppSettings.StatsBarSize.MEDIUM;
            case LARGE -> AppSettings.StatsBarSize.LARGE;
        };
    }
}
