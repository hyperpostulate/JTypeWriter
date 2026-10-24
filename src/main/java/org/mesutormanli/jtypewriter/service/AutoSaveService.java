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
 *
 * <p>A draft the user answered with "later" is never dropped implicitly: it is
 * parked out of the active slot ({@link RecoveryStore#park()}), so it survives
 * this session and the next one while the document the user is actually
 * editing keeps being snapshotted. Parking, not freezing, is what makes both
 * true at once.</p>
 */
public class AutoSaveService {

    static final int AUTOSAVE_SECONDS = 30;

    private static final Logger LOG = AppLogging.logger(AutoSaveService.class);

    private final RecoveryStore store;
    private final FileService fileService;
    private final DialogService dialogService;

    private Timeline timeline;

    /**
     * Fallback latch, set only when a postponed draft could not be parked. In
     * that state the slot still belongs to the deferred text, so snapshots stay
     * suppressed rather than overwriting its only copy. It is not part of the
     * normal "later" path: that one parks the draft and keeps snapping.
     */
    private boolean draftFrozen;

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
                if (!draftFrozen) {
                    store.clear();
                }
            }

            @Override
            public void onSaved(java.nio.file.Path file) {
                if (!draftFrozen) {
                    store.clear(); // content is on disk now, nothing left to recover
                }
            }

            /**
             * A new document also resolves the previous one (it was saved or
             * explicitly discarded), so any draft left from it is stale.
             */
            @Override
            public void onNewDocument() {
                if (!draftFrozen) {
                    store.clear();
                }
            }
        });
    }

    /** Starts periodic snapshots and ties cleanup to the window lifecycle. */
    public void attach(Stage stage, EditorArea editorArea) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(AUTOSAVE_SECONDS), e -> snapshot(editorArea.getText())));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        stage.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) {
                snapshot(editorArea.getText());
            }
        });
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> shutdown());
    }

    /**
     * Offers the surviving draft, if any, during startup. A restored draft
     * stays in the store until the next successful save, so a second crash
     * right after recovery cannot lose it. Closing the prompt (or choosing
     * "later") keeps the draft untouched for the next start.
     *
     * @return {@code true} when a draft was restored (the caller must not load
     *         anything else into the editor), {@code false} when there was no
     *         draft, the user discarded it, or the answer was "later"
     */
    public boolean offerRecovery(Stage stage, EditorArea editorArea) {
        var draft = store.loadDraft();
        if (draft.isEmpty()) {
            return false;
        }
        var choice = dialogService.showRecoveryDialog(stage, draft.get().savedAt());
        if (choice == RecoveryChoice.RESTORE) {
            draftFrozen = false;
            fileService.restoreRecovered(draft.get().originalFile(), draft.get().content());
            editorArea.setDocument(fileService.getCurrentContent());
            return true;
        }
        if (choice == RecoveryChoice.DISCARD) {
            draftFrozen = false;
            store.clear();
        } else {
            // LATER (or the prompt was closed): the draft is the only copy, so
            // keep it queued for the next start instead of silently deleting it
            keepDraftForNextStart();
        }
        return false;
    }

    /**
     * Parks the postponed draft beside the session's slot. Afterwards the slot
     * is free and snapshots continue as usual; if parking fails the slot is
     * frozen instead, so the text is never overwritten.
     */
    void keepDraftForNextStart() {
        draftFrozen = !store.park();
    }

    /** Clean exit: stop snapping and drop the draft unless one was postponed. */
    public void shutdown() {
        if (timeline != null) {
            timeline.stop();
        }
        if (!draftFrozen) {
            store.clear();
        }
    }

    /**
     * One autosave tick. Takes the text as an argument (rather than the control
     * that holds it) so the whole decision stays testable without a toolkit.
     */
    void snapshot(String text) {
        if (draftFrozen) {
            return; // the slot could not be freed; never overwrite its only copy
        }
        try {
            if (fileService.hasUnsavedChanges()) {
                store.saveDraft(text, fileService.getCurrentFilePath());
            } else {
                store.clear();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "autosave snapshot failed", e);
        }
    }
}
