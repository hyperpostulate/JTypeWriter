/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.AppLogging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validation of the document path a recovery draft claims to belong to.
 *
 * <p>That path decides what the next {@code Ctrl+S} writes to, so it is the one
 * piece of draft metadata that must not be trusted. The draft directory is
 * writable by the user and a draft can arrive from another machine (a synced
 * profile, a restored backup); without a check a crafted header redirects the
 * save to any file the user can write. The rules are therefore structural, not
 * cosmetic:</p>
 * <ol>
 *   <li>the path must be absolute - a relative path would silently resolve
 *       against whatever directory the application happens to be started
 *       in;</li>
 *   <li>it must canonicalise: symbolic links are followed, so a link pointing
 *       outside the allowed roots is rejected by what it points at, not by how
 *       it is spelled; a path whose parent cannot be resolved does not
 *       identify a location;</li>
 *   <li>the canonical path must live under one of the allowed roots - by
 *       default the user's home directory;</li>
 *   <li>it must not live under a denied root - the application's own data
 *       directory (settings, logs, drafts) is never a document.</li>
 * </ol>
 *
 * <p>Rejection is never a data loss: the draft text is still offered, only
 * without a target, which forces Save As. An empty allowed-root set rejects
 * every path (fail closed) rather than accepting all of them (fail open).</p>
 *
 * <p>Pure and headless: no JavaFX types, no state, every decision is a pure
 * function of the candidate and the roots.</p>
 */
final class RecoveryTargetPath {

    private static final Logger LOG = AppLogging.logger(RecoveryTargetPath.class);

    private RecoveryTargetPath() {
    }

    /**
     * Validates the candidate and returns the path to keep as the document's
     * identity.
     *
     * <p>The canonical form decides only whether the path is <em>allowed</em>;
     * the value handed back is the candidate as it was written. That matters on
     * Windows, where {@code toRealPath()} expands an 8.3 short name
     * ({@code RUNNER~1} becomes {@code runneradmin}), and for a symbolic link,
     * which would otherwise resurface as its target: the document would
     * suddenly be known by a path the user never opened. Writing is safe
     * either way, because {@link org.mesutormanli.jtypewriter.AtomicFiles}
     * resolves links itself.</p>
     *
     * @return the absolute, normalised candidate when it is acceptable as a
     *         save target, or {@code null} when the draft must be opened
     *         without one
     */
    static Path accept(Path candidate, Collection<Path> allowedRoots,
                       Collection<Path> deniedRoots) {
        if (candidate == null) {
            return null;
        }
        if (!candidate.isAbsolute()) {
            LOG.log(Level.WARNING, "ignoring relative original path in a recovery draft: " + candidate);
            return null;
        }
        var canonical = canonicalize(candidate);
        if (canonical == null || canonical.getFileName() == null) {
            LOG.log(Level.WARNING, "ignoring unresolvable original path in a recovery draft: " + candidate);
            return null;
        }
        if (isUnder(canonical, deniedRoots)) {
            LOG.log(Level.WARNING, "refusing a recovery draft whose original path points into the"
                    + " application data directory: " + canonical);
            return null;
        }
        if (allowedRoots == null || allowedRoots.isEmpty() || !isUnder(canonical, allowedRoots)) {
            LOG.log(Level.WARNING, "refusing a recovery draft whose original path is outside the"
                    + " allowed roots: " + canonical);
            return null;
        }
        return candidate.normalize();
    }

    /**
     * Follows a symbolic link to the file it names. A path that does not exist
     * yet is resolved through its parent, so a draft written for a document
     * that was deleted in the meantime can still be saved back to that
     * location. A dangling link is not such a case - saving "through" it would
     * replace the link with a regular file - and is rejected.
     */
    private static Path canonicalize(Path path) {
        var normalized = path.normalize();
        try {
            return normalized.toRealPath();
        } catch (IOException notResolvable) {
            if (Files.isSymbolicLink(normalized)) {
                return null;
            }
            var parent = normalized.getParent();
            if (parent == null) {
                return null;
            }
            try {
                return parent.toRealPath().resolve(normalized.getFileName());
            } catch (IOException | SecurityException parentUnresolvable) {
                return null;
            }
        }
    }

    private static boolean isUnder(Path path, Collection<Path> roots) {
        if (roots == null) {
            return false;
        }
        for (var root : roots) {
            if (root == null) {
                continue;
            }
            var realRoot = realRoot(root);
            if (realRoot != null && path.startsWith(realRoot)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The root itself may be a link (a home directory on another volume), so it
     * is canonicalised the same way. A root that cannot be resolved is compared
     * in its absolute, normalised form instead of being dropped.
     */
    private static Path realRoot(Path root) {
        try {
            return root.toRealPath();
        } catch (IOException | SecurityException e) {
            return root.toAbsolutePath().normalize();
        }
    }
}
