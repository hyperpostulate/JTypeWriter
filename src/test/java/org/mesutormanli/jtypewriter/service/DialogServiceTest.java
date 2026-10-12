package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The save-as name rules are pure logic kept away from the (headless-hostile)
 * FileChooser so they can be pinned down here.
 */
class DialogServiceTest {

    @Test
    void extensionLessNamesGetTheTxtExtension() {
        assertEquals("notes.txt", DialogService.withDefaultExtension(new File("notes")).getName());
    }

    @Test
    void explicitExtensionsAreKeptAsTyped() {
        assertEquals("notes.md", DialogService.withDefaultExtension(new File("notes.md")).getName());
        assertEquals("archive.tar.gz", DialogService.withDefaultExtension(new File("archive.tar.gz")).getName());
    }

    @Test
    void theSiblingStaysInPlace() {
        var saved = DialogService.withDefaultExtension(new File("novels/notes"));

        assertEquals(new File("novels/notes.txt"), saved);
    }
}