package org.mesutormanli.jtypewriter.ui.theme;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.mesutormanli.jtypewriter.AppSettings;

public class ThemeManager {

    private final AppSettings settings;
    private Theme currentTheme;

    public ThemeManager(AppSettings settings) {
        this.settings = settings;
        this.currentTheme = settings.getTheme();
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /** Applies the persisted theme (startup). */
    public void applyTheme(Scene scene) {
        applyTheme(scene, currentTheme);
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

        // Paint the scene backdrop with the editor surface colour as well, so
        // no unpainted sliver at the window edges can show the default white.
        scene.setFill(Color.web(theme.getEditorFill()));

        currentTheme = theme;
        settings.setTheme(theme);
    }

    public Theme cycleTheme(Scene scene) {
        var next = currentTheme.next();
        applyTheme(scene, next);
        return next;
    }
}