package org.mesutormanli.jtypewriter.ui.component;

import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.AutosaveHealth;
import org.mesutormanli.jtypewriter.stats.SessionStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

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
 *
 * <p><strong>Why a {@code service} enum here:</strong> the warning text depends
 * on <em>what</em> failed (a draft that could not be written, parked or
 * deleted), so the model holds {@link AutosaveHealth.Problem} rather than a
 * boolean. Only the enum type is referenced, never a JavaFX control or the
 * service itself, which keeps this class free of UI types and testable
 * headlessly.</p>
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
    private String warningText = "";
    /** Set by the controller from {@code AutoSaveService.health()}; {@code null} is healthy. */
    private AutosaveHealth.Problem autosaveProblem;
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
        // V4/PW-27: the warning is bundle text, so it is rebuilt on every
        // refresh - which is exactly what a language switch needs.
        warningText = warningFor(autosaveProblem);
    }

    /** The localised warning for a failing state, or {@code ""} while healthy. */
    private String warningFor(AutosaveHealth.Problem problem) {
        if (problem == null) {
            return "";
        }
        // V17/PW-28: a stale draft is a different problem from a failed write,
        // and the user's next action is the same but the text says what is on
        // disk. The dialog copy is about writing a copy, so it would be wrong.
        return problem == AutosaveHealth.Problem.DRAFT_DELETE
                ? messages.staleDraftWarning()
                : messages.autosaveWarning();
    }

    /**
     * V4/PW-27 / V17/PW-28: the safety net's current failure, {@code null}
     * meaning healthy. A failure stands until a later operation succeeds; a
     * no-op repeat does not repaint the bar.
     */
    public void setAutosaveProblem(AutosaveHealth.Problem problem) {
        if (Objects.equals(autosaveProblem, problem)) {
            return;
        }
        autosaveProblem = problem;
        refresh(LocalDateTime.now());
        listener.run();
    }

    /** {@code true} while the autosave warning must be displayed. */
    public boolean isAutosaveFailing() {
        return autosaveProblem != null;
    }

    /** The standing failure kind, empty while the safety net works (PW-28). */
    public Optional<AutosaveHealth.Problem> problem() {
        return Optional.ofNullable(autosaveProblem);
    }

    /**
     * The persistent autosave warning, or {@code ""} when the safety net
     * works. Localised on construction, refresh and language switch.
     */
    public String warningText() {
        return warningText;
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
