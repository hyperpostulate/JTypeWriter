/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import org.junit.jupiter.api.Test;
import org.mesutormanli.jtypewriter.ui.AppFonts;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The font parser answers only what the PDF writer asks of it. It is tested
 * against the bundled file itself, because that file is what the export embeds
 * and any disagreement here would show up as misplaced or missing glyphs in the
 * generated document.
 */
class TrueTypeFontTest {

    @Test
    void readsTheBundledFace() throws IOException {
        TrueTypeFont font = new TrueTypeFont(bundledFont());

        assertEquals(2048, font.unitsPerEm());
        assertTrue(font.numGlyphs() > 0);
        assertTrue(font.ascent() > 0);
    }

    @Test
    void mapsLatinAndTurkishCodePointsToRealGlyphs() throws IOException {
        TrueTypeFont font = new TrueTypeFont(bundledFont());

        assertNotEquals(0, font.glyphId('A'));
        assertNotEquals(0, font.glyphId('ş'));
    }

    @Test
    void theFaceIsMonospaceSoIAndWHaveTheSameAdvance() throws IOException {
        TrueTypeFont font = new TrueTypeFont(bundledFont());

        assertEquals(font.advanceWidth(font.glyphId('i')),
                font.advanceWidth(font.glyphId('W')));
    }

    @Test
    void nonFontBytesAreRejected() {
        assertThrows(IllegalStateException.class,
                () -> new TrueTypeFont("this is not a font".getBytes()));
        assertThrows(IllegalStateException.class,
                () -> new TrueTypeFont(new byte[0]));
    }

    private static byte[] bundledFont() throws IOException {
        try (InputStream stream = TrueTypeFontTest.class
                .getResourceAsStream(AppFonts.MONO_RESOURCE)) {
            return stream.readAllBytes();
        }
    }
}
