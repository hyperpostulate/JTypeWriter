/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The About card must show a real version. The value lives in exactly one
 * place - {@code <version>} of {@code pom.xml} - and reaches the program as a
 * filtered resource; the parsing seam is tested directly because a resource
 * lookup failure must never be confused with a parsing failure.
 */
class AppVersionTest {

    @Test
    void readsAndTrimsTheVersionValue() {
        var properties = new Properties();
        properties.setProperty("version", " 1.2.3 ");

        assertEquals(Optional.of("1.2.3"), AppVersion.versionFrom(properties));
    }

    @Test
    void missingVersionIsEmpty() {
        var properties = new Properties();
        properties.setProperty("artifactId", "jtypewriter");

        assertTrue(AppVersion.versionFrom(properties).isEmpty());
    }

    @Test
    void blankVersionIsEmpty() {
        var properties = new Properties();
        properties.setProperty("version", "   ");

        assertTrue(AppVersion.versionFrom(properties).isEmpty());
    }

    /** Never null, never empty: a broken build still shows something in About. */
    @Test
    void getIsNeverBlank() {
        var version = AppVersion.get();

        assertNotNull(version);
        assertFalse(version.isBlank());
    }

    /**
     * The one-source rule, enforced rather than described.
     *
     * <p>Three things must agree, and any two of them agreeing is not enough:
     * the {@code <version>} in {@code pom.xml} (the source), the filtered
     * resource the running build reads (the carrier), and the schema version
     * stamped into the settings file (the second use of the same number). The
     * carrier is checked for existence as well, because the defect this test
     * exists for was an absent resource: it did not fail, it silently degraded
     * to "dev".</p>
     */
    @Test
    void theVersionComesFromThePomAndEveryReaderAgreesWithIt() throws IOException {
        var pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        var declared = Pattern.compile(
                        "<artifactId>jtypewriter</artifactId>\\s*<version>([^<]+)</version>")
                .matcher(pom);
        assertTrue(declared.find(), "pom.xml no longer declares the project version");

        var pomVersion = declared.group(1).trim();
        assertNotNull(getClass().getClassLoader()
                        .getResource("org/mesutormanli/jtypewriter/version.properties"),
                "the filtered version resource is missing: mvn-resources did not run, so"
                        + " the program has no version to report");
        assertEquals(pomVersion, AppVersion.get(),
                "the running version must be pom.xml's version, not a copy of it");
        assertEquals(pomVersion, AppSettings.CURRENT_SCHEMA_VERSION,
                "the settings schema version is the product version");
        assertFalse(pomVersion.equals(AppVersion.UNSTAMPED),
                "the build carries no version metadata");
    }
}
