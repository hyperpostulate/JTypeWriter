package org.mesutormanli.jtypewriter.service;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Crash safety net for unwritten work. While the document is dirty the current
 * text is snapshotted to the {@link RecoveryStore} every {@value #AUTOSAVE_SECONDS}
 * seconds and whenever the window loses focus. Snapshots are dropped on every
 * successful save (content is safe on disk) and on an answered close request;
 * anything that survives that is a crash - or an exit nobody asked about - and
 * is offered for recovery on next startup.
 *
 * <p>A draft the user answered with "later" is never dropped implicitly: it is
 * parked out of the active slot ({@link RecoveryStore#park()}), so it survives
 * this session and the next one while the document the user is actually
 * editing keeps being snapshotted. Parking, not freezing, is what makes both
 * true at once.</p>
 *
 * <p><strong>Exit rule (V2/PW-26):</strong> deleting the draft is only ever
 * allowed after the window's close request was answered - that is the one path
 * that asked the user and got a decision. Every other way out
 * ({@code Platform.exit()}, a session log-out, a bare {@code stage.hide()})
 * parks the draft instead, so a dirty document survives an exit nobody
 * confirmed. See {@link ExitDisposition}.</p>
 *
 * <p><strong>Failures are visible (V4/PW-27):</strong> a snapshot or park that
 * fails records a {@link AutosaveHealth} problem, which raises a dialog once
 * and keeps a warning in the status bar until a write succeeds again. A broken
 * recovery directory can no longer silently cancel the product's only
 * promise.</p>
 */
public class AutoSaveService {

    static final int AUTOSAVE_SECONDS = 30;

    private static final Logger LOG = AppLogging.logger(AutoSaveService.class);

    private final RecoveryStore store;
    private final FileService fileService;
    private final DialogService dialogService;
    private final Messages messages;
    private final AutosaveHealth health = new AutosaveHealth();

    private Timeline timeline;
    private Stage stage;

    /**
     * Fallback latch, set only when a postponed draft could not be parked. In
     * that state the slot still belongs to the deferred text, so snapshots stay
     * suppressed rather than overwriting its only copy. It is not part of the
     * normal "later" path: that one parks the draft and keeps snapping.
     */
    private boolean draftFrozen;

    /**
     * Set only by {@link #approveExit()}, i.e. when the close request was
     * answered in favour of closing. Every other exit path leaves it
     * {@code false} and therefore parks instead of deleting.
     */
    private boolean closeRequestAnswered;

    /** The dialog is a one-off explanation; the status bar carries the rest. */
    private boolean failureReported;

    public AutoSaveService(RecoveryStore store, FileService fileService,
                           DialogService dialogService, Messages messages) {
        this.store = store;
        this.fileService = fileService;
        this.dialogService = dialogService;
        this.messages = messages;
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
        this.stage = stage;
        timeline = new Timeline(new KeyFrame(Duration.seconds(AUTOSAVE_SECONDS), e -> snapshot(editorArea.getText())));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        stage.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) {
                snapshot(editorArea.getText());
            }
        });
        // Fires on every hide - a close request, Platform.exit(), a session
        // end. Which of those it was decides what happens to the draft
        // (shutdown), never the event itself.
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> shutdown());
    }

    /** The observable state behind the status-bar warning (PW-27). */
    public AutosaveHealth health() {
        return health;
    }

    /**
     * Records that the window's close request was answered in favour of
     * closing - the document was saved, or the user explicitly discarded it.
     * Must be called <em>before</em> the window closes, i.e. while the hide
     * event has not run yet; {@link #shutdown()} then deletes the draft.
     *
     * <p>Deliberately separate from {@link #shutdown()}: the event fires for
     * exits that asked nothing, and only the caller knows whether a user was
     * actually consulted.</p>
     */
    public void approveExit() {
        closeRequestAnswered = true;
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
     * frozen instead, so the text is never overwritten - and the failure is
     * made visible instead of only being logged.
     */
    void keepDraftForNextStart() {
        parkOrFreeze();
    }

    /**
     * End of session: stop snapping and apply {@link ExitDisposition}. Reached
     * from {@code WINDOW_HIDDEN} and from {@code Application.stop()}, so an
     * exit that never produced a close request ({@code Platform.exit()}, a
     * session log-out) parks the draft instead of deleting it.
     */
    public void shutdown() {
        if (timeline != null) {
            timeline.stop();
        }
        switch (ExitDisposition.decide(closeRequestAnswered, draftFrozen)) {
            case CLEAR -> store.clear();
            case PARK -> parkOrFreeze();
            case KEEP -> {
                // a postponed draft that could not be parked: never touch it
            }
        }
    }

    /**
     * Frees the live slot, or freezes it when the move fails. Both the
     * "ask later" path and the exit path go through here, so a parked draft
     * cannot be reported as a silent success on one road and a logged failure
     * on the other.
     */
    private void parkOrFreeze() {
        if (store.park()) {
            draftFrozen = false;
            return;
        }
        draftFrozen = true;
        reportFailure(AutosaveHealth.Problem.DRAFT_PARK);
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
            health.recover(); // a working snapshot withdraws an older warning
        } catch (Exception e) {
            LOG.log(Level.WARNING, "autosave snapshot failed", e);
            reportFailure(AutosaveHealth.Problem.DRAFT_WRITE);
        }
    }

    /**
     * Records a failure and explains it once. The dialog is shown at most once
     * per session (the status bar keeps the warning up afterwards), and not at
     * all when no window is showing - at exit there is nobody to read it, and
     * the parked draft is still offered on the next start.
     */
    private void reportFailure(AutosaveHealth.Problem problem) {
        health.fail(problem);
        if (failureReported) {
            return;
        }
        failureReported = true;
        if (stage == null || stage.isShowing()) {
            dialogService.showError(stage, messages.autosaveFailedHeader(), messages.autosaveFailedContent());
        }
    }
}
