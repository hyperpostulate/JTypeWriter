/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.AutosaveHealth;
import org.mesutormanli.jtypewriter.stats.SessionStats;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PW-20 (findings U9/U10): the stats bar used to fill its labels only from the
 * one-second tick, leaving the first second of startup blank (U9), and it had
 * no locale listener, so a language switch kept the old language until that
 * tick (U10). Both behaviours now live in {@link StatsBarModel}, exercised here
 * without a JavaFX toolkit or any control (AGENTS.md: tests are headless).
 */
class StatsBarModelTest {

    @TempDir
    Path dir;

    /**
     * U9: the model is already complete the moment it is constructed - the
     * old timeline left both labels empty (equivalently {@code ""}) until the
     * first tick. The stats line is compared against the bundle's own format so
     * the expectation holds under the Turkish and German CI locales too.
     */
    @Test
    void isPopulatedImmediatelyAfterConstruction() {
        var stats = new SessionStats();
        var localeManager = localeManagerFor(Language.EN);
        var messages = messagesFor(localeManager);

        var model = new StatsBarModel(stats, messages, localeManager);


        var expected = messages.statsFormat(
                stats.getWordCount(),
                stats.getCharCount(),
                stats.getCharCountNoSpaces(),
                stats.getLineCount(),
                stats.getTotalKeystrokes(),
                stats.getFormattedDuration(),
                stats.getWpm());
        assertEquals(expected, model.statsText(), "U9: the first frame must already carry the stats");
        assertFalse(model.statsText().isBlank(), "U9: an empty label for the first second is the bug");
        assertFalse(model.clockText().isBlank(), "the clock is filled on construction too");
    }

    /** The one-second tick path: a refresh must pick up statistics changed since construction. */
    @Test
    void refreshPicksUpNewStatistics() {
        var stats = new SessionStats();
        var localeManager = localeManagerFor(Language.EN);
        var messages = messagesFor(localeManager);
        var model = new StatsBarModel(stats, messages, localeManager);

        stats.updateText("hello world");
        stats.registerKeystroke();
        stats.registerKeystroke();
        model.refresh(LocalDateTime.now());

        assertTrue(model.statsText().contains("2 words"), "refreshed text: " + model.statsText());
        assertTrue(model.statsText().contains("2 keys"), "refreshed text: " + model.statsText());
    }

    /**
     * U10: a language switch must notify exactly once and the text read at
     * notification time must already be in the new language - no waiting for
     * the next tick. {@link Messages} is constructed before the model on
     * purpose: it reloads the bundle in its own listener, which is the
     * ordering invariant the model depends on (see its Javadoc).
     */
    @Test
    void aLanguageSwitchRefreshesBeforeNotifying() {
        var localeManager = localeManagerFor(Language.EN);
        var messages = messagesFor(localeManager);
        var model = new StatsBarModel(new SessionStats(), messages, localeManager);

        var notifications = new AtomicInteger();
        var textAtNotification = new AtomicReference<String>();
        model.addListener(() -> {
            notifications.incrementAndGet();
            textAtNotification.set(model.statsText());
        });

        localeManager.setLanguage(Language.TR);

        assertEquals(1, notifications.get(), "U10: one repaint per switch");
        assertTrue(textAtNotification.get().contains("kelime"),
                "U10: text at notification time must already be Turkish: " + textAtNotification.get());
        assertFalse(textAtNotification.get().contains("words"),
                "U10: the English text was stale");
        assertTrue(model.statsText().contains("kelime"), "model text: " + model.statsText());
    }

    /** A listener added after construction still fires - no register-at-construction trap. */
    @Test
    void aListenerRegisteredAfterConstructionStillFires() {
        var localeManager = localeManagerFor(Language.EN);
        var messages = messagesFor(localeManager);
        var model = new StatsBarModel(new SessionStats(), messages, localeManager);

        var notifications = new AtomicInteger();
        model.addListener(notifications::incrementAndGet);

        localeManager.setLanguage(Language.DE);

        assertEquals(1, notifications.get());
        assertTrue(model.statsText().contains("Wörter"), "model text: " + model.statsText());
    }

    /** The clock is {@code dd.MM.yyyy  HH:mm:ss} with exactly two spaces, asserted by shape. */
    @Test
    void clockTextMatchesTheExpectedShape() {
        var localeManager = localeManagerFor(Language.EN);
        var model = new StatsBarModel(new SessionStats(), messagesFor(localeManager), localeManager);

        assertTrue(model.clockText().matches("\\d{2}\\.\\d{2}\\.\\d{4}  \\d{2}:\\d{2}:\\d{2}"),
                "clock shape: " + model.clockText());
    }

    /** PW-27: nothing is wrong at startup, so the warning slot is empty. */
    @Test
    void warningTextIsEmptyWhileAutosaveWorks() {
        var localeManager = localeManagerFor(Language.EN);
        var model = new StatsBarModel(new SessionStats(), messagesFor(localeManager), localeManager);

        assertFalse(model.isAutosaveFailing());
        assertEquals("", model.warningText());
    }

    /** PW-27: turning the flag on shows the bundle text and repaints exactly once. */
    @Test
    void settingAutosaveFailingShowsTheWarningAndNotifiesOnce() {
        var localeManager = localeManagerFor(Language.EN);
        var messages = mock(Messages.class);
        when(messages.autosaveWarning()).thenReturn("Autosave is not working - save now");
        var model = new StatsBarModel(new SessionStats(), messages, localeManager);

        var notifications = new AtomicInteger();
        model.addListener(notifications::incrementAndGet);

        model.setAutosaveProblem(AutosaveHealth.Problem.DRAFT_WRITE);

        assertTrue(model.isAutosaveFailing());
        assertEquals("Autosave is not working - save now", model.warningText());
        assertEquals(1, notifications.get());
    }

    /** PW-27: a repeat of the same state is not a transition, so no extra repaint. */
    @Test
    void repeatingTheSameFailingStateDoesNotNotify() {
        var localeManager = localeManagerFor(Language.EN);
        var messages = mock(Messages.class);
        when(messages.autosaveWarning()).thenReturn("Autosave is not working - save now");
        var model = new StatsBarModel(new SessionStats(), messages, localeManager);
        var notifications = new AtomicInteger();
        model.addListener(notifications::incrementAndGet);
        model.setAutosaveProblem(AutosaveHealth.Problem.DRAFT_WRITE);

        model.setAutosaveProblem(AutosaveHealth.Problem.DRAFT_WRITE);

        assertEquals(1, notifications.get(), "a standing warning must not repaint the bar");
    }

    /** PW-27: a later working write withdraws the warning from the bar. */
    @Test
    void clearingAutosaveFailingEmptiesTheWarning() {
        var localeManager = localeManagerFor(Language.EN);
        var messages = mock(Messages.class);
        when(messages.autosaveWarning()).thenReturn("Autosave is not working - save now");
        var model = new StatsBarModel(new SessionStats(), messages, localeManager);
        model.setAutosaveProblem(AutosaveHealth.Problem.DRAFT_WRITE);

        model.setAutosaveProblem(null);

        assertFalse(model.isAutosaveFailing());
        assertEquals("", model.warningText());
    }

    /**
     * PW-27: the warning is bundle text, so a language switch while it stands
     * must re-localise it - it must not stay English until the next tick.
     */
    @Test
    void aLanguageSwitchWhileFailingReLocalisesTheWarning() {
        var localeManager = localeManagerFor(Language.EN);
        var messages = messagesFor(localeManager);
        var model = new StatsBarModel(new SessionStats(), messages, localeManager);
        model.setAutosaveProblem(AutosaveHealth.Problem.DRAFT_WRITE);
        var english = model.warningText();

        localeManager.setLanguage(Language.TR);

        assertFalse(english.isBlank());
        assertEquals(messages.autosaveWarning(), model.warningText());
        assertFalse(english.equals(model.warningText()), "the English warning was stale");
        assertTrue(model.warningText().contains("Otomatik"),
                "warning text: " + model.warningText());
    }

    /**
     * V17/PW-28: a draft that could not be deleted is not a failed write, so it
     * gets its own status text - the user is told a stale copy is still on disk,
     * not that autosave is broken.
     */
    @Test
    void eachFailureKindShowsItsOwnWarning() {
        var localeManager = localeManagerFor(Language.EN);
        var messages = messagesFor(localeManager);
        var model = new StatsBarModel(new SessionStats(), messages, localeManager);

        model.setAutosaveProblem(AutosaveHealth.Problem.DRAFT_DELETE);
        assertEquals(messages.staleDraftWarning(), model.warningText());
        assertEquals(AutosaveHealth.Problem.DRAFT_DELETE, model.problem().orElseThrow());

        model.setAutosaveProblem(AutosaveHealth.Problem.DRAFT_WRITE);
        assertEquals(messages.autosaveWarning(), model.warningText());
        assertFalse(messages.staleDraftWarning().equals(model.warningText()),
                "a failed write keeps the write warning");

        model.setAutosaveProblem(null);
        assertEquals("", model.warningText());
        assertTrue(model.problem().isEmpty());
    }

    private LocaleManager localeManagerFor(Language language) {
        // A per-language settings file keeps the persisted language from one
        // test leaking into the next, and pins the language explicitly: the
        // system locale on CI may be TR or DE.
        var settings = new AppSettings(dir.resolve("settings-" + language.code() + ".properties"));
        var localeManager = new LocaleManager(settings);
        localeManager.setLanguage(language);
        return localeManager;
    }

    /** Mirrors the AppGraph order: messages subscribes before anything that formats through it. */
    private Messages messagesFor(LocaleManager localeManager) {
        return new Messages(localeManager);
    }
}
