/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One corner, one card. The slot is the whole exclusivity rule, so it is
 * exercised headlessly with plain objects: a card is only an identity here,
 * and hiding is a counter.
 */
class OverlaySlotTest {

    @Test
    void showingOneCardHidesEveryOtherWithoutHidingIt() {
        var slot = new OverlaySlot();
        var hidden = new ArrayList<String>();
        var find = new Object();
        var welcome = new Object();
        var about = new Object();
        slot.register(find, () -> hidden.add("find"));
        slot.register(welcome, () -> hidden.add("welcome"));
        slot.register(about, () -> hidden.add("about"));

        slot.aboutToShow(about);

        assertEquals(List.of("find", "welcome"), hidden,
                "the card that is about to show must stay visible");
    }

    /** A stale registration replaces the action instead of firing twice. */
    @Test
    void registeringTheSameCardTwiceHidesItOnce() {
        var slot = new OverlaySlot();
        var hides = new int[]{0};
        var card = new Object();
        slot.register(card, () -> hides[0]++);
        slot.register(card, () -> hides[0]++);

        slot.aboutToShow(new Object());

        assertEquals(1, hides[0]);
    }

    /**
     * An owner that was never registered hides everything: a missing
     * registration can cost an extra hide, never an overlapping pair of cards.
     */
    @Test
    void anUnregisteredOwnerHidesAllMembers() {
        var slot = new OverlaySlot();
        var hidden = new ArrayList<String>();
        slot.register("find", () -> hidden.add("find"));
        slot.register("about", () -> hidden.add("about"));

        slot.aboutToShow("someone-else");

        assertEquals(List.of("find", "about"), hidden);
    }
}
