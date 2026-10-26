package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Recovery-draft lifecycle as driven through {@link FileService} events. */
class AutoSaveServiceTest {

    @Test
    void openingADocumentDropsTheStaleDraftOfThePreviousOne(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var fileService = new FileService(mock(DialogService.class), mock(Messages.class));
        newService(store, fileService);

        // a draft left behind by a document the user then discarded before opening another
        store.saveDraft("discarded previous work", null);
        assertTrue(store.loadDraft().isPresent());

        var opened = dir.resolve("opened.txt");
        Files.writeString(opened, "from disk");
        fileService.loadFile(null, opened);

        // Regression: the draft used to survive until the next autosave tick, so a
        // crash in that window offered the already-discarded work for recovery again.
        assertTrue(store.loadDraft().isEmpty());
    }

    @Test
    void aPostponedDraftSurvivesAnExit(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var service = newService(store, new FileService(mock(DialogService.class), mock(Messages.class)));

        store.saveDraft("unanswered recovery draft", null);
        service.keepDraftForNextStart(); // what the "later" answer does

        service.shutdown();

        // Regression: WINDOW_HIDDEN used to clear the draft unconditionally, so
        // "ask later" followed by closing the app destroyed the text.
        assertTrue(store.loadDraft().isPresent());
    }

    /**
     * PW-26: only an exit whose close request was actually answered may delete
     * the draft. The old code deleted on every {@code WINDOW_HIDDEN}, which also
     * fires for {@code Platform.exit()} and a session log-out - exits that never
     * asked the user anything about unsaved changes.
     */
    @Test
    void answeredExitDropsAResolvedDraft(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var service = newService(store, new FileService(mock(DialogService.class), mock(Messages.class)));

        store.saveDraft("stale draft", null);
        service.approveExit(); // the close request was answered: save or "don't save"
        service.shutdown();

        assertTrue(store.loadDraft().isEmpty());
    }

    /**
     * PW-26: the complement of {@link #answeredExitDropsAResolvedDraft}. An exit
     * nobody asked about parks the live draft instead of deleting it, so the
     * text is offered again on the next start.
     */
    @Test
    void anUnaskedExitParksTheDraftInsteadOfDeletingIt(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var service = newService(store, new FileService(mock(DialogService.class), mock(Messages.class)));

        store.saveDraft("draft of an exit nobody confirmed", null);
        service.shutdown(); // no approveExit(): Platform.exit(), log-out, bare hide

        // Regression: WINDOW_HIDDEN used to delete unconditionally, so an exit
        // that never produced a close request lost unsaved text for good.
        assertEquals(0, countDrafts(dir, "draft-"));
        assertEquals(1, countDrafts(dir, "pending-"));
        assertEquals("draft of an exit nobody confirmed", store.loadDraft().orElseThrow().content());
    }

    @Test
    void aPostponedDraftNoLongerStopsAutosaveOfTheNewDocument(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var service = newService(store, fileService);

        store.saveDraft("ertelenen metin", null);
        service.keepDraftForNextStart(); // what the "later" answer does

        service.snapshot("yeni belgede yazılanlar");

        // Regression: while a draft was postponed, every snapshot returned early,
        // so a session started with "ask later" produced no recovery copy at all
        // for the document the user was actually writing.
        assertEquals(1, countDrafts(dir, "pending-"));
        assertEquals(1, countDrafts(dir, "draft-"));
        assertTrue(Files.readString(dir.resolve(firstByPrefix(dir, "draft-")))
                .contains("yeni belgede yazılanlar"));
    }

    /**
     * PW-26: with the close request answered the session's own draft goes, and
     * the copy parked for the next start is untouched - the two never collapse
     * into one.
     */
    @Test
    void aCleanExitAfterPostponingDropsOnlyTheLiveDraft(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var service = newService(store, fileService);

        store.saveDraft("ertelenen metin", null);
        service.keepDraftForNextStart();
        service.snapshot("yeni belgede yazılanlar");
        service.approveExit();
        service.shutdown();

        // Count before loadDraft(): loading adopts a foreign draft into this
        // session's slot, which would change the file layout under the assertion.
        assertEquals(0, countDrafts(dir, "draft-"));
        assertEquals(1, countDrafts(dir, "pending-"));
        assertEquals("ertelenen metin", store.loadDraft().orElseThrow().content());
    }

    /**
     * PW-26: without an answered close request the live copy is parked too, so
     * nothing written this session is discarded by an unasked exit.
     */
    @Test
    void anUnaskedExitAfterPostponingParksBothCopies(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var service = newService(store, fileService);

        store.saveDraft("ertelenen metin", null);
        service.keepDraftForNextStart();
        service.snapshot("yeni belgede yazılanlar");
        service.shutdown(); // no approveExit()

        assertEquals(0, countDrafts(dir, "draft-"));
        assertEquals(2, countDrafts(dir, "pending-"));
    }

    /**
     * PW-26: a postponed draft that could not be parked freezes the slot. An
     * exit then keeps the file exactly as it is - deleting or re-parking it
     * would destroy the only copy.
     */
    @Test
    void aFrozenSlotSurvivesExitAndReportsTheParkFailure() {
        var store = mock(RecoveryStore.class);
        when(store.park()).thenReturn(false);
        var service = newService(store, mock(FileService.class));

        service.shutdown(); // no approveExit(): PARK, but parking fails -> KEEP

        verify(store, never()).clear();
        assertEquals(AutosaveHealth.Problem.DRAFT_PARK, service.health().problem().orElseThrow());
        assertTrue(service.health().isFailing());
    }

    /**
     * PW-27: a failing snapshot must not be a log line nobody reads. The first
     * failure raises one dialog with the localised text, later failures only
     * keep the health state, and a working snapshot withdraws it.
     */
    @Test
    void aFailedSnapshotReportsOnceAndASuccessClearsTheWarning() throws Exception {
        var store = mock(RecoveryStore.class);
        doThrow(new IOException("read-only recovery directory"))
                .doThrow(new IOException("still read-only"))
                .doNothing()
                .when(store).saveDraft(anyString(), any());
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var dialogService = mock(DialogService.class);
        var messages = mock(Messages.class);
        when(messages.autosaveFailedHeader()).thenReturn("Automatic recovery is not working");
        when(messages.autosaveFailedContent()).thenReturn("Save your document now");
        var service = new AutoSaveService(store, fileService, dialogService, messages);

        service.snapshot("first tick");
        assertEquals(AutosaveHealth.Problem.DRAFT_WRITE, service.health().problem().orElseThrow());
        verify(dialogService, times(1))
                .showError(null, "Automatic recovery is not working", "Save your document now");

        // A second failing tick keeps the warning but must not nag with a dialog.
        service.snapshot("second tick");
        verify(dialogService, times(1))
                .showError(null, "Automatic recovery is not working", "Save your document now");

        service.snapshot("third tick succeeds");
        assertFalse(service.health().isFailing());
        assertTrue(service.health().problem().isEmpty());
    }

    /**
     * PW-27: the health value is sticky, not sampled - only real transitions
     * reach the listener, so the status bar is not repainted every 30 seconds
     * while the failure persists.
     */
    @Test
    void healthNotifiesOnlyOnRealTransitions() throws Exception {
        var store = mock(RecoveryStore.class);
        doThrow(new IOException("read-only"))
                .doThrow(new IOException("read-only"))
                .doNothing()
                .when(store).saveDraft(anyString(), any());
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var service = new AutoSaveService(store, fileService,
                mock(DialogService.class), mock(Messages.class));

        var notifications = new AtomicInteger();
        service.health().addListener(notifications::incrementAndGet);

        service.snapshot("fails");
        assertEquals(1, notifications.get(), "first failure is a transition");
        service.snapshot("fails again");
        assertEquals(1, notifications.get(), "the same failure is not a new transition");
        service.snapshot("succeeds");
        assertEquals(2, notifications.get(), "recovery is a transition");
    }

    private static AutoSaveService newService(RecoveryStore store, FileService fileService) {
        return new AutoSaveService(store, fileService, mock(DialogService.class), mock(Messages.class));
    }

    private static long countDrafts(Path dir, String prefix) throws Exception {
        try (var files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(name -> name.startsWith(prefix) && name.endsWith(".jtw"))
                    .count();
        }
    }

    private static String firstByPrefix(Path dir, String prefix) throws Exception {
        try (var files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(name -> name.startsWith(prefix) && name.endsWith(".jtw"))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
