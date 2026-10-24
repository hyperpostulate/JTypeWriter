package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void aPostponedDraftNoLongerStopsAutosaveOfTheNewDocument(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var service = new AutoSaveService(store, fileService, mock(DialogService.class));

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

    @Test
    void aCleanExitAfterPostponingKeepsBothCopiesApart(@TempDir Path dir) throws Exception {
        var store = new RecoveryStore(dir);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(true);
        var service = new AutoSaveService(store, fileService, mock(DialogService.class));

        store.saveDraft("ertelenen metin", null);
        service.keepDraftForNextStart();
        service.snapshot("yeni belgede yazılanlar");
        service.shutdown();

        // The live slot is dropped on clean exit, the postponed draft is not.
        assertEquals(0, countDrafts(dir, "draft-"));
        assertEquals("ertelenen metin", store.loadDraft().orElseThrow().content());
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
