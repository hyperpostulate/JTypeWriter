/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The product version: the value of {@code <version>} in {@code pom.xml}, which
 * is its one and only source.
 *
 * <p>The number reaches the program through a filtered resource,
 * {@code org/mesutormanli/jtypewriter/version.properties}, written by
 * {@code maven-resources-plugin} at {@code process-resources}. Maven's own
 * {@code pom.properties} descriptor is not enough for that job: it is written
 * by {@code maven-jar-plugin} at {@code package}, into the archive only, so a
 * program running from the class directory - {@code mvn javafx:run}, the IDE,
 * the test run - finds no descriptor at all. That is where "dev" came from: the
 * version existed in the jar and nowhere the user was actually running the app.
 * A filtered resource lands in {@code target/classes} before compilation and is
 * then packed into every archive, so all four paths read the same number.</p>
 *
 * <p>The descriptor is still read as a fallback, because a plain
 * {@code mvn package} jar is a legitimate way to run the app. When neither
 * resource is present the classpath was assembled without the build, and
 * {@link #UNSTAMPED} is returned: a version that is older than every release,
 * so the settings-schema migration treats such a file as needing a stamp.</p>
 */
public final class AppVersion {

    /**
     * Shown when no build metadata is on the classpath at all: an unpacked
     * class directory that Maven did not produce. Numerically older than every
     * release, which is exactly its meaning for schema migration. A Maven
     * build always has a real value - {@code AppVersionTest} fails if the
     * filtered resource is missing or disagrees with {@code pom.xml}.
     */
    static final String UNSTAMPED = "0.0.0";

    private static final String RESOURCE = "org/mesutormanli/jtypewriter/version.properties";

    private static final String MAVEN_DESCRIPTOR =
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
     * Lazy holder: the resources are read on first use, not when the class is
     * loaded, so unrelated code paths pay nothing. The static initializer
     * cannot throw - every failure is logged and mapped to the next source.
     */
    private static final class Holder {

        private static final String VERSION = load();

        private static String load() {
            for (var resource : new String[]{RESOURCE, MAVEN_DESCRIPTOR}) {
                var version = read(resource);
                if (version.isPresent()) {
                    return version.get();
                }
            }
            LOG.warning("no build metadata on the classpath (" + RESOURCE + ", "
                    + MAVEN_DESCRIPTOR + "); reporting the version as " + UNSTAMPED);
            return UNSTAMPED;
        }

        private static Optional<String> read(String resource) {
            try (InputStream stream = AppVersion.class.getClassLoader().getResourceAsStream(resource)) {
                if (stream == null) {
                    return Optional.empty();
                }
                var properties = new Properties();
                properties.load(stream);
                return versionFrom(properties);
            } catch (IOException | RuntimeException e) {
                // A broken artifact must not take the About card down with it.
                LOG.log(Level.FINE, "could not read " + resource, e);
                return Optional.empty();
            }
        }
    }
}
