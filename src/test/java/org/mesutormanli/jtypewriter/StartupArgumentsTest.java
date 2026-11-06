/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The decision behind {@code JTypeWriter <file>} is pure, so the failure modes
 * that would otherwise require a packaged launcher are all here: a missing
 * name, a directory, an unparsable path, two names at once.
 *
 * <p>The important assertion in most of these tests is not just the
 * {@link StartupArguments.Problem} but that {@code document} is empty: a
 * refusal that still opened something would be exactly the silent behaviour
 * the card forbids.</p>
 */
class StartupArgumentsTest {

    @Test
    void noArgumentLeavesTheDecisionToTheCaller() {
        var resolved = StartupArguments.resolve(List.of());

        assertTrue(resolved.document().isEmpty());
        assertTrue(resolved.problem().isEmpty());
        assertNull(resolved.argument());
    }

    @Test
    void aNullArgumentListIsTreatedAsNoArgument() {
        var resolved = StartupArguments.resolve(null);

        assertTrue(resolved.document().isEmpty());
        assertTrue(resolved.problem().isEmpty());
    }

    @Test
    void blankArgumentsAreIgnoredBecauseTheyAreNotADocumentRequest(@TempDir Path dir) {
        var file = dir.resolve("notes.txt");
        write(file);

        var resolved = StartupArguments.resolve(List.of("", "   ", file.toString()));

        assertEquals(file.toAbsolutePath().normalize(), resolved.document().orElseThrow());
        assertTrue(resolved.problem().isEmpty());
    }

    @Test
    void anExistingRegularFileBecomesTheDocumentToOpen(@TempDir Path dir) {
        var file = dir.resolve("chapter.txt");
        write(file);

        var resolved = StartupArguments.resolve(List.of(file.toString()));

        assertEquals(file.toAbsolutePath().normalize(), resolved.document().orElseThrow());
        assertTrue(resolved.problem().isEmpty());
        assertEquals(file.toString(), resolved.argument());
    }

    @Test
    void aRelativePathIsResolvedAgainstTheWorkingDirectory(@TempDir Path dir) {
        var file = dir.resolve("relative.txt");
        write(file);
        var workingDirectory = Path.of("").toAbsolutePath();
        var relative = workingDirectory.relativize(file.toAbsolutePath().normalize());

        var resolved = StartupArguments.resolve(List.of(relative.toString()));

        assertEquals(file.toAbsolutePath().normalize(), resolved.document().orElseThrow());
    }

    @Test
    void aPathThatDoesNotExistIsReportedInsteadOfSilentlyIgnored(@TempDir Path dir) {
        var missing = dir.resolve("not-here.txt");

        var resolved = StartupArguments.resolve(List.of(missing.toString()));

        assertEquals(StartupArguments.Problem.MISSING, resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
        assertEquals(missing.toString(), resolved.argument());
    }

    @Test
    void aDirectoryIsNotADocumentToOpen(@TempDir Path dir) {
        var resolved = StartupArguments.resolve(List.of(dir.toString()));

        assertEquals(StartupArguments.Problem.NOT_A_FILE, resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
    }

    @Test
    void moreThanOneArgumentIsRefusedInsteadOfPickingOne(@TempDir Path dir) {
        var first = dir.resolve("one.txt");
        var second = dir.resolve("two.txt");
        write(first);
        write(second);

        var resolved = StartupArguments.resolve(List.of(first.toString(), second.toString()));

        assertEquals(StartupArguments.Problem.TOO_MANY_ARGUMENTS,
                resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
        assertEquals(first + " " + second, resolved.argument());
    }

    @Test
    void aSymbolicLinkKeepsItsOwnNameInsteadOfBecomingItsTarget(@TempDir Path dir)
            throws IOException {
        var target = dir.resolve("real.txt");
        write(target);
        var link = dir.resolve("shortcut.txt");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException unsupported) {
            assumeTrue(false, "this platform cannot create symbolic links: " + unsupported);
        }

        var resolved = StartupArguments.resolve(List.of(link.toString()));

        // Resolving the link would rename the document into a path the user
        // never typed; the write path follows the link on its own (W2-05).
        assertEquals(link.toAbsolutePath().normalize(), resolved.document().orElseThrow());
    }

    @Test
    void textThePlatformCannotParseAsAPathIsReportedAsInvalid() {
        var resolved = StartupArguments.resolve(List.of("bad\0name.txt"));

        assertEquals(StartupArguments.Problem.INVALID_PATH, resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
    }

    private static void write(Path file) {
        try {
            Files.writeString(file, "text\n");
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
