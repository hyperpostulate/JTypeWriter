/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The whole exit matrix as a pure decision.
 *
 * <p>The four combinations of the two inputs are exhaustive, and they pin the
 * rule that the draft is only ever deleted after the window's close request was
 * answered. A frozen slot wins over every other signal: it means a postponed
 * draft could not be parked and still occupies the only slot, so even an
 * answered exit must leave the file untouched.</p>
 */
class ExitDispositionTest {

    @Test
    void anAnsweredCloseRequestClearsTheDraft() {
        assertEquals(ExitDisposition.CLEAR, ExitDisposition.decide(true, false));
    }

    @Test
    void anUnaskedExitParksTheDraft() {
        assertEquals(ExitDisposition.PARK, ExitDisposition.decide(false, false));
    }

    @Test
    void aFrozenSlotKeepsTheDraftEvenAfterAnAnsweredCloseRequest() {
        assertEquals(ExitDisposition.KEEP, ExitDisposition.decide(true, true));
    }

    @Test
    void aFrozenSlotKeepsTheDraftOnAnUnaskedExitToo() {
        assertEquals(ExitDisposition.KEEP, ExitDisposition.decide(false, true));
    }
}
