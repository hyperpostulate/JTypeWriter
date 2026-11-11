/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The size half of the parked-draft retention policy.
 *
 * <p>Walking newest-first is an ordering convenience, not a size ordering: an
 * older draft can be much smaller than a newer one. An entry that does not fit
 * the budget is therefore skipped individually, never used as a reason to stop
 * considering everything after it.</p>
 */
class RecoveryRetentionTest {

    /**
     * The defect this class exists for: the newest entry fills most of the
     * budget, an older oversized entry must be expired, and an even older but
     * small entry must still be kept - the old {@code break} deleted it.
     */
    @Test
    void anOversizedOlderDraftDoesNotDiscardASmallerOneBehindIt(@TempDir Path dir) {
        var now = Instant.parse("2026-11-04T00:00:00Z");
        var newest = dir.resolve("pending-newest.jtw");
        var oversized = dir.resolve("pending-oversized.jtw");
        var small = dir.resolve("pending-small.jtw");
        var entries = List.of(
                new RecoveryRetention.Entry(newest, now,
                        RecoveryRetention.MAX_TOTAL_BYTES - 100),
                new RecoveryRetention.Entry(oversized, now.minusSeconds(60), 200),
                new RecoveryRetention.Entry(small, now.minusSeconds(120), 50));

        var expired = RecoveryRetention.expired(entries, now);

        assertEquals(List.of(oversized), expired,
                "only the entry that does not fit is expired; the small older one fits");
    }

    /** An oversized entry between two fitting ones is the only one expired. */
    @Test
    void anOversizedEntryBetweenTwoFittingOnesIsTheOnlyOneExpired(@TempDir Path dir) {
        var now = Instant.parse("2026-11-04T00:00:00Z");
        var newest = dir.resolve("pending-newest.jtw");
        var oversized = dir.resolve("pending-oversized.jtw");
        var oldest = dir.resolve("pending-oldest.jtw");
        var entries = List.of(
                new RecoveryRetention.Entry(newest, now, 100),
                new RecoveryRetention.Entry(oversized, now.minusSeconds(60),
                        RecoveryRetention.MAX_TOTAL_BYTES),
                new RecoveryRetention.Entry(oldest, now.minusSeconds(120), 100));

        var expired = RecoveryRetention.expired(entries, now);

        assertEquals(List.of(oversized), expired,
                "both fitting entries stay and only the middle one is expired");
    }

    /**
     * The count cap is the roadmap's product decision: 20 files, generous
     * because each one is user text. Two over the cap means exactly two go.
     */
    @Test
    void theCountCapKeepsTwentyDrafts(@TempDir Path dir) {
        assertEquals(20, RecoveryRetention.MAX_FILES, "30 days + 20 files is the product default");

        var now = Instant.parse("2026-11-04T00:00:00Z");
        var entries = new java.util.ArrayList<RecoveryRetention.Entry>();
        for (var i = 0; i < RecoveryRetention.MAX_FILES + 2; i++) {
            entries.add(new RecoveryRetention.Entry(
                    dir.resolve("draft-" + i + ".jtw"), now.minusSeconds(i), 1024));
        }

        var expired = RecoveryRetention.expired(entries, now);

        assertEquals(2, expired.size());
        assertEquals(dir.resolve("draft-" + (RecoveryRetention.MAX_FILES + 1) + ".jtw"),
                expired.get(0), "the oldest entry goes first");
    }

    /** The newest entry is never deleted for size, even when it is over budget. */
    @Test
    void theNewestEntrySurvivesAnOversizedBudgetByItself(@TempDir Path dir) {
        var now = Instant.parse("2026-11-04T00:00:00Z");
        var newest = dir.resolve("pending-newest.jtw");

        var expired = RecoveryRetention.expired(
                List.of(new RecoveryRetention.Entry(newest, now,
                        RecoveryRetention.MAX_TOTAL_BYTES + 1)), now);

        assertTrue(expired.isEmpty(), "the only candidate is never housekeeping");
    }
}
