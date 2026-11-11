/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * How long a parked recovery draft is worth keeping.
 *
 * <p>Parking exists so that a postponed draft does not block the active slot
 * (see {@link RecoveryStore}), which means a parked file is written once and
 * never again: without a policy parked files accumulate in the user's home
 * directory for the lifetime of the installation, and every "Later" answer
 * costs disk space that nothing ever reclaims.</p>
 *
 * <p><b>Only parked files are candidates.</b> A {@code draft-*.jtw} may belong
 * to a second instance running right now - the slot name carries a per-session
 * UUID - so deleting one could delete another process's live data. A
 * {@code pending-*.jtw} was abandoned by the session that wrote it, which is
 * exactly what makes it safe to expire.</p>
 *
 * <p>The policy is a pure function of the directory listing rather than logic
 * inline in {@link RecoveryStore}, so it can be tested without creating thirty
 * days of files: a rule that deletes text is not a rule to verify by
 * inspection. The rules apply in a fixed order:</p>
 * <ol>
 *   <li><b>age</b> - a draft the user postponed more than {@link #MAX_AGE} ago
 *       is no longer a recovery candidate, it is clutter;</li>
 *   <li><b>count</b> - the newest {@link #MAX_FILES} survivors stay;</li>
 *   <li><b>size</b> - walking newest first, an entry that no longer fits the
 *       {@link #MAX_TOTAL_BYTES} budget is skipped, but the walk continues:
 *       an older draft can be small enough to fit after an oversized one.
 *       The newest entry is kept even when it alone is larger than the
 *       budget: the budget exists to stop accumulation, and a rule that
 *       deletes the only candidate would turn housekeeping into data
 *       loss.</li>
 * </ol>
 */
public final class RecoveryRetention {

    /** Parked drafts older than this are housekeeping, not history. */
    static final Duration MAX_AGE = Duration.ofDays(30);

    /** How many postponed documents are still worth offering. */
    static final int MAX_FILES = 10;

    /** Total bytes the parked set may occupy. */
    static final long MAX_TOTAL_BYTES = 64L * 1024 * 1024;

    private RecoveryRetention() {
    }

    /** A parked file as the scan saw it: when it was written and how big it is. */
    public record Entry(Path file, Instant savedAt, long size) {
    }

    /**
     * @return the parked files that should be deleted, oldest first, so a
     *         caller can log them in the order they were written
     */
    public static List<Path> expired(List<Entry> entries, Instant now) {
        var newestFirst = new ArrayList<>(entries);
        newestFirst.sort(Comparator.comparing(Entry::savedAt).reversed()
                .thenComparing(entry -> entry.file().getFileName().toString()));

        var keep = new LinkedHashSet<Path>();
        long total = 0;
        for (var entry : newestFirst) {
            if (keep.size() >= MAX_FILES) {
                break;
            }
            if (Duration.between(entry.savedAt(), now).compareTo(MAX_AGE) > 0) {
                // skipped for age; newest-first order means the remaining
                // entries are older still, so each will be skipped the same way
                continue;
            }
            if (total + entry.size() > MAX_TOTAL_BYTES && !keep.isEmpty()) {
                // this entry does not fit; a later, older entry may still be
                // small enough, so skip only this one instead of cutting the rest
                continue;
            }
            keep.add(entry.file());
            total += entry.size();
        }

        return newestFirst.stream()
                .filter(entry -> !keep.contains(entry.file()))
                .sorted(Comparator.comparing(Entry::savedAt))
                .map(Entry::file)
                .toList();
    }
}
