/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Keeps the README's test-class table complete instead of aspirational.
 *
 * <p>The table is hand-written, so it decayed exactly the way a hand-written
 * list does: it once documented 46 of 61 classes and silently omitted 15, and
 * nothing failed. The fix is not a stricter review habit but a derivation - the
 * test sources are the list, and the table must name every one of them in both
 * languages. Adding a test class now means adding its row, the same way adding
 * a string means editing six bundles ({@code MessagesTest}) and adding an
 * artifact upload means a retention ({@code ReleaseNotesTest}).</p>
 *
 * <p>Both directions are checked. A missing row makes the document incomplete;
 * a row naming a class that no longer exists makes it wrong, which is the
 * failure the table actually shipped (it described {@code UndoHistoryReset},
 * deleted long before). Neither is a formatting preference, so neither is
 * tolerated.</p>
 *
 * <p>Headless and read-only: two files and one directory walk, no network, no
 * toolkit.</p>
 */
class ReadmeTestTableTest {

    private static final Path EN = Path.of("README.md");
    private static final Path TR = Path.of("README-TR.md");

    /** A table row of the form {@code | `ClassName` | description |}. */
    private static final Pattern ROW = Pattern.compile("^\\| `([A-Za-z0-9]+)` \\|", Pattern.MULTILINE);

    @Test
    void everyTestClassHasARowInBothReadmes() throws IOException {
        var missing = new ArrayList<String>();
        var documented = namesIn(EN);
        for (var name : testClasses()) {
            if (!documented.contains(name)) {
                missing.add(name);
            }
        }
        assertTrue(missing.isEmpty(),
                EN + " does not list these test classes: " + new TreeSet<>(missing));
    }

    /**
     * The Turkish half is a translation, not a summary: a class documented only
     * in English leaves a Turkish reader without the row.
     */
    @Test
    void theTurkishTableIsAsCompleteAsTheEnglishOne() throws IOException {
        var english = namesIn(EN);
        var turkish = namesIn(TR);
        var missing = new TreeSet<>(english);
        missing.removeAll(turkish);
        assertTrue(missing.isEmpty(), TR + " is missing rows present in " + EN + ": " + missing);
    }

    /** A row for a class that no longer exists is worse than a missing row. */
    @Test
    void everyRowNamesATestClassThatExists() throws IOException {
        var existing = testClasses();
        var stale = new TreeSet<String>();
        for (var name : namesIn(EN)) {
            if (name.endsWith("Test") && !existing.contains(name)) {
                stale.add(name);
            }
        }
        assertTrue(stale.isEmpty(),
                EN + " documents test classes that are not on disk: " + stale);
    }

    /** Simple names of every {@code *Test.java} under {@code src/test}. */
    private static Set<String> testClasses() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src", "test"))) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith("Test.java"))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<String> namesIn(Path readme) throws IOException {
        var text = Files.readString(readme, StandardCharsets.UTF_8);
        var names = new TreeSet<String>();
        var matcher = ROW.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
