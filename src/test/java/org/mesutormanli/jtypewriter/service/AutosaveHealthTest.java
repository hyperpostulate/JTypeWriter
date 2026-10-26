/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PW-27: the autosave health value. State is sticky until a real success, and
 * listeners fire only on real transitions so a persistently failing tick does
 * not repaint the status bar every 30 seconds.
 */
class AutosaveHealthTest {

    @Test
    void aFreshHealthValueIsHealthy() {
        var health = new AutosaveHealth();

        assertFalse(health.isFailing());
        assertTrue(health.problem().isEmpty());
    }

    @Test
    void aFailureNotifiesOnceAndARepeatOfTheSameProblemDoesNot() {
        var health = new AutosaveHealth();
        var notifications = new AtomicInteger();
        health.addListener(notifications::incrementAndGet);

        health.fail(AutosaveHealth.Problem.DRAFT_WRITE);
        assertTrue(health.isFailing());
        assertEquals(AutosaveHealth.Problem.DRAFT_WRITE, health.problem().orElseThrow());
        assertEquals(1, notifications.get());

        health.fail(AutosaveHealth.Problem.DRAFT_WRITE);
        assertEquals(1, notifications.get(), "the same standing problem is not a transition");
    }

    @Test
    void switchingToAnotherProblemNotifies() {
        var health = new AutosaveHealth();
        var notifications = new AtomicInteger();
        health.addListener(notifications::incrementAndGet);

        health.fail(AutosaveHealth.Problem.DRAFT_WRITE);
        health.fail(AutosaveHealth.Problem.DRAFT_PARK);

        assertEquals(2, notifications.get());
        assertEquals(AutosaveHealth.Problem.DRAFT_PARK, health.problem().orElseThrow());
    }

    @Test
    void recoverClearsTheProblemAndNotifies() {
        var health = new AutosaveHealth();
        var notifications = new AtomicInteger();
        health.addListener(notifications::incrementAndGet);
        health.fail(AutosaveHealth.Problem.DRAFT_WRITE);

        health.recover();

        assertFalse(health.isFailing());
        assertTrue(health.problem().isEmpty());
        assertEquals(2, notifications.get());
    }

    @Test
    void recoveringWhileHealthyIsSilent() {
        var health = new AutosaveHealth();
        var notifications = new AtomicInteger();
        health.addListener(notifications::incrementAndGet);

        health.recover();

        assertFalse(health.isFailing());
        assertEquals(0, notifications.get());
    }

    @Test
    void aNullListenerIsIgnoredInsteadOfThrowing() {
        var health = new AutosaveHealth();

        health.addListener(null);
        health.fail(AutosaveHealth.Problem.DRAFT_WRITE);

        assertTrue(health.isFailing());
    }
}
