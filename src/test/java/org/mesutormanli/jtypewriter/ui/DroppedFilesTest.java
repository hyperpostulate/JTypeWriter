/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The drop decision, headless. Every case here used to need a drag gesture and
 * a desktop to exercise; resolving a {@code List<Path>} is pure, so the whole
 * matrix is a unit test instead.
 *
 * <p>The assertion that matters in the refusals is not only the
 * {@link DroppedFiles.Problem} but that no document came back: a refusal that
 * still opened something would be the silent behaviour this card forbids.</p>
 */
class DroppedFilesTest {

    @Test
    void aDragboardWithoutFilesHasNothingToOpen() {
        var resolved = DroppedFiles.resolve(null);

        assertTrue(resolved.document().isEmpty());
        assertEquals(DroppedFiles.Problem.NOTHING_TO_OPEN, resolved.problem().orElseThrow());
        assertNull(resolved.description());
    }

    @Test
    void anEmptyFileListHasNothingToOpen() {
        var resolved = DroppedFiles.resolve(List.of());

        assertEquals(DroppedFiles.Problem.NOTHING_TO_OPEN, resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
    }

    @Test
    void nullEntriesInTheDroppedListAreIgnored(@TempDir Path dir) throws IOException {
        var file = dir.resolve("notes.txt");
        Files.writeString(file, "text\n");
        var dropped = Arrays.asList(null, file);

        var resolved = DroppedFiles.resolve(dropped);

        assertEquals(file.toAbsolutePath().normalize(), resolved.document().orElseThrow());
        assertTrue(resolved.problem().isEmpty());
    }

    @Test
    void oneTextDocumentBecomesTheDocumentToOpen(@TempDir Path dir) throws IOException {
        var file = dir.resolve("chapter.txt");
        Files.writeString(file, "text\n");

        var resolved = DroppedFiles.resolve(List.of(file));

        assertEquals(file.toAbsolutePath().normalize(), resolved.document().orElseThrow());
        assertTrue(resolved.problem().isEmpty());
    }

    @Test
    void aMarkdownDocumentIsAccepted(@TempDir Path dir) throws IOException {
        var file = dir.resolve("README.md");
        Files.writeString(file, "# title\n");

        assertTrue(DroppedFiles.resolve(List.of(file)).document().isPresent());
    }

    @Test
    void theExtensionCheckIgnoresCase(@TempDir Path dir) throws IOException {
        var file = dir.resolve("NOTES.TXT");
        Files.writeString(file, "text\n");

        assertTrue(DroppedFiles.resolve(List.of(file)).document().isPresent());
    }

    @Test
    void severalFilesAreRefusedInsteadOfPickingOne(@TempDir Path dir) throws IOException {
        var first = dir.resolve("one.txt");
        var second = dir.resolve("two.md");
        Files.writeString(first, "one\n");
        Files.writeString(second, "two\n");

        var resolved = DroppedFiles.resolve(List.of(first, second));

        assertEquals(DroppedFiles.Problem.MULTIPLE_FILES, resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
        // the message names the count, so the description is the number itself
        assertEquals("2", resolved.description());
    }

    @Test
    void aPdfIsRefusedBeforeTheFileSystemIsConsulted(@TempDir Path dir) throws IOException {
        var file = dir.resolve("paper.pdf");
        Files.writeString(file, "%PDF-1.7\n");

        var resolved = DroppedFiles.resolve(List.of(file));

        assertEquals(DroppedFiles.Problem.UNSUPPORTED_TYPE, resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
        assertEquals(file.toAbsolutePath().normalize().toString(), resolved.description());
    }

    @Test
    void aFileWithoutAnExtensionIsRefused(@TempDir Path dir) throws IOException {
        var file = dir.resolve("LICENSE");
        Files.writeString(file, "text\n");

        assertEquals(DroppedFiles.Problem.UNSUPPORTED_TYPE,
                DroppedFiles.resolve(List.of(file)).problem().orElseThrow());
    }

    @Test
    void aSupportedNameThatVanishedBeforeTheDropIsReported(@TempDir Path dir) {
        var gone = dir.resolve("deleted.txt");

        var resolved = DroppedFiles.resolve(List.of(gone));

        assertEquals(DroppedFiles.Problem.MISSING, resolved.problem().orElseThrow());
        assertTrue(resolved.document().isEmpty());
    }

    @Test
    void aDirectoryNamedLikeADocumentIsNotADocument(@TempDir Path dir) throws IOException {
        var folder = dir.resolve("notes.txt");
        Files.createDirectory(folder);

        assertEquals(DroppedFiles.Problem.NOT_A_FILE,
                DroppedFiles.resolve(List.of(folder)).problem().orElseThrow());
    }

    @Test
    void aSymbolicLinkKeepsItsOwnNameInsteadOfBecomingItsTarget(@TempDir Path dir)
            throws IOException {
        var target = dir.resolve("real.txt");
        Files.writeString(target, "text\n");
        var link = dir.resolve("shortcut.txt");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException unsupported) {
            assumeTrue(false, "this platform cannot create symbolic links: " + unsupported);
        }

        var resolved = DroppedFiles.resolve(List.of(link));

        // the same reason as StartupArguments: the write path follows the link,
        // the document keeps the name the user dropped
        assertEquals(link.toAbsolutePath().normalize(), resolved.document().orElseThrow());
    }

    @Test
    void onlyTxtAndMdAreSupported() {
        assertTrue(DroppedFiles.isSupportedExtension(Path.of("a.txt")));
        assertTrue(DroppedFiles.isSupportedExtension(Path.of("a.md")));
        assertTrue(DroppedFiles.isSupportedExtension(Path.of("/tmp/dir/a.TXT")));
        assertFalse(DroppedFiles.isSupportedExtension(Path.of("a.pdf")));
        assertFalse(DroppedFiles.isSupportedExtension(Path.of("a.txt~")));
        // an extension ends at a dot: the suffix check must not accept a name
        // that merely ends with the same letters
        assertFalse(DroppedFiles.isSupportedExtension(Path.of("multitxt")));
    }
}
