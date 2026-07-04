package org.mesutormanli.jtypewriter.ui.theme;

import org.springframework.stereotype.Component;

@Component
public class TextColorManager {

    private TextColor current = TextColor.DEFAULT;

    public TextColor getCurrent() {
        return current;
    }

    public TextColor cycle() {
        current = current.next();
        return current;
    }

    public String getCurrentCssColor() {
        return current.getCssColor();
    }

    public String getCurrentDisplayName() {
        return current.getDisplayName();
    }
}
