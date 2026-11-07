/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The find/replace model, headless. Everything the panel needs to get right can
 * be decided without a {@code TextArea}: the scan, the cursor, wrapping, and how
 * a replacement moves the text under it. The tests are written as the sentences
 * the panel will rely on, so a change of behaviour reads as a broken promise
 * rather than as a changed assertion.
 */
class FindModelTest {

    @Test
    void anEmptyQueryHasNoMatches() {
        // an empty needle matches at every position in a naive loop; it is not
        // a search, and the panel must not highlight the whole document
        assertEquals(List.of(), FindModel.findAll("abc", "", true));
    }

    @Test
    void anEmptyDocumentHasNoMatches() {
        assertEquals(List.of(), FindModel.findAll("", "abc", true));
    }

    @Test
    void aQueryLongerThanTheDocumentHasNoMatches() {
        assertEquals(List.of(), FindModel.findAll("ab", "abc", true));
    }

    @Test
    void everyNonOverlappingOccurrenceIsReportedOnce() {
        assertEquals(List.of(match(0, 2), match(2, 4)), FindModel.findAll("aaaa", "aa", true));
    }

    @Test
    void overlappingOccurrencesAreNotReportedTwice() {
        // "aaa" contains "aa" at 0 and at 1; a replace pass driven by
        // overlapping ranges would apply the second one to text it no longer
        // describes, so the scan advances past each match
        assertEquals(List.of(match(0, 2)), FindModel.findAll("aaa", "aa", true));
    }

    @Test
    void theQueryIsLiteralTextNotARegularExpression() {
        assertEquals(List.of(match(0, 4)), FindModel.findAll("a.*b axb", "a.*b", true));
    }

    @Test
    void aQueryMaySpanALineBreak() {
        assertEquals(List.of(match(1, 4)), FindModel.findAll("ab\nc", "b\nc", true));
    }

    @Test
    void aCaseSensitiveSearchSkipsADifferentCase() {
        assertEquals(List.of(match(4, 7)), FindModel.findAll("Foo foo", "foo", true));
    }

    @Test
    void aCaseInsensitiveSearchFindsBothCases() {
        assertEquals(List.of(match(0, 3), match(4, 7)), FindModel.findAll("Foo foo", "foo", false));
    }

    @Test
    void caseFoldingDoesNotFollowTheDefaultLocale() {
        // under a Turkish default, String.toLowerCase() turns "I" into "ı", so a
        // locale-sensitive implementation would stop matching ASCII "I" with "i"
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertEquals(List.of(match(0, 1), match(2, 3)),
                    FindModel.findAll("I i", "i", false));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void theFirstNextSelectsTheFirstMatch() {
        var model = new FindModel();
        model.search("one two one", "one", true);

        assertEquals(-1, model.currentIndex());
        assertEquals(match(0, 3), model.next().orElseThrow());
        assertEquals(0, model.currentIndex());
    }

    @Test
    void nextWrapsFromTheLastMatchToTheFirst() {
        var model = new FindModel();
        model.search("a a", "a", true);
        model.next();
        assertEquals(match(2, 3), model.next().orElseThrow());

        assertEquals(match(0, 1), model.next().orElseThrow());
    }

    @Test
    void previousFromTheFirstMatchWrapsToTheLast() {
        var model = new FindModel();
        model.search("a a", "a", true);
        model.next();

        assertEquals(match(2, 3), model.previous().orElseThrow());
    }

    @Test
    void previousWithNoSelectionStartsAtTheLastMatch() {
        var model = new FindModel();
        model.search("a a", "a", true);

        assertEquals(match(2, 3), model.previous().orElseThrow());
    }

    @Test
    void navigationWithoutMatchesStaysEmpty() {
        var model = new FindModel();
        model.search("abc", "zz", true);

        assertTrue(model.next().isEmpty());
        assertTrue(model.previous().isEmpty());
        assertEquals(-1, model.currentIndex());
    }

    @Test
    void aNewSearchStartsBeforeTheFirstMatchAgain() {
        var model = new FindModel();
        model.search("a a a", "a", true);
        model.next();
        model.next();

        model.search("a a a", "a", true);

        assertEquals(-1, model.currentIndex());
        assertTrue(model.current().isEmpty());
        assertEquals(match(0, 1), model.next().orElseThrow());
    }

    @Test
    void editingTheDocumentDropsMatchesUntilTheNextScan() {
        var model = new FindModel();
        model.search("one two", "one", true);
        model.next();

        model.setDocument("one two three");

        // the old range described the old text; the panel rescans off the FX
        // thread and installs the new result
        assertEquals(0, model.matchCount());
        assertTrue(model.current().isEmpty());
    }

    @Test
    void anInstalledScanBecomesTheMatchList() {
        var model = new FindModel();
        model.setQuery("a", true);
        model.setDocument("a a");

        assertTrue(model.install("a a", "a", true, FindModel.findAll("a a", "a", true)));

        assertEquals(2, model.matchCount());
        assertEquals(match(0, 1), model.next().orElseThrow());
    }

    @Test
    void anInstalledScanIsIgnoredWhenTheDocumentHasChanged() {
        var model = new FindModel();
        model.setQuery("ab", true);
        model.setDocument("new text");

        // the worker scanned "old text" and finished after the user typed
        assertFalse(model.install("old text", "ab", true, List.of(match(0, 2))));

        assertEquals(0, model.matchCount());
    }

    @Test
    void anInstalledScanDropsARangeThatRunsPastTheEnd() {
        var model = new FindModel();
        model.setQuery("bc", true);
        model.setDocument("abc");

        assertTrue(model.install("abc", "bc", true, List.of(match(1, 99))));

        assertEquals(0, model.matchCount());
        assertTrue(model.next().isEmpty());
    }

    @Test
    void replaceAllReplacesEveryMatch() {
        var model = new FindModel();
        model.search("one two one", "one", true);

        var result = model.replaceAll("1");

        assertEquals("1 two 1", result.document());
        assertEquals(2, result.count());
        assertTrue(result.changed());
    }

    @Test
    void replaceAllDoesNotRescanItsOwnReplacement() {
        // replacing "a" with "aa" doubles the number of matches every pass; a
        // loop over the changing text would never terminate
        var model = new FindModel();
        model.search("aaa", "a", true);

        var result = model.replaceAll("aa");

        assertEquals("aaaaaa", result.document());
        assertEquals(3, result.count());
    }

    @Test
    void replaceAllAppliesEveryMatchOnceWhenTheLengthChanges() {
        var model = new FindModel();
        model.search("aaaa", "aa", true);

        var result = model.replaceAll("b");

        assertEquals("bb", result.document());
        assertEquals(2, result.count());
    }

    @Test
    void theReplacementIsLiteralTextNotAGroupReference() {
        var model = new FindModel();
        model.search("cat", "cat", true);

        assertEquals("$1", model.replaceAll("$1").document());
    }

    @Test
    void anEmptyReplacementDeletesTheMatches() {
        var model = new FindModel();
        model.search("a-b-c", "-", true);

        assertEquals("abc", model.replaceAll("").document());
    }

    @Test
    void replaceAllWithNoMatchesLeavesTheDocumentAlone() {
        var model = new FindModel();
        model.search("abc", "zz", true);

        var result = model.replaceAll("x");

        assertEquals("abc", result.document());
        assertEquals(0, result.count());
        assertFalse(result.changed());
    }

    @Test
    void replaceCurrentWithoutSelectionChangesNothing() {
        var model = new FindModel();
        model.search("abc", "b", true);

        var result = model.replaceCurrent("x");

        assertEquals("abc", result.document());
        assertEquals(0, result.count());
    }

    @Test
    void replaceCurrentSelectsTheNextMatchAfterTheShift() {
        var model = new FindModel();
        model.search("one two one", "one", true);
        model.next();

        var result = model.replaceCurrent("1");

        assertEquals("1 two one", result.document());
        assertEquals(1, result.count());
        // "one" used to start at 8; losing two characters moves it to 6
        assertEquals(match(6, 9), result.current().orElseThrow());
        assertEquals(match(6, 9), model.current().orElseThrow());
    }

    @Test
    void replaceCurrentMovesPastTheInsertedText() {
        var model = new FindModel();
        model.search("ab x ab", "ab", true);
        model.next();

        var result = model.replaceCurrent("abab");

        assertEquals("abab x ab", result.document());
        // the inserted "abab" holds matches at 0 and 2; the next selection is
        // the original second word, now at 7, never the replacement itself
        assertEquals(match(7, 9), result.current().orElseThrow());
    }

    @Test
    void aRangeThatRunsPastTheEndOfTheDocumentIsRefused() {
        // the exact bug this guard exists for: a match captured before an edit
        // ends past the shortened text, and substring arithmetic would throw
        // after the user already confirmed the replace
        assertTrue(FindModel.replace("abc", match(1, 99), "x").isEmpty());
        assertEquals("axc", FindModel.replace("abc", match(1, 2), "x").orElseThrow());
    }

    private static FindModel.Match match(int start, int end) {
        return new FindModel.Match(start, end);
    }
}
