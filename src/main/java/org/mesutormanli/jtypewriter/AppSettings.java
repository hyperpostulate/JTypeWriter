/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Persisted user preferences in {@code ~/.jtypewriter/settings.properties}.
 *
 * <p>Design rules:</p>
 * <ul>
 *   <li><b>Write-through:</b> every setter stores the value and flushes the
 *       file immediately (small, atomic - see {@link AtomicFiles});</li>
 *   <li><b>Never fatal:</b> a missing, unreadable or malformed settings file
 *       degrades to the built-in defaults and only logs. Preferences are a
 *       convenience, never a data channel. A malformed file is first moved
 *       aside (quarantined) so the evidence is not overwritten by the next
 *       save;</li>
 *   <li><b>Schema-versioned:</b> the file names the build that wrote it in
 *       {@code schema.version}, and that value is the product version from
 *       {@code pom.xml} - one number, one source. A file without the key
 *       predates versioning and is migrated in memory to the current version;
 *       a file from a newer build is read as far as this build understands it
 *       and is never downgraded or stripped of keys it does not recognise.
 *       Migration never rewrites the file - the next real save does that;</li>
 *   <li><b>Write failures are reported:</b> a write that cannot happen (I/O
 *       error or {@link SecurityException}) is logged, never thrown at the
 *       setter's caller, and raises {@link #isSettingsWriteFailurePending()}
 *       so the UI can tell the user once. The in-memory value stays, so the
 *       session keeps working - only persistence is lost;</li>
 *   <li><b>Defensive reads:</b> unknown or invalid values (e.g. a renamed enum
 *       constant from an older version) fall back to the default instead of
 *       breaking startup.</li>
 * </ul>
 * Persisted: language, theme, text colour, font size, sound on/off, toolbar
 * visibility, the stats bar size, the last document (for session restore) and
 * its directory (for file choosers), and the window geometry including
 * fullscreen. YOLO mode is deliberately <em>not</em> persisted: a
 * user who left it on would come back to an editor that refuses to delete.
 */
public final class AppSettings {

    /** Window geometry as remembered at shutdown. */
    public record WindowState(boolean maximized, double x, double y, double width, double height) {
    }

    /**
     * Persisted stats bar text size. A neutral mirror of
     * {@code StatsBar.Size}: this class must never reference a class that
     * extends a JavaFX control (it is constructed before the toolkit exists
     * and used from headless tests), so the two enums are mapped by name in
     * {@code StatsBarState}. An unknown stored value falls back to
     * {@code MEDIUM} like every other corrupt setting.
     */
    public enum StatsBarSize { SMALL, MEDIUM, LARGE }

    private static final Logger LOG = AppLogging.logger(AppSettings.class);

    /**
     * The version of the settings file this build writes: the product version,
     * not a separate counter.
     *
     * <p>Deliberately the same number the About card shows. A second scheme
     * ("schema 3" beside "release 1.1.0") would have to be kept in step by
     * hand, and the file would then answer two questions - which build wrote
     * me, and what shape am I - with two numbers that can disagree. One number
     * answers both, because a release is the only thing that may change the
     * persisted shape: a new setting is a key older builds ignore, which needs
     * no migration at all, while a rename or removal ships in a release and
     * therefore in a new version.</p>
     */
    static final String CURRENT_SCHEMA_VERSION = AppVersion.get();

    private static final String KEY_SCHEMA_VERSION = "schema.version";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";
    private static final String KEY_TEXT_COLOR = "text.color";
    private static final String KEY_FONT_SIZE = "font.size";
    private static final String KEY_SOUND_VOLUME = "sound.volume";
    /**
     * The boolean this setting used to be. Read only as a fallback when
     * {@code sound.volume}
     * is absent, so a settings file written by an older build keeps its
     * meaning: {@code false} is 0.0 and {@code true} is the default level.
     * New writes go to {@code sound.volume} alone - one setting, one key.
     */
    private static final String KEY_SOUND = "sound.enabled";
    private static final String KEY_TOOLBAR_VISIBLE = "toolbar.visible";
    private static final String KEY_STATS_SIZE = "stats.size";
    private static final String KEY_STORAGE_NOTICE = "notice.storage.shown";
    private static final String KEY_WELCOME_ENABLED = "welcome.enabled";

    /** Recent documents are stored one per indexed key, in display order. */
    private static final String KEY_RECENT_PREFIX = "recent.file.";
    private static final String KEY_LAST_FILE = "last.file";
    private static final String KEY_LAST_DIR = "last.directory";
    private static final String KEY_WINDOW_MAXIMIZED = "window.maximized";
    private static final String KEY_WINDOW_FULLSCREEN = "window.fullscreen";
    private static final String KEY_WINDOW_X = "window.x";
    private static final String KEY_WINDOW_Y = "window.y";
    private static final String KEY_WINDOW_WIDTH = "window.width";
    private static final String KEY_WINDOW_HEIGHT = "window.height";

    /** A run of digits: one numeric segment of a dotted version. */
    private static final Pattern VERSION_PART = Pattern.compile("\\d+");

    static final int DEFAULT_FONT_SIZE = org.mesutormanli.jtypewriter.ui.theme.FontSizeManager.DEFAULT_SIZE;

    private final Path file;
    private final Properties props = new Properties();
    private final AtomicBoolean saveFailed = new AtomicBoolean();

    public AppSettings(Path file) {
        this.file = file;
        readFromDisk();
        // Even with no file on disk the in-memory properties must carry the
        // current version, so the very first save writes a versioned file; a
        // migration is a shape decision, not something a later write triggers.
        migrate(props);
    }

    /** The user's standard settings file ({@link AppDirs#dataDir()}). */
    public static AppSettings load() {
        return new AppSettings(AppDirs.dataDir().resolve("settings.properties"));
    }

    // --- language ---------------------------------------------------------

    /** The stored UI language, empty when the user never chose one. */
    public Optional<Language> getLanguage() {
        return optionalEnum(KEY_LANGUAGE, Language.class);
    }

    public void setLanguage(Language language) {
        set(KEY_LANGUAGE, language.name());
    }

    // --- appearance -------------------------------------------------------

    public Theme getTheme() {
        return optionalEnum(KEY_THEME, Theme.class).orElse(Theme.DARK);
    }

    public void setTheme(Theme theme) {
        set(KEY_THEME, theme.name());
    }

    public TextColor getTextColor() {
        return optionalEnum(KEY_TEXT_COLOR, TextColor.class).orElse(TextColor.DEFAULT);
    }

    public void setTextColor(TextColor color) {
        set(KEY_TEXT_COLOR, color.name());
    }

    public int getFontSize() {
        return getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }

    public void setFontSize(int size) {
        set(KEY_FONT_SIZE, String.valueOf(size));
    }

    /**
     * The default level: the middle rung, quiet enough to live under writing
     * and loud enough to be heard. Also what an older {@code sound.enabled=true}
     * file maps to.
     */
    static final double DEFAULT_SOUND_VOLUME = 0.5;

    public double getSoundVolume() {
        var stored = optionalDouble(KEY_SOUND_VOLUME);
        if (stored.isPresent()) {
            return Math.max(0.0, Math.min(1.0, stored.get()));
        }
        return getBool(KEY_SOUND, true) ? DEFAULT_SOUND_VOLUME : 0.0;
    }

    public void setSoundVolume(double volume) {
        set(KEY_SOUND_VOLUME, String.valueOf(Math.max(0.0, Math.min(1.0, volume))));
    }

    /**
     * Whether the sound is audible, i.e. the persisted level is above zero.
     * The toolbar's On/Off dropdown is a shortcut over the same value; it is
     * not a second flag that could disagree with the level.
     */
    public boolean isSoundEnabled() {
        return getSoundVolume() > 0;
    }

    /**
     * The On/Off view of the level. Off writes 0.0; On restores an audible
     * level, keeping the current one if it already is audible.
     */
    public void setSoundEnabled(boolean enabled) {
        setSoundVolume(enabled ? (getSoundVolume() > 0 ? getSoundVolume() : DEFAULT_SOUND_VOLUME) : 0.0);
    }

    /** Whether the toolbar was showing at the last exit (default: showing). */
    public boolean isToolbarVisible() {
        return getBool(KEY_TOOLBAR_VISIBLE, true);
    }

    public void setToolbarVisible(boolean visible) {
        set(KEY_TOOLBAR_VISIBLE, String.valueOf(visible));
    }

    /** The stats bar text size from the last session (default: {@code MEDIUM}). */
    public StatsBarSize getStatsBarSize() {
        return optionalEnum(KEY_STATS_SIZE, StatsBarSize.class).orElse(StatsBarSize.MEDIUM);
    }

    public void setStatsBarSize(StatsBarSize size) {
        set(KEY_STATS_SIZE, size.name());
    }

    /**
     * Whether the user has already been told what saving costs on disk: the
     * document plus the backup generations the keeper rotates
     * ({@link org.mesutormanli.jtypewriter.service.StorageFootprint} owns the
     * numbers). The notice is informational, so repeating it every launch would
     * be nagging; not showing it at all leaves the user to discover the
     * footprint when a disk fills up mid-save.
     */
    public boolean isStorageNoticeShown() {
        return getBool(KEY_STORAGE_NOTICE, false);
    }

    public void setStorageNoticeShown(boolean shown) {
        set(KEY_STORAGE_NOTICE, String.valueOf(shown));
    }

    /**
     * Whether the welcome card is greeted with at launch (default: shown).
     *
     * <p>The card's own checkbox writes this, and the File menu's "Show
     * welcome" clears it again - one preference, two entry points, so the
     * opt-out can never become a door the user cannot reopen.</p>
     */
    public boolean isWelcomeCardEnabled() {
        return getBool(KEY_WELCOME_ENABLED, true);
    }

    public void setWelcomeCardEnabled(boolean enabled) {
        set(KEY_WELCOME_ENABLED, String.valueOf(enabled));
    }

    // --- recent documents -------------------------------------------------

    /**
     * The documents the welcome card offers, newest first: at most
     * {@link RecentFiles#MAX}, one row per document, and only those that still
     * exist. A document deleted outside the app leaves the list silently rather
     * than becoming a row that fails when clicked.
     *
     * <p>Gaps are allowed: an unusable entry (a hand-edited path, a value the
     * file system rejects) is skipped instead of ending the list, so one bad key
     * cannot hide the rest.</p>
     */
    public List<Path> getRecentFiles() {
        var stored = new ArrayList<Path>();
        for (var index = 0; index < RecentFiles.MAX; index++) {
            var raw = props.getProperty(KEY_RECENT_PREFIX + index);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                stored.add(Path.of(raw.trim()));
            } catch (InvalidPathException e) {
                LOG.log(Level.FINE, "ignoring unusable recent document " + raw);
            }
        }
        return RecentFiles.keepingOnly(RecentFiles.normalized(stored), Files::isRegularFile);
    }

    private void writeRecentFiles(List<Path> documents) {
        // Clear every slot first: the list only ever shrinks by dropping its
        // tail, and a stale key beyond the new size would come back as a
        // document the user did not open.
        for (var index = 0; index < RecentFiles.MAX; index++) {
            props.remove(KEY_RECENT_PREFIX + index);
        }
        for (var index = 0; index < documents.size() && index < RecentFiles.MAX; index++) {
            props.setProperty(KEY_RECENT_PREFIX + index,
                    documents.get(index).toAbsolutePath().toString());
        }
    }

    // --- session restore --------------------------------------------------

    /** The document that was open when the application last exited. */
    public Optional<Path> getLastFile() {
        return optionalPath(KEY_LAST_FILE);
    }

    /** Directory of the last opened/saved document, offered by file choosers. */
    public Optional<Path> getLastDirectory() {
        return optionalPath(KEY_LAST_DIR);
    }

    public void setLastDirectory(Path directory) {
        if (directory != null) {
            set(KEY_LAST_DIR, directory.toAbsolutePath().toString());
        }
    }

    /** Remembers a document that was opened or saved (both file and directory). */
    public void rememberFile(Path document) {
        if (document == null) {
            return;
        }
        var absolute = document.toAbsolutePath();
        props.setProperty(KEY_LAST_FILE, absolute.toString());
        var parent = absolute.getParent();
        if (parent != null) {
            props.setProperty(KEY_LAST_DIR, parent.toString());
        }
        // Opening or saving a document is the one event both the session restore
        // and the welcome card's recent list care about, so they are written
        // together - one save, one file state.
        writeRecentFiles(RecentFiles.withNewest(getRecentFiles(), absolute));
        save();
    }

    // --- window geometry --------------------------------------------------

    /** The window geometry from the last session, empty on first start. */
    public Optional<WindowState> getWindowState() {
        var width = optionalDouble(KEY_WINDOW_WIDTH);
        var height = optionalDouble(KEY_WINDOW_HEIGHT);
        if (width.isEmpty() || height.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new WindowState(
                getBool(KEY_WINDOW_MAXIMIZED, true),
                optionalDouble(KEY_WINDOW_X).orElse(0d),
                optionalDouble(KEY_WINDOW_Y).orElse(0d),
                width.get(),
                height.get()));
    }

    public void setWindowState(WindowState state) {
        props.setProperty(KEY_WINDOW_MAXIMIZED, String.valueOf(state.maximized()));
        props.setProperty(KEY_WINDOW_X, String.valueOf(state.x()));
        props.setProperty(KEY_WINDOW_Y, String.valueOf(state.y()));
        props.setProperty(KEY_WINDOW_WIDTH, String.valueOf(state.width()));
        props.setProperty(KEY_WINDOW_HEIGHT, String.valueOf(state.height()));
        save();
    }

    /**
     * Whether the window was in fullscreen at the last exit (default:
     * windowed). A separate key from the geometry because it is persisted the
     * moment fullscreen is toggled, not only when the window hides: a kill
     * after the toggle must not lose it, and while a fullscreen window is
     * showing, {@code setOnHidden} geometry is not the user's intent anyway.
     */
    public boolean isFullScreen() {
        return getBool(KEY_WINDOW_FULLSCREEN, false);
    }

    public void setFullScreen(boolean fullScreen) {
        set(KEY_WINDOW_FULLSCREEN, String.valueOf(fullScreen));
    }

    // --- storage ----------------------------------------------------------

    /**
     * Brings the loaded properties up to {@link #CURRENT_SCHEMA_VERSION} in
     * memory. Never writes: a read must leave the file exactly as it found it,
     * so an older build cannot rewrite a newer file just by looking at it.
     *
     * <p>There is exactly one step today, because no persisted key has ever
     * been renamed or removed: a file with no {@code schema.version} at all was
     * written before versioning existed, and it already has the shape this
     * build reads, so migrating it is stamping the version and nothing else. A
     * later change adds its step here, keyed to the versions it separates,
     * instead of special-casing the read path.</p>
     *
     * <p>A file stamped with a <em>higher</em> version is left untouched: this
     * build does not know what its shape promises, so it keeps the stored value
     * and every key it cannot interpret. Downgrading would make the next write
     * claim a version it did not produce, and stripping unknown keys would
     * destroy the newer build's settings.</p>
     */
    static void migrate(Properties props) {
        var stored = props.getProperty(KEY_SCHEMA_VERSION);
        if (versionSegments(stored).length == 0) {
            // No usable marker: an unversioned file, or a hand edit that
            // replaced it with something that is not a version at all.
            props.setProperty(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
            return;
        }
        var order = compareVersions(stored, CURRENT_SCHEMA_VERSION);
        if (order > 0) {
            LOG.log(Level.FINE, "settings file was written by " + stored.trim()
                    + ", newer than " + CURRENT_SCHEMA_VERSION + "; reading it as-is");
            return;
        }
        if (order < 0) {
            props.setProperty(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
        }
    }

    /**
     * Compares two dotted version strings by their numeric segments, missing
     * segments counting as zero, so {@code 1.0.3 < 1.1.0 < 99} and
     * {@code 1.0 == 1.0.0}. This is deliberately not a general SemVer
     * implementation: the only question asked here is "was this file written
     * by an older or a newer build than me", and a pre-release suffix such as
     * {@code -SNAPSHOT} is not part of a releasable version anyway
     * ({@code package.sh} rejects it).
     */
    static int compareVersions(String left, String right) {
        var first = versionSegments(left);
        var second = versionSegments(right);
        for (var index = 0; index < Math.max(first.length, second.length); index++) {
            var a = index < first.length ? first[index] : 0L;
            var b = index < second.length ? second[index] : 0L;
            if (a != b) {
                return Long.compare(a, b);
            }
        }
        return 0;
    }

    /**
     * The numeric segments of a version, empty when it contains none. Empty is
     * the "no usable marker" signal for {@link #migrate}: a blank value, a
     * comment pasted into the wrong place and a word are all the same thing
     * here - no build ever wrote a version like that.
     */
    private static long[] versionSegments(String version) {
        if (version == null) {
            return new long[0];
        }
        var matcher = VERSION_PART.matcher(version);
        var segments = new ArrayList<Long>();
        while (matcher.find()) {
            try {
                segments.add(Long.parseLong(matcher.group()));
            } catch (NumberFormatException overflow) {
                LOG.log(Level.FINE, "ignoring unreadable settings schema version " + version);
                return new long[0];
            }
        }
        var values = new long[segments.size()];
        for (var index = 0; index < segments.size(); index++) {
            values[index] = segments.get(index);
        }
        return values;
    }

    private void readFromDisk() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException | IllegalArgumentException e) {
            quarantineCorruptFile(e);
        }
    }

    /**
     * Moves a corrupt settings file aside so the following {@link #save()}
     * cannot silently destroy it. Never throws: if the file cannot be
     * preserved the app continues with defaults and says so in the log.
     */
    private void quarantineCorruptFile(Exception cause) {
        var target = quarantineTarget();
        try {
            // A move of an existing file, not a write: AtomicFiles creates the
            // destination from a buffer and is the wrong tool here.
            move(file, target);
        } catch (IOException moveFailure) {
            LOG.log(Level.WARNING, "cannot read settings from " + file + "; using defaults", cause);
            LOG.log(Level.WARNING,
                    "cannot preserve corrupt settings " + file
                            + " (tried to quarantine as " + target + "); the file is left in place",
                    moveFailure);
            return;
        }
        LOG.log(Level.WARNING,
                "cannot read settings from " + file + "; quarantined corrupt settings "
                        + file + " as " + target + "; using defaults",
                cause);
    }

    /**
     * {@code <file>.corrupt}, or {@code <file>.corrupt.<millis>} (incremented
     * until free) so an earlier quarantined copy is never overwritten.
     */
    private Path quarantineTarget() {
        var name = file.getFileName().toString();
        var plain = file.resolveSibling(name + ".corrupt");
        if (!Files.exists(plain, LinkOption.NOFOLLOW_LINKS)) {
            return plain;
        }
        var stamp = System.currentTimeMillis();
        Path candidate;
        do {
            candidate = file.resolveSibling(name + ".corrupt." + stamp);
            stamp++;
        } while (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS));
        return candidate;
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    /**
     * Flushes the whole file; a failure is logged and must never break the app.
     *
     * <p>{@link SecurityException} joins {@link IOException} here on purpose:
     * both mean "this write did not happen" to the user, and a security policy
     * that denies the file is exactly the case a preference write must not turn
     * into a crash on the FX thread. The failure is remembered so the UI can
     * say so once (see {@link #isSettingsWriteFailurePending()}); the value the
     * caller just set stays in memory, so the session keeps working.</p>
     */
    public void save() {
        try {
            var buffer = new ByteArrayOutputStream();
            props.store(buffer, "JTypeWriter settings");
            AtomicFiles.writeMoving(file, buffer.toByteArray());
        } catch (IOException | SecurityException e) {
            saveFailed.set(true);
            LOG.log(Level.WARNING, "cannot write settings to " + file, e);
        }
    }

    /** The file these preferences live in; also named in the write-failure notice. */
    public Path getFile() {
        return file;
    }

    /**
     * Whether a settings write has failed since the last
     * {@link #clearSettingsWriteFailure()}. Settings are written before the
     * window exists (theme, geometry, the last document), so the failure cannot
     * be shown where it happens: the composition root reads this flag once a
     * stage is available and clears it after the notice has been shown, exactly
     * like the storage-footprint notice. A failure that repeats raises it
     * again.
     */
    public boolean isSettingsWriteFailurePending() {
        return saveFailed.get();
    }

    /** Marks the pending write failure as reported; the next failure raises it again. */
    public void clearSettingsWriteFailure() {
        saveFailed.set(false);
    }

    private void set(String key, String value) {
        props.setProperty(key, value);
        save();
    }

    private String getString(String key, String fallback) {
        var value = props.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(getString(key, null));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * A finite {@code double} from the settings file, or empty.
     *
     * <p>{@link Double#parseDouble} happily accepts the literals {@code NaN},
     * {@code Infinity} and {@code -Infinity}, so a hand-edited settings file
     * could smuggle a non-finite value into {@code Stage.setWidth/setHeight}.
     * The window geometry guard in {@code MainStage} cannot screen that:
     * {@code Infinity > 0} is true and an infinite rectangle intersects every
     * screen. A non-finite value is therefore treated like a missing or
     * malformed one, and the caller falls back to its default.</p>
     */
    private Optional<Double> optionalDouble(String key) {
        var raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            var value = Double.parseDouble(raw.trim());
            return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private boolean getBool(String key, boolean fallback) {
        var value = getString(key, null);
        if (value == null) {
            return fallback;
        }
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> fallback;
        };
    }

    /**
     * A path setting is a convenience, never a data channel: a value that the
     * file system rejects (corrupt file, hand edit, a path copied from another
     * platform) is ignored instead of breaking whatever reads it - including
     * application startup.
     */
    private Optional<Path> optionalPath(String key) {
        var value = getString(key, null);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(value));
        } catch (InvalidPathException e) {
            LOG.log(Level.WARNING, "ignoring unusable path in setting " + key + "=" + value);
            return Optional.empty();
        }
    }

    private <E extends Enum<E>> Optional<E> optionalEnum(String key, Class<E> type) {
        var value = getString(key, null);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, value));
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "ignoring unknown setting " + key + "=" + value);
            return Optional.empty();
        }
    }
}