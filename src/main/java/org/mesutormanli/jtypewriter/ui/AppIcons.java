/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppLogging;

/**
 * Single place that knows which classpath resources make up the application
 * window icon. The bundled PNGs live under {@code /icons/} and are offered to
 * JavaFX as a set of sizes, so the platform can pick the best fit for the
 * window/taskbar/dock.
 *
 * <p>Icon loading is best-effort by design: a missing resource (for example an
 * exploded development classpath without the asset directory) or a corrupt
 * image must never break application startup. Nothing here starts the JavaFX
 * toolkit, so the class stays safe to touch from headless tests.</p>
 */
public final class AppIcons {

    /** Window icon sizes in ascending order; JavaFX chooses per platform. */
    private static final List<String> WINDOW_ICONS = List.of(
            "/icons/jtypewriter-16.png",
            "/icons/jtypewriter-32.png",
            "/icons/jtypewriter-48.png",
            "/icons/jtypewriter-64.png",
            "/icons/jtypewriter-128.png",
            "/icons/jtypewriter-256.png");

    /**
     * The single artwork used by the helper windows (welcome, about). They are
     * small, undecorated and shown one at a time, so one high-resolution image
     * covers every window list and title-bar size without juggling a set.
     */
    private static final String DIALOG_ICON = "/icons/jtypewriter-512.png";

    private AppIcons() {
    }

    /** The classpath names of the window icons, in ascending size order. */
    public static List<String> windowIconResources() {
        return WINDOW_ICONS;
    }

    /** The classpath name of the single icon used by the helper windows. */
    public static String dialogIconResource() {
        return DIALOG_ICON;
    }

    /**
     * Loads the image drawn inside the helper windows (welcome, about). Like
     * the window icons this is best-effort: a missing resource or a corrupt
     * image is logged and reported as {@code null} so the caller simply shows
     * no picture instead of failing to open the dialog.
     */
    public static Image dialogIcon() {
        try {
            URL url = AppIcons.class.getResource(DIALOG_ICON);
            if (url == null) {
                AppLogging.logger(AppIcons.class).log(Level.WARNING,
                        "dialog icon resource is missing: " + DIALOG_ICON);
                return null;
            }
            var image = new Image(url.toExternalForm());
            if (image.isError()) {
                AppLogging.logger(AppIcons.class).log(Level.WARNING,
                        "dialog icon resource cannot be decoded: " + DIALOG_ICON);
                return null;
            }
            return image;
        } catch (Throwable error) {
            // A cosmetic asset must never take a dialog down, but the failure
            // has to be visible in the log rather than swallowed.
            AppLogging.logger(AppIcons.class).log(Level.WARNING, "cannot load dialog icon", error);
            return null;
        }
    }

    /**
     * Applies the bundled icons to the given stage. Resources that are absent
     * or fail to decode are skipped; the stage keeps its previous icons when
     * nothing could be loaded.
     *
     * @return {@code true} when at least one icon was applied
     */
    public static boolean applyTo(Stage stage) {
        return apply(stage, WINDOW_ICONS);
    }

    private static boolean apply(Stage stage, List<String> resources) {
        try {
            var loaded = new ArrayList<Image>();
            for (var name : resources) {
                URL url = AppIcons.class.getResource(name);
                if (url == null) {
                    continue; // an exploded classpath without the assets
                }
                var image = new Image(url.toExternalForm());
                if (image.isError()) {
                    continue;
                }
                loaded.add(image);
            }
            if (loaded.isEmpty()) {
                return false;
            }
            stage.getIcons().setAll(loaded);
            return true;
        } catch (Throwable error) {
            // Startup must never fail because of cosmetic assets, but the
            // failure has to be visible in the log rather than swallowed.
            AppLogging.logger(AppIcons.class).log(Level.WARNING, "cannot apply window icons", error);
            return false;
        }
    }
}
