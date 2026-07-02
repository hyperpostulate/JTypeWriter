package org.mesutormanli.jtypewriter.ui.theme;

public enum TextColor {
    DEFAULT(null, "Default"),
    SAGE("#7A9E7E", "Sage"),
    LAVENDER("#8B7DAB", "Lavender"),
    SAND("#B8976A", "Sand"),
    STEEL("#6B8FA8", "Steel"),
    ROSE("#B07A7A", "Rose");

    private final String cssColor;
    private final String displayName;

    TextColor(String cssColor, String displayName) {
        this.cssColor = cssColor;
        this.displayName = displayName;
    }

    public String getCssColor() {
        return cssColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextColor next() {
        TextColor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
