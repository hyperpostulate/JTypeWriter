/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The guard between a recovery draft's header and the next {@code Ctrl+S}.
 *
 * <p>A draft lives in a directory the user never watches, and it can arrive
 * from another machine (a synced profile, a restored backup). The path in its
 * header becomes the save target, so every rule here is a rule about what that
 * header is allowed to decide.</p>
 */
class RecoveryTargetPathTest {

    @Test
    void anAbsolutePathUnderAnAllowedRootIsAccepted(@TempDir Path dir) throws IOException {
        var document = Files.writeString(dir.resolve("roman.txt"), "metin");

        assertEquals(document.toAbsolutePath(),
                RecoveryTargetPath.accept(document, List.of(dir), List.of()));
    }

    /**
     * A relative path would resolve against whatever directory the application
     * happens to be started in - the desktop launcher's, the shell's, a test
     * runner's. A save must never depend on that.
     */
    @Test
    void aRelativePathIsRejected(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("roman.txt"), "metin");

        assertNull(RecoveryTargetPath.accept(Path.of("roman.txt"), List.of(dir), List.of()));
    }

    @Test
    void aPathOutsideTheAllowedRootsIsRejected(@TempDir Path dir) throws IOException {
        var allowed = Files.createDirectory(dir.resolve("izinli"));
        var outside = Files.writeString(dir.resolve("baska.txt"), "baska");

        assertNull(RecoveryTargetPath.accept(outside, List.of(allowed), List.of()));
    }

    /**
     * Fail closed: an empty root set means "no location is trusted", never
     * "every location is". A configuration that silently widened the accepted
     * paths would turn this check into decoration.
     */
    @Test
    void noAllowedRootsRejectsEveryPath(@TempDir Path dir) throws IOException {
        var document = Files.writeString(dir.resolve("roman.txt"), "metin");

        assertNull(RecoveryTargetPath.accept(document, Set.of(), List.of()));
        assertNull(RecoveryTargetPath.accept(document, null, List.of()));
    }

    /**
     * The resolution is by real target, not by spelling: a link placed inside
     * the trusted folder must not be a way out of it.
     */
    @Test
    void aSymbolicLinkIsJudgedByTheTargetItPointsAt(@TempDir Path dir) throws IOException {
        var allowed = Files.createDirectory(dir.resolve("izinli"));
        var secret = Files.writeString(dir.resolve("gizli.txt"), "gizli");
        var link = allowed.resolve("zararsiz.txt");
        try {
            Files.createSymbolicLink(link, secret);
        } catch (IOException | UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("symbolic links unavailable: " + e);
        }

        assertNull(RecoveryTargetPath.accept(link, List.of(allowed), List.of()),
                "the link is inside the root, but its target is not");
    }

    /** The parent traversal spellings are normalised before the root check. */
    @Test
    void parentTraversalCannotEscapeTheAllowedRoot(@TempDir Path dir) throws IOException {
        var allowed = Files.createDirectory(dir.resolve("izinli"));
        Files.writeString(dir.resolve("baska.txt"), "baska");

        var escape = allowed.resolve("..").resolve("baska.txt");

        assertNull(RecoveryTargetPath.accept(escape, List.of(allowed), List.of()));
    }

    /**
     * Validation may follow a link, but the returned identity must not: a
     * document opened through a link stays known by the name the user used.
     * Canonicalising the value instead would resurface every document as its
     * real path - and on Windows it rewrites an 8.3 short name
     * ({@code RUNNER~1}) into its long form, which the draft's own header never
     * held.
     */
    @Test
    void anAcceptedSymbolicLinkKeepsItsOwnName(@TempDir Path dir) throws IOException {
        var real = Files.writeString(dir.resolve("gercek.txt"), "metin");
        var link = dir.resolve("kisa-yol.txt");
        try {
            Files.createSymbolicLink(link, real);
        } catch (IOException | UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("symbolic links unavailable: " + e);
        }

        assertEquals(link.toAbsolutePath(),
                RecoveryTargetPath.accept(link, List.of(dir), List.of()));
    }

    /**
     * A document deleted while the editor was closed is still a legitimate
     * target: the draft holds its only text, and the user's intent is to write
     * it back where it was. The file itself cannot be resolved, so its parent
     * decides.
     */
    @Test
    void aDocumentThatNoLongerExistsIsAcceptedThroughItsParent(@TempDir Path dir) throws IOException {
        var gone = dir.resolve("silinmis.txt");

        assertEquals(gone.toAbsolutePath(),
                RecoveryTargetPath.accept(gone, List.of(dir), List.of()));
    }

    /**
     * A dangling link is different: saving "through" it would replace the link
     * with a regular file. That is the same defect the write path already
     * avoids, so it must not be introduced here.
     */
    @Test
    void aDanglingSymbolicLinkIsRejected(@TempDir Path dir) throws IOException {
        var link = dir.resolve("kirik.txt");
        try {
            Files.createSymbolicLink(link, dir.resolve("yok.txt"));
        } catch (IOException | UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("symbolic links unavailable: " + e);
        }

        assertNull(RecoveryTargetPath.accept(link, List.of(dir), List.of()));
    }

    /**
     * The application's own directory holds settings, logs and drafts. None of
     * them is a document, and a draft that names one is asking the editor to
     * rewrite its own state or another session's draft.
     */
    @Test
    void aPathInsideTheDeniedRootIsRejected(@TempDir Path dir) throws IOException {
        var home = Files.createDirectory(dir.resolve("home"));
        var dataDir = Files.createDirectories(home.resolve(".jtypewriter"));
        var settings = Files.writeString(dataDir.resolve("settings.properties"), "theme=dark");
        var document = Files.writeString(home.resolve("roman.txt"), "metin");

        assertNull(RecoveryTargetPath.accept(settings, List.of(home), List.of(dataDir)));
        assertEquals(document.toAbsolutePath(),
                RecoveryTargetPath.accept(document, List.of(home), List.of(dataDir)));
    }
}
