package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

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
        when(dialogService.showSaveDialog(any())).thenReturn(Optional.empty());

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
        when(dialogService.showSaveDialog(any())).thenReturn(Optional.of(backup.toFile()));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));

        assertTrue(fileService.openFile(null).isPresent());

        assertEquals("draft", Files.readString(backup));
        assertEquals("from disk", fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
    }
}