package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mesutormanli.jtypewriter.locale.Messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FileServiceTest {

    private FileService fileService;

    @BeforeEach
    void setUp() {
        var messages = mock(Messages.class);
        var dialogService = mock(DialogService.class);
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
}
