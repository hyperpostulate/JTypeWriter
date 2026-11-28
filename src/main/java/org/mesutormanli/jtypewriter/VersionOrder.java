/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The one implementation of "is this version newer than that one", shared by the
 * settings-schema migration and the update check.
 *
 * <p>Before this class the comparison lived inside {@link AppSettings}, where it
 * answered exactly one question: was the preferences file written by an older or
 * a newer build than this one? The update check asks the same question about a
 * release tag, and a second implementation would be a second definition of
 * "newer" - the kind of pair that agrees until a version has four segments. The
 * behaviour is therefore unchanged from {@code AppSettings.compareVersions},
 * which now delegates here; {@code AppSettingsTest} still pins it.</p>
 *
 * <p><strong>What this is not.</strong> Not a SemVer implementation. Versions
 * are compared by their numeric segments, missing segments counting as zero, so
 * {@code 1.0.3 < 1.1.0 < 99} and {@code 1.0 == 1.0.0}. Pre-release ordering
 * ({@code 1.0.0-beta < 1.0.0}) is not modelled because the project publishes no
 * pre-release tags: {@code package.sh} rejects a {@code -SNAPSHOT} version, and a
 * tag that carries a suffix is refused by {@link #isReleaseVersion} instead of
 * being silently ordered by the digits in front of it.</p>
 *
 * <p>Pure and side-effect free apart from one log line about an unrepresentable
 * segment; no JavaFX, no I/O.</p>
 */
public final class VersionOrder {

    /** A run of digits: one numeric segment of a dotted version. */
    static final Pattern VERSION_PART = Pattern.compile("\\d+");

    /** A dotted numeric version with an optional {@code v} prefix, nothing else. */
    private static final Pattern RELEASE_VERSION = Pattern.compile("v?\\d+(\\.\\d+)*");

    private static final Logger LOG = AppLogging.logger(VersionOrder.class);

    private VersionOrder() {
    }

    /**
     * Compares two dotted version strings by their numeric segments, missing
     * segments counting as zero, so {@code 1.0.3 < 1.1.0 < 99} and
     * {@code 1.0 == 1.0.0}. This is deliberately not a general SemVer
     * implementation: the only question asked is "was this written by an older
     * or a newer build than me", and a pre-release suffix such as
     * {@code -SNAPSHOT} is not part of a releasable version anyway
     * ({@code package.sh} rejects it).
     */
    public static int compare(String left, String right) {
        var first = segments(left);
        var second = segments(right);
        for (var index = 0; index < Math.max(first.length, second.length); index++) {
            var a = index < first.length ? first[index] : 0L;
            var b = index < second.length ? second[index] : 0L;
            if (a != b) {
                return Long.compare(a, b);
            }
        }
        return 0;
    }

    /** Whether {@code candidate} names a later version than {@code current}. */
    public static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    /**
     * Whether the text is a plain dotted numeric version, optionally prefixed
     * with {@code v}: {@code 1.1.5} and {@code v1.1.5} are, while
     * {@code v2.0.0-beta}, {@code nightly} and an empty string are not.
     *
     * <p>The update check uses this to refuse an unrecognised tag rather than
     * derive {@code 2.0.0} from {@code 2.0.0-beta} and announce a final release
     * that was never published. A tag this project did not produce is not
     * evidence of an update.</p>
     */
    public static boolean isReleaseVersion(String text) {
        return text != null && RELEASE_VERSION.matcher(text).matches();
    }

    /**
     * The numeric segments of a version, empty when it contains none or when a
     * segment overflows a {@code long}.
     *
     * <p>Those two cases look alike to this method but not to
     * {@link AppSettings#migrate}: a value with no digit run was never written
     * by a build and is stamped as unversioned, while an overflowing run is
     * evidence of a build whose version this one cannot represent and must be
     * left alone. The caller therefore checks {@link #VERSION_PART} before
     * trusting the empty result.</p>
     */
    static long[] segments(String version) {
        if (version == null) {
            return new long[0];
        }
        var matcher = VERSION_PART.matcher(version);
        var segments = new ArrayList<Long>();
        while (matcher.find()) {
            try {
                segments.add(Long.parseLong(matcher.group()));
            } catch (NumberFormatException overflow) {
                LOG.log(Level.FINE, "ignoring unreadable version " + version);
                return new long[0];
            }
        }
        var values = new long[segments.size()];
        for (var index = 0; index < segments.size(); index++) {
            values[index] = segments.get(index);
        }
        return values;
    }
}
