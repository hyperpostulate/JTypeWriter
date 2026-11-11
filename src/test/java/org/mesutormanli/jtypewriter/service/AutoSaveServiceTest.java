/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.IoExecutor;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        var store = new RecoveryStore(dir, List.of(dir));
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
        var store = new RecoveryStore(dir, List.of(dir));
        var service = newService(store, new FileService(mock(DialogService.class), mock(Messages.class)));

        store.saveDraft("unanswered recovery draft", null);
        service.keepDraftForNextStart(); // what the "later" answer does

        service.shutdown();

        // Regression: WINDOW_HIDDEN used to clear the draft unconditionally, so
        // "ask later" followed by closing the app destroyed the text.
        assertTrue(store.loadDraft().isPresent());
    }

    /**
     * Only an exit whose close request was actually answered may delete the
     * draft. The old code deleted on every {@code WINDOW_HIDDEN}, which also
     * fires for {@code Platform.exit()} and a session log-out - exits that never
     * asked the user anything about unsaved changes.
     */
    @Test
    void answeredExitDropsAResolvedDraft(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir, List.of(dir));
        var service = newService(store, new FileService(mock(DialogService.class), mock(Messages.class)));

        store.saveDraft("stale draft", null);
        service.approveExit(); // the close request was answered: save or "don't save"
        service.shutdown();

        assertTrue(store.loadDraft().isEmpty());
    }

    /**
     * The complement of {@link #answeredExitDropsAResolvedDraft}. An exit nobody
     * asked about parks the live draft instead of deleting it, so the text is
     * offered again on the next start.
     */
    @Test
    void anUnaskedExitParksTheDraftInsteadOfDeletingIt(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir, List.of(dir));
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
        var store = new RecoveryStore(dir, List.of(dir));
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
     * With the close request answered the session's own draft goes, and the
     * copy parked for the next start is untouched - the two never collapse into
     * one.
     */
    @Test
    void aCleanExitAfterPostponingDropsOnlyTheLiveDraft(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir, List.of(dir));
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
     * Without an answered close request the live copy is parked too, so nothing
     * written this session is discarded by an unasked exit.
     */
    @Test
    void anUnaskedExitAfterPostponingParksBothCopies(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir, List.of(dir));
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
     * A postponed draft that could not be parked freezes the slot. An exit then
     * keeps the file exactly as it is - deleting or re-parking it would destroy
     * the only copy.
     */
    @Test
    void aFrozenSlotSurvivesExitAndReportsTheParkFailure() {
        var store = mock(RecoveryStore.class);
        when(store.park()).thenReturn(false);
        when(store.clear()).thenReturn(true);
        var service = newService(store, mock(FileService.class));
        service.shutdown(); // no approveExit(): PARK, but parking fails -> KEEP

        verify(store, never()).clear();
        assertEquals(AutosaveHealth.Problem.DRAFT_PARK, service.health().problem().orElseThrow());
        assertTrue(service.health().isFailing());
    }

    /**
     * A failing snapshot must not be a log line nobody reads. The first failure
     * raises one dialog with the localised text, later failures only keep the
     * health state, and a working snapshot withdraws it.
     */
    @Test
    void aFailedSnapshotReportsOnceAndASuccessClearsTheWarning() throws Exception {
        var store = mock(RecoveryStore.class);
        when(store.clear()).thenReturn(true);
        doThrow(new IOException("read-only recovery directory"))
                .doThrow(new IOException("still read-only"))
                .doNothing()
                .when(store).saveDraft(anyString(), any(), anyInt(), any(), anyBoolean());
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
     * An {@code Error} is not an {@code Exception}: a snapshot that once hit an
     * {@code OutOfMemoryError} skipped the failure reporting entirely
     * and the status bar kept claiming the draft was fine.
     */
    @Test
    void aSnapshotThatFailsWithAnErrorStillRecordsTheDraftWriteProblem() throws Exception {
        var store = mock(RecoveryStore.class);
        when(store.clear()).thenReturn(true);
        doThrow(new OutOfMemoryError("no memory"))
                .when(store).saveDraft(anyString(), any(), anyInt(), any(), anyBoolean());
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var service = newService(store, fileService);

        service.snapshot("text");

        assertEquals(AutosaveHealth.Problem.DRAFT_WRITE, service.health().problem().orElseThrow());
    }

    /**
     * The save-side counterpart: an {@code OutOfMemoryError} during the write
     * must reach the error path, leave the document dirty and therefore keep the
     * recovery draft instead of clearing it as a successful save would.
     */
    @Test
    void aSaveThatFailsWithAnErrorKeepsTheRecoveryDraft(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir, List.of(dir));
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "old");
        var messages = mock(Messages.class);
        when(messages.errorSave()).thenReturn("Could not save file");
        var dialogService = mock(DialogService.class);
        var fileService = new FileService(dialogService, messages, new BackupKeeper(),
                IoExecutor.synchronous(), (path, bytes) -> {
                    throw new OutOfMemoryError("out of memory");
                });
        var service = new AutoSaveService(store, fileService, dialogService, messages);

        fileService.restoreRecovered(target, "unsaved text");
        service.snapshot("unsaved text");
        assertTrue(store.loadDraft().isPresent());

        var saved = fileService.saveFile(null, "unsaved text");

        assertTrue(saved.isEmpty());
        assertTrue(fileService.hasUnsavedChanges());
        assertEquals("unsaved text", store.loadDraft().orElseThrow().content());
        verify(dialogService).showError(null, "Could not save file", "out of memory");
    }

    /**
     * The health value is sticky, not sampled - only real transitions reach the
     * listener, so the status bar is not repainted every 30 seconds while the
     * failure persists.
     */
    @Test
    void healthNotifiesOnlyOnRealTransitions() throws Exception {
        var store = mock(RecoveryStore.class);
        when(store.clear()).thenReturn(true);
        doThrow(new IOException("read-only"))
                .doThrow(new IOException("read-only"))
                .doNothing()
                .when(store).saveDraft(anyString(), any(), anyInt(), any(), anyBoolean());
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

    /**
     * A failed delete must not look like a clean slot. The warning stays, but
     * the one-off dialog is reserved for write failures - its copy is about
     * writing a recovery copy, which is not what went wrong here.
     */
    @Test
    void aFailedDeleteKeepsTheWarningAndRaisesNoDialog() {
        var store = mock(RecoveryStore.class);
        when(store.clear()).thenReturn(false);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(false);
        var dialogService = mock(DialogService.class);
        var service = new AutoSaveService(store, fileService, dialogService, mock(Messages.class));

        var notifications = new AtomicInteger();
        service.health().addListener(notifications::incrementAndGet);

        service.snapshot("clean document");

        assertEquals(AutosaveHealth.Problem.DRAFT_DELETE, service.health().problem().orElseThrow());
        verify(dialogService, never()).showError(any(), any(), any());
        assertEquals(1, notifications.get(), "the status bar must hear about it");
    }

    /**
     * The delete failure is sticky like any other problem, and a later delete
     * that actually works withdraws it on its own.
     */
    @Test
    void aLaterSuccessfulDeleteWithdrawsIt() {
        var store = mock(RecoveryStore.class);
        when(store.clear()).thenReturn(false).thenReturn(true);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(false);
        var service = newService(store, fileService);

        service.snapshot("clean document");
        assertEquals(AutosaveHealth.Problem.DRAFT_DELETE, service.health().problem().orElseThrow());

        service.snapshot("still clean");
        assertTrue(service.health().problem().isEmpty(),
                "a successful delete is the success that withdraws DRAFT_DELETE");
    }

    /**
     * A snapshot success is not evidence that an earlier failed delete
     * happened. The warning must survive until a real delete succeeds.
     */
    @Test
    void aSuccessfulSnapshotDoesNotWithdrawAFailedDeleteWarning() {
        var store = mock(RecoveryStore.class);
        when(store.clear()).thenReturn(false);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(false);
        var service = newService(store, fileService);

        service.snapshot("clean document");
        assertEquals(AutosaveHealth.Problem.DRAFT_DELETE, service.health().problem().orElseThrow());

        // The document becomes dirty and the snapshot write succeeds. That says
        // nothing about the stale draft the failed delete left on disk.
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        when(fileService.getCurrentCharset()).thenReturn(StandardCharsets.UTF_8);
        service.snapshot("dirty document");

        assertEquals(AutosaveHealth.Problem.DRAFT_DELETE, service.health().problem().orElseThrow(),
                "only a successful delete may withdraw DRAFT_DELETE");
    }

    /**
     * Encoding and caret are read on the FX thread before the write is
     * submitted, so the saved draft describes the editor as it was at the tick.
     */
    @Test
    void aSnapshotRecordsTheCaretAndEncodingCapturedFromTheEditor() throws Exception {
        var store = mock(RecoveryStore.class);
        var fileService = mock(FileService.class);
        var target = Path.of("belge.txt");
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        when(fileService.getCurrentFilePath()).thenReturn(target);
        when(fileService.getCurrentCharset()).thenReturn(StandardCharsets.UTF_8);
        when(fileService.isCurrentBom()).thenReturn(false);
        var service = newService(store, fileService);

        service.snapshot(() -> "text", () -> 9);

        verify(store).saveDraft(eq("text"), eq(target), eq(9),
                eq(StandardCharsets.UTF_8), eq(false));
    }

    /**
     * Hiding the window stops the timeline; showing it again must not leave
     * autosave dead. The actual restart cannot be exercised headlessly (a
     * {@code Timeline} needs the toolkit), so the guarding decision is asserted
     * here: with no attached window there is nothing to resume.
     */
    @Test
    void resumingSnapshotsWithoutAnAttachedWindowIsANoOp() {
        var service = newService(mock(RecoveryStore.class), mock(FileService.class));

        assertFalse(service.resumeSnapshots());
    }

    /**
     * The DISCARD answer must not pretend a failed delete happened. The user
     * chose to drop the draft; if the file survives, the status bar and the log
     * are the only channels, never a modal dialog.
     */
    @Test
    void aFailedDiscardDeleteLeavesTheDraftDeleteWarning() {
        var store = mock(RecoveryStore.class);
        when(store.loadDraft()).thenReturn(Optional.of(
                new RecoveryStore.Draft("text", null, Instant.now())));
        when(store.clear()).thenReturn(false);
        var dialogService = mock(DialogService.class);
        when(dialogService.showRecoveryDialog(any(), any())).thenReturn(RecoveryChoice.DISCARD);
        var service = new AutoSaveService(store, mock(FileService.class),
                dialogService, mock(Messages.class));

        service.offerRecovery(null, null);

        assertEquals(AutosaveHealth.Problem.DRAFT_DELETE, service.health().problem().orElseThrow());
        verify(dialogService, never()).showError(any(), any(), any());
    }

    /** A discard whose delete actually happened leaves no warning behind. */
    @Test
    void aSuccessfulDiscardDeleteStaysHealthy() {
        var store = mock(RecoveryStore.class);
        when(store.loadDraft()).thenReturn(Optional.of(
                new RecoveryStore.Draft("text", null, Instant.now())));
        when(store.clear()).thenReturn(true);
        var dialogService = mock(DialogService.class);
        when(dialogService.showRecoveryDialog(any(), any())).thenReturn(RecoveryChoice.DISCARD);
        var service = new AutoSaveService(store, mock(FileService.class),
                dialogService, mock(Messages.class));

        service.offerRecovery(null, null);

        assertTrue(service.health().problem().isEmpty(), "a real delete is a clean state");
    }

    /** Only a strictly newer file makes a draft stale. */
    @Test
    void draftPrecedesFileOnlyWhenTheFileIsStrictlyNewer() {
        var savedAt = Instant.parse("2024-01-01T00:00:00Z");
        var draft = new RecoveryStore.Draft("text", Path.of("doc.txt"), savedAt);

        assertTrue(AutoSaveService.draftPrecedesFile(draft, savedAt.plusSeconds(1)));
        assertFalse(AutoSaveService.draftPrecedesFile(draft, savedAt.minusSeconds(1)));
        assertFalse(AutoSaveService.draftPrecedesFile(draft, savedAt),
                "equal timestamps are not stale");
        assertFalse(AutoSaveService.draftPrecedesFile(
                new RecoveryStore.Draft("text", null, savedAt), savedAt.plusSeconds(1)),
                "a draft without an original path has nothing to overwrite");
        assertFalse(AutoSaveService.draftPrecedesFile(draft, null));
    }

    /**
     * The stale-draft hazard does not have to be prevented by the delete alone:
     * even when the stale draft survives, restoring it must not target the newer
     * document's path. The text is still recovered - forcing Save As - never
     * silently dropped.
     *
     * <p>The decision is asserted through the package-private
     * {@code restoreTarget}: {@code EditorArea} cannot be mocked headlessly
     * (its JavaFX supertypes fail to initialise without a toolkit).</p>
     */
    @Test
    void aDraftOlderThanTheDocumentIsNotRestoredOntoItsPath(@TempDir Path dir) throws Exception {
        var doc = dir.resolve("doc.txt");
        Files.writeString(doc, "newer text");
        Files.setLastModifiedTime(doc, FileTime.from(Instant.now().plusSeconds(60)));

        var store = new RecoveryStore(dir, List.of(dir));
        store.saveDraft("stale text", doc); // savedAt is now, before the future mtime
        var draft = store.loadDraft().orElseThrow();

        var service = newService(store, mock(FileService.class));

        assertNull(service.restoreTarget(draft),
                "the stale draft must be restored without its path so Save As is forced");
    }

    /** The ordinary case keeps its path, so Save writes back to the document. */
    @Test
    void anOrdinaryDraftStillRestoresOntoItsPath(@TempDir Path dir) throws Exception {
        var doc = dir.resolve("doc.txt");
        Files.writeString(doc, "older text");

        var store = new RecoveryStore(dir, List.of(dir));
        store.saveDraft("text", doc);
        var draft = store.loadDraft().orElseThrow();

        var service = newService(store, mock(FileService.class));

        assertEquals(doc.toAbsolutePath(), service.restoreTarget(draft));
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
