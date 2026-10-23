package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Recovery-draft lifecycle as driven through {@link FileService} events. */
class AutoSaveServiceTest {

    @Test
    void openingADocumentDropsTheStaleDraftOfThePreviousOne(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var fileService = new FileService(mock(DialogService.class), mock(Messages.class));
        new AutoSaveService(store, fileService, mock(DialogService.class));

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
    void closingTheRecoveryPromptKeepsTheDraftAcrossCleanExit(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var service = new AutoSaveService(store,
                new FileService(mock(DialogService.class), mock(Messages.class)),
                mock(DialogService.class));

        store.saveDraft("unanswered recovery draft", null);
        service.keepDraftForNextStart(); // what the "later" answer does

        service.shutdown();

        // Regression: WINDOW_HIDDEN used to clear the draft unconditionally, so
        // "ask later" followed by closing the app destroyed the text.
        assertTrue(store.loadDraft().isPresent());
    }

    @Test
    void cleanExitStillDropsAResolvedDraft(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var service = new AutoSaveService(store,
                new FileService(mock(DialogService.class), mock(Messages.class)),
                mock(DialogService.class));

        store.saveDraft("stale draft", null);
        service.shutdown();

        assertTrue(store.loadDraft().isEmpty());
    }
}
