package org.mesutormanli.jtypewriter.service;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Crash safety net for unwritten work. While the document is dirty the current
 * text is snapshotted to the {@link RecoveryStore} every {@value #AUTOSAVE_SECONDS}
 * seconds and whenever the window loses focus. Snapshots are dropped on every
 * successful save (content is safe on disk) and on clean exit; anything that
 * survives that is a crash and is offered for recovery on next startup.
 */
public class AutoSaveService {

    static final int AUTOSAVE_SECONDS = 30;

    private static final Logger LOG = AppLogging.logger(AutoSaveService.class);

    private final RecoveryStore store;
    private final FileService fileService;
    private final DialogService dialogService;

    private Timeline timeline;

    public AutoSaveService(RecoveryStore store, FileService fileService, DialogService dialogService) {
        this.store = store;
        this.fileService = fileService;
        this.dialogService = dialogService;
        fileService.addListener(new FileService.Listener() {
            /**
             * A load always means the previous document was resolved first
             * (saved, or explicitly discarded): any draft left from it is stale
             * and must not be offered for recovery later. Clearing here closes
             * the window during which a discarded draft survived until the next
             * autosave tick.
             */
            @Override
            public void onLoaded(java.nio.file.Path file) {
                store.clear();
            }

            @Override
            public void onSaved(java.nio.file.Path file) {
                store.clear(); // content is on disk now, nothing left to recover
            }
        });
    }

    /** Starts periodic snapshots and ties cleanup to the window lifecycle. */
    public void attach(Stage stage, EditorArea editorArea) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(AUTOSAVE_SECONDS), e -> snapshot(editorArea)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        stage.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) {
                snapshot(editorArea);
            }
        });
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> shutdown());
    }

    /**
     * Offers the surviving draft, if any, during startup. A restored draft
     * stays in the store until the next successful save, so a second crash
     * right after recovery cannot lose it.
     *
     * @return {@code true} when a draft was restored (the caller must not load
     *         anything else into the editor), {@code false} when there was no
     *         draft or the user discarded it
     */
    public boolean offerRecovery(Stage stage, EditorArea editorArea) {
        var draft = store.loadDraft();
        if (draft.isEmpty()) {
            return false;
        }
        if (dialogService.showRecoveryDialog(stage, draft.get().savedAt())) {
            fileService.restoreRecovered(draft.get().originalFile(), draft.get().content());
            editorArea.setText(fileService.getCurrentContent());
            return true;
        }
        store.clear();
        return false;
    }

    /** Clean exit: stop snapping and drop the draft. */
    public void shutdown() {
        if (timeline != null) {
            timeline.stop();
        }
        store.clear();
    }

    private void snapshot(EditorArea editorArea) {
        try {
            if (fileService.hasUnsavedChanges()) {
                store.saveDraft(editorArea.getText(), fileService.getCurrentFilePath());
            } else {
                store.clear();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "autosave snapshot failed", e);
        }
    }
}