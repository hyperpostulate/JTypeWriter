/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.theme;

/**
 * WCAG 2.1 relative-luminance contrast maths, deliberately free of JavaFX so
 * the theme palettes can be audited headlessly (see {@code ContrastRatioTest},
 * which parses the real {@code /css/*.css} resources from the classpath).
 *
 * <p>Ratio follows the WCAG definition: {@code (L_light + 0.05) / (L_dark + 0.05)}
 * over sRGB relative luminance, so the result is symmetric in the two colours
 * and ranges from 1 (identical) to 21 (black on white).</p>
 *
 * <p>This class lives in test scope because only {@code ContrastRatioTest}
 * uses it; the production JAR must not ship a test helper.</p>
 */
public final class ContrastRatio {

    /** Minimum ratio for normal-size body text (WCAG 2.1 SC 1.4.3). */
    public static final double AA_NORMAL = 4.5;

    /** Minimum ratio for large text and for non-text UI components (1.4.11). */
    public static final double AA_LARGE = 3.0;

    private ContrastRatio() {
    }

    /**
     * Contrast ratio between two {@code #rrggbb} (or {@code #rgb}) hex colours.
     *
     * @param fgHex foreground hex colour, leading {@code #} optional
     * @param bgHex background hex colour, leading {@code #} optional
     * @return the WCAG contrast ratio, from 1.0 to 21.0
     * @throws IllegalArgumentException if either string is not a hex colour
     */
    public static double ratio(String fgHex, String bgHex) {
        var lighter = Math.max(luminance(fgHex), luminance(bgHex));
        var darker = Math.min(luminance(fgHex), luminance(bgHex));
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Whether a ratio clears the WCAG AA threshold.
     *
     * @param ratio     a ratio from {@link #ratio(String, String)}
     * @param largeText {@code true} for large text (>= 18.66 px bold / 24 px)
     *                  or non-text UI components such as focus rings, which
     *                  need 3:1; {@code false} for normal text, which needs 4.5:1
     */
    public static boolean meetsAa(double ratio, boolean largeText) {
        return ratio >= (largeText ? AA_LARGE : AA_NORMAL);
    }

    /** WCAG relative luminance of a hex colour, 0.0 (black) to 1.0 (white). */
    static double luminance(String hex) {
        var digits = hexDigits(hex);
        var r = channelValue(digits, 0) / 255.0;
        var g = channelValue(digits, 1) / 255.0;
        var b = channelValue(digits, 2) / 255.0;
        return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b);
    }

    private static String hexDigits(String hex) {
        var value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        // Short form #rgb expands to #rrggbb, exactly like CSS does.
        if (value.length() == 3) {
            var expanded = new StringBuilder(6);
            for (var i = 0; i < 3; i++) {
                expanded.append(value.charAt(i)).append(value.charAt(i));
            }
            value = expanded.toString();
        }
        if (value.length() != 6) {
            throw new IllegalArgumentException("not a hex colour: " + hex);
        }
        // Character.digit is locale-independent, unlike toLowerCase(): under
        // the Turkish locale a hex "I" would... there is no "I" in hex, but a
        // stray bad character must throw, not silently fall back (AGENTS.md).
        for (var i = 0; i < 6; i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                throw new IllegalArgumentException("not a hex colour: " + hex);
            }
        }
        return value;
    }

    private static int channelValue(String digits, int index) {
        return Character.digit(digits.charAt(index * 2), 16) * 16
                + Character.digit(digits.charAt(index * 2 + 1), 16);
    }

    private static double linearize(double channel) {
        return channel <= 0.03928
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
