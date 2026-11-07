/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The recent-document list the welcome card offers, as pure list operations.
 *
 * <p><strong>Why this is not in the card.</strong> Three rules decide whether a
 * row is offered at all - newest first, one row per document, at most
 * {@link #MAX} rows - and a fourth decides whether it is offered <em>now</em>:
 * the document must still exist. In the card those rules would be untestable
 * without a toolkit and would have to be re-derived by every caller; here they
 * are four static methods.</p>
 *
 * <p><strong>Why the existing filter drops instead of marking.</strong> The card
 * is shown at launch, so a row that fails when clicked would be a permanent
 * error the user cannot clear. A document deleted outside the app therefore
 * leaves the list silently - the same rule the session-restore path already
 * follows for a vanished last document.</p>
 *
 * <p>Identity is the absolute, normalised path: the same file named with a
 * relative path, or through {@code ..}, is the same document and must move to
 * the front rather than appear twice.</p>
 */
public final class RecentFiles {

    /**
     * How many documents the card offers. Ten is the largest list that still
     * fits under the shortcut table without turning the greeting into a file
     * manager, and it is also the number of keys the settings file stores.
     */
    public static final int MAX = 10;

    private RecentFiles() {
    }

    /**
     * {@code opened} at the front of {@code current}, without it repeating and
     * without the result exceeding {@link #MAX} entries. The oldest entries fall
     * off the end; nothing else about the order changes.
     */
    public static List<Path> withNewest(List<Path> current, Path opened) {
        var all = new ArrayList<Path>();
        if (opened != null) {
            all.add(opened);
        }
        if (current != null) {
            all.addAll(current);
        }
        return normalized(all);
    }

    /**
     * The same list in stored order with the duplicates removed and the tail cut
     * at {@link #MAX}: the shape a settings file can be read back into, where a
     * hand edit may repeat a path or list more than the card shows.
     */
    public static List<Path> normalized(List<Path> candidates) {
        var result = new ArrayList<Path>();
        for (var candidate : Objects.requireNonNull(candidates, "candidates")) {
            if (result.size() >= MAX) {
                break;
            }
            var identity = identityOf(candidate);
            if (identity == null || result.stream().anyMatch(existing ->
                    identity.equals(identityOf(existing)))) {
                continue;
            }
            result.add(candidate.toAbsolutePath());
        }
        return List.copyOf(result);
    }

    /** The entries that still exist, in order; a vanished document is dropped. */
    public static List<Path> keepingOnly(List<Path> candidates, Predicate<Path> stillThere) {
        var result = new ArrayList<Path>();
        for (var candidate : Objects.requireNonNull(candidates, "candidates")) {
            if (stillThere.test(candidate)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    /**
     * The comparison key for "is this the same document": absolute and
     * normalised, or {@code null} for a path the file system cannot even
     * normalise (a hand-edited settings file can contain anything).
     */
    private static Path identityOf(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (RuntimeException unusable) {
            return null; // InvalidPathException and friends: not a document we can offer
        }
    }
}
