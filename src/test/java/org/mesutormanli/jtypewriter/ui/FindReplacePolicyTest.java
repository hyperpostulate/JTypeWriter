/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The find panel asks exactly one question of YOLO mode: may a replacement
 * remove the characters it matched? The rule is pure, so it is pinned here
 * without a toolkit.
 */
class FindReplacePolicyTest {

    @Test
    void aReplacementNeedsExplicitConfirmationWhileYoloModeIsOn() {
        assertEquals(FindReplacePolicy.Confirmation.REQUIRED,
                FindReplacePolicy.replacement(true));
    }

    @Test
    void anOrdinaryReplacementAsksNothingWhenYoloModeIsOff() {
        assertEquals(FindReplacePolicy.Confirmation.NONE,
                FindReplacePolicy.replacement(false));
    }
}
