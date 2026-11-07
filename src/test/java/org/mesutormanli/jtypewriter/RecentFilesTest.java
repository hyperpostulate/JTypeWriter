/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The recent-document list is a convenience surface on the welcome card, so
 * every rule about it is a pure list operation: newest first, no duplicates, at
 * most {@link RecentFiles#MAX} entries, and a document that has since been
 * deleted simply stops being offered. Keeping it out of the JavaFX class is what
 * makes those rules testable without a toolkit.
 */
class RecentFilesTest {

    @Test
    void theNewestDocumentComesFirst() {
        var list = RecentFiles.withNewest(List.of(Path.of("/a.txt")), Path.of("/b.txt"));

        assertEquals(List.of(Path.of("/b.txt"), Path.of("/a.txt")), list);
    }

    /**
     * Opening the same document again must move it to the front, not add a
     * second row for it - and the identity is the absolute path, so the same
     * file named with a relative path is the same document.
     */
    @Test
    void reopeningADocumentMovesItToTheFrontInsteadOfDuplicatingIt() {
        var first = Path.of("/one.txt");
        var second = Path.of("/two.txt");

        var list = RecentFiles.withNewest(List.of(second, first), first);

        assertEquals(List.of(first.toAbsolutePath(), second.toAbsolutePath()), list);
    }

    @Test
    void theListNeverGrowsPastTheCap() {
        // The list is newest-first, so the tail is the oldest entry.
        var list = new ArrayList<Path>();
        for (var index = 0; index < RecentFiles.MAX + 5; index++) {
            list.add(Path.of("/document-" + index + ".txt"));
        }

        var recent = RecentFiles.withNewest(list, Path.of("/newest.txt"));

        assertEquals(RecentFiles.MAX, recent.size());
        assertEquals(Path.of("/newest.txt").toAbsolutePath(), recent.get(0));
        assertTrue(recent.contains(Path.of("/document-0.txt").toAbsolutePath()),
                "the cap drops the oldest entries, not the newest");
        assertFalse(recent.contains(
                        Path.of("/document-" + (RecentFiles.MAX + 4) + ".txt").toAbsolutePath()),
                "the oldest entry must fall off the end");
    }

    /**
     * The card must never offer a document that no longer exists: a row that
     * fails when clicked would be a permanent error the user cannot clear.
     */
    @Test
    void documentsThatNoLongerExistAreDroppedFromTheList() {
        var present = Path.of("/present.txt");
        var gone = Path.of("/gone.txt");

        var list = RecentFiles.keepingOnly(List.of(present, gone), path -> path.equals(present));

        assertEquals(List.of(present), list);
    }

    /** An empty list stays empty, and adding to it is not an error. */
    @Test
    void anEmptyListIsValidInBothDirections() {
        assertEquals(List.of(Path.of("/a.txt").toAbsolutePath()),
                RecentFiles.withNewest(List.of(), Path.of("/a.txt")));
        assertEquals(List.of(), RecentFiles.keepingOnly(List.of(), path -> true));
    }

    @Test
    void anEmptyPathIsIgnoredRatherThanStored() {
        assertEquals(List.of(), RecentFiles.withNewest(List.of(), null));
    }
}
