package org.mesutormanli.jtypewriter.ui.theme;

import org.mesutormanli.jtypewriter.locale.Messages;

public enum TextColor {
    DEFAULT(null),
    SAGE("#7A9E7E"),
    LAVENDER("#8B7DAB"),
    SAND("#B8976A"),
    STEEL("#6B8FA8"),
    ROSE("#B07A7A");

    private final String cssColor;

    TextColor(String cssColor) {
        this.cssColor = cssColor;
    }

    public String getCssColor() {
        return cssColor;
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
