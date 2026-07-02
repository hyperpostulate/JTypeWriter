package org.mesutormanli.jtypewriter.ui.theme;

import org.mesutormanli.jtypewriter.locale.Language;

public enum Theme {
    LIGHT("css/light.css"),
    DARK("css/dark.css"),
    SEPIA("css/sepia.css");

    private final String cssPath;

    Theme(String cssPath) {
        this.cssPath = cssPath;
    }

    public String getCssPath() {
        return cssPath;
    }

    public String getDisplayName(Language language) {
        return switch (this) {
            case LIGHT -> language == Language.EN ? "Light" : "Açık";
            case DARK -> language == Language.EN ? "Dark" : "Koyu";
            case SEPIA -> language == Language.EN ? "Sepia" : "Sepya";
        };
    }

    public Theme next() {
        Theme[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
