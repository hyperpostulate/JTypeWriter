package org.mesutormanli.jtypewriter.ui.theme;


import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.List;

public class TextColorManager {

    private final AppSettings settings;
    private TextColor current;

    public TextColorManager(AppSettings settings) {
        this.settings = settings;
        this.current = settings.getTextColor();
    }

    public TextColor getCurrent() {
        return current;
    }

    /**
     * The single entry point used by the toolbar's colour dropdown: stores and
     * persists the chosen colour. A repeat request for the colour already in
     * force is ignored, so the dropdown can never double-persist.
     */
    public void set(TextColor color) {
        if (color == current) {
            return;
        }
        current = color;
        settings.setTextColor(color);
    }

    /** Advances to the next colour through {@link #set} - one persistence path. */
    public TextColor cycle() {
        set(current.next());
        return current;
    }

    public String getCurrentCssColor() {
        return current.getCssColor();
    }

    public String getCurrentDisplayName(Messages messages) {
        return current.getDisplayName(messages);
    }

    /**
     * The dropdown's option list: every {@link TextColor} in declaration order
     * (Default, Sage, Lavender, Sand, Steel, Rose) - the same order the cycle
     * button used, so the two list and cycle through the colours identically.
     */
    public static List<TextColor> options() {
        return List.of(TextColor.values());
    }
}
