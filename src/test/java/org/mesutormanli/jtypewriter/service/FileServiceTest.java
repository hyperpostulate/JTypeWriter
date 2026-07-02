package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(null);
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
    void noUnsavedChangesAfterMatchingSavedContent() throws IOException {
        fileService.setCurrentContent("content");
        assertTrue(fileService.hasUnsavedChanges());
    }

    @Test
    void hasUnsavedChangesReturnsFalseForMatchingContent() {
        assertFalse(fileService.hasUnsavedChanges());
    }
}
