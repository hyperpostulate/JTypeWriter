/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a recent-document row is named.
 *
 * <p>This is the one rule of the recent-files card that is not already covered
 * by {@code RecentFiles} and {@code AppSettings}: which text a row shows. It is
 * pinned here because the answer has to be a file name - a card full of
 * absolute paths is unreadable and leaks the home directory into every
 * screenshot - while the full path stays available as the row's tooltip.</p>
 */
class RecentFilesCardTest {

    @Test
    void aRowIsNamedByItsFileNameNotItsFullPath() {
        assertEquals("notes.txt", RecentFilesCard.rowLabel(Path.of("/home/user/documents/notes.txt")));
    }

    /**
     * A path with no file name - the root of a file system, or an artificial
     * path the settings file could contain - must not produce an empty row.
     */
    @Test
    void aPathWithoutAFileNameFallsBackToTheWholePath() {
        assertEquals(Path.of("/").toString(), RecentFilesCard.rowLabel(Path.of("/")));
    }
}
