/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The update decision, without a socket: the redirect address GitHub ends at
 * carries the version, so everything here is string handling and one
 * comparison.
 */
class UpdateCheckTest {

    private static final String OWNER = "https://github.com/hyperpostulate/JTypeWriter";

    @Test
    void theVersionIsTheLastSegmentOfATagAddress() {
        assertEquals(Optional.of("1.1.5"),
                UpdateCheck.versionOf(OWNER + "/releases/tag/v1.1.5"));
    }

    @Test
    void aTrailingSlashAQueryOrAFragmentDoesNotHideTheVersion() {
        assertEquals(Optional.of("1.1.5"),
                UpdateCheck.versionOf(OWNER + "/releases/tag/v1.1.5/"));
        assertEquals(Optional.of("1.1.5"),
                UpdateCheck.versionOf(OWNER + "/releases/tag/v1.1.5?ref=main"));
        assertEquals(Optional.of("1.1.5"),
                UpdateCheck.versionOf(OWNER + "/releases/tag/v1.1.5#notes"));
    }

    @Test
    void anAddressThatNamesNoReleaseYieldsNothingRatherThanAGuess() {
        assertEquals(Optional.empty(), UpdateCheck.versionOf(OWNER + "/releases/latest"));
        assertEquals(Optional.empty(), UpdateCheck.versionOf(OWNER + "/releases/tag/"));
        assertEquals(Optional.empty(), UpdateCheck.versionOf(OWNER + "/releases/tag/v2.0.0-beta"));
        assertEquals(Optional.empty(), UpdateCheck.versionOf(OWNER + "/releases/tag/v1.1.5/assets"));
        assertEquals(Optional.empty(), UpdateCheck.versionOf(""));
        assertEquals(Optional.empty(), UpdateCheck.versionOf(null));
    }

    @Test
    void aNewerTagIsNewerAndTheSameTagIsUpToDate() {
        var newer = UpdateCheck.evaluate(OWNER + "/releases/tag/v1.1.6", "1.1.5").orElseThrow();
        assertEquals(UpdateCheck.Status.NEWER, newer.status());
        assertEquals("1.1.6", newer.latestVersion());
        assertEquals(OWNER + "/releases/tag/v1.1.6", newer.pageUrl());

        var same = UpdateCheck.evaluate(OWNER + "/releases/tag/v1.1.5", "1.1.5").orElseThrow();
        assertEquals(UpdateCheck.Status.UP_TO_DATE, same.status());

        var older = UpdateCheck.evaluate(OWNER + "/releases/tag/v1.1.4", "1.1.5").orElseThrow();
        assertEquals(UpdateCheck.Status.UP_TO_DATE, older.status(),
                "a tag behind the running build is not an update");
    }

    @Test
    void anUnreadableAddressProducesNoResultAndNeverAFailedOneByAccident() {
        assertTrue(UpdateCheck.evaluate(OWNER + "/releases/latest", "1.1.5").isEmpty());
        var failed = UpdateCheck.Result.failed();
        assertEquals(UpdateCheck.Status.FAILED, failed.status());
        assertEquals("", failed.latestVersion());
        assertEquals("", failed.pageUrl());
        assertFalse(failed.status() == UpdateCheck.Status.UP_TO_DATE);
    }
}
