/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.theme;

import org.mesutormanli.jtypewriter.locale.Messages;

/**
 * Editor text colours. Each colour carries two hex values: {@code darkCssColor}
 * is used on {@link Theme#DARK}, {@code brightCssColor} on the light editor
 * surfaces ({@link Theme#LIGHT}, {@link Theme#SEPIA}). One hex cannot clear the
 * WCAG AA 4.5:1 bar on both a near-black (#1a1a1a) and a near-white (#ffffff /
 * #f4ecd8) canvas, so the palette is theme-dependent by design.
 *
 * <p>The second value keeps the hue family of the first, so the colour still
 * reads as sage/lavender/sand/steel/rose. Measured WCAG ratios are recorded
 * against #ffffff / #f4ecd8 (light/sepia) and #1a1a1a (dark) and are pinned by
 * {@code ContrastRatioTest}.
 */
public enum TextColor {
    /** The theme's own editor text colour - no inline override in either theme. */
    DEFAULT(null, null),
    SAGE("#7A9E7E", "#4D6B50"),       // dark 5.82, light 5.94, sepia 5.05
    LAVENDER("#8B7DAB", "#5F5278"),   // dark 4.66, light 7.08, sepia 6.02
    SAND("#B8976A", "#7A6238"),       // dark 6.36, light 5.78, sepia 4.91
    STEEL("#6B8FA8", "#3D5F78"),      // dark 5.07, light 6.76, sepia 5.74
    ROSE("#B07A7A", "#8A4F4F");       // dark 4.90, light 6.34, sepia 5.39

    private final String darkCssColor;
    private final String brightCssColor;

    TextColor(String darkCssColor, String brightCssColor) {
        this.darkCssColor = darkCssColor;
        this.brightCssColor = brightCssColor;
    }

    /**
     * The hex value for the given theme: the historical colour on the dark
     * canvas, the deepened one on light and sepia. {@code null} for
     * {@link #DEFAULT} (and for a {@code null} theme), meaning the theme's own
     * editor text colour applies.
     */
    public String getCssColor(Theme theme) {
        if (theme == null) {
            return null;
        }
        return switch (theme) {
            case DARK -> darkCssColor;
            case LIGHT, SEPIA -> brightCssColor;
        };
    }

    /** Localised colour name for the current UI language (bundle key {@code color*}). */
    public String getDisplayName(Messages messages) {
        return switch (this) {
            case DEFAULT -> messages.colorDefault();
            case SAGE -> messages.colorSage();
            case LAVENDER -> messages.colorLavender();
            case SAND -> messages.colorSand();
            case STEEL -> messages.colorSteel();
            case ROSE -> messages.colorRose();
        };
    }

    public TextColor next() {
        TextColor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
