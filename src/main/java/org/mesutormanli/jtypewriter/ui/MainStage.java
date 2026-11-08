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

    /**
     * The widest {@code COMPACT} toolbar row across the six bundles, measured
     * from the real localised labels ({@code ToolbarView.fitControls} +
     * {@link org.mesutormanli.jtypewriter.ui.component.ToolbarFit#requiredWidth}):
     * Spanish needs 872.3 px, the most of the six, so the floor is 873.
     *
     * <p><strong>Why it is a pinned number and not a live measurement.</strong>
     * The minimum has to be known before the window exists and has to be the
     * same whichever language is selected, so measuring "the current one" at
     * startup would make the floor depend on the UI language and would move
     * when the user switches. Measuring all six at every launch would also add
     * a font pass to startup for a value that only changes when a translation
     * does. {@code ToolbarFitTest.theMinimumWindowWidthIsTheWidestCollapsedRowAcrossEveryLanguage}
     * re-derives it from the bundles, so a longer label fails the build instead
     * of silently clipping.</p>
     */
    public static final double WIDEST_COMPACT_ROW_PX = 873;

    /**
     * Width reserved for the window's own decoration, which the toolbar does
     * not get to use. Deliberately the larger of the two supported platforms'
     * borders (Windows resizes with about 8 px per side; Linux is usually
     * slimmer), so the floor errs wide: too much room only leaves a little
     * slack, too little would bring back the clipping this minimum removes.
     */
    public static final double DECORATION_ALLOWANCE_PX = 16;

    /**
     * The window's minimum width: the widest collapsed row plus the decoration
     * the row cannot occupy. At this width the toolbar is in {@code COMPACT}
     * in all six languages and nothing is clipped, which is what the minimum
     * promises - the row used to keep shrinking and clipping because the
     * window had no minimum at all.
     */
    public static final double MINIMUM_WIDTH_PX = WIDEST_COMPACT_ROW_PX + DECORATION_ALLOWANCE_PX;

    private final FileService fileService;
    private final AppSettings settings;
    private final AutoSaveService autoSaveService;
    private final DropHandler dropHandler;

    public MainStage(FileService fileService, AppSettings settings, AutoSaveService autoSaveService,
                     DropHandler dropHandler) {
        this.fileService = fileService;
        this.settings = settings;
        this.autoSaveService = autoSaveService;
        this.dropHandler = dropHandler;
    }

    public void configure(Stage stage, Scene scene) {
        // Dropping a document works anywhere in the window, not just over the
        // text: the gesture is installed once, on the scene the stage shows.
        dropHandler.install(stage, scene);

        // The window cannot be narrowed past the widest collapsed toolbar row,
        // so the toolbar stops clipping at the bottom of the range. Set before
        // the geometry restore below, which applies the same floor itself.
        stage.setMinWidth(MINIMUM_WIDTH_PX);

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
                stage.setWidth(restoredWidth(state.width(), MINIMUM_WIDTH_PX));
                stage.setHeight(state.height());
            }
            stage.setMaximized(maximizeOnRestore(state.maximized(), restoreFullScreen));
        } else {
            stage.setMaximized(maximizeOnRestore(true, restoreFullScreen));
        }
        stage.setFullScreenExitKeyCombination(
                KeyCombination.valueOf(EditorAction.EXIT_FULLSCREEN.primaryChord()));
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
        // decides between deleting and parking from exactly that fact. A save
        // may run on the I/O worker, so the event is consumed up front and the
        // window is closed programmatically once the verdict arrives: closing
        // from the callback cannot re-enter this handler (Stage.close() only
        // hides the window).
        stage.setOnCloseRequest(event -> {
            event.consume();
            fileService.requestExitAsync(stage).thenAccept(answer -> {
                if (answer == FileService.ExitAnswer.CLOSE) {
                    // The verdict may be immediate (clean document); closing
                    // from a later pulse keeps Stage.close() out of the close
                    // request dispatch itself.
                    javafx.application.Platform.runLater(() -> {
                        autoSaveService.approveExit();
                        stage.close();
                    });
                }
            });
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
     * A remembered width, floored at the window's minimum.
     *
     * <p>Applying the floor here rather than trusting JavaFX to clamp a width
     * below {@code minWidth} keeps the restore deterministic on both supported
     * platforms, and it is what makes the setting repair itself: the window is
     * hidden with the width it actually has, so the next session restores a
     * legal number instead of the too-narrow one that was stored.</p>
     */
    static double restoredWidth(double savedWidth, double minimumWidth) {
        return Math.max(savedWidth, minimumWidth);
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