/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared version rule: settings migration and the update check must agree
 * on what "newer" means, so the cases that used to be pinned inside
 * {@code AppSettingsTest} are exercised here as well.
 */
class VersionOrderTest {

    @Test
    void numericSegmentsDecideAndMissingSegmentsCountAsZero() {
        assertTrue(VersionOrder.compare("1.0.3", "1.1.0") < 0);
        assertTrue(VersionOrder.compare("1.1.0", "1.0.3") > 0);
        assertTrue(VersionOrder.compare("1.0.10", "1.0.9") > 0,
                "10 is ten, not the string '10' after '9'");
        assertEquals(0, VersionOrder.compare("1.0", "1.0.0"));
    }

    @Test
    void aReleaseTagWithAVPrefixComparesLikeTheNumberItNames() {
        assertTrue(VersionOrder.isNewer("v1.1.6", "1.1.5"));
        assertFalse(VersionOrder.isNewer("v1.1.5", "1.1.5"));
        assertFalse(VersionOrder.isNewer("v1.1.4", "1.1.5"));
    }

    @Test
    void onlyPlainDottedNumbersAreReleaseVersions() {
        assertTrue(VersionOrder.isReleaseVersion("1.1.5"));
        assertTrue(VersionOrder.isReleaseVersion("v1.1.5"));
        assertTrue(VersionOrder.isReleaseVersion("99"));
        assertFalse(VersionOrder.isReleaseVersion("v2.0.0-beta"),
                "a suffixed tag must not be read as the release 2.0.0");
        assertFalse(VersionOrder.isReleaseVersion("nightly"));
        assertFalse(VersionOrder.isReleaseVersion(""));
        assertFalse(VersionOrder.isReleaseVersion(null));
    }
}
