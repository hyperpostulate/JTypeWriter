package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The window title is the only place a user can see which document is open and
 * whether it is saved (the toolbar can be hidden). The composition is pure so
 * it can be pinned down here without a JavaFX toolkit.
 */
class MainControllerTest {

    @Test
    void titleShowsTheApplicationAndDocumentName() {
        assertEquals("JTypeWriter - novel.txt",
                MainController.titleFor("JTypeWriter", "novel.txt", false, false));
    }

    @Test
    void unsavedChangesAreMarkedWithAStar() {
        assertEquals("JTypeWriter - novel.txt *",
                MainController.titleFor("JTypeWriter", "novel.txt", true, false));
    }

    @Test
    void yoloBadgeIsAppendedAfterTheDirtyMarker() {
        assertEquals("JTypeWriter - novel.txt *   [YOLO]",
                MainController.titleFor("JTypeWriter", "novel.txt", true, true));
        assertEquals("JTypeWriter - novel.txt   [YOLO]",
                MainController.titleFor("JTypeWriter", "novel.txt", false, true));
    }
}
