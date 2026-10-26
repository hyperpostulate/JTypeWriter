/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.theme;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.mesutormanli.jtypewriter.AppSettings;

import java.util.List;

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

    /**
     * The single entry point used by the toolbar's theme dropdown: applies the
     * chosen theme and persists it exactly like the cycle path did. A repeat
     * request for the theme already in force is ignored, so re-selecting the
     * dropdown's current entry does not reload the stylesheets.
     */
    public void setTheme(Scene scene, Theme theme) {
        if (theme == currentTheme) {
            return;
        }
        applyTheme(scene, theme);
    }

    public Theme cycleTheme(Scene scene) {
        var next = currentTheme.next();
        setTheme(scene, next);
        return currentTheme;
    }

    /**
     * The dropdown's option list: every {@link Theme} in declaration order
     * (Light, Dark, Sepia), the same order the cycle button used.
     */
    public static List<Theme> options() {
        return List.of(Theme.values());
    }
}
