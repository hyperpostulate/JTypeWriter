/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.service.AutoSaveService;
import org.mesutormanli.jtypewriter.service.FileService;

import java.util.List;

public class MainStage {

    private final FileService fileService;
    private final AppSettings settings;
    private final AutoSaveService autoSaveService;

    public MainStage(FileService fileService, AppSettings settings, AutoSaveService autoSaveService) {
        this.fileService = fileService;
        this.settings = settings;
        this.autoSaveService = autoSaveService;
    }

    public void configure(Stage stage, Scene scene) {
        // A session that ended in fullscreen comes back in fullscreen.
        // Fullscreen and maximized are mutually exclusive states, so only one
        // of them is ever restored (fullscreen wins); the normal geometry is
        // still applied, so leaving fullscreen lands on the remembered
        // position and size instead of a jarring contradictory restore.
        var restoreFullScreen = settings.isFullScreen();

        // Restore the geometry of the last session; a maximized window keeps
        // its unmaximized bounds so leaving maximized lands where it used to.
        var saved = settings.getWindowState();
        if (saved.isPresent()) {
            var state = saved.get();
            if (!state.maximized() && state.width() > 0 && state.height() > 0) {
                // A monitor may have been removed since the geometry was stored
                // (or DPI changed): a position that no longer touches any screen
                // would open the window off-screen and unreachable. Keep the
                // remembered size but let the platform place it instead.
                if (isOnAnyScreen(state.x(), state.y(), state.width(), state.height())) {
                    stage.setX(state.x());
                    stage.setY(state.y());
                }
                stage.setWidth(state.width());
                stage.setHeight(state.height());
            }
            stage.setMaximized(maximizeOnRestore(state.maximized(), restoreFullScreen));
        } else {
            stage.setMaximized(maximizeOnRestore(true, restoreFullScreen));
        }
        stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("Escape"));
        stage.setFullScreenExitHint("");
        stage.setFullScreen(restoreFullScreen);

        // Persist fullscreen the moment it is toggled (F11 or Escape), not only
        // in setOnHidden: a kill right after the toggle must not lose it.
        // Hiding a fullscreen window resets the flag as a side effect, which is
        // not a user decision - only changes while the window is showing are
        // taken for intent. Registered after the restore above so startup does
        // not rewrite the value it just read.
        stage.fullScreenProperty().addListener((obs, was, is) -> {
            if (stage.isShowing()) {
                settings.setFullScreen(is);
            }
        });

        // This is the only exit path that asks the user anything, so
        // it is the only one allowed to delete the recovery draft. The request
        // is answered first, then the autosave service is told that this exit
        // was sanctioned - before the window hides, because the hide handler
        // decides between deleting and parking from exactly that fact. Not
        // consuming the event lets JavaFX close the window, so no path here
        // can open a second prompt.
        stage.setOnCloseRequest(event -> {
            if (fileService.requestExit(stage) == FileService.ExitAnswer.STAY) {
                event.consume();
                return;
            }
            autoSaveService.approveExit();
        });

        // Geometry is remembered on hide, so it survives both a clean close
        // and a close that goes through the unsaved-changes prompt.
        stage.setOnHidden(event -> settings.setWindowState(new AppSettings.WindowState(
                stage.isMaximized(), stage.getX(), stage.getY(),
                stage.getWidth(), stage.getHeight())));
    }

    /**
     * Pure restore decision kept out of the stage so it can be unit tested:
     * fullscreen and maximized must never both be applied at startup, and
     * fullscreen (the more specific, explicitly re-entered state) wins.
     */
    static boolean maximizeOnRestore(boolean savedMaximized, boolean restoreFullScreen) {
        return savedMaximized && !restoreFullScreen;
    }

    /**
     * Whether a remembered geometry still has a visible part on one of the
     * currently attached screens. A window is considered reachable when it
     * overlaps a screen's visual bounds at all, so a partially off-screen but
     * still usable position is preserved. Falls back to {@code true} when the
     * screen list is unavailable, so a toolkit oddity can never hide a window.
     */
    static boolean isOnAnyScreen(double x, double y, double width, double height) {
        try {
            var screens = Screen.getScreens().stream().map(Screen::getVisualBounds).toList();
            return intersectsAny(new Rectangle2D(x, y, width, height), screens);
        } catch (Throwable screenUnavailable) {
            return true;
        }
    }

    /** Pure geometry used by {@link #isOnAnyScreen}; testable without a toolkit. */
    static boolean intersectsAny(Rectangle2D candidate, List<Rectangle2D> screens) {
        for (var screen : screens) {
            if (candidate.getMinX() < screen.getMaxX() && candidate.getMaxX() > screen.getMinX()
                    && candidate.getMinY() < screen.getMaxY() && candidate.getMaxY() > screen.getMinY()) {
                return true;
            }
        }
        return false;
    }
}