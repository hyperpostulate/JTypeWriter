package org.mesutormanli.jtypewriter.ui.component;

import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.stats.SessionStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The text logic behind {@link StatsBar}, kept free of JavaFX so it stays
 * testable headless (AGENTS.md: no toolkit, no control construction).
 *
 * <p>Fixes two findings from the analysis:</p>
 * <ul>
 *   <li><strong>U9</strong> - the bar only filled its labels from the
 *       one-second {@code Timeline} tick, so the first second of startup showed
 *       an empty bar. The constructor computes both texts immediately, so a
 *       caller that reads the model right after construction already has the
 *       first frame's content.</li>
 *   <li><strong>U10</strong> - the bar had no locale listener, so switching
 *       the language left stale text until the next tick. The constructor
 *       subscribes to {@link LocaleManager} and refreshes before notifying the
 *       consumer, which lets the control repaint within the same locale-change
 *       event.</li>
 * </ul>
 *
 * <p><strong>Ordering invariant:</strong> this relies on {@code AppGraph}
 * declaring {@code localeManager} <em>before</em> {@code messages}, and
 * {@code messages} before the stats bar. {@link Messages} reloads its bundle in
 * the listener it registers in its own constructor, so listeners registered
 * afterwards - this model's included - always run after the bundle is fresh
 * and therefore format in the new language. Do not reorder those fields.</p>
 */
public final class StatsBarModel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** The date and the clock are joined by exactly two spaces, as they always were. */
    private static final String DATE_TIME_SEPARATOR = "  ";

    private final SessionStats stats;
    private final Messages messages;

    private String statsText = "";
    private String clockText = "";
    private Runnable listener = () -> {
    };

    public StatsBarModel(SessionStats stats, Messages messages, LocaleManager locales) {
        this.stats = stats;
        this.messages = messages;

        // U9: fill the first frame now instead of waiting for the first tick.
        refresh(LocalDateTime.now());

        // U10: the bundle is already reloaded when this runs (see the class
        // Javadoc ordering invariant), so the text painted here is localised.
        locales.addListener(language -> {
            refresh(LocalDateTime.now());
            listener.run();
        });
    }

    /**
     * Recomputes both texts from the current statistics and the given instant.
     * Exposed so the one-second tick and tests can drive it explicitly.
     */
    public void refresh(LocalDateTime now) {
        clockText = now.format(DATE_FORMAT) + DATE_TIME_SEPARATOR + now.format(TIME_FORMAT);
        statsText = messages.statsFormat(
                stats.getWordCount(),
                stats.getCharCount(),
                stats.getCharCountNoSpaces(),
                stats.getLineCount(),
                stats.getTotalKeystrokes(),
                stats.getFormattedDuration(),
                stats.getWpm());
    }

    /** The formatted statistics line; populated from construction on. */
    public String statsText() {
        return statsText;
    }

    /** The formatted {@code dd.MM.yyyy  HH:mm:ss} clock; populated from construction on. */
    public String clockText() {
        return clockText;
    }

    /**
     * Registers the consumer that repaints the bar after a locale-driven
     * refresh. Defaults to a no-op, and a listener registered after
     * construction is enough: the model already holds correct text, and the
     * consumer only has to be in place before the next language switch.
     */
    public void addListener(Runnable listener) {
        this.listener = listener == null ? () -> {
        } : listener;
    }
}
