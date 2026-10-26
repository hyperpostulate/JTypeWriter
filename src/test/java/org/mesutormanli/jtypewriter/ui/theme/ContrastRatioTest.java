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
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /** PW-15 (analysis U6): the welcome call-to-action label on both its
        resting and hover surfaces. In the dark theme the fix is the label
        colour (dark-on-amber), in the others the surface itself. */
    @Test
    void welcomeCallToActionClearsFourPointFiveInEveryTheme() {
        for (var theme : THEMES) {
            var css = readCss("/css/" + theme + ".css");
            var label = color(css, "-welcome-button-text");
            for (var surface : new String[]{"-welcome-button-bg", "-welcome-button-hover"}) {
                var background = color(css, surface);
                var ratio = ContrastRatio.ratio(label, background);
                assertTrue(ContrastRatio.meetsAa(ratio, false),
                        "%s -welcome-button-text %s on %s (%s) is %.2f:1, below AA %s:1"
                                .formatted(theme, label, surface, background, ratio, TEXT_MIN));
            }
        }
    }

    /** The About e-mail Hyperlink takes its text fill from -welcome-button-bg,
        which must therefore read as body text on the dialog surface (U6). */
    @Test
    void aboutEmailLinkClearsFourPointFiveInEveryTheme() {
        for (var theme : THEMES) {
            var css = readCss("/css/" + theme + ".css");
            var link = color(css, "-welcome-button-bg");
            var surface = color(css, "-welcome-bg");
            var ratio = ContrastRatio.ratio(link, surface);
            assertTrue(ContrastRatio.meetsAa(ratio, false),
                    "%s -welcome-button-bg %s as e-mail link on -welcome-bg %s is %.2f:1, below AA %s:1"
                            .formatted(theme, link, surface, ratio, TEXT_MIN));
        }
    }

    /** The welcome tagline uses -welcome-muted on the dialog surface (U6). */
    @Test
    void welcomeTaglineClearsFourPointFiveInEveryTheme() {
        for (var theme : THEMES) {
            var css = readCss("/css/" + theme + ".css");
            var tagline = color(css, "-welcome-muted");
            var surface = color(css, "-welcome-bg");
            var ratio = ContrastRatio.ratio(tagline, surface);
            assertTrue(ContrastRatio.meetsAa(ratio, false),
                    "%s -welcome-muted %s on -welcome-bg %s is %.2f:1, below AA %s:1"
                            .formatted(theme, tagline, surface, ratio, TEXT_MIN));
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

    /** U17: {@code -fx-letter-spacing} and {@code -fx-transition} do not exist
        in JavaFX CSS, so they are silently ignored while looking like working
        styling. No shipped sheet may carry them again. */
    @Test
    void noIgnoredJavaFxCssPropertiesInAnyShippedSheet() {
        var ignored = Pattern.compile("-fx-(letter-spacing|transition)\\s*:", Pattern.CASE_INSENSITIVE);
        for (var sheet : new String[]{"/css/base.css", "/css/light.css", "/css/dark.css", "/css/sepia.css"}) {
            var matcher = ignored.matcher(readCss(sheet));
            assertFalse(matcher.find(),
                    sheet + " declares an ignored JavaFX CSS property (U17 regression)");
        }
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

    /** PW-07: each {@link TextColor} carries a dark-canvas and a light-canvas
        hex, because a single value cannot clear 4.5:1 on both #1a1a1a and
        #ffffff / #f4ecd8. This recomputes every colour/theme pair, so a
        regression to the old inline constants goes red. */
    @Test
    void textColoursClearFourPointFiveOnEveryEditorBackground() {
        for (var color : TextColor.values()) {
            if (color == TextColor.DEFAULT) {
                continue; // the theme's own editor text colour, no inline hex
            }
            for (var theme : Theme.values()) {
                var hex = color.getCssColor(theme);
                var background = theme.getEditorFill();
                var ratio = ContrastRatio.ratio(hex, background);
                assertTrue(ContrastRatio.meetsAa(ratio, false),
                        "%s %s on %s %s is %.2f:1, below AA %s:1"
                                .formatted(color, hex, theme, background, ratio, TEXT_MIN));
            }
        }
    }

    /** PW-07 acceptance: "koyu tema değişmez" - the dark palette keeps the
        historical hexes, pinned so a future lightening/darkening cannot slip in. */
    @Test
    void darkThemeTextColoursAreUnchanged() {
        assertEquals("#7A9E7E", TextColor.SAGE.getCssColor(Theme.DARK));
        assertEquals("#8B7DAB", TextColor.LAVENDER.getCssColor(Theme.DARK));
        assertEquals("#B8976A", TextColor.SAND.getCssColor(Theme.DARK));
        assertEquals("#6B8FA8", TextColor.STEEL.getCssColor(Theme.DARK));
        assertEquals("#B07A7A", TextColor.ROSE.getCssColor(Theme.DARK));
        assertNull(TextColor.DEFAULT.getCssColor(Theme.DARK));
        assertNull(TextColor.DEFAULT.getCssColor(Theme.LIGHT));
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
