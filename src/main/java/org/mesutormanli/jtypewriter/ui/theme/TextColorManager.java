package org.mesutormanli.jtypewriter.ui.theme;


import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;

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

    public TextColor cycle() {
        current = current.next();
        settings.setTextColor(current);
        return current;
    }

    public String getCurrentCssColor() {
        return current.getCssColor();
    }

    public String getCurrentDisplayName(Messages messages) {
        return current.getDisplayName(messages);
    }
}