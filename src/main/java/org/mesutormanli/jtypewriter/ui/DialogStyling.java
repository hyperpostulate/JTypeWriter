/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.net.URL;
import java.util.List;

/**
 * Puts the application stylesheets on an {@link javafx.scene.control.Alert}'s
 * dialog pane. Every Alert must be themed: without this the pane falls back to
 * Modena's white box with black text, contradicting the dark or sepia window
 * around it (analysis U3).
 */
public final class DialogStyling {

    private DialogStyling() {
    }

    /**
     * Copies the scene's stylesheets onto the dialog pane - the pattern the
     * welcome and about dialogs use. A null scene (or pane) is a no-op, so a
     * dialog shown before the main scene exists cannot fail here.
     */
    public static void applyFromScene(DialogPane pane, Scene scene) {
        if (pane == null || scene == null) {
            return;
        }
        pane.getStylesheets().addAll(scene.getStylesheets());
    }

    /**
     * Loads the base and theme stylesheets straight from the classpath, for
     * dialogs that have no scene to copy from yet (the single-instance
     * warning). A missing resource is skipped rather than fatal.
     */
    public static void applyTheme(DialogPane pane, Theme theme) {
        if (pane == null || theme == null) {
            return;
        }
        for (var path : stylesheetPaths(theme)) {
            URL css = DialogStyling.class.getResource(path);
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
            }
        }
    }

    /**
     * The classpath paths every dialog needs for {@code theme}, base first so
     * the theme's tokens win. Pure and platform-free, so the wiring is testable
     * without a toolkit.
     */
    public static List<String> stylesheetPaths(Theme theme) {
        return List.of("/css/base.css", "/" + theme.getCssPath());
    }
}
