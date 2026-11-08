/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * What the process arguments asked this start to open, resolved without a
 * single JavaFX type.
 *
 * <p>A desktop editor is reachable in two ways that both arrive here as one
 * argument: {@code JTypeWriter notes.txt} on a command line, and a file
 * association handing the same path to the same launcher. Both therefore end
 * in {@code Application.getParameters()}, and the decision what to do with
 * those words is pure - no toolkit, no stage - so it can be tested headlessly
 * like {@code ExitDisposition} or {@code RecoveryTargetPath}.</p>
 *
 * <p><strong>Nothing is guessed away.</strong> A path that does not exist, a
 * path that is not a regular file, a string the platform cannot even parse as
 * a path, and more than one argument are all reported through
 * {@link Problem} instead of being dropped: silently starting with an empty
 * editor after {@code JTypeWriter chapter2.txt} would look exactly like a lost
 * document. Multiple arguments are refused rather than narrowed to the first,
 * because picking one of them would be a guess the user never confirmed.</p>
 *
 * <p><strong>Why the named path, not the real path:</strong> a symbolic link
 * stays the document's name. Loading resolves nothing, so the window title and
 * the remembered session file are the path the user typed, and the write path
 * already follows the link to its target ({@code AtomicFiles.writeMoving}).
 * Resolving here would silently rename a document into a directory the
 * user never mentioned - the same reasoning as
 * {@code RecoveryTargetPath}'s decision/canonical split.</p>
 *
 * <p>The result is always exactly one of three states: a {@code document} to
 * open, a {@code problem} to report, or neither (no argument at all - the
 * caller falls back to the remembered session and the welcome card).</p>
 *
 * @param document the absolute, normalized path to open; empty unless an
 *                 argument was given and it names a readable regular file
 * @param problem  why an argument was refused; empty unless one was refused
 * @param argument the raw argument text, kept for the message and the log -
 *                 {@code null} only when no argument was given
 */
public record StartupArguments(Optional<Path> document, Optional<Problem> problem, String argument) {

    /** Why a startup argument was not accepted as a document. */
    public enum Problem {
        /** The platform cannot turn the text into a path. */
        INVALID_PATH,
        /** The path is syntactically fine but nothing exists there. */
        MISSING,
        /** The path exists but is a directory, device or fifo. */
        NOT_A_FILE,
        /** More than one argument: opening one of them would be a guess. */
        TOO_MANY_ARGUMENTS
    }

    /** No argument on the command line: the caller decides what to open. */
    public static StartupArguments none() {
        return new StartupArguments(Optional.empty(), Optional.empty(), null);
    }

    /**
     * Resolves the raw process arguments (see {@link javafx.application.Application.Parameters#getRaw()}).
     *
     * <p>Blank arguments are ignored, because a shell can hand over an empty
     * word ({@code JTypeWriter ""}) and that is not a document request. The
     * relative path a user types is resolved against the current working
     * directory, which is the only meaning a command-line relative path
     * has.</p>
     */
    public static StartupArguments resolve(List<String> rawArguments) {
        var arguments = rawArguments == null
                ? List.<String>of()
                : rawArguments.stream()
                        .filter(argument -> argument != null && !argument.isBlank())
                        .toList();
        if (arguments.isEmpty()) {
            return none();
        }
        if (arguments.size() > 1) {
            return refused(Problem.TOO_MANY_ARGUMENTS, String.join(" ", arguments));
        }
        return resolveOne(arguments.get(0));
    }

    private static StartupArguments resolveOne(String argument) {
        Path path;
        try {
            path = Path.of(argument).toAbsolutePath().normalize();
        } catch (InvalidPathException invalid) {
            return refused(Problem.INVALID_PATH, argument);
        }
        // A missing path and a permission-denied path both make these return
        // false, and both mean "there is no document to open here". The
        // readable case still has its own error dialog: the load path reports
        // an unreadable file with the platform's reason.
        if (!Files.exists(path)) {
            return refused(Problem.MISSING, argument);
        }
        // Follows links on purpose: a symbolic link to a document is a
        // document. A directory, a device or a fifo is not - opening the last
        // two would block the reader, not show text.
        if (!Files.isRegularFile(path)) {
            return refused(Problem.NOT_A_FILE, argument);
        }
        return new StartupArguments(Optional.of(path), Optional.empty(), argument);
    }

    private static StartupArguments refused(Problem problem, String argument) {
        return new StartupArguments(Optional.empty(), Optional.of(problem), argument);
    }
}
