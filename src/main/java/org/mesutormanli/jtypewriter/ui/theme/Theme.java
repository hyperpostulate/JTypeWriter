package org.mesutormanli.jtypewriter.ui.theme;

import org.mesutormanli.jtypewriter.locale.Language;

public enum Theme {
    LIGHT("css/light.css", "#f8f6f2", "#ffffff"),
    DARK("css/dark.css", "#252526", "#1a1a1a"),
    SEPIA("css/sepia.css", "#efe6d4", "#f4ecd8");

    private final String cssPath;

    /**
     * Surface colours mirrored from the CSS ({@code -welcome-bg} and the
     * {@code .editor-area} background). Opaque borderless windows paint their
     * scene with these so every anti-aliased edge blends into an identical
     * colour and can never form a white fringe or flash.
     */
    private final String dialogFill;
    private final String editorFill;

    Theme(String cssPath, String dialogFill, String editorFill) {
        this.cssPath = cssPath;
        this.dialogFill = dialogFill;
        this.editorFill = editorFill;
    }

    public String getCssPath() {
        return cssPath;
    }

    /** Matches {@code -welcome-bg}: dialog surface and scene fill colour. */
    public String getDialogFill() {
        return dialogFill;
    }

    /** Matches the {@code .editor-area} background: editor canvas colour. */
    public String getEditorFill() {
        return editorFill;
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
