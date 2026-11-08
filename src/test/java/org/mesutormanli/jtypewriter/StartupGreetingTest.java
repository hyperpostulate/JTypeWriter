/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the welcome card is allowed to arrive uninvited.
 *
 * <p>The rule is two booleans, but getting it wrong is only visible on a
 * launch, so it is pinned as a pure decision instead of living on the startup
 * path: a document the user named must never be covered by a greeting, and an
 * opted-out card must never come back on its own - only the File menu can ask
 * for it.</p>
 */
class StartupGreetingTest {

    @Test
    void aFreshInstallIsGreeted() {
        assertTrue(FxApplication.greetsAtStartup(false, true));
    }

    /** The opt-out has to survive the next launch, which is the whole point of storing it. */
    @Test
    void theOptOutStopsTheGreetingWithoutOne() {
        assertFalse(FxApplication.greetsAtStartup(false, false));
    }

    /** A document named on the command line outranks the greeting, opt-out or not. */
    @Test
    void aDocumentNamedAtStartupIsNeverCoveredByTheCard() {
        assertFalse(FxApplication.greetsAtStartup(true, true));
        assertFalse(FxApplication.greetsAtStartup(true, false));
    }
}
