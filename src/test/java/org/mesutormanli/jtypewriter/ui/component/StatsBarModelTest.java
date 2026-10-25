package org.mesutormanli.jtypewriter.ui.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.stats.SessionStats;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
