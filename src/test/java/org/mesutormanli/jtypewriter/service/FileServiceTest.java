package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private static final String TURKISH = "çöğü";

    private DialogService dialogService;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        var messages = mock(Messages.class);
        dialogService = mock(DialogService.class);
        fileService = new FileService(dialogService, messages);
    }

    @Test
    void noUnsavedChangesInitially() {
        assertFalse(fileService.hasUnsavedChanges());
    }

    @Test
    void detectsUnsavedChanges() {
        fileService.setCurrentContent("new content");
        assertTrue(fileService.hasUnsavedChanges());
    }

    @Test
    void openIsAbortedWhenUnsavedChangesAreCancelled() {
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.CANCEL);

        assertTrue(fileService.openFile(null).isEmpty());

        verify(dialogService, never()).showOpenDialog(any());
        assertEquals("draft", fileService.getCurrentContent());
    }

    @Test
    void openDiscardsDraftOnlyOnExplicitConfirmation(@TempDir Path dir) throws IOException {
        var target = dir.resolve("opened.txt");
        Files.writeString(target, "from disk");
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.DISCARD);
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));

        var opened = fileService.openFile(null);

        assertTrue(opened.isPresent());
        assertEquals("from disk", fileService.getCurrentContent());
    }

    @Test
    void openIsAbortedWhenSavingIsCancelled() {
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.SAVE);
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.empty());

        assertTrue(fileService.openFile(null).isEmpty());

        verify(dialogService, never()).showOpenDialog(any());
        assertEquals("draft", fileService.getCurrentContent());
    }

    @Test
    void openSavesDraftFirstWhenRequested(@TempDir Path dir) throws IOException {
        var backup = dir.resolve("backup.txt");
        var target = dir.resolve("opened.txt");
        Files.writeString(target, "from disk");
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.SAVE);
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(backup.toFile()));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));

        assertTrue(fileService.openFile(null).isPresent());

        assertEquals("draft", Files.readString(backup));
        assertEquals("from disk", fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
    }

    // --- data safety: atomic writes, backups, listener hooks ---------------

    @Test
    void overwriteKeepsPreviousVersionInBackup(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        fileService.saveFile(null, "v1");
        fileService.saveFile(null, "v2");

        assertEquals("v2", Files.readString(target));
        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));
    }

    @Test
    void interruptedSavesLeaveNoTempFilesBehind(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        fileService.saveFile(null, "v1");
        fileService.saveFile(null, "v2");

        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    // --- encoding stability -------------------------------------------------

    @Test
    void legacyEncodedFileSurvivesOpenAndSaveRoundTrip(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        Files.write(target, ansi);
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);

        assertTrue(fileService.openFile(null).isPresent());
        assertEquals(TURKISH, fileService.getCurrentContent());

        fileService.saveFile(null, fileService.getCurrentContent());

        // byte-identical round trip: saving must never switch the file to UTF-8
        assertArrayEquals(ansi, Files.readAllBytes(target));
    }

    @Test
    void cancellingTheEncodingPromptAbortsTheOpen(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        Files.write(target, TURKISH.getBytes(TextCodec.WINDOWS_1254));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.CANCEL);

        assertTrue(fileService.openFile(null).isEmpty());
    }

    @Test
    void validUtf8FileOpensWithoutAnyEncodingPrompt(@TempDir Path dir) throws IOException {
        var target = dir.resolve("utf8.txt");
        Files.write(target, "héllo wörld".getBytes(StandardCharsets.UTF_8));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));

        assertTrue(fileService.openFile(null).isPresent());

        assertEquals("héllo wörld", fileService.getCurrentContent());
        verify(dialogService, never()).showEncodingDialog(any(), any());
    }

    // --- lifecycle listeners and recovery attachment ------------------------

    @Test
    void loadFileRestoresADocumentWithoutAFilePicker(@TempDir Path dir) throws IOException {
        var target = dir.resolve("last-session.txt");
        Files.writeString(target, "where I left off");
        fileService.setCurrentContent("untitled scratch");

        var loaded = fileService.loadFile(null, target);

        assertTrue(loaded.isPresent());
        assertEquals("where I left off", fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
        verify(dialogService, never()).showOpenDialog(any());
    }

    @Test
    void notifiesListenersAboutLoadsAndSaves(@TempDir Path dir) throws IOException {
        var events = new ArrayList<String>();
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onLoaded(Path file) {
                events.add("loaded:" + file.getFileName());
            }

            @Override
            public void onSaved(Path file) {
                events.add("saved:" + file.getFileName());
            }
        });
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "from disk");
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        fileService.openFile(null);
        fileService.saveFile(null, "changed");

        assertEquals(List.of("loaded:doc.txt", "saved:doc.txt"), events);
    }

    @Test
    void restoredRecoveryDraftIsDirtyAndCanBeSavedThereAgain(@TempDir Path dir) throws IOException {
        var target = dir.resolve("novel.txt");
        Files.write(target, "published chapter".getBytes(TextCodec.WINDOWS_1254));

        fileService.restoreRecovered(target, "new chapter");

        assertEquals("new chapter", fileService.getCurrentContent());
        assertTrue(fileService.hasUnsavedChanges());

        fileService.saveFile(null, fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
    }

    @Test
    void restoredDraftWithoutAFileIsDirty(@TempDir Path dir) {
        fileService.restoreRecovered(null, "scratch text");

        assertTrue(fileService.hasUnsavedChanges());
        assertEquals(null, fileService.getCurrentFilePath());
    }

    // --- encoding upgrade: never silently turn characters into '?' ---------

    private static final String MODERN = "Merhaba \uD83D\uDE00 \u4F60\u597D";

    @Test
    void charactersThatDoNotFitTheEncodingAreSavedAsUtf8Instead(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        Files.write(target, TURKISH.getBytes(TextCodec.WINDOWS_1254));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);
        assertTrue(fileService.openFile(null).isPresent());

        when(dialogService.showEncodingUpgradeDialog(any(), any())).thenReturn(true);
        fileService.setCurrentContent(MODERN);

        var saved = fileService.saveFile(null, fileService.getCurrentContent());

        assertTrue(saved.isPresent());
        verify(dialogService).showEncodingUpgradeDialog(any(), eq(TextCodec.WINDOWS_1254));
        // the text is intact and the document now lives in UTF-8
        assertEquals(MODERN, Files.readString(target, StandardCharsets.UTF_8));
        assertFalse(fileService.hasUnsavedChanges());
    }

    @Test
    void cancellingTheEncodingUpgradeLeavesTheFileUntouched(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        Files.write(target, ansi);
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);
        assertTrue(fileService.openFile(null).isPresent());

        when(dialogService.showEncodingUpgradeDialog(any(), any())).thenReturn(false);
        fileService.setCurrentContent(MODERN);

        assertTrue(fileService.saveFile(null, fileService.getCurrentContent()).isEmpty());

        assertArrayEquals(ansi, Files.readAllBytes(target)); // nothing written, no '?' anywhere
        assertTrue(fileService.hasUnsavedChanges()); // the draft is still there to be saved
    }
}