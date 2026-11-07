/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.theme;

import org.mesutormanli.jtypewriter.locale.Messages;

public enum Theme {
    LIGHT("css/light.css", "#ffffff"),
    DARK("css/dark.css", "#1a1a1a"),
    SEPIA("css/sepia.css", "#f4ecd8");

    private final String cssPath;

    /**
     * The canvas colour mirrored from the CSS ({@code .editor-area}). The
     * window paints its scene with it so a resize never flashes another colour
     * before the editor lays itself out.
     */
    private final String editorFill;

    Theme(String cssPath, String editorFill) {
        this.cssPath = cssPath;
        this.editorFill = editorFill;
    }

    public String getCssPath() {
        return cssPath;
    }

    /** Matches the {@code .editor-area} background: editor canvas colour. */
    public String getEditorFill() {
        return editorFill;
    }

    /** Localised theme name for the current UI language (bundle key {@code labelTheme*}). */
    public String getDisplayName(Messages messages) {
        return switch (this) {
            case LIGHT -> messages.labelThemeLight();
            case DARK -> messages.labelThemeDark();
            case SEPIA -> messages.labelThemeSepia();
        };
    }

    public Theme next() {
        Theme[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
