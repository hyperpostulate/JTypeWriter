package org.mesutormanli.jtypewriter;

import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 *   <li><b>Defensive reads:</b> unknown or invalid values (e.g. a renamed enum
 *       constant from an older version) fall back to the default instead of
 *       breaking startup.</li>
 * </ul>
 * Persisted: language, theme, text colour, font size, sound on/off, the last
 * document (for session restore) and its directory (for file choosers), and
 * the window geometry. YOLO mode is deliberately <em>not</em> persisted: a
 * user who left it on would come back to an editor that refuses to delete.
 */
public final class AppSettings {

    /** Window geometry as remembered at shutdown. */
    public record WindowState(boolean maximized, double x, double y, double width, double height) {
    }

    private static final Logger LOG = AppLogging.logger(AppSettings.class);

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";
    private static final String KEY_TEXT_COLOR = "text.color";
    private static final String KEY_FONT_SIZE = "font.size";
    private static final String KEY_SOUND = "sound.enabled";
    private static final String KEY_LAST_FILE = "last.file";
    private static final String KEY_LAST_DIR = "last.directory";
    private static final String KEY_WINDOW_MAXIMIZED = "window.maximized";
    private static final String KEY_WINDOW_X = "window.x";
    private static final String KEY_WINDOW_Y = "window.y";
    private static final String KEY_WINDOW_WIDTH = "window.width";
    private static final String KEY_WINDOW_HEIGHT = "window.height";

    static final int DEFAULT_FONT_SIZE = org.mesutormanli.jtypewriter.ui.theme.FontSizeManager.DEFAULT_SIZE;

    private final Path file;
    private final Properties props = new Properties();

    public AppSettings(Path file) {
        this.file = file;
        readFromDisk();
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

    public boolean isSoundEnabled() {
        return getBool(KEY_SOUND, true);
    }

    public void setSoundEnabled(boolean enabled) {
        set(KEY_SOUND, String.valueOf(enabled));
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

    // --- storage ----------------------------------------------------------

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

    /** Flushes the whole file; a failure is logged and must never break the app. */
    public void save() {
        try {
            var buffer = new ByteArrayOutputStream();
            props.store(buffer, "JTypeWriter settings");
            AtomicFiles.writeMoving(file, buffer.toByteArray());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "cannot write settings to " + file, e);
        }
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

    private Optional<Double> optionalDouble(String key) {
        var raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(raw.trim()));
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