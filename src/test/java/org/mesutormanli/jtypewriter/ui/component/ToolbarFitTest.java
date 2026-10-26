/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.component.ToolbarFit.LayoutMode;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overflow policy, pinned headlessly: the width model (text plus
 * documented chrome plus padding and spacing), the FULL/COMPACT decision
 * including its hysteresis, and the measured numbers - the real German and
 * French bundle labels must not clip at 1280 px and must fit 900 px once the
 * dropdown captions collapse.
 */
class ToolbarFitTest {

    @TempDir
    Path dir;

    /** Deterministic stand-in for the font engine: 0.6 px per char per 10 px of size. */
    private static final ToolbarFit.TextMetrics FAKE_METRICS =
            (text, fontSizePx) -> text.length() * fontSizePx * 0.06;

    private static final double FONT = 10;

    // --- the width model ------------------------------------------------------

    @Test
    void requiredWidthAddsPaddingTextChromeAndGaps() {
        // two 5-char buttons: 10 px text each at the fake metrics, +8% safety
        var row = List.of(ToolbarFit.Control.button("abcde"), ToolbarFit.Control.button("abcde"));
        double expected = ToolbarFit.TOOLBAR_PADDING_X
                + 2 * (ToolbarFit.TEXT_SAFETY_FACTOR * 3 + ToolbarFit.BUTTON_CHROME)
                + ToolbarFit.TOOLBAR_SPACING;
        assertEquals(expected,
                ToolbarFit.requiredWidth(row, FONT, FAKE_METRICS), 0.001);
    }

    @Test
    void aComboIsMeasuredByItsWidestStateCaptionedOrNot() {
        // popup value is tiny but the captioned form dominates; in compact
        // mode the same dropdown is only as wide as its widest popup item
        var captioned = ToolbarFit.Control.combo("Theme", List.of("Light", "Sepia"));
        var compact = ToolbarFit.Control.combo(null, List.of("Light", "Sepia"));
        double captionedWidth = ToolbarFit.requiredWidth(List.of(captioned), FONT, FAKE_METRICS);
        double compactWidth = ToolbarFit.requiredWidth(List.of(compact), FONT, FAKE_METRICS);
        double saving = captionedWidth - compactWidth;
        // "Theme: Sepia" (12) vs "Sepia" (5) -> 7 chars of fake text
        assertEquals(ToolbarFit.TEXT_SAFETY_FACTOR * 7 * FONT * 0.06, saving, 0.001);
    }

    @Test
    void spacerCostsAGapButNoWidth() {
        double without = ToolbarFit.requiredWidth(
                List.of(ToolbarFit.Control.button("ab")), FONT, FAKE_METRICS);
        double with = ToolbarFit.requiredWidth(
                List.of(ToolbarFit.Control.button("ab"), ToolbarFit.Control.spacer()),
                FONT, FAKE_METRICS);
        assertEquals(without + ToolbarFit.TOOLBAR_SPACING, with, 0.001);
    }

    @Test
    void flatLabelOverloadMatchesUniformChromeControls() {
        var labels = List.of("Open", "Save", "Undo");
        assertEquals(
                ToolbarFit.requiredWidth(labels.stream()
                        .map(ToolbarFit.Control::button).toList(), FONT, FAKE_METRICS),
                ToolbarFit.requiredWidth(labels, FONT, FAKE_METRICS, ToolbarFit.BUTTON_CHROME),
                0.001);
    }

    // --- the decision ----------------------------------------------------------

    @Test
    void fullRowIsKeptWhileItFitsAndDroppedAsSoonAsItDoesNot() {
        double required = 1000;
        assertEquals(LayoutMode.FULL, ToolbarFit.decide(1000, required, LayoutMode.FULL));
        assertEquals(LayoutMode.COMPACT, ToolbarFit.decide(999.9, required, LayoutMode.FULL));
    }

    @Test
    void hysteresisKeepsCompactUntilTheWindowClearlyFitsFullAgain() {
        double required = 1000;
        // just crossing back is not enough - it flickers at the threshold
        assertEquals(LayoutMode.COMPACT,
                ToolbarFit.decide(1000, required, LayoutMode.COMPACT));
        assertEquals(LayoutMode.COMPACT, ToolbarFit.decide(
                required + ToolbarFit.MODE_HYSTERESIS_PX - 0.1, required, LayoutMode.COMPACT));
        assertEquals(LayoutMode.FULL, ToolbarFit.decide(
                required + ToolbarFit.MODE_HYSTERESIS_PX, required, LayoutMode.COMPACT));
    }

    @Test
    void fitsIsTheSameQuestionAtTheExactBoundary() {
        var row = List.of(ToolbarFit.Control.button("abcde"));
        double required = ToolbarFit.requiredWidth(row, FONT, FAKE_METRICS);
        assertTrue(ToolbarFit.fits(required, row, FONT, FAKE_METRICS));
        assertFalse(ToolbarFit.fits(required - 0.01, row, FONT, FAKE_METRICS));
    }

    // --- the real metrics --------------------------------------------------------

    @Test
    void awtMetricsAreSaneWithoutAToolkit() {
        assertEquals(0, ToolbarFit.awtTextWidth("", ToolbarFit.TOOLBAR_FONT_SIZE_PX));
        double shortness = ToolbarFit.awtTextWidth("Open", ToolbarFit.TOOLBAR_FONT_SIZE_PX);
        double length = ToolbarFit.awtTextWidth("Open Open Open", ToolbarFit.TOOLBAR_FONT_SIZE_PX);
        assertTrue(shortness > 10 && shortness < 100, "unexpected: " + shortness);
        assertTrue(length > 2 * shortness);
    }

    // --- the measured numbers (real bundles, real font engine) --------------------

    /** Headroom the measured rows must keep below their width bound. */
    private static final double HEADROOM_PX = 64;

    /**
     * First half: at 1280 px the German and French rows are
     * rendered FULL (fully captioned) and must not clip - with headroom, so
     * a system font wider than the measured one cannot break the promise.
     */
    @Test
    void germanAndFrenchFullRowsAreNotClippedAt1280() {
        for (var language : List.of(Language.DE, Language.FR)) {
            double full = requiredRow(language, false);
            assertTrue(full <= 1280 - HEADROOM_PX,
                    language + " full row needs " + round(full) + " px");
        }
    }

    /**
     * Second half: at 900 px nothing overflows - with the
     * captions collapsed, every control that stays in row one (all of them:
     * the five buttons plus all six dropdowns) fits, with headroom. No
     * language may overflow 900 px in its compact form either.
     */
    @Test
    void rowOneWithCollapsedCaptionsFits900InEveryLanguage() {
        for (var language : Language.values()) {
            double compact = requiredRow(language, true);
            double bound = (language == Language.DE || language == Language.FR)
                    ? 900 - HEADROOM_PX
                    : 900;
            assertTrue(compact <= bound,
                    language + " compact row needs " + round(compact) + " px");
        }
    }

    /**
     * The degradation must pay for more than the hysteresis band, or a window
     * parked at the threshold would oscillate between the modes on every
     * refresh: collapse has to free at least a band's worth of width.
     */
    @Test
    void collapsingCaptionsFreesMoreThanTheHysteresisBandInEveryLanguage() {
        for (var language : Language.values()) {
            double saving = requiredRow(language, false) - requiredRow(language, true);
            assertTrue(saving > ToolbarFit.MODE_HYSTERESIS_PX + HEADROOM_PX / 2,
                    language + " collapse only frees " + round(saving) + " px");
        }
    }

    /** Both modes measure the same row shape - collapse changes captions, not the set of controls. */
    @Test
    void bothModesMeasureTheSameSetOfControls() {
        assertEquals(
                ToolbarView.fitControls(messagesFor(Language.EN), true).size(),
                ToolbarView.fitControls(messagesFor(Language.EN), false).size());
    }

    private double requiredRow(Language language, boolean compact) {
        return ToolbarFit.requiredWidth(
                ToolbarView.fitControls(messagesFor(language), compact),
                ToolbarFit.TOOLBAR_FONT_SIZE_PX,
                ToolbarFit::awtTextWidth);
    }

    private static String round(double px) {
        return String.format("%.1f", px);
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("fit-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }
}
