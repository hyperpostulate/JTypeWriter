/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.component.StatsBarModel;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The performance evidence for the incremental statistics (W2-01 / X5).
 *
 * <p>Before the counters, {@link StatsBarModel#refresh} rescanned the whole
 * document four times; on a 10 MB draft that measured ~164 ms, once a second,
 * on the FX thread. The card asks for a single refresh under 5 ms on the
 * reference machine and under 50 ms in CI, where a slower runner and a cold
 * JIT make the tighter number meaningless. 50 ms still catches the regression:
 * the O(n) version cannot come in under it on this document size.</p>
 *
 * <p>The real refresh path is measured rather than the bare getters, because
 * it also formats the line through {@code MessageFormat}. That keeps the
 * measurement honest for two reasons: it is what the timeline actually calls,
 * and formatting allocates, so the JIT cannot hoist the work out of the
 * measured loop and report a near-zero time for a regressed implementation.</p>
 */
class StatsPerfHarnessTest {

    private static final int DOCUMENT_CHARS = 10 * 1024 * 1024;
    private static final long CI_THRESHOLD_MS = 50;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 12, 0);

    @TempDir
    Path dir;

    /** Written so the raw number shows up in the Surefire report next to the pass/fail. */
    private static volatile String lastStatsText;

    @Test
    void oneRefreshOnTenMegabytesStaysUnderTheCiThreshold() {
        var stats = new SessionStats();
        stats.updateText(sampleDocument());

        var settings = new AppSettings(dir.resolve("settings.properties"));
        var localeManager = new LocaleManager(settings);
        localeManager.setLanguage(Language.EN);
        var messages = new Messages(localeManager);
        var model = new StatsBarModel(stats, messages, localeManager);

        // Warm-up: class loading, formatter construction and the first
        // MessageFormat parse are not part of a steady-state refresh.
        for (var i = 0; i < 20; i++) {
            model.refresh(NOW);
        }

        var bestNanos = Long.MAX_VALUE;
        for (var i = 0; i < 7; i++) {
            var start = System.nanoTime();
            model.refresh(NOW);
            bestNanos = Math.min(bestNanos, System.nanoTime() - start);
            lastStatsText = model.statsText();
        }
        var bestMillis = bestNanos / 1_000_000.0;
        System.out.printf(Locale.ROOT,
                "StatsPerfHarness: one refresh over %d chars = %.3f ms (CI threshold %d ms)%n",
                DOCUMENT_CHARS, bestMillis, CI_THRESHOLD_MS);

        assertTrue(lastStatsText != null && !lastStatsText.isBlank(),
                "the measured refresh must have produced a stats line");
        assertTrue(stats.getCharCount() >= DOCUMENT_CHARS,
                "the sample document must actually be ten megabytes of text");
        assertTrue(bestMillis < CI_THRESHOLD_MS,
                "one stats refresh over 10 MB took " + bestMillis
                        + " ms; the incremental counters are O(1), so anything near "
                        + "the old ~164 ms means a full scan crept back into a getter");
    }

    /** Plain prose-shaped content: many short words and line breaks, no giant token. */
    private static String sampleDocument() {
        var builder = new StringBuilder(DOCUMENT_CHARS + 64);
        while (builder.length() < DOCUMENT_CHARS) {
            builder.append("the quick brown fox jumps over the lazy dog\n");
        }
        return builder.toString();
    }
}
