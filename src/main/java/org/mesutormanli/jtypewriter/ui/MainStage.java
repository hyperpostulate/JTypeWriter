package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.service.FileService;

import java.util.List;

public class MainStage {

    private final FileService fileService;
    private final AppSettings settings;

    public MainStage(FileService fileService, AppSettings settings) {
        this.fileService = fileService;
        this.settings = settings;
    }

    public void configure(Stage stage, Scene scene) {
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
            stage.setMaximized(state.maximized());
        } else {
            stage.setMaximized(true);
        }
        stage.setFullScreen(false);
        stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("Escape"));
        stage.setFullScreenExitHint("");

        stage.setOnCloseRequest(event -> {
            if (fileService.hasUnsavedChanges()) {
                event.consume();
                fileService.promptSaveOnExit(stage);
            }
        });

        // Geometry is remembered on hide, so it survives both a clean close
        // and a close that goes through the unsaved-changes prompt.
        stage.setOnHidden(event -> settings.setWindowState(new AppSettings.WindowState(
                stage.isMaximized(), stage.getX(), stage.getY(),
                stage.getWidth(), stage.getHeight())));
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