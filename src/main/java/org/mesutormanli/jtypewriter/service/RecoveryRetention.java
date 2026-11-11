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
 * How long a recovery draft is worth keeping.
 *
 * <p>Parking exists so that a postponed draft does not block the active slot
 * (see {@link RecoveryStore}), which means a parked file is written once and
 * never again. Live drafts have the opposite problem: a crash leaves the last
 * snapshot behind and nothing ever writes it again. Without a policy both
 * kinds accumulate in the user's home directory for the lifetime of the
 * installation, and every "Later" answer or crashed session costs disk space
 * that nothing ever reclaims.</p>
 *
 * <p><b>The caller filters live files; this class does not know about them.</b>
 * A {@code draft-*.jtw} may belong to a second instance running right now - the
 * slot name carries a per-session UUID - so the class itself cannot tell a
 * live file from an abandoned one. {@link RecoveryStore} therefore removes its
 * own slot and every recently written file before calling here; what remains
 * is abandoned by construction. The policy is prefix-agnostic on purpose: it
 * ages {@code draft-*} and {@code pending-*} alike.</p>
 *
 * <p>The limits are deliberately generous because every candidate holds user
 * text. Thirty days and twenty files is the product decision: a draft is a
 * recovery copy, and it is preferable to spend a little disk than to delete the
 * only surviving version of a document one day too early.</p>
 *
 * <p>The policy is a pure function of the directory listing rather than logic
 * inline in {@link RecoveryStore}, so it can be tested without creating thirty
 * days of files: a rule that deletes text is not a rule to verify by
 * inspection. The rules apply in a fixed order:</p>
 * <ol>
 *   <li><b>age</b> - a draft written more than {@link #MAX_AGE} ago is no
 *       longer a recovery candidate, it is clutter;</li>
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

    /** Drafts older than this are housekeeping, not history. */
    static final Duration MAX_AGE = Duration.ofDays(30);

    /**
     * How many drafts survive the count rule. Twenty is the roadmap's
     * product-decision default (30 days + 20 files) and is intentionally
     * generous: the files hold user text, so a small folder is not worth
     * deleting a possible only copy for.
     */
    static final int MAX_FILES = 20;

    /** Total bytes the retained set may occupy. */
    static final long MAX_TOTAL_BYTES = 64L * 1024 * 1024;

    private RecoveryRetention() {
    }

    /** A draft file as the scan saw it: when it was written and how big it is. */
    public record Entry(Path file, Instant savedAt, long size) {
    }

    /**
     * @return the draft files that should be deleted, oldest first, so a
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
