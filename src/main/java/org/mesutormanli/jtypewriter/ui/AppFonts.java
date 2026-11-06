/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The UI chrome font that ships with the application, and the metres for it.
 *
 * <p><strong>Why a bundled font instead of the system one.</strong> The
 * toolbar measures its labels before deciding whether the captions fit (see
 * {@code ToolbarFit}), and that decision has to produce the same number on
 * every machine. A system {@code SansSerif} does not: the same build measured
 * 817 px for the French compact row on one runner and 851 px on another,
 * because each resolved the alias to a different font file. Bundling
 * DejaVu Sans Condensed makes the measurement a property of the release, not
 * of the host, so a fit test that passes here passes in CI.</p>
 *
 * <p>This class is deliberately AWT-only and loads nothing until asked, so
 * {@code ToolbarFit} can measure headlessly - no toolkit, no display. The
 * JavaFX half lives in {@code FxApplication}, which registers the same files
 * with the scene graph before the first {@code Scene} exists; the CSS then
 * names {@link #FAMILY} so rendering and measuring read the same bytes.</p>
 */
public final class AppFonts {

    /**
     * Family name of the bundled files. It is what {@code -fx-font-family}
     * names in {@code base.css}; changing a file means changing this constant,
     * the CSS and the provenance note together.
     */
    public static final String FAMILY = "DejaVu Sans Condensed";

    /** Regular face: UI text, and the font every toolbar measurement uses. */
    public static final String REGULAR_RESOURCE = "/fonts/DejaVuSansCondensed.ttf";

    /** Bold face: the chrome's {@code -fx-font-weight: 700/800} headings. */
    public static final String BOLD_RESOURCE = "/fonts/DejaVuSansCondensed-Bold.ttf";

    /** Every file that has to register for the CSS family to resolve. */
    public static final List<String> RESOURCES = List.of(REGULAR_RESOURCE, BOLD_RESOURCE);

    private AppFonts() {
    }

    /**
     * The bundled regular face at {@code sizePx}, for AWT text layout. Throws
     * an {@link IllegalStateException} rather than falling back to a system
     * font: a silent fallback is exactly the non-determinism this class
     * exists to remove.
     */
    public static Font awt(double sizePx) {
        return Loaded.REGULAR.deriveFont((float) sizePx);
    }

    /**
     * Whether every code point in {@code text} exists in the bundled face.
     * The toolbar is measured in this font, so a label it cannot render would
     * be measured from a different face than the one on screen.
     */
    public static boolean canDisplay(String text) {
        return text != null && Loaded.REGULAR.canDisplayUpTo(text) < 0;
    }

    /** Lazy holder: one classloader read per face, never a partial font. */
    private static final class Loaded {
        static final Font REGULAR = load(REGULAR_RESOURCE);

        private static Font load(String resource) {
            try (InputStream stream = AppFonts.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    throw new IllegalStateException("bundled font " + resource
                            + " is not on the classpath");
                }
                return Font.createFont(Font.TRUETYPE_FONT, stream);
            } catch (FontFormatException | IOException error) {
                throw new IllegalStateException("bundled font " + resource
                        + " cannot be read", error);
            }
        }
    }
}
