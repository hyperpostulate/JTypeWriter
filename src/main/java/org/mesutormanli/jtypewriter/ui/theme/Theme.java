/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.theme;

import org.mesutormanli.jtypewriter.locale.Messages;

public enum Theme {
    LIGHT("css/light.css", "#f8f6f2", "#ffffff"),
    DARK("css/dark.css", "#252526", "#1a1a1a"),
    SEPIA("css/sepia.css", "#efe6d4", "#f4ecd8");

    private final String cssPath;

    /**
     * Surface colours mirrored from the CSS ({@code -welcome-bg} and the
     * {@code .editor-area} background). Opaque borderless windows paint their
     * scene with these so every anti-aliased edge blends into an identical
     * colour and can never form a white fringe or flash.
     */
    private final String dialogFill;
    private final String editorFill;

    Theme(String cssPath, String dialogFill, String editorFill) {
        this.cssPath = cssPath;
        this.dialogFill = dialogFill;
        this.editorFill = editorFill;
    }

    public String getCssPath() {
        return cssPath;
    }

    /** Matches {@code -welcome-bg}: dialog surface and scene fill colour. */
    public String getDialogFill() {
        return dialogFill;
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
