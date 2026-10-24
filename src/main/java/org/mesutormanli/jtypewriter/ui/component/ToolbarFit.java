package org.mesutormanli.jtypewriter.ui.component;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure overflow policy of the toolbar (RW-15). Deliberately free of JavaFX
 * types - like {@link YoloPolicy} and {@link MainStage}'s intersection helper
 * it can be unit tested headlessly, without a toolkit or a display.
 *
 * <p><strong>The strategy.</strong> The toolbar stays a single row at every
 * width. A {@code FULL} row renders each dropdown as {@code "Theme: Dark"};
 * when that row no longer fits, the policy switches to {@code COMPACT}, which
 * drops the caption and renders just {@code "Dark"}. The five buttons (open,
 * save, undo, redo, about) are never collapsed - they are the priority
 * controls and always stay in row one together with all six dropdowns. A
 * {@link #MODE_HYSTERESIS_PX}-wide dead band keeps a window dragged right at
 * the threshold from flickering between the two modes.</p>
 */
public final class ToolbarFit {

    private ToolbarFit() {
    }

    /** Measure-only view of a text: how wide does it render at a font size. */
    @FunctionalInterface
    public interface TextMetrics {
        double widthOf(String text, double fontSizePx);
    }

    /** Which caption style the dropdowns render (see the class javadoc). */
    public enum LayoutMode {
        /** {@code "Theme: Dark"}-style captions inside the dropdowns. */
        FULL,
        /** Values only - captions collapse to free width at narrow windows. */
        COMPACT
    }

    // --- chrome constants, each traced to its source ------------------------

    /** Toolbar padding: {@code ToolbarView}'s {@code new Insets(8, 10, 8, 10)}, left + right. */
    public static final double TOOLBAR_PADDING_X = 10 + 10;

    /** Toolbar spacing: {@code ToolbarView}'s {@code setSpacing(3)}, one gap per child pair. */
    public static final double TOOLBAR_SPACING = 3;

    /**
     * {@code .toolbar-button} horizontal chrome: {@code -fx-padding: 4px 7px}
     * (14) plus the transparently-reserved focus-ring border, 2px left and
     * right ({@code base.css} {@code .toolbar-button} {@code -fx-border-width: 2px}).
     */
    public static final double BUTTON_CHROME = 7 + 7 + 2 + 2;

    /**
     * {@code .toolbar-combo} horizontal chrome, summed from {@code base.css}:
     * combo {@code -fx-padding: 1 1 1 4} (5) + closed cell
     * {@code -fx-padding: 2 4 2 3} (7) + arrow-button
     * {@code -fx-padding: 0 0 0 2} (2) + arrow glyph
     * {@code -fx-padding: 0 2.5px 0 2.5px} (5) + 2px border left and right (4).
     * Kept in sync with the CSS by the acceptance numbers of
     * {@code ToolbarFitTest} - if the toolbar rules move, the numbers move.
     */
    public static final double COMBO_CHROME = 5 + 7 + 2 + 5 + 4;

    /** The CSS toolbar font size: {@code base.css} sets 11px on {@code .toolbar-button}, {@code .toolbar-combo} and {@code .toolbar-combo .list-cell}. */
    public static final double TOOLBAR_FONT_SIZE_PX = 11;

    /**
     * Text widths measured with AWT undershoot JavaFX slightly (AWT SansSerif
     * at 11px measured 106px against JavaFX's 106.865px for the same string),
     * and CI runs with different system fonts than the reference desktop -
     * so every text width is scaled up by 8% and the fit decisions keep
     * enough slack that a font swap cannot flip the mode or clip a label.
     */
    public static final double TEXT_SAFETY_FACTOR = 1.08;

    /**
     * Dead band for the mode decision: a {@code COMPACT} row only switches
     * back to {@code FULL} once the window is this much wider than the full
     * row needs. Without it, dragging to the exact threshold flickers.
     */
    public static final double MODE_HYSTERESIS_PX = 40;

    /** The caption separator of the closed dropdowns, e.g. {@code "Theme: Dark"}. */
    public static final String CAPTION_SEPARATOR = ": ";

    /** One measured child of the toolbar row, with its chrome footprint. */
    public enum Kind {
        BUTTON(BUTTON_CHROME),
        COMBO(COMBO_CHROME),
        /** The hgrow spacer {@code Region}: a child, so it costs a gap, but no width. */
        SPACER(0);

        private final double chrome;

        Kind(double chrome) {
            this.chrome = chrome;
        }

        /** Horizontal non-text pixels this child always occupies. */
        public double chrome() {
            return chrome;
        }
    }

    /**
     * A toolbar child reduced to pure measurement data: every string it can
     * ever render (its closed text and, for a dropdown, every popup item -
     * the popup contributes to the preferred width too), plus its chrome.
     */
    public record Control(Kind kind, List<String> texts) {

        public Control {
            texts = List.copyOf(texts);
        }

        /** A button that always renders {@code text}. */
        public static Control button(String text) {
            return new Control(Kind.BUTTON, List.of(text));
        }

        /**
         * A dropdown: the popup always lists {@code values}; closed, each
         * value renders under {@code caption} ({@code "Theme: Dark"}) when a
         * caption is given, or bare in compact mode ({@code caption == null}).
         */
        public static Control combo(String caption, List<String> values) {
            var texts = new ArrayList<>(values);
            if (caption != null) {
                for (var value : values) {
                    texts.add(caption + CAPTION_SEPARATOR + value);
                }
            }
            return new Control(Kind.COMBO, texts);
        }

        /** The stretchable spacer between the settings dropdowns and About. */
        public static Control spacer() {
            return new Control(Kind.SPACER, List.of());
        }
    }

    // --- the decisions -------------------------------------------------------

    /**
     * The row's preferred width: toolbar padding, every child's widest text
     * (scaled by {@link #TEXT_SAFETY_FACTOR}) plus chrome, and one
     * {@link #TOOLBAR_SPACING} gap between each pair of children.
     */
    public static double requiredWidth(List<Control> row, double fontSizePx, TextMetrics metrics) {
        double width = TOOLBAR_PADDING_X;
        for (var control : row) {
            double text = 0;
            for (var candidate : control.texts()) {
                text = Math.max(text, metrics.widthOf(candidate, fontSizePx));
            }
            width += TEXT_SAFETY_FACTOR * text + control.kind().chrome() + TOOLBAR_SPACING;
        }
        return row.isEmpty() ? width : width - TOOLBAR_SPACING;
    }

    /** Whether the row fits into {@code availableWidth}. */
    public static boolean fits(double availableWidth, List<Control> row, double fontSizePx, TextMetrics metrics) {
        return requiredWidth(row, fontSizePx, metrics) <= availableWidth;
    }

    /**
     * Convenience view for a row of plain identically-chromed text labels -
     * each one gets the same {@code chromeWidth}.
     */
    public static double requiredWidth(List<String> labels, double fontSizePx, TextMetrics metrics,
                                       double chromeWidth) {
        double width = TOOLBAR_PADDING_X;
        for (var label : labels) {
            width += TEXT_SAFETY_FACTOR * metrics.widthOf(label, fontSizePx) + chromeWidth + TOOLBAR_SPACING;
        }
        return labels.isEmpty() ? width : width - TOOLBAR_SPACING;
    }

    /** Whether identically-chromed labels fit into {@code availableWidth}. */
    public static boolean fits(double availableWidth, List<String> labels, double fontSizePx,
                               TextMetrics metrics, double chromeWidth) {
        return requiredWidth(labels, fontSizePx, metrics, chromeWidth) <= availableWidth;
    }

    /**
     * The mode for a window of {@code availableWidth}. A {@code FULL} row is
     * kept while it fits; a {@code COMPACT} row only earns a switch back once
     * the window clears the full row's width by {@link #MODE_HYSTERESIS_PX}.
     * If even {@code COMPACT} cannot fit, it is still the answer - the
     * dropdowns shrink further and nothing in the policy asks them not to.
     */
    public static LayoutMode decide(double availableWidth, double requiredFullWidth, LayoutMode current) {
        double switchBackMargin = current == LayoutMode.FULL ? 0 : MODE_HYSTERESIS_PX;
        return requiredFullWidth <= availableWidth - switchBackMargin
                ? LayoutMode.FULL
                : LayoutMode.COMPACT;
    }

    /**
     * The real metrics supplier: AWT text layout, which is headless (no X11,
     * no JavaFX native libraries) and ships with the JDK. See
     * {@link #TEXT_SAFETY_FACTOR} for why callers must treat it as an
     * underestimate of the JavaFX scene graph.
     */
    public static double awtTextWidth(String text, double fontSizePx) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return new TextLayout(text, awtFont(fontSizePx), FRC).getAdvance();
    }

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private static Font awtFont(double fontSizePx) {
        return AWT_BASE_FONT.deriveFont((float) fontSizePx);
    }

    private static final Font AWT_BASE_FONT = Font.decode(Font.SANS_SERIF);
}
