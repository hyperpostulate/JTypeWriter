package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The application version shown in the About dialog.
 *
 * <p>The value is never hard-coded: it comes from the {@code pom.properties}
 * that Maven writes into {@code META-INF/maven/<groupId>/<artifactId>/} of
 * every artifact. Reading the same resource in a test run (where the file may
 * not exist yet) or in an unpacked IDE build must not fail, so every failure
 * degrades to {@link #FALLBACK} and only logs at {@code FINE}.</p>
 */
public final class AppVersion {

    /** Shown when the Maven metadata is absent: an unpacked build is not a release. */
    static final String FALLBACK = "dev";

    private static final String RESOURCE =
            "META-INF/maven/org.mesutormanli/jtypewriter/pom.properties";

    private static final Logger LOG = AppLogging.logger(AppVersion.class);

    private AppVersion() {
    }

    /** The version to display; never {@code null} or blank. */
    public static String get() {
        return Holder.VERSION;
    }

    /**
     * The pure seam: the trimmed, non-blank {@code version} value of a parsed
     * properties file, or empty. Kept separate from the resource lookup so a
     * headless test can cover the parsing without a classpath artifact.
     */
    static Optional<String> versionFrom(Properties properties) {
        if (properties == null) {
            return Optional.empty();
        }
        var value = properties.getProperty("version");
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    /**
     * Lazy holder: the resource is read on first use, not when the class is
     * loaded, so unrelated code paths pay nothing. The static initializer
     * cannot throw - every failure is logged and mapped to {@link #FALLBACK}.
     */
    private static final class Holder {

        private static final String VERSION = load();

        private static String load() {
            try (InputStream stream = AppVersion.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                if (stream == null) {
                    LOG.fine(() -> "no " + RESOURCE + " on the classpath, showing " + FALLBACK);
                    return FALLBACK;
                }
                var properties = new Properties();
                properties.load(stream);
                var version = versionFrom(properties);
                if (version.isEmpty()) {
                    LOG.fine(() -> "blank version in " + RESOURCE + ", showing " + FALLBACK);
                    return FALLBACK;
                }
                return version.get();
            } catch (IOException | RuntimeException e) {
                // A broken artifact must not take the About dialog down with it.
                LOG.log(Level.FINE, "could not read " + RESOURCE + ", showing " + FALLBACK, e);
                return FALLBACK;
            }
        }
    }
}
