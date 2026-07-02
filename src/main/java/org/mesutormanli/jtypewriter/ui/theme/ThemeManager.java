package org.mesutormanli.jtypewriter.ui.theme;

import javafx.scene.Scene;
import org.springframework.stereotype.Component;

@Component
public class ThemeManager {

    private Theme currentTheme = Theme.DARK;

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void applyTheme(Scene scene, Theme theme) {
        scene.getStylesheets().clear();

        var baseCss = getClass().getResource("/css/base.css");
        if (baseCss != null) {
            scene.getStylesheets().add(baseCss.toExternalForm());
        }

        var themeCss = getClass().getResource("/" + theme.getCssPath());
        if (themeCss != null) {
            scene.getStylesheets().add(themeCss.toExternalForm());
        }

        currentTheme = theme;
    }

    public Theme cycleTheme(Scene scene) {
        var next = currentTheme.next();
        applyTheme(scene, next);
        return next;
    }
}
