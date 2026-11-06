/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What a drag-and-drop gesture asked the window to open, decided without a
 * single JavaFX type.
 *
 * <p>A {@code Dragboard} hands over {@code java.io.File} objects, which is the
 * only part of a drop that matters here; the rest is the same question the
 * command line asks - is there exactly one readable document? Keeping the answer
 * pure means the whole decision is testable headlessly, the
 * {@code StartupArguments} precedent.</p>
 *
 * <p><strong>Why this is not {@code StartupArguments}.</strong> Both resolve a
 * path, but they filter different inputs. The process argument is text the user
 * typed and may name anything that is a regular file; a drop is a set - the user
 * can drag several files, or a folder, or a PDF - and this application edits
 * plain text, so the type filter belongs here and nowhere else. Sharing one
 * resolver would mean either dropping the filter (a dropped PDF would land in a
 * UTF-8 editor as mojibake) or teaching the startup path to refuse documents it
 * has always opened.</p>
 *
 * <p><strong>Nothing is guessed away.</strong> Zero files, several files, an
 * unsupported extension, a directory, a path that vanished between the drag and
 * the drop: each is a {@link Problem} the caller reports by name. Silently
 * doing nothing after a deliberate drop looks exactly like a broken window.</p>
 *
 * <p><strong>Why the named path, not the real path:</strong> as in
 * {@code StartupArguments}, a symbolic link stays the document's name; the write
 * path already follows the link to its target.</p>
 *
 * @param document    the absolute, normalized path to open; empty unless the drop
 *                    named exactly one readable text document
 * @param problem     why the drop was refused; empty unless one was
 * @param description the dropped path (or the count of dropped paths) as the user
 *                    would name it, kept for the message and the log;
 *                    {@code null} only when the dragboard carried no file at all
 */
public record DroppedFiles(Optional<Path> document, Optional<Problem> problem, String description) {

    /** The document types this editor is willing to take from a drop. */
    static final List<String> SUPPORTED_EXTENSIONS = List.of(".txt", ".md");

    /** Why a drop was not accepted as a document. */
    public enum Problem {
        /** The dragboard carried no file (plain text, a URL, an image, ...). */
        NOTHING_TO_OPEN,
        /** Several files were dropped: opening one of them would be a guess. */
        MULTIPLE_FILES,
        /** Not a {@code .txt} or {@code .md}, so it is not a document for this editor. */
        UNSUPPORTED_TYPE,
        /** The path was valid when the drag started but nothing is there now. */
        MISSING,
        /** A directory, a device or a fifo - not text. */
        NOT_A_FILE
    }

    /** Resolves the files a drop carried; {@code null} means the dragboard had none. */
    public static DroppedFiles resolve(List<Path> dropped) {
        var files = dropped == null
                ? List.<Path>of()
                : dropped.stream().filter(path -> path != null).toList();
        if (files.isEmpty()) {
            return refused(Problem.NOTHING_TO_OPEN, null);
        }
        if (files.size() > 1) {
            return refused(Problem.MULTIPLE_FILES, Integer.toString(files.size()));
        }
        var path = files.get(0).toAbsolutePath().normalize();
        var description = path.toString();
        // The type comes first: "not a .txt file" is the useful answer for a
        // folder of photos, and it is also the only check that does not touch
        // the file system.
        if (!isSupportedExtension(path)) {
            return refused(Problem.UNSUPPORTED_TYPE, description);
        }
        if (!Files.exists(path)) {
            return refused(Problem.MISSING, description);
        }
        if (!Files.isRegularFile(path)) {
            return refused(Problem.NOT_A_FILE, description);
        }
        return new DroppedFiles(Optional.of(path), Optional.empty(), description);
    }

    /** Whether {@code path} has a suffix this editor opens; case is not significant. */
    public static boolean isSupportedExtension(Path path) {
        var name = path.getFileName();
        if (name == null) {
            return false;
        }
        var lowerCase = name.toString().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerCase::endsWith);
    }

    private static DroppedFiles refused(Problem problem, String description) {
        return new DroppedFiles(Optional.empty(), Optional.of(problem), description);
    }
}
