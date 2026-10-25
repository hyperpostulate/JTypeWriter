package org.mesutormanli.jtypewriter.ui.theme;

import org.junit.jupiter.api.Test;
import org.mesutormanli.jtypewriter.ui.component.StatsBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RW-17 "measure, don't assert": the contrast numbers are not prose in a
 * roadmap, they are recomputed here from the real {@code /css/*.css}
 * resources on the classpath. Edit a theme colour back below WCAG AA and
 * this suite goes red, in any locale.
 */
class ContrastRatioTest {

    private static final String[] THEMES = {"light", "dark", "sepia"};

    /** WCAG AA thresholds used by the product: text 4.5:1, UI components 3:1. */
    private static final double TEXT_MIN = 4.5;
    private static final double COMPONENT_MIN = 3.0;

    // --- The maths itself ----------------------------------------------------

    @Test
    void blackOnWhiteIsTheMaximumTwentyOne() {
        assertEquals(21.0, ContrastRatio.ratio("#000000", "#ffffff"), 0.01);
        assertEquals(21.0, ContrastRatio.ratio("000000", "ffffff"), 0.01);
    }

    @Test
    void identicalColorsAreTheMinimumOne() {
        assertEquals(1.0, ContrastRatio.ratio("#8b7355", "#8b7355"), 0.001);
    }

    @Test
    void theRatioIsSymmetricAndAcceptsShortHex() {
        var forward = ContrastRatio.ratio("#777777", "#1e1e1e");
        var backward = ContrastRatio.ratio("#1e1e1e", "#777777");
        assertEquals(forward, backward, 0.0001);
        assertEquals(forward, ContrastRatio.ratio("#777", "#1e1e1e"), 0.0001);
        assertEquals(forward, ContrastRatio.ratio("#777777", "#1E1E1E"), 0.0001);
    }

    /** The 4.49/4.51 boundary: normal text must clear exactly 4.5, not "about 4.5". */
    @Test
    void meetsAaSitsExactlyOnTheThresholds() {
        assertFalse(ContrastRatio.meetsAa(4.49, false));
        assertTrue(ContrastRatio.meetsAa(4.50, false));
        assertTrue(ContrastRatio.meetsAa(4.51, false));
        // Large text / non-text UI components (focus rings) sit on 3.0.
        assertFalse(ContrastRatio.meetsAa(2.99, true));
        assertTrue(ContrastRatio.meetsAa(3.00, true));
    }

    @Test
    void malformedHexThrowsInsteadOfFallingBack() {
        assertThrows(IllegalArgumentException.class, () -> ContrastRatio.ratio("#12345", "#ffffff"));
        assertThrows(IllegalArgumentException.class, () -> ContrastRatio.ratio("zzzzzz", "#ffffff"));
    }

    // --- The real palettes, parsed from the shipped CSS ------------------------

    /** RW-17 acceptance: "durum metni >= 13 px" in all three themes. */
    @Test
    void statusTextClearsFourPointFiveAgainstTheBarBackgroundInEveryTheme() {
        for (var theme : THEMES) {
            var css = readCss("/css/" + theme + ".css");
            var text = color(css, "-stats-text");
            var bar = color(css, "-toolbar-bg");
            var ratio = ContrastRatio.ratio(text, bar);
            assertTrue(ContrastRatio.meetsAa(ratio, false),
                    "%s -stats-text %s on %s is %.2f:1, below AA %s:1"
                            .formatted(theme, text, bar, ratio, TEXT_MIN));
        }
    }

    /** The "Saved hh:mm" confirmation (RW-19). It used to render in Modena's
        default #333333 on the dark bar: 1.32:1, invisible (analysis U1). */
    @Test
    void saveStatusTextClearsFourPointFiveInEveryTheme() {
        for (var theme : THEMES) {
            var css = readCss("/css/" + theme + ".css");
            var save = color(css, "-stats-save-text");
            var bar = color(css, "-toolbar-bg");
            var ratio = ContrastRatio.ratio(save, bar);
            assertTrue(ContrastRatio.meetsAa(ratio, false),
                    "%s -stats-save-text %s on %s is %.2f:1, below AA %s:1"
                            .formatted(theme, save, bar, ratio, TEXT_MIN));
        }
    }

    /** A focus ring is a non-text UI component: >= 3:1 against every surface
        it can sit on (bar, editor, hover wash) in every theme. */
    @Test
    void focusRingClearsThreeAgainstEveryAdjacentSurfaceInEveryTheme() {
        for (var theme : THEMES) {
            var css = readCss("/css/" + theme + ".css");
            var ring = color(css, "-focus-ring");
            for (var surface : new String[]{"-toolbar-bg", "-editor-bg", "-toolbar-hover"}) {
                var adjacent = color(css, surface);
                var ratio = ContrastRatio.ratio(ring, adjacent);
                assertTrue(ContrastRatio.meetsAa(ratio, true),
                        "%s -focus-ring %s against %s (%s) is %.2f:1, below 3:1"
                                .formatted(theme, ring, surface, adjacent, ratio));
            }
        }
    }

    /** The keyboard-highlighted popup row paints text on the ring colour;
        that pairing must read as body text, so it needs the 4.5:1 bar. */
    @Test
    void onRingTextClearsFourPointFiveInEveryTheme() {
        for (var theme : THEMES) {
            var css = readCss("/css/" + theme + ".css");
            var ring = color(css, "-focus-ring");
            var onRing = color(css, "-focus-ring-text");
            var ratio = ContrastRatio.ratio(onRing, ring);
            assertTrue(ContrastRatio.meetsAa(ratio, false),
                    "%s %s on -focus-ring %s is %.2f:1, below AA"
                            .formatted(theme, onRing, ring, ratio));
        }
    }

    /** The roadmap acceptance criterion verbatim: base.css must not smuggle
        `-fx-focus-color: transparent` back in. */
    @Test
    void baseCssNeverMakesTheFocusColorTransparent() {
        var base = readCss("/css/base.css");
        assertFalse(Pattern.compile("-fx-focus-color:\\s*transparent").matcher(base).find(),
                "base.css kills the focus colour again (RW-17 regression)");
    }

    /** Status text ships at >= 13 px from both sides of the CSS/Java seam. */
    @Test
    void statusBarDefaultsToAtLeastThirteenPixels() {
        assertTrue(StatsBar.defaultStatusFontSizePx() >= 13,
                "StatsBar default size dropped below 13 px");
        var base = readCss("/css/base.css");
        assertTrue(cssFontSize(base, "\\.stats-clock\\s*\\{") >= 13);
        assertTrue(cssFontSize(base, "\\.stats-label\\s*\\{") >= 13);
        assertTrue(cssFontSize(base, "\\.stats-save\\s*\\{") >= 13);
    }

    /** Open dropdown list and closed control share one type scale (11 px). */
    @Test
    void popupCellsMatchTheClosedComboFontSize() {
        var base = readCss("/css/base.css");
        var closed = cssFontSize(base, "\\.toolbar-combo \\.list-cell\\s*\\{");
        var open = cssFontSize(base, "\\.combo-box-popup \\.list-view \\.list-cell\\s*\\{");
        assertEquals(closed, open, "popup cell size drifted from the closed control");
    }

    // --- Tiny CSS readers ------------------------------------------------------

    private static String readCss(String resource) {
        try (InputStream in = ContrastRatioTest.class.getResourceAsStream(resource)) {
            assertTrue(in != null, "missing CSS resource on classpath: " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** First {@code -name: #rrggbb;} declaration in the sheet. */
    private static String color(String css, String name) {
        var matcher = Pattern.compile(Pattern.quote(name) + ":\\s*(#[0-9a-fA-F]{3,6})\\s*;")
                .matcher(css);
        assertTrue(matcher.find(), "no " + name + " declaration found in CSS");
        return matcher.group(1);
    }

    /** The -fx-font-size inside the first rule whose selector matches. */
    private static int cssFontSize(String css, String selectorRegex) {
        var matcher = Pattern
                .compile(selectorRegex + "[^}]*-fx-font-size:\\s*(\\d+)px")
                .matcher(css);
        assertTrue(matcher.find(), "no font-size found for selector " + selectorRegex);
        return Integer.parseInt(matcher.group(1));
    }
}
