package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PW-16: the About dialog must show a real version. The value lives in the
 * Maven {@code pom.properties}; the parsing seam is tested directly because
 * that resource is not guaranteed to be on the test classpath (the jar plugin
 * adds it at {@code package}, after {@code test}).
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
}
