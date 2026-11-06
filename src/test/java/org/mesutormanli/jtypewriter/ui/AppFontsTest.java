/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.component.ToolbarFit;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled UI font is a release asset, not a system dependency: both faces
 * must be on the classpath, report the family the stylesheet names, cover every
 * label the six languages render, and measure a pinned string to the pinned
 * width. That last assertion is the point of the whole exercise - the toolbar
 * fit decision must produce the same number on every host, so a silent
 * fallback to a system font has to fail here rather than in CI.
 */
class AppFontsTest {

    @TempDir
    Path dir;

    @Test
    void bothFacesAreOnTheClasspathAndReportTheBundledFamily()
            throws IOException, FontFormatException {
        for (var resource : AppFonts.RESOURCES) {
            try (InputStream stream = AppFontsTest.class.getResourceAsStream(resource)) {
                assertNotNull(stream, resource + " is not on the classpath");
                var font = Font.createFont(Font.TRUETYPE_FONT, stream);
                assertEquals(AppFonts.FAMILY, font.getFamily(),
                        resource + " is not the bundled family");
            }
        }
    }

    /** A pinned width: a different font file, or a system alias, cannot match it. */
    @Test
    void theMeasuredFaceIsTheBundledFileNotASystemAlias() {
        assertEquals(AppFonts.FAMILY, AppFonts.awt(ToolbarFit.TOOLBAR_FONT_SIZE_PX).getFamily());
        assertEquals(30.7979, ToolbarFit.awtTextWidth("Ouvrir", 11), 0.5);
        assertEquals(48.9468, ToolbarFit.awtTextWidth("D\u00e9sactiv\u00e9", 11), 0.5);
        assertEquals(8.8730, ToolbarFit.awtTextWidth(MainMenu.GLYPH, 11), 0.5);
    }

    /**
     * The toolbar is measured in the bundled face, so every string it measures
     * must exist in that face - otherwise the width comes from one font and
     * the pixels from another.
     */
    @Test
    void everyToolbarLabelRendersInTheMeasuredFace() {
        for (var language : Language.values()) {
            var messages = messagesFor(language);
            for (var compact : new boolean[]{false, true}) {
                for (var control : ToolbarView.fitControls(messages, compact)) {
                    for (var text : control.texts()) {
                        assertTrue(AppFonts.canDisplay(text),
                                language + " label is missing from " + AppFonts.FAMILY + ": "
                                        + text);
                    }
                }
            }
        }
    }

    /** The stylesheet and the measurement must name the same family. */
    @Test
    void theStylesheetNamesTheBundledFamily() throws IOException {
        String css;
        try (InputStream stream = AppFontsTest.class.getResourceAsStream("/css/base.css")) {
            assertNotNull(stream, "/css/base.css is not on the classpath");
            css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(css.contains("-fx-font-family: \"" + AppFonts.FAMILY + "\""),
                "base.css does not name the bundled font family " + AppFonts.FAMILY);
        assertFalse(css.contains("SansSerif"),
                "base.css still names the system SansSerif alias");
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("font-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }
}
